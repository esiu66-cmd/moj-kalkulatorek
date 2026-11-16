package pl.cisnienie.kalendarz;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Drobne pomocniki do budowania interfejsu w kodzie. */
public final class Ui {
    private Ui() {}

    public static final Locale PL = new Locale("pl", "PL");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", PL);
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", PL);
    public static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("dd.MM.yyyy", PL);
    public static final DateTimeFormatter DAY_HEADER = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", PL);
    public static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("LLLL yyyy", PL);

    public static final int TEXT_PRIMARY = 0xFF1F2937;
    public static final int TEXT_SECONDARY = 0xFF6B7280;
    public static final int ACCENT = 0xFFC62828;
    public static final int SURFACE = Color.WHITE;
    public static final int BACKGROUND = 0xFFF3F4F6;

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static GradientDrawable outlined(int fill, int stroke, int strokePx, float radiusPx) {
        GradientDrawable d = rounded(fill, radiusPx);
        d.setStroke(strokePx, stroke);
        return d;
    }

    public static GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(rounded(SURFACE, dp(c, 14)));
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        l.setElevation(dp(c, 1));
        return l;
    }

    public static LinearLayout.LayoutParams matchWrap(Context c, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topMarginDp);
        return lp;
    }

    public static View spacer(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, dp), dp(c, dp)));
        return v;
    }

    public static TextView chip(Context c, String label, int color, boolean selected) {
        TextView t = text(c, label, 14, selected ? Color.WHITE : color, true);
        t.setGravity(Gravity.CENTER);
        int ph = dp(c, 14), pv = dp(c, 8);
        t.setPadding(ph, pv, ph, pv);
        t.setBackground(selected
                ? rounded(color, dp(c, 20))
                : outlined(SURFACE, color, dp(c, 1.5f), dp(c, 20)));
        return t;
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(PL) + s.substring(1);
    }

    public static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    public static long startOfDay(LocalDate d) {
        return d.atStartOfDay(zone()).toInstant().toEpochMilli();
    }

    public static LocalDateTime toDateTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone());
    }

    public static LocalDate toDate(long millis) {
        return toDateTime(millis).toLocalDate();
    }
}
