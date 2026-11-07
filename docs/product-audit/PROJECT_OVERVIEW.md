# Project Overview: AlumniBridge

**Audit Date:** November 2026  
**Auditor:** Senior Product Architect, Full-Stack Technical Lead & QA Engineer  
**Repository Working Directory:** `AlumniBridge-project/Alumni-Bridge`  
**Current Application Version:** `0.0.1-SNAPSHOT`  

---

## 1. Executive Summary

**AlumniBridge** is a monolithic web application designed to connect educational institutions, current students, and alumni. Its primary mission is to foster professional networking, facilitate mentorship, streamline career opportunities via an internal job portal, coordinate institutional events, and provide a direct communication channel between alumni and students.

The codebase represents an early-stage Minimum Viable Product (MVP) built using **Spring Boot 3 (Java 21)** and **Thymeleaf**. While the core UI pages and baseline database entities exist, the application contains significant architectural defects, critical security vulnerabilities (such as plaintext password storage and arbitrary file uploads), absent validation layers, and numerous incomplete end-to-end workflows that prevent production deployment.

---

## 2. Problem Statement & Value Proposition

### 2.1 The Problem
- **Disjointed Alumni Communities:** Most academic institutions lose active contact with their graduates, relying on fragmented third-party platforms (LinkedIn, Facebook groups, WhatsApp) where institutional data cannot be organized or leveraged.
- **Limited Mentorship & Career Guidance for Students:** Current students struggle to identify alumni in target industries, specific companies, or geographic locations for career advice, job referrals, and mentorship.
- **Scattered Event Coordination:** Campus reunions, webinars, guest lectures, and networking dinners lack a centralized registration and management portal.
- **No Centralized Referral Mechanism:** Alumni looking to hire from their alma mater lack a dedicated, trusted job board tailored specifically for student applications.

### 2.2 The Proposed Solution (AlumniBridge)
AlumniBridge seeks to solve these challenges through a unified platform featuring:
1. **Centralized Alumni Directory:** Searchable by alumni name, role, and industry domain.
2. **Internal Job & Internship Portal:** Alumni and administrators post career opportunities; students apply directly.
3. **Event Management System:** Centralized publishing of campus and virtual events with instant registration.
4. **Direct Messaging System:** Direct 1-on-1 chat enabling direct networking and mentorship.
5. **Administrative Governance:** Moderation controls for users, jobs, and events.

---

## 3. Technology Stack & Environment Details

| Category | Technology / Library | Version Specified | Actual Detected Status |
|---|---|---|---|
| **Runtime / Language** | Java (OpenJDK) | Java 21 | Source compatibility set to Java 21 in `pom.xml` |
| **Backend Framework** | Spring Boot | 3.5.13 (Parent POM) | Declared as `3.5.13` (Non-standard version identifier) |
| **MVC Architecture** | Spring Web MVC | 3.x | Monolithic Controller-View architecture |
| **ORM / Data Access** | Spring Data JPA / Hibernate | 3.x (Jakarta Persistence) | Entity and Repository-based persistence |
| **Database** | MySQL | MySQL 8.x Connector (`mysql-connector-j`) | Configured for `localhost:3306/alumni_db` |
| **Template Engine** | Thymeleaf | `spring-boot-starter-thymeleaf` | Server-side HTML rendering |
| **Frontend Framework** | Bootstrap 5 | 5.3.0 | Sourced via jsDelivr CDN in templates |
| **Frontend Scripting** | Vanilla JavaScript | ES6 | Inline scripts in HTML templates (no JS framework) |
| **Build & Dependency Tool** | Apache Maven | 3.x (Maven Wrapper `mvnw`) | `pom.xml` present with Maven wrapper scripts |
| **Security Framework** | Spring Security | 6.x | **COMMENTED OUT** in `pom.xml`; ad-hoc `HttpSession` used |
| **Testing Framework** | JUnit / Mockito | Spring Boot Test | **COMMENTED OUT** in `pom.xml`; zero test suites present |

---

## 4. Architectural Profile & Folder Structure

The project adheres to a standard Maven multi-tier structure under `Alumni-Bridge/`:

