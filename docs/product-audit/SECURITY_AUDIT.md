# Security & Vulnerability Audit: AlumniBridge

**Audit Date:** November 2026  
**Auditor:** Senior Product Architect, Technical Lead & QA Engineer  
**Security Framework Status:** `spring-boot-starter-security` is **commented out** in `pom.xml`. The application operates with zero built-in Spring Security filters or default security headers.

---

## 1. Vulnerability Classification Summary

| Severity | Issue Description | Location | CWE ID |
|---|---|---|---|
| **CRITICAL** | Plaintext Password Storage & Comparison | `User.java`, `UserService.java`, `AuthController.java` | CWE-256 / CWE-312 |
| **CRITICAL** | Privilege Escalation to ADMIN on Registration | `AuthController.java`, `register.html` | CWE-269 / CWE-284 |
| **CRITICAL** | Unrestricted Arbitrary File Upload (RCE Risk) | `ProfileController.java` | CWE-434 |
| **CRITICAL** | Lack of CSRF Protection on State-Changing Actions | Application-wide (All `POST` and stateful `GET`s) | CWE-352 |
| **HIGH** | Path Traversal via Unsanitized Filename | `ProfileController.java` | CWE-22 |
| **HIGH** | State Mutation via HTTP GET Requests | `JobController.java`, `EventController.java`, `AdminController.java` | CWE-652 |
| **HIGH** | Broken Authentication / Null Session Crash (NPE) | Multiple Controllers (`/apply`, `/chat`, `/my-events`) | CWE-287 |
| **HIGH** | Blocked Account Status Not Enforced | `UserService.java`, `AuthController.java` | CWE-284 |
| **HIGH** | Mass Assignment on User & Entity Models | `AuthController.java`, `JobController.java` | CWE-915 |
| **MEDIUM** | Sensitive Data Exposure in Sessions & Views | `AuthController.java`, `User.java` | CWE-200 |
| **MEDIUM** | Missing Input Validation & XSS Risk in Templates | All entity models and HTML templates | CWE-79 / CWE-20 |
| **MEDIUM** | Unbounded In-Memory Collection Processing (DoS) | `UserService.java`, `DashboardController.java` | CWE-400 |
| **LOW** | Verbose Error Stack Traces Exposed | Default Spring Boot Whitelabel Error Pages | CWE-209 |

---

## 2. In-Depth Vulnerability Analysis

### 2.1 CRITICAL: Plaintext Password Storage & Comparison (CWE-256)
- **Vulnerability Description:** Passwords submitted during registration are directly persisted as plaintext strings in the `users` table. During authentication, raw strings are compared via `user.getPassword().equals(password)`.
- **Proof of Concept / Code Evidence:**
  ```java
  // In UserService.java:
  public User loginUser(String email, String password) {
      User user = repo.findByEmail(email);
      if (user != null && user.getPassword().equals(password)) {
          return user;
      }
      return null;
  }
  ```
- **Impact:** Any database leak, SQL dump, or administrative SQL inspection immediately compromises every user's credentials across the organization.
- **Remediation:**
  1. Re-enable `spring-boot-starter-security`.
  2. Configure a `BCryptPasswordEncoder` bean with strength 12.
  3. Encode passwords on registration: `user.setPassword(passwordEncoder.encode(rawPassword))`.
  4. Authenticate using `passwordEncoder.matches(rawPassword, user.getPassword())`.

---

### 2.2 CRITICAL: Privilege Escalation to ADMIN on Registration (CWE-269)
- **Vulnerability Description:** The registration controller takes an unsanitized `User` entity directly from the request form:
  ```java
  @PostMapping("/register")
  public String register(@ModelAttribute User user) {
      service.registerUser(user);
      return "redirect:/login";
  }
  ```
  The HTML template only shows `STUDENT` and `ALUMNI` in the dropdown, but any user can craft an HTTP POST payload containing `role=ADMIN`. The backend binds this value directly and saves an active administrator account with no verification.
- **Impact:** Anyone on the internet can become a super-administrator with full permissions to block users, delete data, and view private messages.
- **Remediation:**
  1. Never bind database entities directly in controllers; use a dedicated `RegisterRequestDto`.
  2. Hardcode allowed registration roles: if role is neither `STUDENT` nor `ALUMNI`, reject the request or default to `STUDENT`. Admin roles should only be provisioned via database seed scripts or by existing verified administrators.

---

### 2.3 CRITICAL: Unrestricted Arbitrary File Upload (CWE-434) & Path Traversal (CWE-22)
- **Vulnerability Description:** In `ProfileController.java`:
  ```java
  if (file != null && !file.isEmpty()) {
      String uploadDir = System.getProperty("user.dir") + "/uploads/";
      File uploadPath = new File(uploadDir);
      if (!uploadPath.exists()) {
          uploadPath.mkdirs();
      }
      String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
      fileName = fileName.replaceAll(" ", "_");
      file.transferTo(new File(uploadDir + fileName));
      user.setProfileImage(fileName);
      userService.registerUser(user);
  }
  ```
  And in `application.properties`:
  ```properties
  spring.web.resources.static-locations=file:./uploads/
  ```
