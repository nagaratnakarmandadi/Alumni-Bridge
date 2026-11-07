# Existing Features Audit: AlumniBridge

**Audit Date:** November 2026  
**Auditor:** Senior Product Architect, Technical Lead & QA Engineer  
**Classification Rules:**
- **FULLY IMPLEMENTED:** Complete end-to-end flow works seamlessly across View, Controller, Service, and Database.
- **PARTIALLY IMPLEMENTED:** Feature works only partially or lacks critical workflow components.
- **IMPLEMENTED BUT BUGGY:** Feature is functionally reachable but contains fatal bugs, security flaws, or edge-case failures.
- **FRONTEND ONLY:** Template exists without backend backing.
- **BACKEND ONLY:** Endpoint/Service exists without UI presentation.

---

## 1. Authentication & Session Module

### 1.1 User Registration
- **Description:** Allows visitors to sign up for an account choosing either the STUDENT or ALUMNI role.
- **User Role:** Anonymous / Guest.
- **Frontend Implementation:** `register.html` with Bootstrap form, field binding via `th:object="${user}"`, role select dropdown (`STUDENT`, `ALUMNI`).
- **Backend Implementation:** `AuthController.showRegister()` (GET `/register`), `AuthController.register()` (POST `/register`), calling `UserService.registerUser()`.
- **Related Routes:** `GET /register`, `POST /register`.
- **Entities Involved:** `User`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Plaintext Password: Saves raw password directly to the database without hashing.
  2. Privilege Escalation: Form submits `role` attribute directly; a malicious request can send `role=ADMIN` and gain complete administrative rights.
  3. No Duplicate Email Check: Does not verify if the email is already in use before saving.
  4. Validation Absent: No `@Valid` or `BindingResult` in controller; `th:errors` in template are non-functional.
- **Recommended Improvements:** Implement BCrypt password encoding, validate uniqueness of email, restrict registration roles strictly to `STUDENT` and `ALUMNI`, add standard Bean Validation (`@NotBlank`, `@Email`, `@Size`).

---

### 1.2 User Login
- **Description:** Authenticates registered users via email and password, establishing an HTTP session.
- **User Role:** All registered users (`STUDENT`, `ALUMNI`, `ADMIN`).
- **Frontend Implementation:** `login.html` with email and password inputs, show/hide password toggle script, error message alert (`th:if="${error}"`).
- **Backend Implementation:** `AuthController.showLogin()` (GET `/login`), `AuthController.loginUser()` (POST `/login`), calling `UserService.loginUser()`. Redirects to `/admin/dashboard` for `ADMIN` role and `/dashboard` for others.
- **Related Routes:** `GET /login`, `POST /login`.
- **Entities Involved:** `User`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Plaintext Comparison: `user.getPassword().equals(password)` in `UserService`.
  2. Blocked User Bypass: Does not check `user.getBlocked()`. Blocked accounts can log in without restriction.
  3. Crash on Duplicate Email: If duplicate emails exist, `repo.findByEmail()` throws `IncorrectResultSizeDataAccessException` (500 crash).
  4. Brute-Force Vulnerability: No rate limiting, account lockout, or CAPTCHA.
- **Recommended Improvements:** Integrate Spring Security authentication provider, check account status (`blocked == true`), sanitize inputs, implement rate-limiting.

---

### 1.3 Session Logout
- **Description:** Terminates current user session and redirects to login.
- **User Role:** Authenticated users.
- **Frontend Implementation:** Logout button in `navbar.html` and `admin-navbar.html`.
- **Backend Implementation:** `AuthController.logout()` (GET `/logout`), executing `session.invalidate()`.
- **Related Routes:** `GET /logout`.
- **Entities Involved:** N/A (Session only).
- **Status:** **PARTIALLY IMPLEMENTED**
- **Issues Identified:** Logout is executed via HTTP GET, which can be triggered unintentionally or via cross-site prefetching.
- **Recommended Improvements:** Convert logout to HTTP POST with CSRF token validation.

