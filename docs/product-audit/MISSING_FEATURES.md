# Missing Features & Gap Analysis: AlumniBridge

**Audit Date:** November 2026  
**Auditor:** Senior Product Architect, Technical Lead & QA Engineer  

---

## 1. Product Gap Analysis Matrix

| Module | Feature | Current Status | Missing Functionality | Priority |
|---|---|---|---|---|
| **Auth & Security** | Password Hashing | NOT IMPLEMENTED | Raw passwords stored in DB; BCrypt / Argon2 missing. | **CRITICAL** |
| **Auth & Security** | Role Privilege Protection | NOT IMPLEMENTED | Registration allows arbitrary `role` submission; any user can become `ADMIN`. | **CRITICAL** |
| **Auth & Security** | Blocked User Enforcement | NOT IMPLEMENTED | Blocked users can still log in and perform all user actions. | **CRITICAL** |
| **Auth & Security** | CSRF Protection | NOT IMPLEMENTED | Spring Security is commented out; all mutations lack CSRF tokens. | **CRITICAL** |
| **Auth & Security** | Session Protection / Guard | PARTIALLY IMPLEMENTED | Several endpoints (`/apply/{id}`, `/chat/{id}`, `/my-events`) lack null checks. | **HIGH** |
| **Auth & Security** | Email Uniqueness Validation | NOT IMPLEMENTED | Allows duplicate email registrations, causing subsequent login query crashes. | **HIGH** |
| **Auth & Security** | Password Reset / Forgot Password | NOT IMPLEMENTED | No password recovery or email reset workflow exists. | **MEDIUM** |
| **Profile** | Academic & Educational History | NOT IMPLEMENTED | Entity lacks Degree, Branch/Department, Graduation Year, Batch, and Roll Number. | **HIGH** |
| **Profile** | Contact & Social Networking Links | NOT IMPLEMENTED | Entity lacks Phone Number, LinkedIn URL, GitHub URL, Portfolio Link. | **MEDIUM** |
| **Profile** | File Upload Security & Validation | NOT IMPLEMENTED | No MIME type checks, no file size checks, path traversal risks present. | **CRITICAL** |
| **Alumni Directory** | Multi-Criteria Filtering | PARTIALLY IMPLEMENTED | Can only search by name; cannot filter by company, skills, or batch. | **MEDIUM** |
| **Alumni Directory** | Database-Level Pagination | NOT IMPLEMENTED | Loads all records into JVM memory; lacks pagination controls. | **HIGH** |
| **Job Portal** | Job Details View Page | NOT IMPLEMENTED | Job description is invisible in `jobs.html`; no `/jobs/{id}` view page exists. | **CRITICAL** |
| **Job Portal** | Resume & Application Attachment | NOT IMPLEMENTED | Users cannot upload a resume or link portfolio when applying. | **HIGH** |
| **Job Portal** | Job Poster Management & Editing | NOT IMPLEMENTED | Alumni cannot edit, close, or delete their own posted jobs. | **HIGH** |
| **Job Portal** | Applicant Review for Employers | NOT IMPLEMENTED | Job poster cannot view who applied for their job listing. | **CRITICAL** |
| **Job Portal** | Application Status Lifecycle | NOT IMPLEMENTED | Status permanently stuck at "APPLIED"; no review, shortlist, or reject actions. | **HIGH** |
| **Job Portal** | Extended Job Metadata | NOT IMPLEMENTED | Entity lacks salary, employment type, deadline, and experience level. | **MEDIUM** |
| **Event Management** | Event Time & Scheduling | NOT IMPLEMENTED | Entity has `LocalDate` but no start/end time. | **HIGH** |
| **Event Management** | Mode & Location Details | PARTIALLY IMPLEMENTED | Lacks meeting link for webinars and capacity limits for in-person events. | **MEDIUM** |
| **Event Management** | Past vs. Upcoming Event Separation | NOT IMPLEMENTED | Expired events are shown forever alongside future events. | **HIGH** |
| **Event Management** | Attendee List for Event Organizers | NOT IMPLEMENTED | Event creators cannot view who registered for their event. | **HIGH** |
| **Event Management** | Event Cancellation / Unregister | NOT IMPLEMENTED | Users cannot cancel their registration; organizers cannot cancel events safely. | **MEDIUM** |
| **Messaging** | Conversation Grouping in Inbox | NOT IMPLEMENTED | Displays raw message records; does not group by user into chat threads. | **HIGH** |
| **Messaging** | Real-Time Messaging (WebSocket) | NOT IMPLEMENTED | Standard synchronous HTTP redirect on every message; requires page reload. | **MEDIUM** |
| **Messaging** | Input Validation & Self-Messaging Guard | NOT IMPLEMENTED | Allows blank messages and sending messages to oneself. | **MEDIUM** |
| **Dashboard** | Admin Metrics & Analytics | NOT IMPLEMENTED | Admin dashboard is static cards; zero metrics or charts provided. | **HIGH** |
| **Admin Oversight** | Application Oversight / Management | NOT IMPLEMENTED | Advertised in README, but 0 endpoints or templates exist for Admin. | **HIGH** |
| **Admin Oversight** | Safe Cascade Deletions | NOT IMPLEMENTED | Deleting users/jobs/events throws unhandled database foreign key errors. | **CRITICAL** |
| **System Testing** | Unit & Integration Test Suites | NOT IMPLEMENTED | Zero tests exist in repository; `spring-boot-starter-test` commented out. | **CRITICAL** |

---

## 2. Completely Missing Functional Modules

