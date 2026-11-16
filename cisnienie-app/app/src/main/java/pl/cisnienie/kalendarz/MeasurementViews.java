package pl.cisnienie.kalendarz;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

final class MeasurementViews {
    private MeasurementViews() {}

    static View row(Context c, Measurement m, boolean showPerson, boolean showDate,
                    View.OnClickListener click, View.OnLongClickListener longClick) {
        BpCategory cat = m.category();

        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackground(Ui.rounded(Ui.SURFACE, Ui.dp(c, 12)));
        card.setElevation(Ui.dp(c, 1));
        card.setGravity(Gravity.CENTER_VERTICAL);

        View bar = new View(c);
        bar.setBackground(Ui.rounded(cat.color, Ui.dp(c, 3)));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(Ui.dp(c, 6), LinearLayout.LayoutParams.MATCH_PARENT);
        barLp.setMargins(Ui.dp(c, 8), Ui.dp(c, 10), 0, Ui.dp(c, 10));
        card.addView(bar, barLp);

        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(c, 12);
        body.setPadding(p, p, p, p);
        card.addView(body, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout top = new LinearLayout(c);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.BOTTOM);
        top.addView(Ui.text(c, m.systolic + "/" + m.diastolic, 22, Ui.TEXT_PRIMARY, true));
        TextView unit = Ui.text(c, " mmHg", 13, Ui.TEXT_SECONDARY, false);
        unit.setPadding(0, 0, 0, Ui.dp(c, 3));
        top.addView(unit);
        if (m.pulse > 0) {
            TextView pulse = Ui.text(c, "   ♥ " + m.pulse, 15, 0xFFD81B60, true);
            pulse.setPadding(0, 0, 0, Ui.dp(c, 2));
            top.addView(pulse);
        }
        body.addView(top);

        String when = Ui.toDateTime(m.timestamp).format(showDate
                ? java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm", Ui.PL) : Ui.TIME);
        TextView sub = Ui.text(c, when, 13, Ui.TEXT_SECONDARY, false);
        body.addView(sub);

        if (m.note != null && !m.note.isEmpty()) {
            TextView note = Ui.text(c, m.note, 14, Ui.TEXT_PRIMARY, false);
            note.setPadding(0, Ui.dp(c, 4), 0, 0);
            body.addView(note);
        }

        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        right.setPadding(0, p, p, p);
        TextView badge = Ui.text(c, cat.label, 12, Color.WHITE, true);
        badge.setBackground(Ui.rounded(cat.color, Ui.dp(c, 10)));
        badge.setPadding(Ui.dp(c, 8), Ui.dp(c, 3), Ui.dp(c, 8), Ui.dp(c, 3));
        right.addView(badge);
        if (showPerson) {
            TextView who = Ui.text(c, m.personName, 13, m.personColor, true);
            who.setGravity(Gravity.END);
            who.setPadding(0, Ui.dp(c, 6), 0, 0);
            right.addView(who);
        }
        card.addView(right);

        if (click != null) card.setOnClickListener(click);
        if (longClick != null) card.setOnLongClickListener(longClick);
        return card;
    }
}