---

## 2. User Profile Module

### 2.1 View Own Profile
- **Description:** Renders personal profile details, including skills, company, experience, bio, and profile avatar.
- **User Role:** Authenticated users (`STUDENT`, `ALUMNI`, `ADMIN`).
- **Frontend Implementation:** `profile.html` displaying avatar, user full name, email, role badge, skills, company, experience, and bio.
- **Backend Implementation:** `ProfileController.viewProfile()` (GET `/profile`), invoking `ProfileService.getProfile(user)`.
- **Related Routes:** `GET /profile`.
- **Entities Involved:** `User`, `Profile`.
- **Status:** **FULLY IMPLEMENTED**
- **Issues Identified:** Minimal fields only. Missing standard academic information (Degree, Graduation Year, Major, Roll Number), phone number, and social links (LinkedIn, GitHub).
- **Recommended Improvements:** Expand `Profile` entity to support education and social links.

---

### 2.2 Edit Profile & Avatar Upload
- **Description:** Allows user to update skills, company, experience, bio, and upload an avatar image.
- **User Role:** Authenticated users.
- **Frontend Implementation:** `edit-profile.html` with multipart form, preview image JavaScript, file input, and text fields.
- **Backend Implementation:** `ProfileController.editProfile()` (GET `/edit-profile`), `ProfileController.saveProfile()` (POST `/save-profile`). Saves file to `./uploads/` directory on disk and saves profile record.
- **Related Routes:** `GET /edit-profile`, `POST /save-profile`.
- **Entities Involved:** `User`, `Profile`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Arbitrary File Upload: No file type/MIME verification. Any file (`.exe`, `.jsp`, `.html`, `.sh`) can be uploaded.
  2. Path Traversal: Uses `file.getOriginalFilename()` directly without sanitizing path traversal characters (`..`).
  3. Direct Static Exposure: `uploads/` folder is exposed at web root via `spring.web.resources.static-locations`.
  4. Session Stale Data: Modifies user entity in memory without re-synchronizing the HTTP session object.
- **Recommended Improvements:** Validate MIME types and file extensions (allow only JPEG/PNG/WebP), generate secure UUID file names, store uploads outside web root or serve through a protected controller, update session user.

---

## 3. Alumni Directory Module

### 3.1 Browse & Search Directory
- **Description:** Lists all registered users (excluding current user and admins) with keyword search and role filtering.
- **User Role:** Authenticated users.
- **Frontend Implementation:** `alumni-list.html` with search input (keyword), role dropdown, search/reset buttons, and user card grid.
- **Backend Implementation:** `AlumniController.viewAlumni()` (GET `/alumni`), calling `UserService.searchUsers()`.
- **Related Routes:** `GET /alumni`.
- **Entities Involved:** `User`.
- **Status:** **FULLY IMPLEMENTED**
- **Issues Identified:**
  1. In-memory Filtering: Performs filtering (`!u.getId().equals(...)` and `!u.getRole().equals("ADMIN")`) via Java Streams after pulling rows from the database.
  2. No Pagination: Renders every user matching the criteria in a single page load.
  3. Limited Search: Searches only `name`. Cannot search by skills, company, or graduation year.
  4. Card Display: Cards only show name, email, and role; avatars, companies, and skills are omitted.
- **Recommended Improvements:** Implement Spring Data JPA repository query with `Pageable` and database-level `WHERE` clauses matching name, company, and skills; display avatar and company on directory cards.

---

### 3.2 View Individual User Profile
- **Description:** Displays the public profile of an alumni or student member with a direct "Send Message" action.
- **User Role:** Authenticated users.
- **Frontend Implementation:** `view-user.html` displaying member avatar, name, email, role, skills, company, experience, bio, and a "Send Message" button.
- **Backend Implementation:** `AlumniController.viewUser()` (GET `/view-user/{id}`), fetching user via `UserService.getUserById(id)`.
- **Related Routes:** `GET /view-user/{id}`.
- **Entities Involved:** `User`, `Profile`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Null Pointer Crash: If an invalid `id` is provided in the URL, `getUserById(id)` returns `null`, causing an unhandled `NullPointerException` during Thymeleaf rendering.
  2. Inactive/Blocked User Exposure: Displays profiles of blocked users without restriction.
