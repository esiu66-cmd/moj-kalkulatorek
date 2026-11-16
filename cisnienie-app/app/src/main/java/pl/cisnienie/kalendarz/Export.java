package pl.cisnienie.kalendarz;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import java.util.List;

/** Eksport pomiarów jako tekst CSV przez systemowe „Udostępnij”. */
final class Export {
    private Export() {}

    static void share(Activity a, long personId) {
        List<Measurement> list = Db.get(a).getAllMeasurements(personId);
        if (list.isEmpty()) {
            Toast.makeText(a, "Brak pomiarów do eksportu", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Osoba;Data;Godzina;Skurczowe;Rozkurczowe;Puls;Kategoria;Notatka\n");
        for (Measurement m : list) {
            sb.append(csv(m.personName)).append(';')
              .append(Ui.toDateTime(m.timestamp).format(Ui.DATE_SHORT)).append(';')
              .append(Ui.toDateTime(m.timestamp).format(Ui.TIME)).append(';')
              .append(m.systolic).append(';')
              .append(m.diastolic).append(';')
              .append(m.pulse > 0 ? String.valueOf(m.pulse) : "").append(';')
              .append(m.category().label).append(';')
              .append(csv(m.note)).append('\n');
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Pomiary ciśnienia");
        send.putExtra(Intent.EXTRA_TEXT, sb.toString());
        a.startActivity(Intent.createChooser(send, "Eksportuj pomiary"));
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