### 2.1 Password Reset & Account Recovery
- **Missing Components:**
  - Token-based password reset model / entity (`PasswordResetToken`).
  - Forgot Password form (`/forgot-password`) and Reset Password form (`/reset-password?token=...`).
  - Email dispatch service using Spring Mail (`JavaMailSender`).
- **Product Impact:** Users who lose their password are permanently locked out unless an admin directly updates the database table.

### 2.2 Employer / Poster Applicant Review System
- **Missing Components:**
  - Route `/jobs/{jobId}/applications` to list all candidates who applied for a specific job.
  - Candidate profile inspection with one-click messaging link.
  - Status progression workflow: `APPLIED` -> `IN_REVIEW` -> `SHORTLISTED` -> `REJECTED` -> `ACCEPTED`.
- **Product Impact:** Currently, alumni can post a job, students can click apply, but the alumni who posted the job **cannot see who applied**! This makes the job portal fundamentally non-functional for recruitment.

### 2.3 Event Organizer Attendee Management
- **Missing Components:**
  - Route `/events/{eventId}/attendees` for event organizers and admins.
  - Export attendee list to CSV / Excel.
  - Mark attendance on event day.
- **Product Impact:** Organizers have no visibility into who registered for their events.

### 2.4 Administrative Application Oversight
- **Missing Components:**
  - Route `/admin/applications` to monitor platform-wide application volume and status.
  - Administrative override on fraudulent or stale job postings and applications.

---

## 3. Missing Database Fields and Relationships

### 3.1 Entity: `User`
- `email`: Missing `@Column(unique = true, nullable = false)`.
- `created_at` / `updated_at`: Missing audit timestamps.
- `last_login_at`: Missing security tracking field.
- `email_verified`: Missing account verification flag.

### 3.2 Entity: `Profile`
- `degree`: Academic degree (e.g., B.Tech, M.Tech, MBA, Ph.D.).
- `branch`: Field of study (e.g., Computer Science, Mechanical).
- `batch_year` / `graduation_year`: Year of graduation (crucial for alumni filtering).
- `roll_number` / `student_id`: Institutional identifier.
- `phone`: Contact telephone number.
- `linkedin_url`: Professional LinkedIn profile link.
- `github_url`: GitHub / Portfolio link.
- `city` / `country`: Current geographic location.

### 3.3 Entity: `Job`
- `salary_range`: Compensation or stipend details.
- `job_type`: Enum (`FULL_TIME`, `PART_TIME`, `INTERNSHIP`, `CONTRACT`, `REMOTE`).
- `experience_required`: Years of experience expected.
- `deadline`: Application cutoff date (`LocalDate`).
- `status`: Enum (`ACTIVE`, `CLOSED`, `EXPIRED`).
- `created_at` / `updated_at`: Audit timestamps.

### 3.4 Entity: `Application`
- `applied_date`: Timestamp when application was submitted.
- `resume_url`: File path or cloud link to uploaded resume document.
- `cover_note`: Candidate cover message to employer.
- Missing Composite Unique Constraint: `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "user_id"}))`.

### 3.5 Entity: `Event`
- `start_time` / `end_time`: `LocalTime` for scheduling.
- `event_mode`: Enum (`IN_PERSON`, `VIRTUAL`, `HYBRID`).
- `meeting_link`: URL for virtual webinars (Zoom, Google Meet, Teams).
- `max_capacity`: Maximum permitted registrations.
- `registration_deadline`: Cutoff timestamp for event registration.
- `status`: Enum (`SCHEDULED`, `COMPLETED`, `CANCELLED`).

### 3.6 Entity: `EventRegistration`
- `registered_at`: Timestamp of registration.
- `attended`: Boolean tracking physical/virtual attendance.
- Missing Composite Unique Constraint: `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}))`.

---

## 4. Missing Business Logic & Validations

1. **Self-Application Prevention:** Business rule preventing job posters from applying to their own jobs.
2. **Expired Job Application Guard:** Business rule preventing applications to jobs where deadline has passed or status is CLOSED.
3. **Past Event Registration Guard:** Business rule rejecting registration requests for events whose dates have already passed.
4. **Event Capacity Enforcement:** Business rule ensuring registrations do not exceed `max_capacity`.
5. **Admin Self-Deletion Guard:** Business rule preventing administrators from deleting or deactivating their own active accounts.
6. **Input Sanitization:** XSS sanitization on rich text inputs (job descriptions, bios, and messages).

---

## 5. Mandatory Requirements vs. Optional Enhancements

### Mandatory Requirements (Must be implemented before production)
1. Spring Security integration with BCrypt password hashing.
2. Fix privilege escalation on registration (restrict to `STUDENT` and `ALUMNI`).
3. Enforce blocked user checks during login and request authentication.
4. Convert all state-mutating GET endpoints (`/apply/*`, `/register-event/*`, `/admin/delete-*`, `/admin/toggle-block/*`) to POST with CSRF tokens.
5. Secure profile image uploads (MIME validation, secure naming, restricted directory).
6. Implement database foreign key cascading or safe deletion logic in services.
7. Job applicant review interface for job posters (`/jobs/{id}/applications`).
8. View Job Details page rendering full job descriptions.
9. Database unique constraints on `users.email`, `(job_id, user_id)`, and `(event_id, user_id)`.
10. Add unit and integration tests with Mockito and MockMvc.

### Optional Recommendations (Future iterations)
1. Real-time WebSocket (STOMP) messaging replacing synchronous page refreshes.
2. Email notifications for job applications, event confirmations, and direct messages.
3. Export attendee and applicant lists to CSV/Excel.
4. Advanced analytics charts (using Chart.js) on the Admin Dashboard.
5. Cloud storage integration (AWS S3 / Google Cloud Storage) for avatar and resume documents.
