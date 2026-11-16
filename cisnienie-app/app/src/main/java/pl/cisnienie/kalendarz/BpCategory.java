package pl.cisnienie.kalendarz;

/**
 * Klasyfikacja ciśnienia tętniczego wg wytycznych ESC/ESH (pomiar gabinetowy).
 * O kategorii decyduje wyższa z wartości (skurczowa lub rozkurczowa).
 */
public enum BpCategory {
    LOW("Niskie", 0xFF29B6F6),
    OPTIMAL("Optymalne", 0xFF2E7D32),
    NORMAL("Prawidłowe", 0xFF66BB6A),
    HIGH_NORMAL("Wysokie prawidłowe", 0xFFFBC02D),
    GRADE1("Nadciśnienie 1°", 0xFFFB8C00),
    GRADE2("Nadciśnienie 2°", 0xFFE53935),
    GRADE3("Nadciśnienie 3°", 0xFF8E0000);

    public final String label;
    public final int color;

    BpCategory(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public static BpCategory classify(int sys, int dia) {
        if (sys >= 180 || dia >= 110) return GRADE3;
        if (sys >= 160 || dia >= 100) return GRADE2;
        if (sys >= 140 || dia >= 90) return GRADE1;
        if (sys >= 130 || dia >= 85) return HIGH_NORMAL;
        if (sys < 90 || dia < 60) return LOW;
        if (sys >= 120 || dia >= 80) return NORMAL;
        return OPTIMAL;
    }
}
