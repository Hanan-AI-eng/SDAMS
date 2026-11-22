SDAMS

Self Defence Academy Management System

---

# SDAMS – Administrator module (S2)

Java Swing + text files only. Compiles on JDK 8 or newer, with no external libraries.

## How to run

**NetBeans:** File → New Project → Java with Ant → *Java Project with Existing Sources*. Add the `src` folder as the source root, set `Main` as the main class, and keep the project folder as the working directory so the program finds `data/`. Then run.

**Command line (from this folder):**
```
javac -d build $(find src -name "*.java")
java -cp build Main
```
Log in with **siti01 / pass123** (Administrator).

## What is yours (S2) and what is a stand-in

| File | Owner | Notes |
|---|---|---|
| `util/FileHandler.java` | **S2** | Shared utility: read, write, append, find, update and delete by ID, all with safe writes |
| `model/Record.java` | **S2** | Abstract base class for every model class |
| `util/Repository.java` | **S2** | Generic `Repository<T extends Record>` (extra feature) |
| `util/ActivityLog.java` | **S2** | Logs who changed what and when (extra feature) |
| `util/DataFiles.java` | **S2** | File name constants for everyone |
| `model/StudentProfile.java` | **S2** | `student_profiles.txt` |
| `service/AccountService.java` | **S2** | Shared username and search logic (abstract) |
| `service/StaffService.java` | **S2** | Manage Staff |
| `service/StudentService.java` | **S2** | Manage Students (both files) |
| `service/PaymentRecordService.java` | **S2** | Payment Records + export |
| `gui/AdminDashboard.java`, `ManageStaffPanel`, `ManageStudentPanel`, `PaymentRecordsPanel`, `FormPanel` | **S2** | Screens |
| `model/User`, `Role`, `UserFactory`, the 6 role subclasses, `gui/LoginFrame` | stand-in for **S1** | Replace with S1's code. Keep `Administrator.openDashboard()` opening `AdminDashboard` |
| `util/Validator`, `util/IdGenerator` | stand-in for **S5** | Replace with S5's code, keeping the method names or updating the calls |
| `gui/UiStyle`, `gui/BaseDashboardFrame`, `gui/PlaceholderDashboard` | stand-in for **S6** | Replace with S6's shared GUI kit |

## Assumptions (for the report)
1. The Administrator also manages **Attendance Officer** accounts, together with Instructors and Facilities Officers.
2. A staff member's **role cannot be changed** after creation. To change it, delete the account and add it again.
3. When updating, a **blank password keeps the current password**.
4. An **instructor with upcoming classes** cannot be deleted. Upcoming means a class dated today or later whose status is not CANCELLED or COMPLETED.
5. A **student with active bookings** (status BOOKED or RESCHEDULED) cannot be deleted.
6. Adding a student writes users.txt first, then student_profiles.txt. If the second write fails, the first is rolled back.
7. A student shown with "(missing)" profile data can be repaired by selecting the student and clicking Update.
8. Payment Records is **view only**. The total covers all filtered rows, whatever their status.
9. Usernames are 4–20 letters, digits or underscores and are unique across all users, ignoring case. Phone numbers have 10–11 digits and start with 0.

## Where the OOP concepts are
- **Abstraction:** `Record` (abstract `toFileLine()`), `AccountService`, `User.openDashboard()`
- **Inheritance:** model classes extend `Record`. `StaffService` and `StudentService` extend `AccountService`. Panels extend `JPanel` and the dashboard extends `BaseDashboardFrame`
- **Polymorphism:** `FileHandler.save(file, Record)` and `Repository<T>` store any record through `toFileLine()`. The login screen calls `user.openDashboard()`
- **Encapsulation:** private fields with validated setters. All file access is inside `FileHandler`, and the GUI only calls service methods
- **Extras:** generics (`Repository<T extends Record>`), Factory pattern, enums, the `Refreshable` interface

## Test checklist (all passed)
- Add, update and delete an Instructor and a Facilities Officer, checking users.txt after each
- Duplicate username, empty name, bad email, `|` in the name and an invalid phone are all rejected
- Deleting an instructor who has an upcoming class is blocked, and deleting one who has none works
- Adding a student writes both files. Deleting a student removes both lines. Deleting a student with active bookings is blocked
- A future join date and a belt rank that is not in the list are rejected
- Payments can be filtered by student, by date range and with no results (RM 0.00). An invalid range where From is after To is rejected
- FileHandler creates a missing file, and update or delete with an unknown ID returns false

---

# SDAMS – Student module (S3)

