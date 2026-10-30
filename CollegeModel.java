package college.model;

import college.mediator.Participant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MVC - the MODEL. Owns all college data (students, courses, attendance,
 * marks, fees, timetables) and the business rules for changing it.
 * It knows nothing about the console or the controller.
 */
public class CollegeModel {
    private static final double BASE_TUITION = 60000;
    private static final double HOSTEL_FEE = 25000;

    private final Map<String, Student> students = new LinkedHashMap<>();
    private final Map<String, Faculty> faculty = new LinkedHashMap<>();
    private final Map<String, Course> courses = new LinkedHashMap<>();
    private final Map<String, TimetableConfig> timetables = new LinkedHashMap<>();
    private final Map<String, Set<String>> enrollments = new LinkedHashMap<>();      // course -> roll numbers
    private final Map<String, Map<String, Double>> marks = new LinkedHashMap<>();      // course -> roll -> %
    private final Map<String, Map<String, int[]>> attendance = new LinkedHashMap<>();  // roll -> course -> {attended,total}
    private final Map<String, FeeAccount> fees = new LinkedHashMap<>();
    private Administrator admin;

    // ---------------------------------------------------------------- people
    public void setAdmin(Administrator admin) { this.admin = admin; }
    public Administrator getAdmin() { return admin; }

    public void addFaculty(Faculty f) { faculty.put(f.getId(), f); }

    public void addStudent(Student s) {
        if (students.containsKey(s.getId())) throw new IllegalArgumentException("Roll number already exists: " + s.getId());
        students.put(s.getId(), s);
        double tuition = BASE_TUITION * (1 - s.getScholarshipPercent() / 100.0);
        fees.put(s.getId(), new FeeAccount(tuition + (s.isHostelRequired() ? HOSTEL_FEE : 0)));
    }

    public Student requireStudent(String roll) {
        Student s = students.get(roll);
        if (s == null) throw new IllegalArgumentException("No student with roll number " + roll);
        return s;
    }

    public Faculty requireFaculty(String id) {
        Faculty f = faculty.get(id);
        if (f == null) throw new IllegalArgumentException("No faculty with id " + id);
        return f;
    }

    public Participant findParticipant(String id) {
        if (students.containsKey(id)) return students.get(id);
        if (faculty.containsKey(id)) return faculty.get(id);
        if (admin != null && admin.getId().equals(id)) return admin;
        throw new IllegalArgumentException("No participant with id " + id);
    }

    public Collection<Student> getStudents() { return students.values(); }
    public Collection<Faculty> getFaculty() { return faculty.values(); }

    // ---------------------------------------------------------------- courses
    public void addCourse(Course c) {
        if (courses.containsKey(c.getCode())) throw new IllegalArgumentException("Course code already exists: " + c.getCode());
        courses.put(c.getCode(), c);
    }

    public Course requireCourse(String code) {
        Course c = courses.get(code);
        if (c == null) throw new IllegalArgumentException("No course with code " + code);
        return c;
    }

    public Collection<Course> getCourses() { return courses.values(); }

    public void enroll(String roll, String courseCode) {
        Student s = requireStudent(roll);
        Course c = requireCourse(courseCode);
        Set<String> enrolled = enrollments.computeIfAbsent(courseCode, k -> new LinkedHashSet<>());
        if (enrolled.contains(roll)) throw new IllegalStateException(roll + " is already enrolled in " + courseCode);
        if (enrolled.size() >= c.getMaxSeats()) throw new IllegalStateException(courseCode + " is full");
        enrolled.add(roll);
        s.enrollIn(courseCode);
    }

    public List<Student> enrolledStudents(String courseCode) {
        List<Student> list = new ArrayList<>();
        for (String roll : enrollments.getOrDefault(courseCode, Set.of())) list.add(students.get(roll));
        return list;
    }

    public int enrolledCount(String courseCode) {
        return enrollments.getOrDefault(courseCode, Set.of()).size();
    }

    private void requireEnrolled(String roll, String courseCode) {
        requireStudent(roll);
        requireCourse(courseCode);
        if (!enrollments.getOrDefault(courseCode, Set.of()).contains(roll))
            throw new IllegalStateException(roll + " is not enrolled in " + courseCode);
    }

    // ------------------------------------------------------------ timetables
    public void addTimetable(TimetableConfig t) {
        if (timetables.containsKey(t.getName())) throw new IllegalArgumentException("Timetable already exists: " + t.getName());
        timetables.put(t.getName(), t);
    }

    public Collection<TimetableConfig> getTimetables() { return timetables.values(); }

    // ------------------------------------------------------------ attendance
    public void markAttendance(String roll, String courseCode, boolean present) {
        requireEnrolled(roll, courseCode);
        int[] a = attendance.computeIfAbsent(roll, k -> new LinkedHashMap<>()).computeIfAbsent(courseCode, k -> new int[2]);
        a[1]++;
        if (present) a[0]++;
    }

    public double attendancePercent(String roll, String courseCode) {
        int[] a = attendance.getOrDefault(roll, Map.of()).get(courseCode);
        return (a == null || a[1] == 0) ? 0 : 100.0 * a[0] / a[1];
    }

    // ----------------------------------------------------------------- marks
    public void recordMarks(String roll, String courseCode, double percentage) {
        requireEnrolled(roll, courseCode);
        if (percentage < 0 || percentage > 100) throw new IllegalArgumentException("Marks must be between 0 and 100");
        marks.computeIfAbsent(courseCode, k -> new LinkedHashMap<>()).put(roll, percentage);
    }

    public Double getMarks(String roll, String courseCode) {
        return marks.getOrDefault(courseCode, Map.of()).get(roll);
    }

    public double classAverage(String courseCode) {
        Collection<Double> values = marks.getOrDefault(courseCode, Map.of()).values();
        return values.isEmpty() ? 0 : values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    // ------------------------------------------------------------------ fees
    public FeeAccount getFees(String roll) {
        requireStudent(roll);
        return fees.get(roll);
    }

    public List<Student> studentsWithDues() {
        List<Student> list = new ArrayList<>();
        for (Student s : students.values()) if (fees.get(s.getId()).due() > 0) list.add(s);
        return list;
    }
}
