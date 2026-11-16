package pl.cisnienie.kalendarz;

public class Person {
    public final long id;
    public final String name;
    public final int color;

    public Person(long id, String name, int color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    @Override
    public String toString() {
        return name;
    }
}
