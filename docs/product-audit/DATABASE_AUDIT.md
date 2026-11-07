# Database & Data Access Layer Audit: AlumniBridge

**Audit Date:** November 2026  
**Auditor:** Senior Product Architect, Technical Lead & QA Engineer  
**Database Engine:** MySQL 8.x  
**Persistence Framework:** Spring Data JPA / Hibernate (Jakarta Persistence)  

---

## 1. Database Configuration Analysis

The database connection is defined in `Alumni-Bridge/src/main/resources/application.properties`:

```properties
spring.datasource.url=${MYSQL_JDBC_URL:jdbc:mysql://localhost:3306/alumni_db}
spring.datasource.username=${MYSQLUSER:root}
spring.datasource.password=${MYSQLPASSWORD:root}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

### Configuration Observations & Risks:
1. **`ddl-auto=update` in Production/Default Profile:**
   - Using `update` allows Hibernate to alter schemas automatically at boot.
   - It will add new columns and tables, but will never drop obsolete columns or adjust existing constraint definitions.
   - For production stability, a database migration tool such as **Flyway** or **Liquibase** should be adopted.
2. **Missing Connection Pool Tuning:**
   - HikariCP settings (`maximumPoolSize`, `minimumIdle`, `connectionTimeout`) are not explicitly configured.
3. **Missing Dialect Specification:**
   - Hibernate dialect is not explicitly set; relies on auto-detection from JDBC metadata.

---

## 2. Entity & Schema Audit

The application defines 7 JPA entity classes under `com.yaswanth.Alumni_Bridge.entity`:

```text
User ────(1:1)──── Profile
 │
 ├───(1:N)─── Job ────(1:N)─── Application ───(N:1)─── User
 │
 ├───(1:N)─── Event ────(1:N)─── EventRegistration ───(N:1)─── User
 │
 └───(1:N)─── Message ───(N:1)─── User
```

---

### 2.1 Entity: `User` (`users` table)

| Field | Java Type | JPA Annotations | Column Constraints | Audit Evaluation |
|---|---|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue(strategy = IDENTITY)` | `PRIMARY KEY, AUTO_INCREMENT` | Valid |
| `name` | `String` | None | `VARCHAR(255), NULL` | Should be `NOT NULL` |
| `email` | `String` | None | `VARCHAR(255), NULL` | **CRITICAL DEFECT: Missing UNIQUE constraint** |
| `password` | `String` | None | `VARCHAR(255), NULL` | **CRITICAL DEFECT: Stores plaintext** |
| `role` | `String` | None | `VARCHAR(255), NULL` | Should be Enum (`STUDENT`, `ALUMNI`, `ADMIN`) |
| `profile` | `Profile` | `@OneToOne(mappedBy = "user", cascade = ALL)` | Bi-directional mapping | Valid |
| `profileImage`| `String` | None | `VARCHAR(255), NULL` | Stores raw filename string |
| `blocked` | `Boolean` | `@Column(nullable = false)` | `TINYINT(1) NOT NULL DEFAULT 0` | Valid |

#### Key Issues:
1. **No Unique Email Constraint:** Multiple accounts can register with the same email. This breaks `findByEmail()` and corrupts user identification.
2. **Missing Audit Timestamps:** No `@CreationTimestamp` or `@UpdateTimestamp`.
3. **No Cascade on Associations:** Deleting a user with posted jobs, events, applications, or messages will trigger an SQL foreign key violation.

---

### 2.2 Entity: `Profile` (`profile` table)

