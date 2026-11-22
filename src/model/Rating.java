package model;

import util.FileHandler;
import util.Validator;

import java.time.LocalDate;

/**
 * A student's rating and feedback for a class they took (owner: S3, Student).
 *
 * ratings.txt: ratingId | studentId | classId | instructorId | stars | comment | ratingDate
 */
public class Rating extends Record {

    public static final int MIN_STARS = 1;
    public static final int MAX_STARS = 5;
    public static final int MAX_COMMENT = 200;

    private final String studentId;
    private final String classId;
    private final String instructorId;
    private int stars;
    private String comment;
    private final LocalDate ratingDate;

    public Rating(String ratingId, String studentId, String classId, String instructorId,
                  int stars, String comment, LocalDate ratingDate) {
        super(ratingId);
        this.studentId = Validator.requireText(studentId, "Student ID", 10);
        this.classId = Validator.requireText(classId, "Class ID", 10);
        this.instructorId = Validator.requireText(instructorId, "Instructor ID", 10);
        if (ratingDate == null) {
            throw new IllegalArgumentException("Rating date is required.");
        }
        this.ratingDate = ratingDate;
        setStars(stars);
        setComment(comment);
    }

    public static Rating fromFileLine(String line) {
        String[] f = FileHandler.split(line);
        if (f.length < 7) {
            throw new IllegalArgumentException("ratings.txt line needs 7 fields");
        }
        LocalDate date = Validator.parseDate(f[6]);
        if (date == null) {
            throw new IllegalArgumentException("Bad date: " + f[6]);
        }
        String comment = FileHandler.EMPTY_FIELD.equals(f[5].trim()) ? "" : f[5];
        return new Rating(f[0].trim(), f[1].trim(), f[2].trim(), f[3].trim(),
                Integer.parseInt(f[4].trim()), comment, date);
    }

    @Override
    public String toFileLine() {
        return FileHandler.join(getId(), studentId, classId, instructorId, stars, comment, ratingDate);
    }

    public String getStudentId() { return studentId; }
    public String getClassId() { return classId; }
    public String getInstructorId() { return instructorId; }
    public int getStars() { return stars; }
    public String getComment() { return comment; }
    public LocalDate getRatingDate() { return ratingDate; }

    public final void setStars(int stars) {
        if (stars < MIN_STARS || stars > MAX_STARS) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars.");
        }
        this.stars = stars;
    }

    public final void setComment(String comment) {
        String c = comment == null ? "" : comment.trim().replace('\n', ' ').replace('\r', ' ');
        if (Validator.hasPipe(c)) {
            throw new IllegalArgumentException("Comment must not contain the | character.");
        }
        if (c.length() > MAX_COMMENT) {
            throw new IllegalArgumentException("Comment must be at most " + MAX_COMMENT + " characters.");
        }
        this.comment = c;
    }
}