- **Recommended Improvements:** Return 404 or redirect with flash message when user is not found; suppress messaging button for blocked users.

---

## 4. Job Portal Module

### 4.1 Browse Jobs
- **Description:** Displays all active job opportunities posted on the platform.
- **User Role:** Authenticated users.
- **Frontend Implementation:** `jobs.html` displaying job title, company, location, poster name link, and contextual "Apply" / "Applied" button.
- **Backend Implementation:** `JobController.viewJobs()` (GET `/jobs`), calling `JobService.getAllJobs()`.
- **Related Routes:** `GET /jobs`.
- **Entities Involved:** `Job`, `Application`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Missing Description: The job cards in `jobs.html` do not render `job.description`, and there is no "View Job Details" page. Applicants must apply without seeing job requirements.
  2. Severe N+1 Query Problem: In the HTML loop, `@applicationService.alreadyApplied(session.loggedUser, job)` triggers an independent SQL query for every single job rendered.
  3. No Search / Filter / Pagination: Displays all jobs in database unconditionally.
- **Recommended Improvements:** Add a dedicated job details modal or page, preload user's applied job IDs into a `Set<Long>` in the controller to eliminate N+1 queries, add search and pagination.

---

### 4.2 Post Job
- **Description:** Enables Alumni and Administrators to create and publish a new job opening.
- **User Role:** `ALUMNI`, `ADMIN`.
- **Frontend Implementation:** `post-job.html` with form fields for title, company, location, and description.
- **Backend Implementation:** `JobController.createJobPage()` (GET `/post-job`), `JobController.saveJob()` (POST `/save-job`), calling `JobService.saveJob()`.
- **Related Routes:** `GET /post-job`, `POST /save-job`.
- **Entities Involved:** `Job`, `User`.
- **Status:** **FULLY IMPLEMENTED**
- **Issues Identified:**
  1. Missing Validation: Submitting empty fields succeeds without server-side validation.
  2. Missing Fields: No salary/stipend, job type (Full-time/Internship), deadline, or external application link.
  3. No Creator Management: Job poster cannot edit or delete their own posted jobs.
- **Recommended Improvements:** Add `@Valid` annotations, add job editing/deletion capabilities for the posting user, include deadline and employment type fields.

---

