# Bugs & Technical Issues Inventory: AlumniBridge

**Audit Date:** November 2026  
**Auditor:** Senior Product Architect, Technical Lead & QA Engineer  

---

## 1. Summary Defect Registry

| Bug ID | Module | Severity | Summary | Location |
|---|---|---|---|---|
| **BUG-001** | Admin / DB | **CRITICAL** | Foreign key crash when deleting users with dependencies | `AdminController.java:54`, `UserService.java:69` |
| **BUG-002** | Admin / DB | **CRITICAL** | Foreign key crash when deleting jobs with applications | `AdminController.java:82`, `JobService.java:30` |
| **BUG-003** | Admin / DB | **CRITICAL** | Foreign key crash when deleting events with registrations | `AdminController.java:101`, `EventService.java:46` |
| **BUG-004** | Auth | **CRITICAL** | Duplicate email registrations crash subsequent logins | `UserService.java:19`, `User.java:16` |
| **BUG-005** | Security / Auth | **CRITICAL** | Blocked users can log in and perform all actions | `UserService.java:24-31` |
| **BUG-006** | Security / Auth | **CRITICAL** | Privilege escalation: Any user can register as ADMIN | `AuthController.java:28-31` |
| **BUG-007** | Profile / Security | **CRITICAL** | Unrestricted file upload & path traversal in avatar upload | `ProfileController.java:78-97` |
| **BUG-008** | Job Portal | **HIGH** | Job description is never rendered to users | `jobs.html:47-83` |
| **BUG-009** | Job Portal | **HIGH** | Null pointer crash when applying while unauthenticated | `JobController.java:75-95` |
| **BUG-010** | Event Portal | **HIGH** | Broken authorization: Students can create events via POST | `EventController.java:62-70` |
| **BUG-011** | Event Portal | **HIGH** | Null pointer crash when registering while unauthenticated | `EventController.java:74-82` |
| **BUG-012** | Event Portal | **HIGH** | Null pointer crash in My Events when unauthenticated | `EventController.java:85-94` |
| **BUG-013** | Messaging | **HIGH** | Null pointer crash in Chat when unauthenticated | `MessageController.java:43-58` |
| **BUG-014** | Messaging | **HIGH** | Null pointer crash when viewing non-existent user chat | `MessageController.java:47-50` |
| **BUG-015** | Directory | **HIGH** | Null pointer crash when viewing non-existent user profile | `AlumniController.java:43-55` |
| **BUG-016** | Performance | **HIGH** | Extreme N+1 query loop in Job Portal template | `jobs.html:71,77` |
| **BUG-017** | Performance | **HIGH** | Extreme N+1 query loop in Events template | `events.html:77,83` |
| **BUG-018** | Dashboard | **MEDIUM** | Misleading alumni count in Dashboard | `DashboardController.java:41` |
| **BUG-019** | Architecture | **MEDIUM** | Misplaced `@Service` on JPA repository interfaces | `EventRepository.java:10`, `EventRegistrationRepository.java:12` |
| **BUG-020** | UI / Template | **LOW** | Syntax typo `</html>F` in user dashboard | `dashboard.html:165` |

---

## 2. In-Depth Defect Breakdown

### BUG-001: Foreign Key Crash on User Deletion (CRITICAL)
- **Module:** Administrative Oversight / Database
- **Severity:** CRITICAL
- **Location:** `AdminController.java:54` invoking `UserService.deleteUser(id)` (`repo.deleteById(id)`).
- **Root Cause:** In the database schema, `User` is a foreign key referenced by `job.posted_by`, `application.user_id`, `event.created_by_id`, `event_registration.user_id`, `message.sender_id`, and `message.receiver_id`. None of these relationships have `ON DELETE CASCADE` configured in JPA or DDL.
- **Trigger Condition:** Administrator clicks "Delete" on any user who has created an event, applied for a job, posted a job, or sent/received a message.
- **Observed Behavior:** Hibernate executes `DELETE FROM users WHERE id = ?`. MySQL rejects the query with error: `Cannot delete or update a parent row: a foreign key constraint fails`. An unhandled `DataIntegrityViolationException` triggers a 500 white-label error page.
- **Recommended Fix:** Implement a soft-delete mechanism (e.g., `user.setDeleted(true)` and `@SQLRestriction("deleted = false")`), or programmatically delete or disassociate child records in a `@Transactional` service method before deleting the user.