```text
AlumniBridge-project/
├── .project
├── README.md
└── Alumni-Bridge/
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    ├── uploads/                          # Local file storage for profile avatars
    ├── screenshots/                      # UI preview assets
    └── src/
        └── main/
            ├── java/com/yaswanth/Alumni_Bridge/
            │   ├── AlumniBridgeApplication.java    # Application entry point
            │   ├── controller/                     # 8 Spring MVC Controllers
            │   │   ├── AdminController.java
            │   │   ├── AlumniController.java
            │   │   ├── AuthController.java
            │   │   ├── DashboardController.java
            │   │   ├── EventController.java
            │   │   ├── JobController.java
            │   │   ├── MessageController.java
            │   │   └── ProfileController.java
            │   ├── entity/                         # 7 JPA Entities
            │   │   ├── Application.java
            │   │   ├── Event.java
            │   │   ├── EventRegistration.java
            │   │   ├── Job.java
            │   │   ├── Message.java
            │   │   ├── Profile.java
            │   │   └── User.java
            │   ├── repository/                     # 7 Spring Data JPA Repositories
            │   │   ├── ApplicationRepository.java
            │   │   ├── EventRegistrationRepository.java
            │   │   ├── EventRepository.java
            │   │   ├── JobRepository.java
            │   │   ├── MessageRepository.java
            │   │   ├── ProfileRepository.java
            │   │   └── UserRepository.java
            │   └── service/                        # 6 Business Logic Services
            │       ├── ApplicationService.java
            │       ├── EventService.java
            │       ├── JobService.java
            │       ├── MessageService.java
            │       ├── ProfileService.java
            │       └── UserService.java
            └── resources/
                ├── application.properties          # Server & database configuration
                └── templates/                      # 19 Thymeleaf HTML templates
                    ├── fragments/
                    │   ├── navbar.html
                    │   └── admin-navbar.html
                    ├── admin-dashboard.html
                    ├── admin-events.html
                    ├── admin-jobs.html
                    ├── admin-users.html
                    ├── alumni-list.html
                    ├── chat.html
                    ├── create-event.html
                    ├── dashboard.html
                    ├── edit-profile.html
                    ├── events.html
                    ├── index.html
                    ├── jobs.html
                    ├── login.html
                    ├── messages.html
                    ├── my-applications.html
                    ├── my-events.html
                    ├── post-job.html
                    ├── profile.html
                    ├── register.html
                    └── view-user.html
```

---

## 5. User Roles and Intended Access Matrix

| Role | Intended Description | Permitted Capabilities in Codebase |
|---|---|---|
| **STUDENT** | Currently enrolled student seeking networking, jobs, and events. | View alumni directory, view profiles, apply for jobs, view own applications, register for events, view own registrations, send/receive direct messages, update personal profile. |
| **ALUMNI** | Graduate offering mentorship, posting opportunities, and attending events. | All Student capabilities + Post jobs, Create events. |
| **ADMIN** | Institutional administrator governing the platform. | All Alumni/Student capabilities + Access Admin Dashboard, Toggle block/unblock user, Delete user, Delete job, Delete event. |

---

## 6. Key Architectural Observations

1. **Monolithic Server-Side Rendering (SSR):** The application does not expose REST APIs or headless DTO endpoints. It returns full HTML pages via Thymeleaf templates.
2. **Absence of DTO Layer:** Entity objects are directly bound to HTTP form submissions in controllers (e.g., `@ModelAttribute User user`, `@ModelAttribute Job job`). This exposes internal database models to mass assignment vulnerabilities.
3. **Manual Session Security:** In place of Spring Security, authentication is managed via `HttpSession.setAttribute("loggedUser", user)` in `AuthController`. Role checking is manually performed using `session.getAttribute("loggedUser")` checks.
4. **Tight View-to-Service Coupling:** Thymeleaf templates directly call Spring service beans (e.g., `@applicationService.alreadyApplied(...)` and `eventService.isRegistered(...)`) from SpEL expressions inside HTML loops.
5. **Static Resource Reconfiguration:** `spring.web.resources.static-locations` is configured to `file:./uploads/`, overriding default static directories and directly exposing local file system uploads.