### 4.3 Apply for Job
- **Description:** Registers a user's application for a specific job listing.
- **User Role:** Authenticated users.
- **Frontend Implementation:** "Apply" link on job card in `jobs.html`.
- **Backend Implementation:** `JobController.applyJob()` (GET `/apply/{jobId}`), checking duplicate via `ApplicationService.alreadyApplied()` and persisting `Application`.
- **Related Routes:** `GET `/apply/{jobId}`.
- **Entities Involved:** `Application`, `Job`, `User`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. State Mutation via GET: Application submission is triggered via HTTP GET.
  2. Unauthenticated Null Pointer Crash: No null check on `session.getAttribute("loggedUser")`; navigating to `/apply/{id}` while logged out crashes with NPE.
  3. Invalid Job ID Crash: If `jobId` does not exist, passing `null` into `alreadyApplied` causes an NPE or database failure.
  4. Self-Application: Job posters can apply to their own job listings.
  5. No Resume/Profile Submission: No option to upload a resume or provide a cover note.
- **Recommended Improvements:** Convert to POST request, check authentication and job existence, prevent posters from applying to own jobs, support resume attachment.

---

### 4.4 My Applications
- **Description:** Lists all jobs the currently logged-in user has applied for.
- **User Role:** Authenticated users (`STUDENT`, `ALUMNI`).
- **Frontend Implementation:** `my-applications.html` with card list showing title, company, location, and status badge.
- **Backend Implementation:** `JobController.myApplications()` (GET `/my-applications`), querying `ApplicationService.getUserApplications(user)`.
- **Related Routes:** `GET /my-applications`.
- **Entities Involved:** `Application`, `Job`.
- **Status:** **FULLY IMPLEMENTED**
- **Issues Identified:** Status is permanently static as "APPLIED"; neither recruiters nor admins have an interface to update application statuses.
- **Recommended Improvements:** Build recruiter applicant review portal, allow applicants to withdraw applications.

---

## 5. Event Management Module

### 5.1 View Events
- **Description:** Lists all events scheduled on the platform.
- **User Role:** Authenticated users.
- **Frontend Implementation:** `events.html` showing event title, description, location, date, creator name link, and "Register" button.
- **Backend Implementation:** `EventController.viewEvents()` (GET `/events`), querying `EventService.getAllEvents()`.
- **Related Routes:** `GET /events`.
- **Entities Involved:** `Event`, `EventRegistration`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Architecture Anti-pattern: Controller injects Spring `@Service` into view model (`model.addAttribute("eventService", eventService)`), and template calls service directly during rendering.
  2. N+1 Query Problem: Template queries registration status individually per event card.
  3. No Past/Upcoming Separation: Shows expired events from the past alongside upcoming events.
- **Recommended Improvements:** Separate upcoming and past events via database query, compute registration state in controller, remove service injection into template.

---

### 5.2 Create Event
- **Description:** Enables Alumni and Administrators to organize and publish campus or community events.
- **User Role:** `ALUMNI`, `ADMIN`.
- **Frontend Implementation:** `create-event.html` with inputs for title, description, location, date.
- **Backend Implementation:** `EventController.createEventPage()` (GET `/create-event`), `EventController.saveEvent()` (POST `/save-event`), invoking `EventService.saveEvent()`.
- **Related Routes:** `GET /create-event`, `POST /save-event`.
- **Entities Involved:** `Event`, `User`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Broken Authorization in POST: `createEventPage` checks role, but `saveEvent` has NO role check! Any student can post directly to `/save-event`.
  2. Null Session Vulnerability: If session expires or user is null, `event.setCreatedBy(null)` or NPE occurs.
  3. Missing Time Field: Event has `LocalDate` date but no start/end time.
- **Recommended Improvements:** Add strict authorization check on POST `/save-event`, add event time and event mode (Virtual/Physical) with meeting link.

---

### 5.3 Register for Event
- **Description:** Registers current user for an event.
- **User Role:** Authenticated users.
- **Frontend Implementation:** "Register" button on event card.
- **Backend Implementation:** `EventController.register()` (GET `/register-event/{id}`), invoking `EventService.register()`.
- **Related Routes:** `GET /register-event/{id}`.
- **Entities Involved:** `EventRegistration`, `Event`, `User`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. State Mutation via GET: Modifies database through a GET link.
  2. In-Memory Filter Anti-pattern: Uses `eventService.getAllEvents().stream().filter(...)` instead of `findById(id)`.
  3. No Session Null Check: Causes NPE if user is not logged in.
  4. Cannot Cancel Registration: No unregister functionality.
- **Recommended Improvements:** Convert to POST, query event directly by ID, add cancellation support.

---

### 5.4 My Registered Events
- **Description:** Displays the list of events for which the current user is registered.
- **User Role:** Authenticated users.
- **Frontend Implementation:** `my-events.html` displaying card list of registered events.
- **Backend Implementation:** `EventController.myEvents()` (GET `/my-events`), querying `EventRegistrationRepository.findByUser()`.
- **Related Routes:** `GET /my-events`.
- **Entities Involved:** `EventRegistration`, `Event`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Missing Auth Check: Does not verify if `loggedUser` is null in session; crashes with NPE if accessed when session expires.
  2. Architectural Layer Violation: `EventController` bypasses `EventService` and directly injects `EventRegistrationRepository`.
- **Recommended Improvements:** Add session verification, route database operations strictly through `EventService`.

---

## 6. Messaging & Chat Module

### 6.1 Messages Inbox
- **Description:** Lists incoming messages for the current user.
- **User Role:** Authenticated users.
- **Frontend Implementation:** `messages.html` showing list of messages and sender names.
- **Backend Implementation:** `MessageController.inbox()` (GET `/messages`), calling `MessageService.getInbox(user)`.
- **Related Routes:** `GET /messages`.
- **Entities Involved:** `Message`, `User`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Non-Grouped Messages: Queries `findByReceiver(user)` and renders every single message as a separate row. If user A sent 20 messages, 20 identical rows appear.
  2. Missing Sent Conversations: If the current user sent a message to someone who has not replied, the conversation does not appear in their inbox at all.
- **Recommended Improvements:** Query distinct conversational partners and show the latest message preview with timestamp and unread badge.

---

### 6.2 1-on-1 Chat Conversation
- **Description:** Displays chat history between two users and provides an input form to send messages.
- **User Role:** Authenticated users.
- **Frontend Implementation:** `chat.html` with chat bubble layout, auto-scroll script, read receipt display, and message input form.
- **Backend Implementation:** `MessageController.chat()` (GET `/chat/{id}`), calling `msgService.markAsSeen()` and `msgService.getConversation()`.
- **Related Routes:** `GET /chat/{id}`.
- **Entities Involved:** `Message`, `User`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Unauthenticated Crash: No null check on session user. Calling `current.getId()` crashes with NPE if logged out.
  2. Invalid Target Crash: If `{id}` does not exist, `other` is null and triggers NPE.
  3. Synchronous Page Refresh: Every message sent forces a complete HTTP redirect and full HTML page reload.
- **Recommended Improvements:** Add session and recipient validation, introduce WebSocket (STOMP) or lightweight AJAX polling for real-time messaging.

---

### 6.3 Send Message
- **Description:** Submits a new direct message to another user.
- **User Role:** Authenticated users.
- **Frontend Implementation:** Form in `chat.html` posting to `/send-message`.
- **Backend Implementation:** `MessageController.sendMessage()` (POST `/send-message`), calling `MessageService.sendMessage()`.
- **Related Routes:** `POST /send-message`.
- **Entities Involved:** `Message`, `User`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Missing Auth & Null Checks: Does not verify if sender or receiver is null.
  2. Empty Messages: Does not validate message content (allows blank/whitespace messages).
  3. Self-Messaging: Users can send messages to themselves.
  4. Blocked Communication: Does not prevent blocked users from sending messages or receiving them.
- **Recommended Improvements:** Validate content (`@NotBlank`), verify neither participant is blocked, prevent self-messaging.

---

## 7. Dashboards Module

### 7.1 User Dashboard
- **Description:** Landing page after login showing summary statistics and quick action links tailored to user role.
- **User Role:** `STUDENT`, `ALUMNI`, `ADMIN`.
- **Frontend Implementation:** `dashboard.html` with welcome banner, 4 stat cards (Alumni, Jobs, Applications, Events), and role-based quick action cards.
- **Backend Implementation:** `DashboardController.dashboard()` (GET `/dashboard`), aggregating counts via services.
- **Related Routes:** `GET /dashboard`.
- **Entities Involved:** `User`, `Job`, `Event`, `Application`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. In-Memory Count Performance: Calls `userService.getAllUsers().size()`, `jobService.getAllJobs().size()`, etc., loading every database record into JVM memory just to calculate size.
  2. Inaccurate Metric: `alumniCount` counts all registered users (including students and admins), misleading the user.
  3. Template Typo: Trailing letter `F` at line 165 (`</html>F`).
- **Recommended Improvements:** Use JPA repository `.count()` and `.countByRole("ALUMNI")` queries; remove trailing syntax error.

---

### 7.2 Admin Dashboard
- **Description:** Dedicated administrative landing page.
- **User Role:** `ADMIN`.
- **Frontend Implementation:** `admin-dashboard.html` with 3 cards linking to Users, Jobs, and Events.
- **Backend Implementation:** `AdminController.adminDashboard()` (GET `/admin/dashboard`), verifies admin role.
- **Related Routes:** `GET /admin/dashboard`.
- **Entities Involved:** None passed to model.
- **Status:** **PARTIALLY IMPLEMENTED**
- **Issues Identified:**
  1. Zero Analytics: Claims "Dashboard Analytics" in documentation, but passes zero data to the view.
  2. Missing Application Moderation: No link or view for managing job applications.
- **Recommended Improvements:** Inject platform analytics (total users, active postings, applications submitted, registration growth), add application oversight.

---

## 8. Administrative Oversight Module

### 8.1 Admin User Management
- **Description:** Table listing all users with actions to toggle block status and delete accounts.
- **User Role:** `ADMIN`.
- **Frontend Implementation:** `admin-users.html` table displaying Name, Email, Role, Status badge, Block/Unblock link, Delete link.
- **Backend Implementation:** `AdminController.users()`, `AdminController.blockUser()` (GET `/admin/toggle-block/{id}`), `AdminController.deleteUser()` (GET `/admin/delete-user/{id}`).
- **Related Routes:** `GET /admin/users`, `GET /admin/toggle-block/{id}`, `GET /admin/delete-user/{id}`.
- **Entities Involved:** `User`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Critical Foreign Key Crash on Delete: Deleting a user with dependent records (profile, jobs, events, applications, messages) throws `DataIntegrityViolationException` (500 crash) because foreign key cascades are missing.
  2. CSRF Vulnerability: State-mutating actions (block/delete) use GET links without confirmation or CSRF tokens.
  3. Self-Deactivation: Admins can delete or block their own currently logged-in account.
  4. Non-Functional Block: `UserService.loginUser()` never checks `blocked`, making blocking useless.
- **Recommended Improvements:** Convert delete/block to POST requests with confirmation modals, implement soft deletes or cascading deletes, prevent admin self-deletion, enforce block check at login and session filter.

---

### 8.2 Admin Job Moderation
- **Description:** Lists all posted jobs with administrative delete capability.
- **User Role:** `ADMIN`.
- **Frontend Implementation:** `admin-jobs.html` with card grid and Delete button.
- **Backend Implementation:** `AdminController.jobs()`, `AdminController.deleteJob()` (GET `/admin/delete-job/{id}`).
- **Related Routes:** `GET /admin/jobs`, `GET /admin/delete-job/{id}`.
- **Entities Involved:** `Job`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Foreign Key Constraint Failure: Deleting a job that has applications fails with SQL constraint error.
  2. Potential Template Crash: If `job.postedBy` is null, rendering `job.postedBy.name` crashes the page.
  3. State change via GET.
- **Recommended Improvements:** Handle cascading deletion or mark jobs as closed; convert delete to POST; guard against null job posters.

---

### 8.3 Admin Event Moderation
- **Description:** Lists all events with administrative delete capability.
- **User Role:** `ADMIN`.
- **Frontend Implementation:** `admin-events.html` with card grid and Delete button.
- **Backend Implementation:** `AdminController.events()`, `AdminController.deleteEvent()` (GET `/admin/delete-event/{id}`).
- **Related Routes:** `GET /admin/events`, `GET /admin/delete-event/{id}`.
- **Entities Involved:** `Event`.
- **Status:** **IMPLEMENTED BUT BUGGY**
- **Issues Identified:**
  1. Foreign Key Constraint Failure: Deleting an event that has registrations fails with SQL constraint error.
  2. Template Crash on Null Creator: `event.createdBy.name` crashes if creator was deleted.
  3. State change via GET.
- **Recommended Improvements:** Cascade delete registrations or archive event; convert delete to POST; handle null creator safely.
