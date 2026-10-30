package college.view;

import college.model.Course;
import college.model.CollegeModel;
import college.model.GradeRow;
import college.model.Student;
import college.model.TimetableConfig;

import java.util.Collection;
import java.util.List;
import java.util.Scanner;

/**
 * MVC - the VIEW. Only prints and reads text. It contains no business logic
 * and never changes the model.
 */
public class ConsoleView {
    private final Scanner in = new Scanner(System.in);

    // ------------------------------------------------------------------ input
    public String ask(String label) {
        System.out.print(label + ": ");
        return in.nextLine().trim(); // NoSuchElementException on EOF is handled by the controller
    }

    public int askInt(String label) {
        String text = ask(label);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is not a whole number");
        }
    }

    public double askDouble(String label) {
        String text = ask(label);
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is not a number");
        }
    }

    public boolean askYesNo(String label) {
        return ask(label + " (y/n)").equalsIgnoreCase("y");
    }

    // ----------------------------------------------------------------- output
    public void println(String text) {
        System.out.println(text);
    }

    public void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    public void showMenu() {
        header("MAIN MENU");
        println(" 1. Register student                 [Builder]");
        println(" 2. Create custom course             [Builder]");
        println(" 3. Clone course from template       [Prototype]");
        println(" 4. Clone timetable configuration    [Prototype]");
        println(" 5. Enroll student in course");
        println(" 6. Mark attendance");
        println(" 7. Record marks");
        println(" 8. Grade report                     [Strategy]");
        println(" 9. Pay fees");
        println("10. Send notification                [Mediator]");
        println("11. View inbox                       [Mediator]");
        println("12. List students / courses / timetables");
        println(" 0. Exit");
    }

    public void showStudents(Collection<Student> students) {
        header("STUDENTS");
        if (students.isEmpty()) println("(none)");
        for (Student s : students) println(s + " | courses=" + s.getEnrolledCourses());
    }

    public void showCourses(Collection<Course> courses, CollegeModel model) {
        header("COURSES");
        if (courses.isEmpty()) println("(none)");
        for (Course c : courses) {
            println(c + " | enrolled=" + model.enrolledCount(c.getCode()));
            println("      topics=" + c.getTopics() + "  weights=" + c.getAssessmentWeights());
        }
    }

    public void showTimetable(TimetableConfig t) {
        header("TIMETABLE: " + t.getName() + " (" + t.getTerm() + ")");
        if (t.getSlots().isEmpty()) println("(no slots)");
        t.getSlots().forEach((slot, entry) -> println(String.format("  %-10s %s", slot, entry)));
    }

    public void showGradeReport(String courseCode, String policyName, double classAverage, List<GradeRow> rows) {
        header("GRADE REPORT " + courseCode + " - " + policyName);
        println(String.format("Class average: %.1f%%", classAverage));
        println(String.format("%-8s %-20s %8s %7s %7s", "Roll", "Name", "Marks %", "Grade", "Points"));
        if (rows.isEmpty()) println("(no marks recorded)");
        for (GradeRow r : rows) {
            println(String.format("%-8s %-20s %8.1f %7s %7.1f", r.rollNo(), r.name(), r.percentage(), r.grade(), r.points()));
        }
    }

    public void showInbox(String owner, List<String> messages) {
        header("INBOX of " + owner);
        if (messages.isEmpty()) println("(empty)");
        for (int i = 0; i < messages.size(); i++) println((i + 1) + ". " + messages.get(i));
    }

    public void showLines(String title, List<String> lines) {
        header(title);
        if (lines.isEmpty()) println("(nothing)");
        lines.forEach(this::println);
    }
}