Log in with **danielw / stud123** (Student S001). Other students: aisyahr / stud456, kavinr / stud789.

## Features
| Menu | What the student can do |
|---|---|
| Home | Upcoming booked classes, amount still to pay, attendance rate |
| My Profile | Update name, phone, email and emergency contact; change password (current password required) |
| Book a Class | Search upcoming classes by keyword or discipline, see fee and seats left, book a seat |
| My Bookings | Filter by status, cancel a booking, reschedule to another class of the same discipline |
| Payments | Payment history, pay an UNPAID booking (Card, Online banking, E-wallet, Cash), totals paid and owed |
| Attendance | Own attendance records (read-only) and attendance rate |
| Rate a Class | Rate a class they took (1–5 stars + comment), once per class; see past ratings |

## Files (S3)
| File | Notes |
|---|---|
| `model/Booking.java` | `bookings.txt`: bookingId\|studentId\|classId\|bookingDate\|status\|paymentStatus |
| `model/Payment.java` | `payments.txt`: paymentId\|studentId\|bookingId\|classId\|amount\|paymentDate\|method\|status |
| `model/Rating.java` | `ratings.txt`: ratingId\|studentId\|classId\|instructorId\|stars\|comment\|ratingDate |
| `model/BookingStatus`, `PaymentStatus`, `PaymentMethod` | Enums |
| `service/StudentPortalService.java` | Every student rule, in one place. The GUI only calls this class |
| `gui/StudentDashboard.java` + `StudentHomePanel`, `StudentProfilePanel`, `BookClassPanel`, `MyBookingsPanel`, `StudentPaymentsPanel`, `StudentAttendancePanel`, `RateClassPanel`, `StudentUi` | Screens |
| `model/Student.java` | `openDashboard()` now opens `StudentDashboard` |

Reuses S2's `Record`, `Repository<T>`, `FileHandler`, `ActivityLog`, `IdGenerator`, the Instructor part's `ClassService`/`TrainingClass` and the Attendance part's `AttendanceService`.
Also added the missing `import service.AttendanceService;` to `model/AttendanceReport.java` (the project did not compile without it) and five sample martial-arts classes (TC003–TC007) to `classes.txt` for testing.

## Business rules (assumptions for the report)
1. Only classes with status OPEN or ACTIVE and a date **after today** can be booked.
2. A student cannot book the same class twice, a full class, or a class that **overlaps in time** with another class they booked.
3. Booking creates the booking (BOOKED, UNPAID) and an UNPAID payment for the class fee. If the payment cannot be saved, the booking is rolled back.
4. Bookings can be cancelled or rescheduled **up to the day before** the class.
5. Cancelling a **paid** booking marks it and its payment REFUNDED. Cancelling an **unpaid** booking removes the pending payment.
6. Rescheduling is only to a class of the **same discipline** with free seats. A paid booking can only move to a class with the **same fee**; an unpaid booking's amount is updated to the new fee.
7. A student can only rate a class they attended (PRESENT/LATE) or a booked class that has already taken place, **once per class**. Stars 1–5, comment up to 200 characters, no `|`.
8. Username, discipline, belt rank and join date are managed by staff; the student cannot change them.
9. Students can only see and change their own bookings, payments and ratings.

## OOP concepts (S3)
- **Inheritance:** `Booking`, `Payment`, `Rating` extend `Record`; all panels extend `JPanel`; `StudentDashboard` extends `BaseDashboardFrame`
- **Encapsulation:** private fields with validated setters; file access only through `Repository`/`FileHandler`
- **Polymorphism:** `Student.openDashboard()` overrides `User.openDashboard()`; `Repository<T>` saves any record via `toFileLine()`; panels implement `Refreshable`
- **Abstraction:** the GUI never touches files, only `StudentPortalService`
- **Extras:** enums with behaviour (`BookingStatus.isActive()`), inner view classes, generics, rollback on failed writes, activity logging

## Test checklist (all passed)
- Search by keyword, discipline and instructor name; cancelled and past classes are hidden
- Book a class; double booking, full class, time clash, cancelled, past and unknown classes are blocked
- Pay an unpaid booking; paying twice or paying another student's payment is blocked; totals update
- Reschedule a paid booking to a same-fee class works; different fee and different discipline are blocked
- Cancel a paid booking → REFUNDED; cancel an unpaid booking → pending payment removed; cancelling twice is blocked
- Rate a taken class; 9 stars, `|` in the comment, rating twice and rating a class not taken are blocked
- Profile: bad phone, duplicate email and `|` in the name are rejected; valid changes saved
- Password: wrong current password, mismatched confirmation and too-short password are rejected