| Field | Java Type | JPA Annotations | Column Constraints | Audit Evaluation |
|---|---|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue(strategy = IDENTITY)` | `PRIMARY KEY, AUTO_INCREMENT` | Valid |
| `skills` | `String` | None | `VARCHAR(255), NULL` | Too short for detailed skill sets |
| `company` | `String` | None | `VARCHAR(255), NULL` | Valid |
| `experience`| `String` | None | `VARCHAR(255), NULL` | Should specify format or years |
| `bio` | `String` | None | `VARCHAR(255), NULL` | Should be `TEXT` or `VARCHAR(2000)` |
| `user` | `User` | `@OneToOne @JoinColumn(name = "user_id")` | `FOREIGN KEY (user_id) REFERENCES users(id)` | Valid |

#### Key Issues:
1. **Missing Educational Fields:** Entirely lacks Degree, Major, Batch/Graduation Year, and Roll Number, which are foundational for an Alumni portal.
2. **Missing Contact & Social Fields:** Lacks Phone Number, LinkedIn URL, GitHub URL, and City/State.

---

### 2.3 Entity: `Job` (`job` table)

| Field | Java Type | JPA Annotations | Column Constraints | Audit Evaluation |
|---|---|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue(strategy = IDENTITY)` | `PRIMARY KEY, AUTO_INCREMENT` | Valid |
| `title` | `String` | None | `VARCHAR(255), NULL` | Should be `NOT NULL` |
| `company` | `String` | None | `VARCHAR(255), NULL` | Should be `NOT NULL` |
| `description` | `String` | `@Column(columnDefinition = "TEXT")` | `TEXT, NULL` | Valid |
| `location` | `String` | None | `VARCHAR(255), NULL` | Valid |
| `postedBy` | `User` | `@ManyToOne @JoinColumn(name = "posted_by")` | `FOREIGN KEY (posted_by) REFERENCES users(id)` | Valid |

#### Key Issues:
1. **Missing Job Details:** Lacks salary/stipend, employment type, application deadline, and job status (`ACTIVE`/`CLOSED`).
2. **No Deletion Cascades:** Deleting a job with linked applications throws a foreign key constraint violation.

---

### 2.4 Entity: `Application` (`application` table)

| Field | Java Type | JPA Annotations | Column Constraints | Audit Evaluation |
|---|---|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue(strategy = IDENTITY)` | `PRIMARY KEY, AUTO_INCREMENT` | Valid |
| `job` | `Job` | `@ManyToOne @JoinColumn(name = "job_id")` | `FOREIGN KEY (job_id) REFERENCES job(id)` | Valid |
| `user` | `User` | `@ManyToOne @JoinColumn(name = "user_id")` | `FOREIGN KEY (user_id) REFERENCES users(id)` | Valid |
| `status` | `String` | None | `VARCHAR(255), NULL` | Hardcoded string `APPLIED` |

#### Key Issues:
1. **Missing Unique Constraint:** No `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "user_id"}))`. Relies solely on application-level checks, risking duplicate applications under race conditions.
2. **Missing Application Metadata:** Lacks applied date, resume URL/attachment, and applicant cover note.

---

### 2.5 Entity: `Event` (`event` table)

| Field | Java Type | JPA Annotations | Column Constraints | Audit Evaluation |
|---|---|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue(strategy = IDENTITY)` | `PRIMARY KEY, AUTO_INCREMENT` | Valid |
| `title` | `String` | None | `VARCHAR(255), NULL` | Should be `NOT NULL` |
| `description`| `String` | None | `VARCHAR(255), NULL` | Should be `TEXT` |
| `location` | `String` | None | `VARCHAR(255), NULL` | Valid |
| `date` | `LocalDate` | None | `DATE, NULL` | Valid date, but lacks time |
| `createdBy` | `User` | `@ManyToOne` | `FOREIGN KEY (created_by_id) REFERENCES users(id)` | Valid |

#### Key Issues:
1. **No Time Field:** Missing event start/end time.
2. **No Event Mode:** Missing virtual meeting link or physical capacity attributes.
3. **Cascade Failure:** Deleting an event that has registrations throws a foreign key constraint violation.

---

### 2.6 Entity: `EventRegistration` (`event_registration` table)

