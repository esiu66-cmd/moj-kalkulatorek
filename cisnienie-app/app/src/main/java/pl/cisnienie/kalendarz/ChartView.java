package pl.cisnienie.kalendarz;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Wykres liniowy ciśnienia skurczowego, rozkurczowego i pulsu w czasie. */
public class ChartView extends View {

    public static final int SYS_COLOR = 0xFFC62828;
    public static final int DIA_COLOR = 0xFF1565C0;
    public static final int PULSE_COLOR = 0xFFD81B60;

    private List<Measurement> data = new ArrayList<>();
    private long from, to;

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ref = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ChartView(Context c) {
        super(c);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Ui.dp(c, 2.5f));
        line.setStrokeJoin(Paint.Join.ROUND);
        grid.setColor(0xFFE5E7EB);
        grid.setStrokeWidth(Ui.dp(c, 1));
        ref.setStyle(Paint.Style.STROKE);
        ref.setStrokeWidth(Ui.dp(c, 1.2f));
        ref.setPathEffect(new DashPathEffect(new float[]{Ui.dp(c, 5), Ui.dp(c, 4)}, 0));
        label.setColor(Ui.TEXT_SECONDARY);
        label.setTextSize(Ui.dp(c, 10));
    }

    /** Dane muszą być posortowane rosnąco po czasie. */
    public void setData(List<Measurement> data, long from, long to) {
        this.data = data;
        this.from = from;
        this.to = to;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int width = MeasureSpec.getSize(w);
        setMeasuredDimension(width, Ui.dp(getContext(), 200));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Context c = getContext();
        float left = Ui.dp(c, 30), right = getWidth() - Ui.dp(c, 8);
        float top = Ui.dp(c, 8), bottom = getHeight() - Ui.dp(c, 18);

        if (data.isEmpty()) {
            label.setTextAlign(Paint.Align.CENTER);
            label.setTextSize(Ui.dp(c, 13));
            canvas.drawText("Brak danych w wybranym okresie", getWidth() / 2f, getHeight() / 2f, label);
            label.setTextSize(Ui.dp(c, 10));
            return;
        }

        int min = 40, max = 200;
        for (Measurement m : data) {
            min = Math.min(min, Math.min(m.diastolic, m.pulse > 0 ? m.pulse : m.diastolic));
            max = Math.max(max, m.systolic);
        }
        min = (min / 20) * 20;
        max = ((max + 19) / 20) * 20;

        long t0 = from, t1 = to;
        if (t0 == Long.MIN_VALUE || t0 <= 0) t0 = data.get(0).timestamp;
        if (t1 == Long.MAX_VALUE) t1 = data.get(data.size() - 1).timestamp;
        if (t1 <= t0) { t0 -= 3600_000L * 12; t1 += 3600_000L * 12; }

        // siatka
        label.setTextAlign(Paint.Align.RIGHT);
        for (int v = min; v <= max; v += 20) {
            float y = y(v, min, max, top, bottom);
            canvas.drawLine(left, y, right, y, grid);
            canvas.drawText(String.valueOf(v), left - Ui.dp(c, 4), y + Ui.dp(c, 3), label);
        }
        // progi nadciśnienia 140 / 90
        ref.setColor(SYS_COLOR & 0x80FFFFFF);
        float y140 = y(140, min, max, top, bottom);
        canvas.drawLine(left, y140, right, y140, ref);
        ref.setColor(DIA_COLOR & 0x80FFFFFF);
        float y90 = y(90, min, max, top, bottom);
        canvas.drawLine(left, y90, right, y90, ref);

        // daty na osi X
        label.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(Ui.toDate(t0).format(Ui.DATE_SHORT), left, getHeight() - Ui.dp(c, 4), label);
        label.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(Ui.toDate(t1).format(Ui.DATE_SHORT), right, getHeight() - Ui.dp(c, 4), label);

        drawSeries(canvas, 2, PULSE_COLOR, t0, t1, min, max, left, right, top, bottom);
        drawSeries(canvas, 1, DIA_COLOR, t0, t1, min, max, left, right, top, bottom);
        drawSeries(canvas, 0, SYS_COLOR, t0, t1, min, max, left, right, top, bottom);
    }

    private void drawSeries(Canvas canvas, int which, int color, long t0, long t1, int min, int max,
                            float left, float right, float top, float bottom) {
        Path path = new Path();
        boolean started = false;
        line.setColor(which == 2 ? (color & 0x99FFFFFF) : color);
        dot.setColor(color);
        float r = Ui.dp(getContext(), which == 2 ? 2 : 3);
        for (Measurement m : data) {
            int v = which == 0 ? m.systolic : which == 1 ? m.diastolic : m.pulse;
            if (v <= 0) continue;
            float x = left + (right - left) * (m.timestamp - t0) / (float) (t1 - t0);
            float y = y(v, min, max, top, bottom);
            if (!started) { path.moveTo(x, y); started = true; } else path.lineTo(x, y);
            canvas.drawCircle(x, y, r, dot);
        }
        canvas.drawPath(path, line);
    }

    private static float y(int v, int min, int max, float top, float bottom) {
        return bottom - (bottom - top) * (v - min) / (float) (max - min);
    }
}
