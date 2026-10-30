package college.strategy;

/**
 * STRATEGY pattern - common interface for all grading policies.
 */
public interface GradingStrategy {
    String getName();

    /** @param percentage student's score (0-100); classAverage average of the whole class */
    String calculateGrade(double percentage, double classAverage);

    /** Grade points on a 10-point scale (0 for policies that are not credit-weighted). */
    double gradePoint(String grade);
}
