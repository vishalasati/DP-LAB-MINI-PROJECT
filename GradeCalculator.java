package college.strategy;

/**
 * STRATEGY pattern - the Context.
 * It does not know HOW grades are calculated; it delegates to whichever
 * strategy is currently set, and the strategy can be swapped at run time.
 */
public class GradeCalculator {
    private GradingStrategy strategy;

    public GradeCalculator(GradingStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(GradingStrategy strategy) {
        this.strategy = strategy;
    }

    public GradingStrategy getStrategy() {
        return strategy;
    }

    public String grade(double percentage, double classAverage) {
        return strategy.calculateGrade(percentage, classAverage);
    }

    public double points(String grade) {
        return strategy.gradePoint(grade);
    }

    /** Small factory so the UI/controller can pick a policy by name. */
    public static GradingStrategy strategyFor(String policy) {
        return switch (policy.toLowerCase()) {
            case "absolute", "standard" -> new AbsoluteGradingStrategy();
            case "passfail" -> new PassFailGradingStrategy();
            default -> throw new IllegalArgumentException("Unknown grading policy '" + policy
                    + "'. Use: standard or passfail");
        };
    }
}
