# API & HTTP Route Audit: AlumniBridge

**Audit Date:** November 2026  
**Auditor:** Senior Product Architect, Technical Lead & QA Engineer  
**Architecture Note:** The application is architected as a Spring Web MVC server-side rendered application. Its HTTP endpoints serve HTML views via Thymeleaf and process form-encoded requests. The audit below inspects the entire HTTP route surface of the application.

---

## 1. Complete HTTP Route Inventory

| HTTP Method | Route | Controller | Method | Required Role | Return View / Action |
|---|---|---|---|---|---|
| `GET` | `/` | `AuthController` | `home()` | Anonymous | `index.html` |
| `GET` | `/register` | `AuthController` | `showRegister()` | Anonymous | `register.html` |
| `POST` | `/register` | `AuthController` | `register()` | Anonymous | `redirect:/login` |
| `GET` | `/login` | `AuthController` | `showLogin()` | Anonymous | `login.html` |
| `POST` | `/login` | `AuthController` | `loginUser()` | Anonymous | `redirect:/admin/dashboard` or `redirect:/dashboard` |
| `GET` | `/logout` | `AuthController` | `logout()` | Authenticated | `redirect:/login` |
| `GET` | `/dashboard` | `DashboardController` | `dashboard()` | Authenticated | `dashboard.html` |
| `GET` | `/profile` | `ProfileController` | `viewProfile()` | Authenticated | `profile.html` |
| `GET` | `/edit-profile` | `ProfileController` | `editProfile()` | Authenticated | `edit-profile.html` |
| `POST` | `/save-profile` | `ProfileController` | `saveProfile()` | Authenticated | `redirect:/profile` |
| `GET` | `/alumni` | `AlumniController` | `viewAlumni()` | Authenticated | `alumni-list.html` |
| `GET` | `/view-user/{id}` | `AlumniController` | `viewUser()` | Authenticated | `view-user.html` |
| `GET` | `/jobs` | `JobController` | `viewJobs()` | Authenticated | `jobs.html` |
| `GET` | `/post-job` | `JobController` | `createJobPage()` | `ALUMNI`, `ADMIN` | `post-job.html` |
| `POST` | `/save-job` | `JobController` | `saveJob()` | `ALUMNI`, `ADMIN` | `redirect:/jobs` |
| `GET` | `/apply/{jobId}` | `JobController` | `applyJob()` | Authenticated | `redirect:/jobs` |
| `GET` | `/my-applications` | `JobController` | `myApplications()` | Authenticated | `my-applications.html` |
| `GET` | `/events` | `EventController` | `viewEvents()` | Authenticated | `events.html` |
| `GET` | `/create-event` | `EventController` | `createEventPage()` | `ALUMNI`, `ADMIN` | `create-event.html` |
| `POST` | `/save-event` | `EventController` | `saveEvent()` | Unenforced | `redirect:/events` |
| `GET` | `/register-event/{id}` | `EventController` | `register()` | Authenticated | `redirect:/events` |
| `GET` | `/my-events` | `EventController` | `myEvents()` | Authenticated | `my-events.html` |
| `GET` | `/messages` | `MessageController` | `inbox()` | Authenticated | `messages.html` |
| `GET` | `/chat/{id}` | `MessageController` | `chat()` | Authenticated | `chat.html` |
| `POST` | `/send-message` | `MessageController` | `sendMessage()` | Authenticated | `redirect:/chat/{id}` |
| `GET` | `/admin/dashboard` | `AdminController` | `adminDashboard()` | `ADMIN` | `admin-dashboard.html` |
| `GET` | `/admin/users` | `AdminController` | `users()` | `ADMIN` | `admin-users.html` |
| `GET` | `/admin/toggle-block/{id}` | `AdminController` | `blockUser()` | `ADMIN` | `redirect:/admin/users` |
| `GET` | `/admin/delete-user/{id}` | `AdminController` | `deleteUser()` | `ADMIN` | `redirect:/admin/users` |
| `GET` | `/admin/jobs` | `AdminController` | `jobs()` | `ADMIN` | `admin-jobs.html` |
| `GET` | `/admin/delete-job/{id}` | `AdminController` | `deleteJob()` | `ADMIN` | `redirect:/admin/jobs` |
| `GET` | `/admin/events` | `AdminController` | `events()` | `ADMIN` | `admin-events.html` |
| `GET` | `/admin/delete-event/{id}` | `AdminController` | `deleteEvent()` | `ADMIN` | `redirect:/admin/events` |

---

## 2. Detailed Endpoint Inspection & Analysis

