package pl.cisnienie.kalendarz;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** Prosta baza SQLite: osoby i pomiary ciśnienia. */
public class Db extends SQLiteOpenHelper {

    private static final String NAME = "cisnienie.db";
    private static final int VERSION = 1;

    public static final int[] PERSON_COLORS = {
            0xFF1E88E5, 0xFFE53935, 0xFF43A047, 0xFF8E24AA,
            0xFFFB8C00, 0xFF00897B, 0xFF6D4C41, 0xFF3949AB
    };

    private static Db instance;

    public static synchronized Db get(Context ctx) {
        if (instance == null) instance = new Db(ctx.getApplicationContext());
        return instance;
    }

    private Db(Context ctx) {
        super(ctx, NAME, null, VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE persons (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "color INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE measurements (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "person_id INTEGER NOT NULL REFERENCES persons(id) ON DELETE CASCADE," +
                "ts INTEGER NOT NULL," +
                "sys INTEGER NOT NULL," +
                "dia INTEGER NOT NULL," +
                "pulse INTEGER," +
                "note TEXT)");
        db.execSQL("CREATE INDEX idx_meas_ts ON measurements(ts)");
        db.execSQL("CREATE INDEX idx_meas_person ON measurements(person_id, ts)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // brak migracji w wersji 1
    }

    // ---------------- Osoby ----------------

    public List<Person> getPersons() {
        List<Person> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, name, color FROM persons ORDER BY name COLLATE NOCASE", null);
        try {
            while (c.moveToNext()) {
                out.add(new Person(c.getLong(0), c.getString(1), c.getInt(2)));
            }
        } finally {
            c.close();
        }
        return out;
    }

    public long addPerson(String name) {
        int count = getPersons().size();
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("color", PERSON_COLORS[count % PERSON_COLORS.length]);
        return getWritableDatabase().insert("persons", null, v);
    }

    public void renamePerson(long id, String name) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        getWritableDatabase().update("persons", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void deletePerson(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("measurements", "person_id=?", new String[]{String.valueOf(id)});
        db.delete("persons", "id=?", new String[]{String.valueOf(id)});
    }

    // ---------------- Pomiary ----------------

    /**
     * Pomiary w przedziale [from, to). personId < 0 oznacza wszystkie osoby.
     * Sortowane rosnąco po czasie.
     */
    public List<Measurement> getMeasurements(long personId, long from, long to) {
        List<Measurement> out = new ArrayList<>();
        String sql = "SELECT m.id, m.person_id, m.ts, m.sys, m.dia, m.pulse, m.note, p.name, p.color " +
                "FROM measurements m JOIN persons p ON p.id = m.person_id " +
                "WHERE m.ts >= ? AND m.ts < ?" +
                (personId >= 0 ? " AND m.person_id = ?" : "") +
                " ORDER BY m.ts ASC";
        String[] args = personId >= 0
                ? new String[]{String.valueOf(from), String.valueOf(to), String.valueOf(personId)}
                : new String[]{String.valueOf(from), String.valueOf(to)};
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try {
            while (c.moveToNext()) {
                Measurement m = new Measurement();
                m.id = c.getLong(0);
                m.personId = c.getLong(1);
                m.timestamp = c.getLong(2);
                m.systolic = c.getInt(3);
                m.diastolic = c.getInt(4);
                m.pulse = c.isNull(5) ? 0 : c.getInt(5);
                m.note = c.isNull(6) ? "" : c.getString(6);
                m.personName = c.getString(7);
                m.personColor = c.getInt(8);
                out.add(m);
            }
        } finally {
            c.close();
        }
        return out;
    }

    public List<Measurement> getAllMeasurements(long personId) {
        return getMeasurements(personId, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public void saveMeasurement(Measurement m) {
        ContentValues v = new ContentValues();
        v.put("person_id", m.personId);
        v.put("ts", m.timestamp);
        v.put("sys", m.systolic);
        v.put("dia", m.diastolic);
        if (m.pulse > 0) v.put("pulse", m.pulse);
        else v.putNull("pulse");
        v.put("note", m.note);
        if (m.id > 0) {
            getWritableDatabase().update("measurements", v, "id=?", new String[]{String.valueOf(m.id)});
        } else {
            m.id = getWritableDatabase().insert("measurements", null, v);
        }
    }

    public void deleteMeasurement(long id) {
        getWritableDatabase().delete("measurements", "id=?", new String[]{String.valueOf(id)});
    }
}