| Field | Java Type | JPA Annotations | Column Constraints | Audit Evaluation |
|---|---|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue(strategy = IDENTITY)` | `PRIMARY KEY, AUTO_INCREMENT` | Valid |
| `user` | `User` | `@ManyToOne` | `FOREIGN KEY (user_id) REFERENCES users(id)` | Valid |
| `event` | `Event` | `@ManyToOne` | `FOREIGN KEY (event_id) REFERENCES event(id)` | Valid |

#### Key Issues:
1. **Missing Unique Constraint:** Lacks unique composite constraint on `(user_id, event_id)` at the database level.
2. **Missing Registration Date:** Lacks timestamp recording when registration occurred.

---

### 2.7 Entity: `Message` (`message` table)

| Field | Java Type | JPA Annotations | Column Constraints | Audit Evaluation |
|---|---|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue(strategy = IDENTITY)` | `PRIMARY KEY, AUTO_INCREMENT` | Valid |
| `sender` | `User` | `@ManyToOne @JoinColumn(name = "sender_id")`| `FOREIGN KEY (sender_id) REFERENCES users(id)` | Valid |
| `receiver` | `User` | `@ManyToOne @JoinColumn(name = "receiver_id")`| `FOREIGN KEY (receiver_id) REFERENCES users(id)` | Valid |
| `content` | `String` | None | `VARCHAR(255), NULL` | Limits messages to 255 chars |
| `timestamp` | `LocalDateTime` | None | `DATETIME(6), NULL` | Valid |
| `seen` | `boolean` | None | `TINYINT(1) NOT NULL DEFAULT 0` | Valid |

#### Key Issues:
1. **Content Length Limitation:** `VARCHAR(255)` truncates or fails on longer messages; should be `TEXT` or `VARCHAR(2000)`.
2. **Missing Indexes on Chat Lookups:** The query for conversations searches `sender_id` and `receiver_id` ordered by `timestamp`. Lacks a composite index on `(sender_id, receiver_id, timestamp)`.

---

## 3. Repositories & DAO Layer Audit

### 3.1 Misplaced `@Service` Annotations on Repository Interfaces
In `EventRepository.java` and `EventRegistrationRepository.java`, the interfaces are annotated with `@Service`:

```java
// Line 10 in EventRepository.java:
@Service
public interface EventRepository extends JpaRepository<Event, Long> { ... }

// Line 12 in EventRegistrationRepository.java:
@Service
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> { ... }
```

**Evaluation:** Misplaced stereotype annotation. In Spring Data JPA, repository interfaces should be annotated with `@Repository` (or left without annotation as Spring Data auto-detects them). Applying `@Service` to a database interface is an architectural anti-pattern.

---

### 3.2 Inefficient In-Memory Querying vs. Database Projections

| Repository | Current In-Memory Pattern | Recommended Database Optimization |
|---|---|---|
| `UserRepository` | Fetches all users, filters out current user and `ADMIN` using Java Streams. | `List<User> findByIdNotAndRoleNot(Long id, String role)` or JPQL query. |
| `JobRepository` | Only has `findAll()`. | Add `findByTitleContainingIgnoreCaseOrCompanyContainingIgnoreCase(...)` with `Pageable`. |
| `EventRepository` | `EventController` fetches all events and filters with stream to find one event. | Use built-in `findById(Long id)`. |
| `DashboardController` | Calls `.findAll().size()` across 4 repositories to calculate count cards. | Call `userRepo.count()`, `jobRepo.count()`, `eventRepo.count()`, etc. |

---

## 4. Query Performance & N+1 Query Traps

### 4.1 The View-Layer N+1 Query Trap in `jobs.html`
In `jobs.html`, Thymeleaf loops through each job and calls the Spring bean directly:

```html
<div th:if="${!@applicationService.alreadyApplied(session.loggedUser, job)}">
```

For a list of 50 jobs, Hibernate executes:
1. 1 query to fetch all jobs: `SELECT * FROM job`.
2. 50 individual queries to check application status:
   `SELECT count(*) FROM application WHERE user_id = ? AND job_id = ?` (executed 50 times).
Total: **51 database roundtrips** for a single page view!

**Remediation:** In `JobController`, query the database for all applied job IDs for the current user in a single query:
`Set<Long> appliedJobIds = appRepo.findAppliedJobIdsByUser(user);`
Pass `appliedJobIds` to the model and evaluate with `th:if="${appliedJobIds.contains(job.id)}"`.

### 4.2 The View-Layer N+1 Query Trap in `events.html`
Similarly, in `events.html`:

```html
<div th:if="${!eventService.isRegistered(session.loggedUser, event)}">
```

Executes an independent `existsByUserAndEvent` query for every event on the page.

**Remediation:** Preload registered event IDs in the controller into a `Set<Long>` in one query.