- **Impact:**
  1. **Arbitrary File Upload:** No extension or MIME-type check is performed. An attacker can upload `.html`, `.svg`, `.jsp`, or executable scripts containing malicious JavaScript (Stored XSS) or server-executable code.
  2. **Direct Static Serving:** Uploaded files are served directly from the root path (`http://localhost:8127/{fileName}`). An uploaded malicious HTML file will execute in the origin domain context, stealing session cookies or mimicking login screens.
  3. **Path Traversal:** If `file.getOriginalFilename()` contains `../../`, it is not sanitized, potentially allowing files to be written outside `./uploads/`.
- **Remediation:**
  1. Enforce a strict whitelist of image extensions: `.jpg`, `.jpeg`, `.png`, `.webp`.
  2. Verify MIME type using Apache Tika or `ImageIO.read()` to confirm actual image bytes.
  3. Generate randomized UUID file names: `UUID.randomUUID().toString() + extension`.
  4. Remove `file:./uploads/` from root static locations. Serve avatars via an authenticated controller with proper `Content-Type` headers or an isolated media subpath.

---

### 2.4 CRITICAL: Missing CSRF Protection & Dangerous State-Mutating GET Requests (CWE-352 / CWE-652)
- **Vulnerability Description:**
  - Spring Security is disabled, leaving CSRF protection completely non-existent for all `POST` requests.
  - Furthermore, several destructive and state-changing actions are implemented using HTTP `GET` links:
    - `GET /admin/delete-user/{id}`
    - `GET /admin/delete-job/{id}`
    - `GET /admin/delete-event/{id}`
    - `GET /admin/toggle-block/{id}`
    - `GET /apply/{jobId}`
    - `GET /register-event/{id}`
- **Impact:** An attacker can embed an image tag `<img src="http://localhost:8127/admin/delete-user/1">` on an external website, in an email, or even in a message within the chat system. If an administrator views it while logged in, the target user or data will be permanently deleted without their consent or knowledge.
- **Remediation:**
  1. Re-enable Spring Security CSRF token validation.
  2. Convert all state-altering endpoints to HTTP `POST` requests with CSRF tokens.

---

### 2.5 HIGH: Blocked User Status Bypass (CWE-284)
- **Vulnerability Description:** The `users` table has a `blocked` flag, and administrators can toggle it in `/admin/users`. However, `UserService.loginUser()` and `AuthController.loginUser()` never inspect `user.getBlocked()`.
- **Impact:** When an administrator blocks a user for malicious activity, the user can still log in, send messages, post jobs, apply for jobs, and access the entire system.
- **Remediation:**
  Add a condition in `loginUser`:
  ```java
  if (Boolean.TRUE.equals(user.getBlocked())) {
      throw new DisabledAccountException("Your account has been suspended by an administrator.");
  }
  ```
  Add a session filter/interceptor to immediately terminate existing active sessions of blocked users.

---

### 2.6 HIGH: Broken Authentication & Unchecked Session Access (CWE-287)
- **Vulnerability Description:** Several endpoints fail to verify whether `session.getAttribute("loggedUser")` is non-null before invoking methods on it:
  - `EventController.register(@PathVariable Long id, HttpSession session)`
  - `EventController.myEvents(Model model, HttpSession session)`
  - `JobController.applyJob(@PathVariable Long jobId, HttpSession session)`
  - `MessageController.chat(@PathVariable Long id, Model model, HttpSession session)`
  - `MessageController.sendMessage(...)`
- **Impact:** Any unauthenticated request or expired session immediately throws an unhandled `NullPointerException`, displaying a 500 error page to the user and leaking server internals.
- **Remediation:** Enforce authentication declaratively at the filter layer through Spring Security so unauthenticated requests are redirected cleanly to `/login` before reaching controllers.

---

### 2.7 MEDIUM: Sensitive Data Exposure in Session & Templates (CWE-200)
- **Vulnerability Description:** Entire JPA `User` entity instances (including plaintext passwords) are placed directly into `HttpSession` (`session.setAttribute("loggedUser", user)`). In Thymeleaf templates, expressions like `${session.loggedUser}` expose the entity instance.
- **Remediation:** Store a lightweight, sanitized principal object (e.g., `UserPrincipal` containing only `id`, `email`, `name`, `role`) in the session. Never retain password fields in session attributes.

---

## 3. Dependency & Code Quality Audit

### 3.1 Commented-Out Dependencies
In `pom.xml`, critical standard dependencies are commented out:
```xml
<!-- <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency> -->

<!-- <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency> -->
```
Meanwhile, `spring-security-test` is left in `<dependencies>` without the core security framework, causing an orphaned test dependency.

### 3.2 Non-Standard Spring Boot Version
`pom.xml` declares:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.13</version>
</parent>
```
Spring Boot 3.5.x is not a released GA version (GA versions are currently 3.2.x, 3.3.x, 3.4.x). Using non-standard version tags in parent POMs risks build inconsistency and unresolved dependency issues. It should be aligned to a stable GA release (e.g., `3.3.5` or `3.4.0`).