---

### BUG-002: Foreign Key Crash on Job Deletion (CRITICAL)
- **Module:** Administrative Oversight / Job Portal
- **Severity:** CRITICAL
- **Location:** `AdminController.java:82` invoking `JobService.deleteJob(id)` (`repo.deleteById(id)`).
- **Root Cause:** `Application` entity contains `@ManyToOne @JoinColumn(name = "job_id") private Job job;` with no cascade rule.
- **Trigger Condition:** Administrator deletes a job that has one or more candidate applications.
- **Observed Behavior:** Unhandled `DataIntegrityViolationException` (HTTP 500) due to MySQL foreign key constraint failure.
- **Recommended Fix:** In `Job.java`, define `cascade = CascadeType.REMOVE` on an `applications` list, or in `JobService.deleteJob(id)` delete all related applications prior to deleting the job.

---

### BUG-003: Foreign Key Crash on Event Deletion (CRITICAL)
- **Module:** Administrative Oversight / Event Portal
- **Severity:** CRITICAL
- **Location:** `AdminController.java:101` invoking `EventService.deleteEvent(id)` (`eventRepo.deleteById(id)`).
- **Root Cause:** `EventRegistration` holds a foreign key to `Event` with no cascade delete behavior.
- **Trigger Condition:** Administrator deletes an event that has at least one registered attendee.
- **Observed Behavior:** Unhandled `DataIntegrityViolationException` (HTTP 500).
- **Recommended Fix:** Delete associated `EventRegistration` records in `EventService` within a `@Transactional` block before calling `eventRepo.deleteById(id)`.

---

### BUG-004: Duplicate Email Registrations Crash Future Logins (CRITICAL)
- **Module:** Authentication & Database
- **Severity:** CRITICAL
- **Location:** `UserService.java:19`, `UserRepository.java:13`, `User.java:16`.
- **Root Cause:**
  1. `User.java` lacks `@Column(unique = true)` on `email`.
  2. `UserService.registerUser()` does not query `repo.findByEmail()` before persisting.
  3. `UserRepository.findByEmail(String email)` returns a single `User` object.
- **Trigger Condition:** Two or more users register with the identical email address.
- **Observed Behavior:** When either user attempts to log in, Spring Data JPA executes `SELECT * FROM users WHERE email = ?`. Because multiple records are returned, it throws `IncorrectResultSizeDataAccessException` (expected 1, got 2), resulting in an immediate 500 error and permanently locking out those email accounts.
- **Recommended Fix:**
  1. Add unique constraint annotation `@Column(unique = true, nullable = false)` to `User.email`.
  2. In `UserService.registerUser()`, throw a custom `DuplicateResourceException` if email exists.
  3. In `AuthController.register()`, handle the error and present an inline message to the user.

---

### BUG-005: Blocked Accounts Can Log In (CRITICAL)
- **Module:** Security / Authentication
- **Severity:** CRITICAL
- **Location:** `UserService.java:24-31`.
- **Root Cause:** `UserService.loginUser()` only verifies password equality:
  ```java
  public User loginUser(String email, String password) {
      User user = repo.findByEmail(email);
      if (user != null && user.getPassword().equals(password)) {
          return user;
      }
      return null;
  }
  ```
  `user.getBlocked()` is completely ignored.
- **Trigger Condition:** An administrator blocks a problematic account from `/admin/users`, and that user subsequently attempts to log in.
- **Observed Behavior:** The blocked user logs in successfully and retains full operational access.
- **Recommended Fix:** Reject login if `user.getBlocked()` is `true`, returning an appropriate error message (`"Account is deactivated"`).

---

### BUG-006: Privilege Escalation to ADMIN on Registration (CRITICAL)
- **Module:** Security / Authentication
- **Severity:** CRITICAL
- **Location:** `AuthController.java:28-31`.
- **Root Cause:** The controller directly binds HTTP form parameters to the entity:
  ```java
  @PostMapping("/register")
  public String register(@ModelAttribute User user) {
      service.registerUser(user);
      return "redirect:/login";
  }
  ```
