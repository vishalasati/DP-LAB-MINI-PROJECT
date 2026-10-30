package college.strategy;

/** Used for labs / audit courses: only PASS or FAIL, no grade points. */
public class PassFailGradingStrategy implements GradingStrategy {
    @Override
    public String getName() {
        return "Pass / Fail (cut-off 40%)";
    }

    @Override
    public String calculateGrade(double p, double classAverage) {
        return p >= 40 ? "PASS" : "FAIL";
    }

    @Override
    public double gradePoint(String grade) {
        return 0;
    }
}
