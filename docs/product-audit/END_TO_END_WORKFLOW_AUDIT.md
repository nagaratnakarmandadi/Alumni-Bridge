# End-to-End Workflow Audit: AlumniBridge

**Audit Date:** November 2026  
**Auditor:** Senior Product Architect, Technical Lead & QA Engineer  
**Methodology:** Exhaustive static code path tracing, control flow analysis, and data lifecycle verification. Workflows requiring live MySQL execution are marked as **[NEEDS RUNTIME VERIFICATION]**.

---

## 1. Authentication & Onboarding Workflows

### 1.1 User Registration Workflow
- **User Action:** Visitor fills out `register.html` (Full Name, Email, Password, Role) and clicks "Sign Up".
- **Frontend Behavior:** Client-side HTML5 `required` attributes validate presence of values. Form submits `POST /register` with `application/x-www-form-urlencoded`.
- **API Request:** `POST /register` containing `name`, `email`, `password`, `role`.
- **Backend Processing:** `AuthController.register(@ModelAttribute User user)` delegates directly to `UserService.registerUser(user)`.
- **Database Operations:** Hibernate generates `INSERT INTO users (name, email, password, role, profile_image, blocked) VALUES (?, ?, ?, ?, NULL, 0)`.
- **API Response:** HTTP 302 Redirect to `/login`.
- **Frontend Response:** Browser navigates to `/login`.
- **Error Handling & Broken Connections:**
  - **Broken:** If an existing user has the same email, Hibernate executes the insert anyway because `User.email` lacks a unique constraint. If a database-level unique index is manually added, Hibernate throws an unhandled `DataIntegrityViolationException`, showing a 500 error instead of a friendly form validation message.
  - **Broken:** If the visitor crafts the request with `role=ADMIN`, the user is created as an administrator.
  - **Verification Status:** **VERIFIED BROKEN VIA CODE TRACE**

---

### 1.2 User Login Workflow
- **User Action:** User enters email and password into `login.html` and clicks "Sign In".
- **Frontend Behavior:** Submits `POST /login` with `email` and `password`.
- **API Request:** `POST /login`.
- **Backend Processing:** `AuthController.loginUser()` calls `UserService.loginUser(email, password)`.
- **Database Operations:** Hibernate executes `SELECT * FROM users WHERE email = ?`.
- **API Response:**
  - If authenticated: HTTP 302 Redirect to `/admin/dashboard` (for `ADMIN`) or `/dashboard` (for `STUDENT`/`ALUMNI`).
  - If failed: Re-renders `login.html` with model attribute `error="Invalid credentials"`.
- **Frontend Response:** Dashboard loads with personalized greeting or red error alert appears on login card.
- **Error Handling & Broken Connections:**
  - **Broken:** Blocked users can log in because `user.getBlocked()` is never checked.
  - **Fatal Defect:** If duplicate emails exist, `findByEmail` throws `IncorrectResultSizeDataAccessException` (HTTP 500 white-label error).
  - **Verification Status:** **VERIFIED BROKEN VIA CODE TRACE**

---

### 1.3 User Logout Workflow
- **User Action:** User clicks "Logout" in navbar.
- **Frontend Behavior:** Navigates via `href="/logout"`.
- **API Request:** `GET /logout`.
- **Backend Processing:** `AuthController.logout()` calls `session.invalidate()`.
- **Database Operations:** None.
- **API Response:** HTTP 302 Redirect to `/login`.
- **Frontend Response:** Login page displays.
- **Error Handling & Broken Connections:**
  - **Broken Design:** Implemented as HTTP GET; can be triggered by link prefetching or third-party web crawlers.
  - **Verification Status:** **PARTIALLY VERIFIED (Requires POST conversion)**

---

## 2. Profile Management Workflows

### 2.1 Edit Profile & Avatar Upload Workflow
- **User Action:** User navigates to `/edit-profile`, selects an image file, inputs skills, company, experience, bio, and clicks "Save Profile".
- **Frontend Behavior:** Form submits `POST /save-profile` with `multipart/form-data`.
- **API Request:** `POST /save-profile`.
- **Backend Processing:** `ProfileController.saveProfile()` saves image to disk (`./uploads/{timestamp}_{filename}`) and updates `Profile` in database.
- **Database Operations:**
  1. `UPDATE users SET profile_image = ? WHERE id = ?`
  2. `SELECT * FROM profile WHERE user_id = ?`
  3. `INSERT INTO profile (skills, company, experience, bio, user_id) VALUES (...)` (or `UPDATE profile ...`).
- **API Response:** HTTP 302 Redirect to `/profile`.
- **Frontend Response:** `/profile` loads, rendering updated text and avatar from `/{profileImage}`.
- **Error Handling & Broken Connections:**
  - **Critical Security Flaw:** No file validation. Malicious scripts or non-image files are saved directly to `./uploads/` and publicly accessible.
  - **Session Stale State:** Session `loggedUser` entity retains old profile state until re-login.
  - **Verification Status:** **VERIFIED BROKEN / VULNERABLE VIA CODE TRACE**

