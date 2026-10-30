package college.model;

public class FeeAccount {
    private final double totalFee;
    private double paid;

    public FeeAccount(double totalFee) {
        this.totalFee = totalFee;
    }

    public void pay(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Payment must be positive");
        if (amount > due()) throw new IllegalArgumentException("Payment exceeds amount due (Rs " + due() + ")");
        paid += amount;
    }

    public double getTotalFee() { return totalFee; }
    public double getPaid() { return paid; }
    public double due() { return totalFee - paid; }
}
