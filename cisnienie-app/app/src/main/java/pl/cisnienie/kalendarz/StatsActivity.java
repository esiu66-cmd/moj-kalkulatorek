package pl.cisnienie.kalendarz;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatsActivity extends Activity {

    private static final int[] PERIOD_DAYS = {7, 30, 90, 0};
    private static final String[] PERIOD_LABELS = {"7 dni", "30 dni", "90 dni", "Wszystko"};

    private long personId;
    private int period = 1;
    private LinearLayout content;
    private LinearLayout periodRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        personId = getIntent().getLongExtra("person", -1);
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 12);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        periodRow = new LinearLayout(this);
        hs.addView(periodRow);
        root.addView(hs);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content);
        setContentView(scroll);
        refresh();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void refresh() {
        periodRow.removeAllViews();
        for (int i = 0; i < PERIOD_LABELS.length; i++) {
            final int idx = i;
            TextView chip = Ui.chip(this, PERIOD_LABELS[i], Ui.ACCENT, i == period);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { period = idx; refresh(); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(this, 8);
            periodRow.addView(chip, lp);
        }

        content.removeAllViews();
        Db db = Db.get(this);
        List<Person> persons = db.getPersons();
        List<Person> shown = new ArrayList<>();
        for (Person p : persons) if (personId < 0 || p.id == personId) shown.add(p);
        setTitle(personId < 0 || shown.isEmpty() ? "Statystyki – wszyscy" : "Statystyki – " + shown.get(0).name);

        if (shown.isEmpty()) {
            content.addView(Ui.text(this, "Brak osób.", 15, Ui.TEXT_SECONDARY, false), Ui.matchWrap(this, 16));
            return;
        }

        long to = Ui.startOfDay(LocalDate.now().plusDays(1));
        long from = PERIOD_DAYS[period] == 0 ? Long.MIN_VALUE
                : Ui.startOfDay(LocalDate.now().minusDays(PERIOD_DAYS[period] - 1));

        for (Person p : shown) {
            List<Measurement> list = db.getMeasurements(p.id, from, to);
            content.addView(personSection(p, list, from, PERIOD_DAYS[period] == 0 ? Long.MAX_VALUE : to),
                    Ui.matchWrap(this, 12));
        }

        // Historia
        List<Measurement> history = db.getMeasurements(personId, from, to);
        Collections.reverse(history);
        TextView h = Ui.text(this, "Historia pomiarów (" + history.size() + ")", 17, Ui.TEXT_PRIMARY, true);
        content.addView(h, Ui.matchWrap(this, 20));
        int limit = Math.min(history.size(), 300);
        for (int i = 0; i < limit; i++) {
            final Measurement m = history.get(i);
            content.addView(MeasurementViews.row(this, m, personId < 0, true, null,
                    new View.OnLongClickListener() {
                        @Override public boolean onLongClick(View v) { confirmDelete(m); return true; }
                    }), Ui.matchWrap(this, 8));
        }
        if (history.size() > limit) {
            content.addView(Ui.text(this, "Pokazano " + limit + " najnowszych. Pełne dane – eksport CSV.",
                    13, Ui.TEXT_SECONDARY, false), Ui.matchWrap(this, 8));
        }
    }

    private View personSection(Person p, List<Measurement> list, long from, long to) {
        LinearLayout card = Ui.card(this);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(this);
        dot.setBackground(Ui.circle(p.color));
        int ds = Ui.dp(this, 12);
        head.addView(dot, new LinearLayout.LayoutParams(ds, ds));
        head.addView(Ui.spacer(this, 8));
        head.addView(Ui.text(this, p.name, 18, Ui.TEXT_PRIMARY, true));
        card.addView(head);

        if (list.isEmpty()) {
            TextView t = Ui.text(this, "Brak pomiarów w tym okresie.", 14, Ui.TEXT_SECONDARY, false);
            t.setPadding(0, Ui.dp(this, 8), 0, 0);
            card.addView(t);
            return card;
        }

        int n = list.size();
        long sSum = 0, dSum = 0, pSum = 0;
        int pCount = 0, sMin = 999, sMax = 0, dMin = 999, dMax = 0;
        int[] catCount = new int[BpCategory.values().length];
        for (Measurement m : list) {
            sSum += m.systolic;
            dSum += m.diastolic;
            if (m.pulse > 0) { pSum += m.pulse; pCount++; }
            sMin = Math.min(sMin, m.systolic); sMax = Math.max(sMax, m.systolic);
            dMin = Math.min(dMin, m.diastolic); dMax = Math.max(dMax, m.diastolic);
            catCount[m.category().ordinal()]++;
        }
        int sAvg = Math.round(sSum / (float) n), dAvg = Math.round(dSum / (float) n);
        BpCategory avgCat = BpCategory.classify(sAvg, dAvg);

        LinearLayout tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        tiles.setPadding(0, Ui.dp(this, 10), 0, 0);
        tiles.addView(tile("Średnia", sAvg + "/" + dAvg, avgCat.label, avgCat.color), tileLp());
        tiles.addView(tile("Puls śr.", pCount > 0 ? String.valueOf(Math.round(pSum / (float) pCount)) : "–",
                "uderzeń/min", ChartView.PULSE_COLOR), tileLp());
        tiles.addView(tile("Pomiary", String.valueOf(n), "w okresie", Ui.TEXT_PRIMARY), tileLp());
        card.addView(tiles);

        TextView range = Ui.text(this, "Skurczowe: " + sMin + "–" + sMax + "   Rozkurczowe: " + dMin + "–" + dMax,
                13, Ui.TEXT_SECONDARY, false);
        range.setPadding(0, Ui.dp(this, 10), 0, 0);
        card.addView(range);

        // Rozkład kategorii
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams barLp = Ui.matchWrap(this, 10);
        barLp.height = Ui.dp(this, 10);
        for (BpCategory cat : BpCategory.values()) {
            int cnt = catCount[cat.ordinal()];
            if (cnt == 0) continue;
            View seg = new View(this);
            seg.setBackgroundColor(cat.color);
            bar.addView(seg, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, cnt));
        }
        card.addView(bar, barLp);
        StringBuilder legend = new StringBuilder();
        for (BpCategory cat : BpCategory.values()) {
            int cnt = catCount[cat.ordinal()];
            if (cnt == 0) continue;
            if (legend.length() > 0) legend.append("  ·  ");
            legend.append(cat.label).append(": ").append(Math.round(cnt * 100f / n)).append('%');
        }
        TextView lg = Ui.text(this, legend.toString(), 12, Ui.TEXT_SECONDARY, false);
        lg.setPadding(0, Ui.dp(this, 4), 0, 0);
        card.addView(lg);

        ChartView chart = new ChartView(this);
        chart.setData(list, from, to);
        card.addView(chart, Ui.matchWrap(this, 12));

        LinearLayout keys = new LinearLayout(this);
        keys.setGravity(Gravity.CENTER);
        keys.addView(key("Skurczowe", ChartView.SYS_COLOR));
        keys.addView(key("Rozkurczowe", ChartView.DIA_COLOR));
        keys.addView(key("Puls", ChartView.PULSE_COLOR));
        card.addView(keys, Ui.matchWrap(this, 4));
        return card;
    }

    private LinearLayout.LayoutParams tileLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
        return lp;
    }

    private View tile(String title, String value, String sub, int color) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setBackground(Ui.rounded(Ui.BACKGROUND, Ui.dp(this, 10)));
        int p = Ui.dp(this, 8);
        t.setPadding(p, p, p, p);
        t.addView(Ui.text(this, title, 12, Ui.TEXT_SECONDARY, false));
        t.addView(Ui.text(this, value, 20, Ui.TEXT_PRIMARY, true));
        TextView s = Ui.text(this, sub, 11, color, true);
        s.setGravity(Gravity.CENTER);
        t.addView(s);
        return t;
    }

    private View key(String label, int color) {
        LinearLayout l = new LinearLayout(this);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        View d = new View(this);
        d.setBackgroundColor(color);
        l.addView(d, new LinearLayout.LayoutParams(Ui.dp(this, 14), Ui.dp(this, 3)));
        l.addView(Ui.spacer(this, 4));
        l.addView(Ui.text(this, label, 12, Ui.TEXT_SECONDARY, false));
        return l;
    }

    private void confirmDelete(final Measurement m) {
        new AlertDialog.Builder(this)
                .setTitle("Usunąć pomiar?")
                .setMessage(m.systolic + "/" + m.diastolic + " mmHg – " + m.personName)
                .setPositiveButton("Usuń", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        Db.get(StatsActivity.this).deleteMeasurement(m.id);
                        refresh();
                    }
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }
}
