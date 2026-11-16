package pl.cisnienie.kalendarz;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Okno dodawania / edycji pomiaru. */
final class MeasurementDialog {
    private MeasurementDialog() {}

    interface Callback {
        void onSaved(Measurement m);
    }

    static void show(final Activity a, Measurement existing, final List<Person> persons,
                     long preferredPersonId, LocalDate defaultDate, final Callback cb) {
        final Measurement m;
        if (existing != null) {
            m = existing;
        } else {
            m = new Measurement();
            LocalTime now = LocalTime.now().withSecond(0).withNano(0);
            m.timestamp = defaultDate.atTime(now).atZone(Ui.zone()).toInstant().toEpochMilli();
            m.personId = preferredPersonId;
        }
        final LocalDateTime[] when = {Ui.toDateTime(m.timestamp)};

        ScrollView scroll = new ScrollView(a);
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 20);
        l.setPadding(p, Ui.dp(a, 8), p, 0);
        scroll.addView(l);

        // Osoba
        l.addView(label(a, "Osoba"));
        final Spinner personSpinner = new Spinner(a);
        ArrayAdapter<Person> adapter = new ArrayAdapter<>(a, android.R.layout.simple_spinner_item, persons);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        personSpinner.setAdapter(adapter);
        int sel = 0;
        for (int i = 0; i < persons.size(); i++) if (persons.get(i).id == m.personId) sel = i;
        personSpinner.setSelection(sel);
        l.addView(personSpinner);

        // Data i godzina
        l.addView(label(a, "Data i godzina"));
        LinearLayout dt = new LinearLayout(a);
        dt.setOrientation(LinearLayout.HORIZONTAL);
        final Button dateBtn = new Button(a);
        final Button timeBtn = new Button(a);
        dateBtn.setAllCaps(false);
        timeBtn.setAllCaps(false);
        dt.addView(dateBtn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3));
        dt.addView(timeBtn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
        l.addView(dt);
        final Runnable updateDt = new Runnable() {
            @Override public void run() {
                dateBtn.setText(when[0].format(Ui.DATE_SHORT));
                timeBtn.setText(when[0].format(Ui.TIME));
            }
        };
        updateDt.run();
        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new DatePickerDialog(a, new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(DatePicker view, int y, int mo, int d) {
                        when[0] = when[0].withYear(y).withMonth(mo + 1).withDayOfMonth(d);
                        updateDt.run();
                    }
                }, when[0].getYear(), when[0].getMonthValue() - 1, when[0].getDayOfMonth()).show();
            }
        });
        timeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new TimePickerDialog(a, new TimePickerDialog.OnTimeSetListener() {
                    @Override public void onTimeSet(TimePicker view, int h, int min) {
                        when[0] = when[0].withHour(h).withMinute(min);
                        updateDt.run();
                    }
                }, when[0].getHour(), when[0].getMinute(), true).show();
            }
        });

        // Wartości
        LinearLayout vals = new LinearLayout(a);
        vals.setOrientation(LinearLayout.HORIZONTAL);
        final EditText sys = number(a, "SYS", m.systolic);
        final EditText dia = number(a, "DIA", m.diastolic);
        final EditText pulse = number(a, "Puls", m.pulse);
        vals.addView(column(a, "Skurczowe", sys), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        vals.addView(column(a, "Rozkurczowe", dia), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        vals.addView(column(a, "Puls", pulse), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        l.addView(vals);

        final TextView preview = Ui.text(a, "", 14, Ui.TEXT_SECONDARY, true);
        preview.setPadding(0, Ui.dp(a, 4), 0, 0);
        l.addView(preview);
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable e) {
                int s = parse(sys), d = parse(dia);
                if (s > 0 && d > 0) {
                    BpCategory cat = BpCategory.classify(s, d);
                    preview.setText("● " + cat.label);
                    preview.setTextColor(cat.color);
                } else {
                    preview.setText("");
                }
            }
        };
        sys.addTextChangedListener(watcher);
        dia.addTextChangedListener(watcher);
        watcher.afterTextChanged(null);

        // Notatka
        l.addView(label(a, "Notatka (opcjonalnie)"));
        final EditText note = new EditText(a);
        note.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setHint("np. po leku, lewa ręka, stres…");
        note.setText(m.note);
        l.addView(note);

        final AlertDialog dlg = new AlertDialog.Builder(a)
                .setTitle(existing == null ? "Nowy pomiar" : "Edytuj pomiar")
                .setView(scroll)
                .setPositiveButton("Zapisz", null)
                .setNegativeButton("Anuluj", null)
                .create();
        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface di) {
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        int s = parse(sys), d = parse(dia), pu = parse(pulse);
                        if (s < 50 || s > 300) { sys.setError("Zakres 50–300"); sys.requestFocus(); return; }
                        if (d < 30 || d > 200) { dia.setError("Zakres 30–200"); dia.requestFocus(); return; }
                        if (d >= s) { dia.setError("Rozkurczowe musi być niższe od skurczowego"); dia.requestFocus(); return; }
                        if (pu != 0 && (pu < 30 || pu > 250)) { pulse.setError("Zakres 30–250"); pulse.requestFocus(); return; }
                        Person person = (Person) personSpinner.getSelectedItem();
                        m.personId = person.id;
                        m.personName = person.name;
                        m.personColor = person.color;
                        m.systolic = s;
                        m.diastolic = d;
                        m.pulse = pu;
                        m.note = note.getText().toString().trim();
                        m.timestamp = when[0].atZone(Ui.zone()).toInstant().toEpochMilli();
                        Db.get(a).saveMeasurement(m);
                        dlg.dismiss();
                        cb.onSaved(m);
                    }
                });
            }
        });
        if (existing == null) {
            dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            sys.requestFocus();
        }
        dlg.show();
    }

    private static TextView label(Activity a, String s) {
        TextView t = Ui.text(a, s, 13, Ui.TEXT_SECONDARY, true);
        t.setPadding(0, Ui.dp(a, 12), 0, Ui.dp(a, 2));
        return t;
    }

    private static EditText number(Activity a, String hint, int value) {
        EditText e = new EditText(a);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        e.setHint(hint);
        e.setGravity(Gravity.CENTER);
        e.setTextSize(22);
        if (value > 0) e.setText(String.valueOf(value));
        return e;
    }

    private static View column(Activity a, String title, EditText e) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(Ui.dp(a, 2), 0, Ui.dp(a, 2), 0);
        TextView t = label(a, title);
        t.setGravity(Gravity.CENTER);
        c.addView(t);
        c.addView(e);
        return c;
    }

    private static int parse(EditText e) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
