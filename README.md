# Smart Healthcare Portal – Hospital Management System

An object-oriented, thread-safe desktop application for hospital administration, built with **Java Swing** and **SQLite (JDBC)**.

**Course:** JAVA  |  **Institution:** Madras Institute of Technology, Anna University

## Features
- **Patient Management** – register patients with validation; saved to the database
- **Doctor Directory** – add doctors with specialization and consultation fee
- **Appointment Scheduler** – `synchronized` booking that prevents double-booking
- **Billing** – asynchronous invoice thread (fee + 18% tax) with a live log console

## Tech Stack
Java 11+, Swing, SQLite, JDBC (`sqlite-jdbc`), `java.time`, Draw.io

## Java Concepts Used
Abstract class (`Person`), inheritance, interface (`Billable`), custom exceptions, multithreading (`Runnable`), synchronization, collections, lambdas, autoboxing, try-with-resources.

## Database Tables
| Table | Purpose |
|---|---|
| PATIENTS | patient_id, full_name, age, date_of_birth |
| DOCTORS | doctor_id, name, specialization, fee |
| APPOINTMENTS | appointment_id, appointment_time, patient_id |
| BILLING | bill_id, patient_id, total_amount |

The database file `hospital_system.db` is created automatically on first run.

## How to Run
1. Install JDK 11+ and download the [sqlite-jdbc JAR](https://github.com/xerial/sqlite-jdbc/releases) (put it in `lib/`).
2. Compile:
   ```bash
   javac -cp "lib/*" HospitalManagementSystem.java
   ```
3. Run (Linux/macOS use `:`; Windows use `;`):
   ```bash
   java -cp ".:lib/*" HospitalManagementSystem
   ```

## Project Structure
```
├── HospitalManagementSystem.java
├── lib/sqlite-jdbc.jar
├── docs/   (report, PPT, diagrams)
└── README.md
```

## Diagrams
Draw.io (DFD Level 0, 1, 2 and Use Case Diagram): (https://drive.google.com/file/d/1JS6fXfndkhKD8LR4teVQX-KH9FCR0WAD/view?usp=sharing)

## Team
| Name | Role |
|---|---|
| **Sudip Mukherjee** | Team Lead & Full-Stack Developer – architecture, OOP design, JDBC, Swing GUI, multithreading, integration |
| Kishor | Requirements & Database Support – requirement gathering, table design review, database testing |
| Rajesh | GUI Testing & Validation – form testing, input validation, error dialogs |
| Nitheesh | Documentation & Diagrams – report, DFD and use case diagram support |
| MD Huzaifa | Testing & Demo Support – appointment and billing test cases, demo, bug reporting |

## Future Scope
Persist appointments/bills, load data on startup, role-based login, search/edit/delete, PDF export.
