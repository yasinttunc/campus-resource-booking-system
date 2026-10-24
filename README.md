# Campus Resource Booking System (CRBAS)

[![CI](https://github.com/yasinttunc/campus-resource-booking-system/actions/workflows/ci.yml/badge.svg)](https://github.com/yasinttunc/campus-resource-booking-system/actions/workflows/ci.yml)

A JavaFX desktop application for booking university study rooms, labs and equipment.
Students, staff and administrators each get a role-specific workspace, and every booking
request is checked against permissions, quotas, maintenance windows and existing bookings
before it is accepted.

Built as a team project at Swansea University (MSc Computer Science, 2025–26).
I led the team, coordinated the Git workflow (feature branches, pull requests, protected
`main`) and worked on the database layer, booking rules and integration.

## Features

- **Role-based access** – Student, Staff and Admin roles. Each resource declares the minimum
  role required to book it (for example, some labs are staff-only).
- **Booking validation** – a request is rejected if it is in the past, the resource is
  inactive, the user's role is too low, the user has reached their active-booking quota,
  the slot overlaps a maintenance window, or it overlaps an existing booking.
- **Availability grid** – per-resource view of free, booked and maintenance slots.
- **Admin panel** – manage users and resources, review bookings and move them through
  `pending → confirmed / cancelled → completed`.
- **Account management** – profile updates, password changes and account deletion.
- **Password security** – passwords are stored as salted PBKDF2-HMAC-SHA256 hashes
  (600,000 iterations) and compared in constant time. Hashes from the original SHA-256
  version are still accepted and upgraded automatically on the next successful login.
- **Relational data model** – 12 SQLite tables with foreign keys, `CHECK` constraints and
  indexes; resource subtypes (study room, lab, equipment) and user subtypes (student, staff,
  admin) are modelled as separate tables keyed on the parent ID.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| UI | JavaFX 21 (FXML + CSS), ControlsFX |
| Persistence | SQLite via JDBC (`sqlite-jdbc`) |
| Build | Maven (wrapper included) |
| Security | PBKDF2-HMAC-SHA256 (JDK `javax.crypto`) |
| Testing | JUnit 5 with a temporary SQLite database |
| CI | GitHub Actions |

## Architecture

```
FXML views ──> controller/   JavaFX controllers, session handling
                   │
                   ▼
               service/      booking rules, availability, user operations
                   │
                   ▼
               data/         JDBC data-access classes (one per aggregate)
                   │
                   ▼
               SQLite        schema + demo data in src/main/resources/db
```

## Getting started

Requirements: JDK 21. No database setup is needed.

```bash
git clone https://github.com/yasinttunc/campus-resource-booking-system.git
cd campus-resource-booking-system
./mvnw javafx:run          # Windows: mvnw.cmd javafx:run
```

On the first start the application creates `university_booking.db` in the working
directory from [`schema.sql`](src/main/resources/db/schema.sql) and loads the synthetic demo
data in [`seed.sql`](src/main/resources/db/seed.sql). Delete the file to reset the data.

### Demo accounts

| Role | Email | Password |
|---|---|---|
| Admin | `admin` | `admin` |
| Staff | `sarah.mitchell@university.ac.uk` | `sarah123` |
| Student | `emily.carter@student.university.ac.uk` | `emily123` |

All names and accounts are fictional.

## Running the tests

```bash
./mvnw test
```

The tests run against a temporary SQLite database built from the real schema and seed
scripts, so they exercise the production code rather than copies of it:

- `BookingServiceTest` – valid bookings, past dates, inactive resources, role
  restrictions, overlapping vs. adjacent slots, maintenance windows, booking quotas and
  cancellation freeing a slot.
- `UserLoginTest` – login by email or student ID, wrong passwords, and the automatic
  upgrade of legacy SHA-256 hashes.
- `PasswordUtilsTest` – salting, verification and malformed stored values.

## Project structure

```
src/main/java/com/project/coursework2/
├── controller/   JavaFX controllers (login, home, resources, bookings, admin, account)
├── service/      BookingService, AvailabilityService, ResourceService, UserService
├── data/         DatabaseConnection, DatabaseInitializer and JDBC managers
├── model/        domain classes and enums (Booking, Resource, Role, BookingStatus, ...)
└── gui/          programmatic page helpers
src/main/resources/
├── com/project/coursework2/   FXML views and stylesheet
└── db/                        schema.sql and seed.sql
```

## Roadmap

- Rename the `com.project.coursework2` package and move the remaining SQL out of the
  controllers into the data layer.
- Replace static data-access classes with injected repositories to simplify testing.

## Team

Developed by a five-person student team; see the commit history for individual
contributions.
