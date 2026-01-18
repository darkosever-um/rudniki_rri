package si.um.feri.maprri.models.enums;

public enum MineralGrade {
    LOW(0.5f),
    MEDIUM(1.0f),
    HIGH(1.5f),
    UNDEFINED(0.0f);

    private final double coefficient;

    MineralGrade(double coefficient) {
        this.coefficient = coefficient;
    }

    public double getCoefficient() {
        return coefficient;
    }
}