### 2.1 Authentication Endpoints (`AuthController.java`)

#### `POST /register`
- **Request Body / Form Parameters:** `name`, `email`, `password`, `role`.
- **Backend Flow:** Binds directly to `com.yaswanth.Alumni_Bridge.entity.User` via `@ModelAttribute` and calls `userService.registerUser(user)`.
- **Defects & Vulnerabilities:**
  - **No Input Validation:** Missing `@Valid` and `BindingResult`. Empty strings or invalid emails are accepted.
  - **Privilege Escalation:** Binds the `role` field from the HTTP request without filtering. Sending `role=ADMIN` creates an admin user.
  - **Plaintext Storage:** Password is saved unhashed into MySQL.
  - **No Duplicate Check:** Email is not checked for existence; creates duplicate records.

#### `POST /login`
- **Request Parameters:** `email` (String), `password` (String).
- **Backend Flow:** Invokes `userService.loginUser(email, password)`. If matched, puts user entity into `session.setAttribute("loggedUser", user)`.
- **Defects & Vulnerabilities:**
  - **Plaintext Equality Check:** `user.getPassword().equals(password)`.
  - **Crash on Duplicate Email:** If duplicate emails exist, `repo.findByEmail()` throws `IncorrectResultSizeDataAccessException` (HTTP 500).
  - **Blocked User Ignored:** `loginUser()` does not check `user.getBlocked()`.
  - **Brute Force Risk:** No rate limiting or lockout protection.

#### `GET /logout`
- **Backend Flow:** Calls `session.invalidate()` and redirects to `/login`.
- **Defects & Vulnerabilities:** Implemented as a GET request; susceptible to cross-site triggering via prefetching or embedded image tags. Should be POST.

---

### 2.2 Profile Endpoints (`ProfileController.java`)

#### `POST /save-profile`
- **Request Parameters:** `@ModelAttribute Profile profile`, `@RequestParam(value = "image", required = false) MultipartFile file`.
- **Backend Flow:**
  - Retrieves `loggedUser` from session.
  - If `file` is not empty, constructs `uploadDir = System.getProperty("user.dir") + "/uploads/"`.
  - Generates filename: `System.currentTimeMillis() + "_" + file.getOriginalFilename()`.
  - Replaces spaces with underscores and writes file using `file.transferTo(...)`.
  - Sets user profile image and saves user, then saves profile fields (skills, company, experience, bio).
- **Defects & Vulnerabilities:**
  - **Arbitrary File Upload:** Zero verification of file extension or content type. Any executable or malicious script can be uploaded.
  - **Path Traversal:** Does not sanitize `file.getOriginalFilename()`. Characters such as `../` in crafted filenames can write files outside the intended uploads directory.
  - **Session Desynchronization:** Session `loggedUser` entity is not refreshed after updating profile attributes.

---

### 2.3 Alumni Directory Endpoints (`AlumniController.java`)

#### `GET /alumni`
- **Query Parameters:** `keyword` (optional String), `role` (optional String).
- **Backend Flow:** Calls `userService.searchUsers(keyword, role, currentUser.getId())`.
- **Defects & Vulnerabilities:**
  - **In-Memory Streaming:** Filters out current user ID and ADMIN users using Java stream filters in memory rather than database SQL predicates.
  - **No Pagination:** Loads all matching rows into memory and renders them on one page.

#### `GET /view-user/{id}`
- **Path Variable:** `id` (Long).
- **Backend Flow:** Retrieves user via `userService.getUserById(id)` and puts into model.
- **Defects & Vulnerabilities:**
  - **Null Pointer Crash:** If `id` does not match an existing record, `user` is null. Thymeleaf template accesses `user.name` and throws an unhandled `NullPointerException` (HTTP 500).

---

### 2.4 Job Portal Endpoints (`JobController.java`)

#### `GET /jobs`
- **Backend Flow:** Calls `jobService.getAllJobs()`, adds list to model, and adds `appService` bean into model.
- **Defects & Vulnerabilities:**
  - **N+1 SQL Queries:** In `jobs.html`, template calls `@applicationService.alreadyApplied(session.loggedUser, job)` for every job, triggering separate queries for each card.
  - **Missing Job Description:** Job description is omitted from UI cards, and no detail endpoint exists.

