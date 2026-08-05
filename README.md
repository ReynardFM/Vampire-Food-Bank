# Vampire-Food-Bank

A Spring Boot web application for managing blood donors and blood requests. Donors register,
complete a profile, and raise requests for the blood group a patient needs. Administrators review
a pending queue, approve or reject requests, and record donations against donor accounts.

## Built with

Java 21 · Spring Boot · Spring MVC · Spring Security · Spring Data JPA · Thymeleaf · MySQL · Maven

## Getting started

### Prerequisites

- **JDK 21** or newer
- **XAMPP** (or any local MySQL server)
- An IDE such as IntelliJ IDEA, or just the bundled Maven wrapper

### Setup

1. Clone the repository and open the `BloodBank` folder as the project root.
2. Start **MySQL** from the XAMPP control panel. The database and tables are created
   automatically on first run — no manual SQL needed.
3. If your MySQL uses a different username or password, update `spring.datasource.username`
   and `spring.datasource.password` in `src/main/resources/application.properties`. The
   defaults assume XAMPP's `root` account with an empty password.

### Run

From your IDE, run the `BloodBankApplication` class. Or from a terminal:

```bash
./mvnw spring-boot:run
```

Then open <http://localhost:8080>.

## Signing in

An empty database is filled with demo data on first start, so there is nothing to set up by
hand. Two accounts to try:

| Role | Email | Password |
| --- | --- | --- |
| Administrator | `admin@lifeline.com` | `Admin123!` |
| Donor | `jane.doe@example.com` | `Password123!` |

**Change the administrator password before showing this to anyone.** All seeded donors share
the same password.

The seed also creates 15 donors covering every blood group, a dozen requests spread across all
four statuses, and roughly a year of donation history — enough that the dashboard, search and
admin queues all have something in them straight away.

It is safe to leave enabled: accounts are matched by email and history is only written once, so
restarting never duplicates anything. To turn it off, or to change the credentials, add to
`application.properties`:

```properties
bloodbank.seed.enabled=false
bloodbank.seed.admin-email=you@example.com
bloodbank.seed.admin-password=your-password
```

## Other settings

Every listing shows 10 rows per page. To change that, set:

```properties
bloodbank.page-size=25
```

## What you can do

**As a donor** — complete your profile, search for donors who can give to a particular blood
group, raise a blood request, and view your own request history.

**As an administrator** — review the pending queue and approve or reject requests, browse every
request regardless of status, read the daily activity reports, manage accounts, and record
donations on a donor's behalf.
Recording a donation updates that donor's history and, if it is linked to an approved request,
marks the request fulfilled.

## Routes

| Path | Access | Purpose |
| --- | --- | --- |
| `/`, `/home`, `/about-us` | Public | Landing and information pages |
| `/login`, `/register` | Public | Authentication |
| `/donor/profile` | Authenticated | Profile and donation history |
| `/donor/profile-edit` | Authenticated | Edit profile |
| `/donor/search` | Authenticated | Find donors who can give to a blood group |
| `/requests/create` | Authenticated | Raise a blood request |
| `/requests/list` | Authenticated | Your own requests |
| `/requests/{id}` | Owner or admin | Request detail |
| `/dashboard` | Admin | Statistics overview |
| `/admin/pending` | Admin | Approve / reject queue |
| `/admin/requests` | Admin | Every request, any status |
| `/admin/donors` | Admin | Account directory |
| `/admin/reports` | Admin | Daily activity, last 30 days |
| `/admin/reports/{date}` | Admin | One day in full, e.g. `2026-08-04` |
| `/donations/record/{donorId}` | Admin | Record a donation |

On sign-in, administrators land on `/dashboard` and everyone else on `/donor/profile`.

Every listing is sortable by clicking a column heading, and pages at 10 rows.

## Project structure

```
BloodBank/src/main/
├── java/com/project/BloodBank/
│   ├── config/       Security configuration and the demo data seeder
│   ├── controller/   Request handling
│   ├── dto/          Form objects and validation
│   ├── exception/    Error handling
│   ├── model/        JPA entities and enums
│   ├── repository/   Database access
│   └── service/      Business logic
└── resources/
    ├── static/       CSS and images
    ├── templates/    Thymeleaf views
    └── application.properties
```

## Tests

```bash
./mvnw test
```

Tests run against an in-memory database, so MySQL does **not** need to be running.
See [`BloodBank/src/test/README.md`](BloodBank/src/test/README.md) for what each test covers.

## Common commands

```bash
./mvnw compile
```

```bash
./mvnw spring-boot:run
```

## Troubleshooting

**The app fails to start with a database connection error.** MySQL isn't running — start it in
the XAMPP control panel.

**A build fails with "release version 21 not supported".** Your `JAVA_HOME` points at an older
JDK. Point it at a Java 21 installation, or run from an IDE configured with a Java 21 SDK.

**Admin pages return a 403.** You're signed in as a donor. Use the administrator account above,
and note that a role change only takes effect after signing out and back in.

**The demo data didn't appear.** History is only seeded when the seeded donor accounts have none
yet. To start completely fresh, drop the `blood_donation` database in phpMyAdmin and restart.
