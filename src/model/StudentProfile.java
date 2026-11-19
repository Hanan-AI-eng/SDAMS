package model;

import util.FileHandler;
import util.Validator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Extra student data kept in student_profiles.txt (owner: S2).
 * Login data for the same student stays in users.txt with the same ID.
 *
 * student_profiles.txt: studentId | discipline | beltRank | dateJoined | emergencyContact
 */
public class StudentProfile extends Record {

    public static final List<String> DISCIPLINES = Arrays.asList(
            "Karate", "Taekwondo", "Judo", "Silat", "Muay Thai", "Brazilian Jiu-Jitsu", "Boxing");

    public static final List<String> BELT_RANKS = Arrays.asList(
            "White", "Yellow", "Orange", "Green", "Blue", "Purple", "Brown", "Black");

    private String discipline;
    private String beltRank;
    private LocalDate dateJoined;
    private String emergencyContact;

    public StudentProfile(String studentId, String discipline, String beltRank,
                          LocalDate dateJoined, String emergencyContact) {
        super(studentId);
        if (!studentId.startsWith(Role.STUDENT.getIdPrefix())) {
            throw new IllegalArgumentException("A student profile needs an S### ID.");
        }
        setDiscipline(discipline);
        setBeltRank(beltRank);
        setDateJoined(dateJoined);
        setEmergencyContact(emergencyContact);
    }

    public static StudentProfile fromFileLine(String line) {
        String[] f = FileHandler.split(line);
        if (f.length < 5) {
            throw new IllegalArgumentException("student_profiles.txt line needs 5 fields");
        }
        LocalDate joined = Validator.parseDate(f[3]);
        if (joined == null) {
            throw new IllegalArgumentException("Bad date: " + f[3]);
        }
        return new StudentProfile(f[0].trim(), f[1].trim(), f[2].trim(), joined, f[4].trim());
    }

    @Override
    public String toFileLine() {
        return FileHandler.join(getId(), discipline, beltRank, dateJoined, emergencyContact);
    }

    public String getDiscipline() { return discipline; }
    public String getBeltRank() { return beltRank; }
    public LocalDate getDateJoined() { return dateJoined; }
    public String getEmergencyContact() { return emergencyContact; }

    public final void setDiscipline(String discipline) {
        if (!DISCIPLINES.contains(discipline)) {
            throw new IllegalArgumentException("Please choose a discipline from the list.");
        }
        this.discipline = discipline;
    }

    public final void setBeltRank(String beltRank) {
        if (!BELT_RANKS.contains(beltRank)) {
            throw new IllegalArgumentException("Please choose a belt rank from the list.");
        }
        this.beltRank = beltRank;
    }

    public final void setDateJoined(LocalDate dateJoined) {
        if (dateJoined == null) {
            throw new IllegalArgumentException("Join date must be a real date in yyyy-MM-dd format.");
        }
        if (dateJoined.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Join date cannot be in the future.");
        }
        this.dateJoined = dateJoined;
    }

    public final void setEmergencyContact(String emergencyContact) {
        if (!Validator.isValidPhone(emergencyContact)) {
            throw new IllegalArgumentException(
                    "Emergency contact must be a phone number of 10-11 digits starting with 0.");
        }
        this.emergencyContact = emergencyContact.trim();
    }
}