---

## 3. Alumni Directory & Networking Workflows

### 3.1 Search & Filter Alumni Workflow
- **User Action:** User enters a keyword in `/alumni` search box, selects role "ALUMNI", and clicks "Search".
- **Frontend Behavior:** Navigates to `/alumni?keyword={kw}&role=ALUMNI`.
- **API Request:** `GET /alumni?keyword=...&role=...`.
- **Backend Processing:** `AlumniController` calls `userService.searchUsers()`. Executes repository query and streams in memory to filter out current user and `ADMIN`.
- **Database Operations:** Executes `SELECT * FROM users WHERE LOWER(name) LIKE LOWER(?) AND role = ?`.
- **API Response:** Returns `alumni-list.html` with filtered user list.
- **Frontend Response:** Matching cards render on screen.
- **Error Handling & Broken Connections:**
  - **UX Defect:** Search input and role dropdown do not retain the submitted values (form resets on page load).
  - **Performance Defect:** No database pagination; fetches all records into memory.
  - **Verification Status:** **VERIFIED FUNCTIONAL BUT UNOPTIMIZED**

---

### 3.2 View Member Profile Workflow
- **User Action:** User clicks "View Profile" on an alumni card.
- **Frontend Behavior:** Navigates to `/view-user/{id}`.
- **API Request:** `GET /view-user/{id}`.
- **Backend Processing:** `AlumniController` queries `userService.getUserById(id)`.
- **Database Operations:** `SELECT * FROM users WHERE id = ?` and `SELECT * FROM profile WHERE user_id = ?`.
- **API Response:** Returns `view-user.html`.
- **Frontend Response:** Renders member profile card with "Send Message" button.
- **Error Handling & Broken Connections:**
  - **Fatal Defect:** If `{id}` does not exist, `user` is null. Thymeleaf throws an unhandled `NullPointerException` attempting to evaluate `user.name`, causing an HTTP 500 error.
  - **Verification Status:** **VERIFIED BROKEN ON INVALID ID VIA CODE TRACE**

---

## 4. Job Portal Workflows

### 4.1 Post a Job Workflow
- **User Action:** Alumni or Admin navigates to `/post-job`, fills out Title, Company, Location, Description, and clicks "Post".
- **Frontend Behavior:** Submits `POST /save-job`.
- **API Request:** `POST /save-job`.
- **Backend Processing:** `JobController.saveJob()` verifies role (`ALUMNI` or `ADMIN`), attaches `loggedUser`, and calls `jobService.saveJob(job)`.
- **Database Operations:** `INSERT INTO job (title, company, description, location, posted_by) VALUES (?, ?, ?, ?, ?)`.
- **API Response:** HTTP 302 Redirect to `/jobs`.
- **Frontend Response:** `/jobs` loads with the newly posted job card.
- **Error Handling & Broken Connections:**
  - **Validation Defect:** Submitting completely empty fields succeeds, creating blank job listings.
  - **Verification Status:** **VERIFIED FUNCTIONAL (Lacks Validation)**

---

### 4.2 Apply for a Job Workflow
- **User Action:** Student clicks "Apply" on a job card in `/jobs`.
- **Frontend Behavior:** Follows hyperlink `href="/apply/{jobId}"`.
- **API Request:** `GET /apply/{jobId}`.
- **Backend Processing:** `JobController.applyJob()` checks if already applied; if not, instantiates `Application(job, user, status="APPLIED")` and persists.
- **Database Operations:**
  1. `SELECT count(*) FROM application WHERE user_id = ? AND job_id = ?`
  2. `INSERT INTO application (job_id, user_id, status) VALUES (?, ?, 'APPLIED')`
- **API Response:** HTTP 302 Redirect to `/jobs`.
- **Frontend Response:** `/jobs` re-renders; the "Apply" button turns into a disabled "✔ Applied" button.
- **Error Handling & Broken Connections:**
  - **Fatal Defect:** If unauthenticated, `user` is null; crashes with `NullPointerException` (HTTP 500).
  - **Broken Design:** Executed via GET instead of POST.
  - **Product Defect:** Job poster has no screen to view who applied! The application is stored in MySQL, but invisible to the recruiter.
  - **Verification Status:** **VERIFIED BROKEN / INCOMPLETE VIA CODE TRACE**

---

## 5. Event Management Workflows

### 5.1 Create Event Workflow
- **User Action:** User fills out `create-event.html` (Title, Description, Location, Date) and clicks "Create".
- **Frontend Behavior:** Submits `POST /save-event`.
- **API Request:** `POST /save-event`.
- **Backend Processing:** `EventController.saveEvent()` attaches session user and calls `eventService.saveEvent(event)`.
- **Database Operations:** `INSERT INTO event (title, description, location, date, created_by_id) VALUES (?, ?, ?, ?, ?)`.
- **API Response:** HTTP 302 Redirect to `/events`.
- **Frontend Response:** `/events` loads showing the new event card.
- **Error Handling & Broken Connections:**
  - **Authorization Bypass:** `POST /save-event` does NOT check user role. Any student can bypass `/create-event` and directly submit events via curl or Postman.
  - **Verification Status:** **VERIFIED BROKEN (Authorization Defect)**

