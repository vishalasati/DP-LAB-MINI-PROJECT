package college.strategy;

/** Standard fixed cut-offs: the same marks always give the same grade. */
public class AbsoluteGradingStrategy implements GradingStrategy {
    @Override
    public String getName() {
        return "Standard grading (fixed cut-offs)";
    }

    @Override
    public String calculateGrade(double p, double classAverage) {
        if (p >= 90) return "O";
        if (p >= 80) return "A+";
        if (p >= 70) return "A";
        if (p >= 60) return "B";
        if (p >= 50) return "C";
        if (p >= 40) return "D";
        return "F";
    }

    @Override
    public double gradePoint(String grade) {
        return switch (grade) {
            case "O" -> 10;
            case "A+" -> 9;
            case "A" -> 8;
            case "B" -> 7;
            case "C" -> 6;
            case "D" -> 5;
            default -> 0;
        };
    }
}
