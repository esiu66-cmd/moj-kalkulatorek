package pl.cisnienie.kalendarz;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int MENU_STATS = 1;
    private static final int MENU_PERSONS = 2;
    private static final int MENU_EXPORT = 3;
    private static final int MENU_LEGEND = 4;

    /** -1 = wszystkie osoby */
    private long selectedPersonId = -1;
    private YearMonth shownMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();

    private Db db;
    private SharedPreferences prefs;

    private LinearLayout chipRow;
    private TextView monthLabel;
    private LinearLayout grid;
    private TextView dayHeader;
    private LinearLayout dayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = Db.get(this);
        prefs = getSharedPreferences("ui", MODE_PRIVATE);
        selectedPersonId = prefs.getLong("person", -1);
        if (savedInstanceState != null) {
            shownMonth = YearMonth.parse(savedInstanceState.getString("month", shownMonth.toString()));
            selectedDate = LocalDate.parse(savedInstanceState.getString("date", selectedDate.toString()));
        }
        buildLayout();
        refreshAll();
        if (db.getPersons().isEmpty()) {
            showPersonNameDialog(null);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("month", shownMonth.toString());
        out.putString("date", selectedDate.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();
    }

    // ------------------------------------------------------------------ layout

    private void buildLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 12);
        root.setPadding(pad, pad, pad, Ui.dp(this, 90));
        scroll.addView(root);

        // Osoby (chipy)
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(chipRow);
        root.addView(hs);

        // Kalendarz
        LinearLayout calCard = Ui.card(this);
        root.addView(calCard, Ui.matchWrap(this, 12));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        TextView prev = navButton("‹");
        TextView next = navButton("›");
        monthLabel = Ui.text(this, "", 19, Ui.TEXT_PRIMARY, true);
        monthLabel.setGravity(Gravity.CENTER);
        monthLabel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                shownMonth = YearMonth.now();
                selectedDate = LocalDate.now();
                refreshAll();
            }
        });
        nav.addView(prev);
        nav.addView(monthLabel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        nav.addView(next);
        prev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { shownMonth = shownMonth.minusMonths(1); refreshCalendar(); }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { shownMonth = shownMonth.plusMonths(1); refreshCalendar(); }
        });
        calCard.addView(nav);

        LinearLayout weekdays = new LinearLayout(this);
        weekdays.setOrientation(LinearLayout.HORIZONTAL);
        weekdays.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 4));
        for (int i = 0; i < 7; i++) {
            DayOfWeek dow = DayOfWeek.MONDAY.plus(i);
            String n = Ui.capitalize(dow.getDisplayName(TextStyle.SHORT, Ui.PL));
            TextView t = Ui.text(this, n, 12, i >= 5 ? Ui.ACCENT : Ui.TEXT_SECONDARY, true);
            t.setGravity(Gravity.CENTER);
            weekdays.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        }
        calCard.addView(weekdays);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        calCard.addView(grid);

        // Wybrany dzień
        LinearLayout dayBar = new LinearLayout(this);
        dayBar.setOrientation(LinearLayout.HORIZONTAL);
        dayBar.setGravity(Gravity.CENTER_VERTICAL);
        dayHeader = Ui.text(this, "", 16, Ui.TEXT_PRIMARY, true);
        dayBar.addView(dayHeader, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(dayBar, Ui.matchWrap(this, 16));

        dayList = new LinearLayout(this);
        dayList.setOrientation(LinearLayout.VERTICAL);
        root.addView(dayList, Ui.matchWrap(this, 4));

        // Pływający przycisk "+"
        FrameLayout frame = new FrameLayout(this);
        frame.addView(scroll);
        TextView fab = Ui.text(this, "+", 30, Color.WHITE, false);
        fab.setGravity(Gravity.CENTER);
        fab.setBackground(Ui.circle(Ui.ACCENT));
        fab.setElevation(Ui.dp(this, 6));
        fab.setContentDescription("Dodaj pomiar");
        int size = Ui.dp(this, 60);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(size, size, Gravity.BOTTOM | Gravity.END);
        flp.setMargins(0, 0, Ui.dp(this, 20), Ui.dp(this, 20));
        frame.addView(fab, flp);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openMeasurementDialog(null); }
        });

        setContentView(frame);
    }

    private TextView navButton(String s) {
        TextView t = Ui.text(this, s, 28, Ui.TEXT_PRIMARY, true);
        t.setGravity(Gravity.CENTER);
        int p = Ui.dp(this, 6);
        t.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), p);
        return t;
    }

    // ----------------------------------------------------------------- refresh

    private void refreshAll() {
        refreshChips();
        refreshCalendar();
        refreshDay();
    }

    private void refreshChips() {
        chipRow.removeAllViews();
        List<Person> persons = db.getPersons();
        boolean exists = selectedPersonId < 0;
        for (Person p : persons) if (p.id == selectedPersonId) exists = true;
        if (!exists) selectPerson(-1, false);

        if (persons.size() > 1) {
            addChip("Wszyscy", Ui.TEXT_PRIMARY, selectedPersonId < 0, -1, null);
        }
        if (persons.size() == 1 && selectedPersonId < 0) {
            selectPerson(persons.get(0).id, false);
        }
        for (Person p : persons) {
            addChip(p.name, p.color, p.id == selectedPersonId, p.id, p);
        }
        TextView add = Ui.chip(this, "+ Osoba", Ui.TEXT_SECONDARY, false);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showPersonNameDialog(null); }
        });
        chipRow.addView(add, chipLp());
    }

    private void addChip(String label, int color, boolean selected, final long id, final Person person) {
        TextView chip = Ui.chip(this, label, color, selected);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { selectPerson(id, true); }
        });
        if (person != null) {
            chip.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) { showPersonActions(person); return true; }
            });
        }
        chipRow.addView(chip, chipLp());
    }

    private LinearLayout.LayoutParams chipLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(this, 8);
        return lp;
    }

    private void selectPerson(long id, boolean refresh) {
        selectedPersonId = id;
        prefs.edit().putLong("person", id).apply();
        if (refresh) refreshAll();
    }

    private void refreshCalendar() {
        monthLabel.setText(Ui.capitalize(shownMonth.format(Ui.MONTH)));
        grid.removeAllViews();

        LocalDate first = shownMonth.atDay(1);
        int offset = first.getDayOfWeek().getValue() - 1; // poniedziałek = 0
        LocalDate start = first.minusDays(offset);
        LocalDate end = start.plusDays(42);

        // Pomiary w widocznym zakresie pogrupowane po dniu
        List<Measurement> list = db.getMeasurements(selectedPersonId, Ui.startOfDay(start), Ui.startOfDay(end));
        Map<LocalDate, List<Measurement>> byDay = new HashMap<>();
        for (Measurement m : list) {
            LocalDate d = Ui.toDate(m.timestamp);
            List<Measurement> l = byDay.get(d);
            if (l == null) { l = new ArrayList<>(); byDay.put(d, l); }
            l.add(m);
        }

        LocalDate today = LocalDate.now();
        int weeks = (offset + shownMonth.lengthOfMonth() + 6) / 7;
        for (int w = 0; w < weeks; w++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < 7; i++) {
                LocalDate d = start.plusDays(w * 7 + i);
                row.addView(dayCell(d, byDay.get(d), today),
                        new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));
            }
            grid.addView(row);
        }
    }

    private View dayCell(final LocalDate d, List<Measurement> ms, LocalDate today) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = Ui.dp(this, 2);
        cell.setPadding(p, Ui.dp(this, 6), p, p);

        boolean inMonth = YearMonth.from(d).equals(shownMonth);
        boolean isSelected = d.equals(selectedDate);
        boolean isToday = d.equals(today);
        boolean weekend = d.getDayOfWeek().getValue() >= 6;

        int textColor = inMonth ? (weekend ? Ui.ACCENT : Ui.TEXT_PRIMARY) : 0xFFBDBDBD;
        TextView num = Ui.text(this, String.valueOf(d.getDayOfMonth()), 15, textColor, isToday || isSelected);
        num.setGravity(Gravity.CENTER);
        int s = Ui.dp(this, 30);
        if (isSelected) {
            num.setTextColor(Color.WHITE);
            num.setBackground(Ui.circle(Ui.ACCENT));
        } else if (isToday) {
            num.setBackground(Ui.outlined(Color.TRANSPARENT, Ui.ACCENT, Ui.dp(this, 1.5f), s / 2f));
        }
        cell.addView(num, new LinearLayout.LayoutParams(s, s));

        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        if (ms != null) {
            int shown = Math.min(ms.size(), 4);
            for (int i = 0; i < shown; i++) {
                Measurement m = ms.get(i);
                View dot = new View(this);
                dot.setBackground(Ui.circle(m.category().color));
                int ds = Ui.dp(this, 6);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ds, ds);
                lp.setMargins(Ui.dp(this, 1), Ui.dp(this, 3), Ui.dp(this, 1), 0);
                dots.addView(dot, lp);
            }
        }
        cell.addView(dots);

        cell.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                selectedDate = d;
                if (!YearMonth.from(d).equals(shownMonth)) shownMonth = YearMonth.from(d);
                refreshCalendar();
                refreshDay();
            }
        });
        return cell;
    }

    private void refreshDay() {
        dayHeader.setText(Ui.capitalize(selectedDate.format(Ui.DAY_HEADER)));
        dayList.removeAllViews();
        List<Measurement> ms = db.getMeasurements(selectedPersonId,
                Ui.startOfDay(selectedDate), Ui.startOfDay(selectedDate.plusDays(1)));
        if (ms.isEmpty()) {
            TextView empty = Ui.text(this, db.getPersons().isEmpty()
                    ? "Dodaj osobę, aby zacząć notować pomiary."
                    : "Brak pomiarów tego dnia.\nNaciśnij „+”, aby dodać pomiar.", 14, Ui.TEXT_SECONDARY, false);
            empty.setPadding(Ui.dp(this, 4), Ui.dp(this, 12), 0, 0);
            dayList.addView(empty);
            return;
        }
        for (Measurement m : ms) {
            dayList.addView(measurementRow(m, selectedPersonId < 0, false), Ui.matchWrap(this, 8));
        }
    }

    /** Wiersz z pomiarem – używany też w statystykach. */
    View measurementRow(final Measurement m, boolean showPerson, boolean showDate) {
        return MeasurementViews.row(this, m, showPerson, showDate,
                new View.OnClickListener() {
                    @Override public void onClick(View v) { openMeasurementDialog(m); }
                },
                new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) { confirmDelete(m); return true; }
                });
    }

    // ------------------------------------------------------------- dialogi

    private void openMeasurementDialog(Measurement existing) {
        List<Person> persons = db.getPersons();
        if (persons.isEmpty()) {
            Toast.makeText(this, "Najpierw dodaj osobę", Toast.LENGTH_SHORT).show();
            showPersonNameDialog(null);
            return;
        }
        MeasurementDialog.show(this, existing, persons, selectedPersonId, selectedDate,
                new MeasurementDialog.Callback() {
                    @Override public void onSaved(Measurement m) {
                        LocalDate d = Ui.toDate(m.timestamp);
                        selectedDate = d;
                        shownMonth = YearMonth.from(d);
                        refreshAll();
                    }
                });
    }

    private void confirmDelete(final Measurement m) {
        new AlertDialog.Builder(this)
                .setTitle("Usunąć pomiar?")
                .setMessage(m.systolic + "/" + m.diastolic + " mmHg, "
                        + Ui.toDateTime(m.timestamp).format(Ui.TIME) + " (" + m.personName + ")")
                .setPositiveButton("Usuń", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        db.deleteMeasurement(m.id);
                        refreshAll();
                    }
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void showPersonNameDialog(final Person existing) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint("Imię, np. Mama");
        if (existing != null) {
            input.setText(existing.name);
            input.setSelection(existing.name.length());
        }
        FrameLayout wrap = new FrameLayout(this);
        int p = Ui.dp(this, 20);
        wrap.setPadding(p, Ui.dp(this, 8), p, 0);
        wrap.addView(input);

        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Nowa osoba" : "Zmień nazwę")
                .setView(wrap)
                .setPositiveButton("Zapisz", null)
                .setNegativeButton("Anuluj", null)
                .create();
        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface d) {
                Button b = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) {
                            input.setError("Podaj nazwę");
                            return;
                        }
                        if (existing == null) {
                            long id = db.addPerson(name);
                            selectPerson(id, false);
                        } else {
                            db.renamePerson(existing.id, name);
                        }
                        dlg.dismiss();
                        refreshAll();
                    }
                });
            }
        });
        dlg.show();
    }

    private void showPersonActions(final Person p) {
        new AlertDialog.Builder(this)
                .setTitle(p.name)
                .setItems(new String[]{"Zmień nazwę", "Statystyki", "Usuń osobę"},
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int which) {
                                if (which == 0) showPersonNameDialog(p);
                                else if (which == 1) openStats(p.id);
                                else confirmDeletePerson(p);
                            }
                        })
                .show();
    }

    private void confirmDeletePerson(final Person p) {
        new AlertDialog.Builder(this)
                .setTitle("Usunąć osobę „" + p.name + "”?")
                .setMessage("Zostaną usunięte również wszystkie jej pomiary. Tej operacji nie można cofnąć.")
                .setPositiveButton("Usuń", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        db.deletePerson(p.id);
                        if (selectedPersonId == p.id) selectPerson(-1, false);
                        refreshAll();
                    }
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void showPersonsManager() {
        final List<Person> persons = db.getPersons();
        String[] names = new String[persons.size() + 1];
        for (int i = 0; i < persons.size(); i++) names[i] = persons.get(i).name;
        names[persons.size()] = "+ Dodaj osobę";
        new AlertDialog.Builder(this)
                .setTitle("Osoby")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which == persons.size()) showPersonNameDialog(null);
                        else showPersonActions(persons.get(which));
                    }
                })
                .setNegativeButton("Zamknij", null)
                .show();
    }

    private void showLegend() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 20);
        l.setPadding(p, Ui.dp(this, 8), p, 0);
        String[] ranges = {"< 90 lub < 60", "< 120 i < 80", "120–129 lub 80–84", "130–139 lub 85–89",
                "140–159 lub 90–99", "160–179 lub 100–109", "≥ 180 lub ≥ 110"};
        BpCategory[] cats = BpCategory.values();
        for (int i = 0; i < cats.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
            View dot = new View(this);
            dot.setBackground(Ui.circle(cats[i].color));
            int ds = Ui.dp(this, 14);
            row.addView(dot, new LinearLayout.LayoutParams(ds, ds));
            row.addView(Ui.spacer(this, 10));
            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.addView(Ui.text(this, cats[i].label, 15, Ui.TEXT_PRIMARY, true));
            texts.addView(Ui.text(this, ranges[i] + " mmHg", 13, Ui.TEXT_SECONDARY, false));
            row.addView(texts);
            l.addView(row);
        }
        TextView info = Ui.text(this, "Klasyfikacja wg ESC/ESH. Aplikacja nie zastępuje porady lekarza.",
                12, Ui.TEXT_SECONDARY, false);
        info.setPadding(0, Ui.dp(this, 10), 0, 0);
        l.addView(info);
        new AlertDialog.Builder(this)
                .setTitle("Legenda kolorów")
                .setView(l)
                .setPositiveButton("OK", null)
                .show();
    }

    private void openStats(long personId) {
        Intent i = new Intent(this, StatsActivity.class);
        i.putExtra("person", personId);
        startActivity(i);
    }

    // ----------------------------------------------------------------- menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_STATS, 0, "Statystyki i historia")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, MENU_PERSONS, 1, "Osoby");
        menu.add(0, MENU_LEGEND, 2, "Legenda kolorów");
        menu.add(0, MENU_EXPORT, 3, "Eksportuj (CSV)");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_STATS: openStats(selectedPersonId); return true;
            case MENU_PERSONS: showPersonsManager(); return true;
            case MENU_LEGEND: showLegend(); return true;
            case MENU_EXPORT: Export.share(this, selectedPersonId); return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