- **Trigger Condition:** A user sends an HTTP POST to `/register` with `name=Attacker&email=hacker@test.com&password=pass&role=ADMIN`.
- **Observed Behavior:** The user is created in MySQL with `role = 'ADMIN'`. They can now log in at `/login` and are immediately redirected to `/admin/dashboard` with full administrative privileges.
- **Recommended Fix:** Ignore any submitted `role` field from public registration, or enforce that the role must strictly be `STUDENT` or `ALUMNI`.

---

### BUG-007: Unrestricted File Upload & Path Traversal (CRITICAL)
- **Module:** Profile Management / Security
- **Severity:** CRITICAL
- **Location:** `ProfileController.java:78-97`.
- **Root Cause:**
  1. No check on file extension or MIME type.
  2. The original filename is used directly: `System.currentTimeMillis() + "_" + file.getOriginalFilename()`.
  3. `spring.web.resources.static-locations` exposes `./uploads/` directly at the web root.
- **Trigger Condition:** User uploads `exploit.html` or `shell.jsp` containing script tags or payloads.
- **Observed Behavior:** The file is saved to disk and directly reachable via `http://localhost:8127/{filename}`.
- **Recommended Fix:** Whitelist `.jpg`, `.png`, `.webp`, validate file magic bytes, generate randomized UUID filenames, and serve media through a controlled endpoint.

---

### BUG-008: Job Description Is Never Rendered (HIGH)
- **Module:** Job Portal / UI
- **Severity:** HIGH
- **Location:** `jobs.html:47-83`.
- **Root Cause:** In `jobs.html`, cards display `job.title`, `job.company`, and `job.location`, but completely omit `${job.description}`. Furthermore, no `/jobs/{id}` details endpoint or modal exists in the application.
- **Trigger Condition:** Any user browsing the Job Portal.
- **Observed Behavior:** Applicants see only the job title and company, and are forced to apply without ever knowing the job description, qualifications, or requirements.
- **Recommended Fix:** Display the job description in an expandable card accordion or provide a dedicated `/jobs/{id}` view template.

---

### BUG-009: Null Pointer Crash When Applying While Unauthenticated (HIGH)
- **Module:** Job Portal
- **Severity:** HIGH
- **Location:** `JobController.java:75-95`.
- **Root Cause:** In `applyJob(@PathVariable Long jobId, HttpSession session)`:
  ```java
  User user = (User) session.getAttribute("loggedUser");
  Job job = jobService.getJobById(jobId);
  if (appService.alreadyApplied(user, job)) { ... }
  ```
  If `user` is null (logged out or session expired), `appService.alreadyApplied(null, job)` is called.
- **Trigger Condition:** Unauthenticated visitor clicks an apply link or navigates directly to `/apply/1`.
- **Observed Behavior:** Unhandled `NullPointerException` (HTTP 500).
- **Recommended Fix:** Add session check: `if (user == null) return "redirect:/login";`.

---

### BUG-010: Broken Authorization on Event Creation (HIGH)
- **Module:** Event Management
- **Severity:** HIGH
- **Location:** `EventController.java:62-70`.
- **Root Cause:** `GET /create-event` verifies user role (`ALUMNI` or `ADMIN`), but `POST /save-event` does not perform any role check.
- **Trigger Condition:** A user with role `STUDENT` sends a direct `POST` to `/save-event`.
- **Observed Behavior:** The student's event is persisted and published globally to all users.
- **Recommended Fix:** Add role verification to `saveEvent()`:
  ```java
  if (user == null || (!"ALUMNI".equals(user.getRole()) && !"ADMIN".equals(user.getRole()))) {
      return "redirect:/events";
  }
  ```

---

### BUG-011: Null Pointer Crash on Event Registration When Unauthenticated (HIGH)
- **Module:** Event Management
- **Severity:** HIGH
- **Location:** `EventController.java:74-82`.
- **Root Cause:** `register(@PathVariable Long id, HttpSession session)` does not check if `session.getAttribute("loggedUser")` is null before passing `user` to `eventService.register(user, event)`.
- **Trigger Condition:** An unauthenticated visitor navigates to `/register-event/1`.
- **Observed Behavior:** Throws unhandled `NullPointerException` (HTTP 500).
- **Recommended Fix:** Guard with authentication check and redirect to `/login`.

---

