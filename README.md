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
| Testing | JUnit 5 |
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

Known limitations I plan to address:

- Replace unsalted SHA-256 password hashing with BCrypt.
- Rewrite the unit tests to exercise `BookingService` and the data layer directly, using an
  in-memory SQLite database, instead of re-implementing the rules inside the tests.
- Rename the `com.project.coursework2` package and move SQL out of the controllers.

## Team

Developed by a five-person student team; see the commit history for individual
contributions.