#### `GET /apply/{jobId}`
- **Path Variable:** `jobId` (Long).
- **Backend Flow:** Checks `appService.alreadyApplied(user, job)`, creates `Application` object with status `APPLIED`, and saves.
- **Defects & Vulnerabilities:**
  - **State Mutation via GET:** Action triggers database inserts on a GET request.
  - **Unauthenticated Null Pointer Exception:** No null check on `session.getAttribute("loggedUser")`. Unauthenticated access crashes with NPE.
  - **Invalid Job ID Crash:** No check if `job` is null if an invalid ID is provided.
  - **Self-Application:** Does not prevent the job creator from applying.

---

### 2.5 Event Management Endpoints (`EventController.java`)

#### `POST /save-event`
- **Request Body / Form Parameters:** `title`, `description`, `location`, `date`.
- **Backend Flow:** Attaches `loggedUser` as creator and invokes `eventService.saveEvent(event)`.
- **Defects & Vulnerabilities:**
  - **Broken Authorization:** Unlike `GET /create-event` which checks for `ALUMNI` or `ADMIN`, `POST /save-event` performs NO role verification. A `STUDENT` user can post events directly.
  - **No Bean Validation:** Submitting empty titles or past dates succeeds without error.

#### `GET /register-event/{id}`
- **Path Variable:** `id` (Long).
- **Backend Flow:** Fetches all events via `eventService.getAllEvents()`, streams in memory to find matching ID, and calls `eventService.register(user, event)`.
- **Defects & Vulnerabilities:**
  - **Inefficient Querying:** Uses `getAllEvents().stream().filter(...)` instead of `eventRepo.findById(id)`.
  - **State Mutation via GET:** Modifies database state on HTTP GET.
  - **Missing Null Check:** If user is not logged in, throws NPE.

---

### 2.6 Messaging Endpoints (`MessageController.java`)

#### `GET /chat/{id}`
- **Path Variable:** `id` (Long).
- **Backend Flow:** Retrieves `other = userService.getUserById(id)`, calls `msgService.markAsSeen()`, and fetches conversation messages.
- **Defects & Vulnerabilities:**
  - **Unauthenticated NPE:** If `loggedUser` is null in session, calling `current.getId()` crashes immediately.
  - **Non-Existent User NPE:** If `{id}` does not match a user, `other` is null and crashes when marked as seen.

#### `POST /send-message`
- **Form Parameters:** `receiverId` (Long), `content` (String).
- **Backend Flow:** Calls `msgService.sendMessage(sender, receiver, content)` and redirects to `/chat/{receiverId}`.
- **Defects & Vulnerabilities:**
  - **No Content Validation:** Allows sending empty or whitespace-only strings.
  - **Self-Messaging:** Allows sending messages to oneself.
  - **No Blocked Account Guard:** Blocked users can continue sending messages.

---

### 2.7 Administrative Endpoints (`AdminController.java`)

#### `GET /admin/delete-user/{id}`, `GET /admin/delete-job/{id}`, `GET /admin/delete-event/{id}`
- **Defects & Vulnerabilities:**
  - **Database Constraint Crashes:** All 3 delete actions perform raw `deleteById(id)` without handling dependent child entities:
    - Deleting a user with applications, events, jobs, or messages throws `DataIntegrityViolationException`.
    - Deleting a job with applications throws `DataIntegrityViolationException`.
    - Deleting an event with registrations throws `DataIntegrityViolationException`.
  - **State Changes via GET:** All delete operations are exposed as GET links, vulnerable to accidental clicks, search crawler execution, and CSRF.
  - **Self-Deletion:** Admin can delete their own active account.

#### `GET /admin/toggle-block/{id}`
- **Defects & Vulnerabilities:**
  - Exposed as GET request.
  - Admin can block themselves.
  - The block status is never enforced at login or in request filters.

---

## 3. Recommended Endpoint Architecture Overhaul

1. **Adopt HTTP Method Semantics:**
   - Convert all deletion endpoints to `POST /admin/.../delete` or `DELETE` requests with CSRF tokens.
   - Convert `/apply/{id}` and `/register-event/{id}` to `POST` requests.
   - Convert `/logout` to `POST /logout`.
2. **Implement Global Exception Handling:**
   - Create a `@ControllerAdvice` to handle `NoSuchElementException`, `DataIntegrityViolationException`, and `MethodArgumentNotValidException`, displaying user-friendly error views instead of 500 white-label error pages.
3. **Transition to DTOs & Validation:**
   - Introduce request DTOs (`UserRegistrationRequest`, `JobPostRequest`, `EventCreateRequest`, `MessageSendRequest`) with Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`, `@Future`).
4. **Declarative Security:**
   - Replace manual session if-checks with Spring Security `@PreAuthorize("hasRole('ADMIN')")` annotations and standard filter chains.