### BUG-012: Null Pointer Crash in My Events When Unauthenticated (HIGH)
- **Module:** Event Management
- **Severity:** HIGH
- **Location:** `EventController.java:85-94`.
- **Root Cause:** `myEvents()` calls `regRepo.findByUser(user)` without verifying if `user` is null.
- **Trigger Condition:** Unauthenticated visitor navigates to `/my-events`.
- **Observed Behavior:** Queries `findByUser(null)`, throwing a 500 error or returning empty registrations unexpectedly.
- **Recommended Fix:** Redirect to `/login` if `loggedUser` is null.

---

### BUG-013 & BUG-014: Null Pointer Crashes in Chat (HIGH)
- **Module:** Messaging
- **Severity:** HIGH
- **Location:** `MessageController.java:44-58`.
- **Root Cause:**
  1. `chat(@PathVariable Long id, ...)` does not check if `current` (from session) is null before executing `msgService.markAsSeen(current, other)`.
  2. If `{id}` is not found, `other` is null, causing `current.getId()` or `other.getId()` to throw NPE.
- **Trigger Condition:**
  - Visiting `/chat/1` while logged out.
  - Visiting `/chat/99999` with a non-existent user ID.
- **Observed Behavior:** Unhandled `NullPointerException` (HTTP 500).
- **Recommended Fix:** Check both `current` and `other` for null and redirect to `/login` or `/alumni` with error message.

---

### BUG-015: Null Pointer Crash When Viewing Non-Existent User Profile (HIGH)
- **Module:** Alumni Directory
- **Severity:** HIGH
- **Location:** `AlumniController.java:43-55`.
- **Root Cause:** In `viewUser(@PathVariable Long id, ...)`, if `userService.getUserById(id)` returns `null`, the controller passes `model.addAttribute("user", null)`. `view-user.html` then attempts to render `${user.name}`.
- **Trigger Condition:** Visiting `/view-user/999999`.
- **Observed Behavior:** Template rendering fails with `PropertyNotFoundException` / `NullPointerException` (HTTP 500).
- **Recommended Fix:** If user is null, redirect to `/alumni` with an alert: "User not found".

---

### BUG-016 & BUG-017: Extreme N+1 Query Loop in Templates (HIGH)
- **Module:** Job Portal & Event Management
- **Severity:** HIGH
- **Location:** `jobs.html:71,77`, `events.html:77,83`.
- **Root Cause:** Thymeleaf templates invoke Spring `@Service` methods directly inside the HTML `th:each` loop (`@applicationService.alreadyApplied(...)` and `eventService.isRegistered(...)`).
- **Trigger Condition:** Loading `/jobs` or `/events` when there are numerous jobs or events in the database.
- **Observed Behavior:** Executes 1 database query per card. For 100 items, 101 database queries are fired in sequence, generating unnecessary database latency and I/O load.
- **Recommended Fix:** Fetch the user's applied job IDs and registered event IDs in a single batch query in the controller, pass a `Set<Long>` into the model, and check membership in memory in the template.

---

### BUG-018: Inaccurate Alumni Count on User Dashboard (MEDIUM)
- **Module:** User Dashboard
- **Severity:** MEDIUM
- **Location:** `DashboardController.java:41`.
- **Root Cause:** Controller sets `model.addAttribute("alumniCount", userService.getAllUsers().size())`.
- **Observed Behavior:** The card labeled "Alumni" displays the count of all registered users (including students and administrators).
- **Recommended Fix:** Query specifically for alumni: `userRepo.countByRole("ALUMNI")`.

---

### BUG-019: Misplaced `@Service` on JPA Repositories (MEDIUM)
- **Module:** Repository Layer
- **Severity:** MEDIUM
- **Location:** `EventRepository.java:10`, `EventRegistrationRepository.java:12`.
- **Root Cause:** Interface declarations are annotated with `@Service` instead of `@Repository`.
- **Observed Behavior:** Misleading architectural semantics; Spring registers the interfaces as both service and repository beans.
- **Recommended Fix:** Remove `@Service` annotation from repository interfaces.

---

### BUG-020: Stray Character Syntax Typo in Dashboard Template (LOW)
- **Module:** User Dashboard / Template
- **Severity:** LOW
- **Location:** `dashboard.html:165`.
- **Root Cause:** The template terminates with `</html>F`.
- **Observed Behavior:** Stray letter `F` is output at the very bottom of the HTML page source.
- **Recommended Fix:** Remove trailing letter `F` from `dashboard.html`.
