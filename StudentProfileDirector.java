package college.model;

/**
 * BUILDER pattern - the (optional) Director.
 * Encapsulates common recipes so callers do not repeat the same builder calls.
 */
public final class StudentProfileDirector {
    private StudentProfileDirector() { }

    /** Day scholar, no scholarship. */
    public static Student regular(String roll, String name, String dept, int sem) {
        return new Student.Builder(roll, name, dept, sem).build();
    }

    /** Hostel resident with a scholarship. */
    public static Student hostelScholar(String roll, String name, String dept, int sem, double scholarship) {
        return new Student.Builder(roll, name, dept, sem)
                .hostelRequired(true)
                .scholarshipPercent(scholarship)
                .build();
    }
}
