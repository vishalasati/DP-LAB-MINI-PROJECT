package college.model;

import college.mediator.Participant;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * BUILDER pattern - a student registration profile has a few mandatory fields
 * and many optional ones. The Builder avoids a telescoping constructor and
 * validates everything in build().
 */
public class Student extends Participant {
    private final String department;
    private final int semester;
    private final String email;
    private final String phone;
    private final String guardianName;
    private final boolean hostelRequired;
    private final double scholarshipPercent;
    private final Set<String> enrolledCourses = new LinkedHashSet<>();

    private Student(Builder b) {
        super(b.rollNo, b.name, Participant.STUDENT);
        this.department = b.department;
        this.semester = b.semester;
        this.email = b.email;
        this.phone = b.phone;
        this.guardianName = b.guardianName;
        this.hostelRequired = b.hostelRequired;
        this.scholarshipPercent = b.scholarshipPercent;
    }

    public String getDepartment() { return department; }
    public int getSemester() { return semester; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getGuardianName() { return guardianName; }
    public boolean isHostelRequired() { return hostelRequired; }
    public double getScholarshipPercent() { return scholarshipPercent; }
    public Set<String> getEnrolledCourses() { return Collections.unmodifiableSet(enrolledCourses); }

    void enrollIn(String courseCode) {
        enrolledCourses.add(courseCode);
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | Sem %d | hostel=%s | scholarship=%.0f%% | email=%s",
                getId(), getName(), department, semester, hostelRequired ? "yes" : "no",
                scholarshipPercent, email.isEmpty() ? "-" : email);
    }

    // ------------------------------------------------------------------ Builder
    public static class Builder {
        // mandatory
        private final String rollNo;
        private final String name;
        private final String department;
        private final int semester;
        // optional (with defaults)
        private String email = "";
        private String phone = "";
        private String guardianName = "";
        private boolean hostelRequired = false;
        private double scholarshipPercent = 0;

        public Builder(String rollNo, String name, String department, int semester) {
            this.rollNo = rollNo;
            this.name = name;
            this.department = department;
            this.semester = semester;
        }

        public Builder email(String email) { this.email = email; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder guardianName(String guardianName) { this.guardianName = guardianName; return this; }
        public Builder hostelRequired(boolean hostelRequired) { this.hostelRequired = hostelRequired; return this; }
        public Builder scholarshipPercent(double scholarshipPercent) { this.scholarshipPercent = scholarshipPercent; return this; }

        public Student build() {
            if (rollNo == null || rollNo.isBlank()) throw new IllegalArgumentException("Roll number is required");
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
            if (department == null || department.isBlank()) throw new IllegalArgumentException("Department is required");
            if (semester < 1 || semester > 8) throw new IllegalArgumentException("Semester must be between 1 and 8");
            if (!email.isEmpty() && !email.contains("@")) throw new IllegalArgumentException("Invalid email: " + email);
            if (scholarshipPercent < 0 || scholarshipPercent > 100)
                throw new IllegalArgumentException("Scholarship must be between 0 and 100");
            return new Student(this);
        }
    }
}
