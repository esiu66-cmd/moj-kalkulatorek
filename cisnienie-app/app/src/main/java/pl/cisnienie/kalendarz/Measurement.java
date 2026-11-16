package pl.cisnienie.kalendarz;

public class Measurement {
    public long id;
    public long personId;
    public long timestamp;
    public int systolic;
    public int diastolic;
    public int pulse;
    public String note = "";

    // pola pomocnicze (z JOIN)
    public String personName = "";
    public int personColor;

    public BpCategory category() {
        return BpCategory.classify(systolic, diastolic);
    }
}
