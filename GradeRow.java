package college.model;

/** One line of a grade report (plain data passed from controller to view). */
public record GradeRow(String rollNo, String name, double percentage, String grade, double points) { }