---

### 5.2 Register for Event Workflow
- **User Action:** User clicks "Register" on an event card in `/events`.
- **Frontend Behavior:** Follows hyperlink `href="/register-event/{id}"`.
- **API Request:** `GET /register-event/{id}`.
- **Backend Processing:** `EventController.register()` fetches all events, streams to find matching ID, and saves `EventRegistration`.
- **Database Operations:**
  1. `SELECT * FROM event` (fetches all events into JVM memory!)
  2. `SELECT count(*) FROM event_registration WHERE user_id = ? AND event_id = ?`
  3. `INSERT INTO event_registration (user_id, event_id) VALUES (?, ?)`
- **API Response:** HTTP 302 Redirect to `/events`.
- **Frontend Response:** Button toggles to disabled "✔ Registered".
- **Error Handling & Broken Connections:**
  - **Fatal Defect:** Unauthenticated request crashes with `NullPointerException`.
  - **Performance Defect:** Streams all events in memory rather than calling `findById`.
  - **Verification Status:** **VERIFIED BROKEN VIA CODE TRACE**

---

## 6. Direct Messaging Workflows

### 6.1 View Inbox Workflow
- **User Action:** User clicks "Messages" in navbar (`/messages`).
- **Frontend Behavior:** Navigates to `/messages`.
- **API Request:** `GET /messages`.
- **Backend Processing:** `MessageController.inbox()` queries `msgService.getInbox(user)`.
- **Database Operations:** `SELECT * FROM message WHERE receiver_id = ?`.
- **API Response:** Returns `messages.html`.
- **Frontend Response:** Renders list of incoming message rows.
- **Error Handling & Broken Connections:**
  - **UX / Architectural Defect:** Every single incoming message is rendered as a standalone item rather than grouped conversations. If user X sent 10 messages, 10 separate rows appear. Sent conversations with no reply are invisible.
  - **Verification Status:** **VERIFIED FLAWED VIA CODE TRACE**

---

### 6.2 Send Direct Message Workflow
- **User Action:** User types message in `chat.html` and clicks "Send".
- **Frontend Behavior:** Submits `POST /send-message` with `receiverId` and `content`.
- **API Request:** `POST /send-message`.
- **Backend Processing:** `MessageController.sendMessage()` persists `Message(sender, receiver, content, timestamp, seen=false)`.
- **Database Operations:** `INSERT INTO message (sender_id, receiver_id, content, timestamp, seen) VALUES (?, ?, ?, ?, 0)`.
- **API Response:** HTTP 302 Redirect to `/chat/{receiverId}`.
- **Frontend Response:** Page reloads completely; new message appears in blue bubble on right; auto-scroll scrolls to bottom.
- **Error Handling & Broken Connections:**
  - **Defect:** Allows empty/whitespace messages.
  - **Defect:** Allows sending messages to oneself.
  - **Defect:** Synchronous full-page reload on every message.
  - **Verification Status:** **VERIFIED FUNCTIONAL BUT FLAWED**

---

## 7. Administrative Moderation Workflows

### 7.1 Admin Delete User Workflow
- **User Action:** Administrator clicks "Delete" on a user row in `/admin/users`.
- **Frontend Behavior:** Follows hyperlink `href="/admin/delete-user/{id}"`.
- **API Request:** `GET /admin/delete-user/{id}`.
- **Backend Processing:** `AdminController.deleteUser()` calls `userService.deleteUser(id)`.
- **Database Operations:** `DELETE FROM users WHERE id = ?`.
- **API Response:** HTTP 302 Redirect to `/admin/users`.
- **Error Handling & Broken Connections:**
  - **Fatal Defect:** If the user has applied to jobs, posted jobs, created events, registered for events, or sent messages, MySQL throws a foreign key constraint violation. Results in an unhandled `DataIntegrityViolationException` (HTTP 500 error).
  - **Defect:** Admin can delete their own account.
  - **Verification Status:** **VERIFIED FATAL CRASH ON ACTIVE USERS VIA CODE TRACE**

---

### 7.2 Admin Delete Job & Delete Event Workflows
- **User Action:** Administrator clicks "Delete" on `/admin/jobs` or `/admin/events`.
- **Frontend Behavior:** Follows hyperlink `href="/admin/delete-job/{id}"` or `href="/admin/delete-event/{id}"`.
- **Backend Processing:** Invokes `deleteJob()` or `deleteEvent()`.
- **Database Operations:** `DELETE FROM job WHERE id = ?` or `DELETE FROM event WHERE id = ?`.
- **Error Handling & Broken Connections:**
  - **Fatal Defect:** Deleting a job with applications or an event with registrations immediately throws `DataIntegrityViolationException` (HTTP 500 error).
  - **Verification Status:** **VERIFIED FATAL CRASH ON ACTIVE ITEMS VIA CODE TRACE**
