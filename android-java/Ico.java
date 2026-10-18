package __APP_ID__;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

/** أيقونات مرسومة بالكود (مقياس 24×24) — بلا ملفات موارد. */
final class Ico extends Drawable {
    static final int BACK = 1, FORWARD = 2, REFRESH = 3, CLOSE = 4, PLUS = 5, HOME = 6, MENU = 7, STAR = 8,
            STAR_FILLED = 9, SEARCH = 10, LOCK = 11, DOWNLOAD = 12, HISTORY = 13, SHARE = 14, INCOGNITO = 15,
            GRID = 16, LIST = 17, TRASH = 18, SETTINGS = 19, DESKTOP = 20, HELP = 21, NEWTAB = 22, RECENT = 23,
            MIC = 24, INFO = 25, GLOBE = 26, FOLDER = 27, DOTS_H = 28, CHECK = 29, COPY = 30, OPEN = 31,
            SORT = 32, FOLDER_PLUS = 33, READING = 34, PENCIL = 35, UNGROUP = 36;

    private final int kind;
    private int color;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    Ico(int kind, int color) {
        this.kind = kind;
        this.color = color;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        fill.setStyle(Paint.Style.FILL);
        setColor(color);
    }

    void setColor(int c) {
        color = c;
        stroke.setColor(c);
        fill.setColor(c);
        invalidateSelf();
    }

    @Override
    public boolean isAutoMirrored() {
        return kind == BACK || kind == FORWARD;
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        c.save();
        c.translate(b.left, b.top);
        c.scale(b.width() / 24f, b.height() / 24f);
        if (isAutoMirrored() && getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) c.scale(-1f, 1f, 12f, 12f);
        Paint s = stroke;
        Paint f = fill;
        switch (kind) {
            case BACK:
                c.drawLine(19, 12, 5, 12, s);
                c.drawLine(12, 5, 5, 12, s);
                c.drawLine(5, 12, 12, 19, s);
                break;
            case FORWARD:
                c.drawLine(5, 12, 19, 12, s);
                c.drawLine(12, 5, 19, 12, s);
                c.drawLine(19, 12, 12, 19, s);
                break;
            case REFRESH:
                rect.set(5, 5, 19, 19);
                c.drawArc(rect, -50, 290, false, s);
                path.reset();
                path.moveTo(20.5f, 3.5f);
                path.lineTo(20.5f, 9.5f);
                path.lineTo(14.5f, 9.5f);
                path.close();
                c.drawPath(path, f);
                break;
            case CLOSE:
                c.drawLine(6, 6, 18, 18, s);
                c.drawLine(18, 6, 6, 18, s);
                break;
            case PLUS:
                c.drawLine(12, 5, 12, 19, s);
                c.drawLine(5, 12, 19, 12, s);
                break;
            case HOME:
                path.reset();
                path.moveTo(5, 11);
                path.lineTo(5, 20);
                path.lineTo(19, 20);
                path.lineTo(19, 11);
                c.drawPath(path, s);
                path.reset();
                path.moveTo(3, 12);
                path.lineTo(12, 4);
                path.lineTo(21, 12);
                c.drawPath(path, s);
                break;
            case MENU:
                c.drawCircle(12, 5, 1.9f, f);
                c.drawCircle(12, 12, 1.9f, f);
                c.drawCircle(12, 19, 1.9f, f);
                break;
            case DOTS_H:
                c.drawCircle(5, 12, 1.9f, f);
                c.drawCircle(12, 12, 1.9f, f);
                c.drawCircle(19, 12, 1.9f, f);
                break;
            case STAR:
            case STAR_FILLED:
                path.reset();
                for (int i = 0; i < 10; i++) {
                    double ang = -Math.PI / 2 + i * Math.PI / 5;
                    float r = (i % 2 == 0) ? 9f : 4f;
                    float x = 12f + (float) (r * Math.cos(ang));
                    float y = 12.8f + (float) (r * Math.sin(ang));
                    if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                }
                path.close();
                c.drawPath(path, kind == STAR_FILLED ? f : s);
                if (kind == STAR_FILLED) c.drawPath(path, s);
                break;
            case SEARCH:
                c.drawCircle(10, 10, 6, s);
                c.drawLine(14.5f, 14.5f, 20, 20, s);
                break;
            case LOCK:
                rect.set(6, 11, 18, 20);
                c.drawRoundRect(rect, 2, 2, s);
                rect.set(8, 4, 16, 14);
                c.drawArc(rect, 180, 180, false, s);
                c.drawLine(8, 9, 8, 11, s);
                c.drawLine(16, 9, 16, 11, s);
                break;
            case DOWNLOAD:
                c.drawLine(12, 4, 12, 15, s);
                c.drawLine(7, 10.5f, 12, 15.5f, s);
                c.drawLine(17, 10.5f, 12, 15.5f, s);
                c.drawLine(5, 20, 19, 20, s);
                break;
            case HISTORY:
                rect.set(4, 4, 20, 20);
                c.drawArc(rect, -60, 320, false, s);
                c.drawLine(12, 8, 12, 12.5f, s);
                c.drawLine(12, 12.5f, 15.5f, 14.5f, s);
                path.reset();
                path.moveTo(3.5f, 4);
                path.lineTo(3.5f, 9.5f);
                path.lineTo(9, 9.5f);
                path.close();
                c.drawPath(path, f);
                break;
            case SHARE:
                c.drawCircle(18, 5.5f, 2.6f, f);
                c.drawCircle(6, 12, 2.6f, f);
                c.drawCircle(18, 18.5f, 2.6f, f);
                c.drawLine(8, 11, 16, 6.5f, s);
                c.drawLine(8, 13, 16, 17.5f, s);
                break;
            case INCOGNITO:
                path.reset();
                path.moveTo(5, 12);
                path.lineTo(8, 4.5f);
                path.lineTo(16, 4.5f);
                path.lineTo(19, 12);
                path.close();
                c.drawPath(path, f);
                c.drawLine(3, 12, 21, 12, s);
                c.drawCircle(8, 17, 2.6f, s);
                c.drawCircle(16, 17, 2.6f, s);
                c.drawLine(10.6f, 16.5f, 13.4f, 16.5f, s);
                break;
            case GRID:
                rect.set(4, 4, 10.5f, 10.5f); c.drawRoundRect(rect, 1.5f, 1.5f, s);
                rect.set(13.5f, 4, 20, 10.5f); c.drawRoundRect(rect, 1.5f, 1.5f, s);
                rect.set(4, 13.5f, 10.5f, 20); c.drawRoundRect(rect, 1.5f, 1.5f, s);
                rect.set(13.5f, 13.5f, 20, 20); c.drawRoundRect(rect, 1.5f, 1.5f, s);
                break;
            case LIST:
                c.drawLine(4, 6, 20, 6, s);
                c.drawLine(4, 12, 20, 12, s);
                c.drawLine(4, 18, 20, 18, s);
                break;
            case TRASH:
                c.drawLine(4.5f, 7, 19.5f, 7, s);
                path.reset();
                path.moveTo(9, 7); path.lineTo(9, 4); path.lineTo(15, 4); path.lineTo(15, 7);
                c.drawPath(path, s);
                path.reset();
                path.moveTo(6.5f, 7); path.lineTo(7.5f, 20); path.lineTo(16.5f, 20); path.lineTo(17.5f, 7);
                c.drawPath(path, s);
                c.drawLine(10, 11, 10, 16, s);
                c.drawLine(14, 11, 14, 16, s);
                break;
            case SETTINGS:
                c.drawCircle(12, 12, 3.3f, s);
                c.drawCircle(12, 12, 7f, s);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    float cx = (float) Math.cos(a), cy = (float) Math.sin(a);
                    c.drawLine(12 + cx * 7f, 12 + cy * 7f, 12 + cx * 9.6f, 12 + cy * 9.6f, s);
                }
                break;
            case DESKTOP:
                rect.set(3, 4.5f, 21, 16);
                c.drawRoundRect(rect, 2, 2, s);
                c.drawLine(9, 20, 15, 20, s);
                c.drawLine(12, 16, 12, 20, s);
                break;
            case HELP:
                c.drawCircle(12, 12, 9, s);
                path.reset();
                path.moveTo(9.3f, 9.6f);
                path.cubicTo(9.5f, 6.4f, 14.8f, 6.6f, 14.6f, 9.7f);
                path.cubicTo(14.5f, 11.8f, 12, 11.8f, 12, 14f);
                c.drawPath(path, s);
                c.drawCircle(12, 17.2f, 1.1f, f);
                break;
            case INFO:
                c.drawCircle(12, 12, 9, s);
                c.drawLine(12, 11, 12, 17, s);
                c.drawCircle(12, 7.6f, 1.1f, f);
                break;
            case NEWTAB:
                rect.set(3.5f, 3.5f, 20.5f, 20.5f);
                c.drawRoundRect(rect, 4, 4, s);
                c.drawLine(12, 8, 12, 16, s);
                c.drawLine(8, 12, 16, 12, s);
                break;
            case RECENT:
                rect.set(3, 5, 15, 17);
                c.drawRoundRect(rect, 2, 2, s);
                rect.set(16.5f, 9, 21, 19);
                c.drawRoundRect(rect, 1.5f, 1.5f, s);
                c.drawLine(7, 20.5f, 11, 20.5f, s);
                break;
            case MIC:
                rect.set(9, 3, 15, 14);
                c.drawRoundRect(rect, 3, 3, s);
                rect.set(6, 6, 18, 18);
                c.drawArc(rect, 20, 140, false, s);
                c.drawLine(12, 18, 12, 21, s);
                break;
            case GLOBE:
                c.drawCircle(12, 12, 9, s);
                rect.set(8, 3, 16, 21);
                c.drawOval(rect, s);
                c.drawLine(3, 12, 21, 12, s);
                break;
            case FOLDER:
                path.reset();
                path.moveTo(3.5f, 18.5f); path.lineTo(3.5f, 6); path.lineTo(9.5f, 6); path.lineTo(11.5f, 8.5f);
                path.lineTo(20.5f, 8.5f); path.lineTo(20.5f, 18.5f); path.close();
                c.drawPath(path, s);
                break;
            case CHECK:
                c.drawLine(5, 12.5f, 10, 17.5f, s);
                c.drawLine(10, 17.5f, 19, 7, s);
                break;
            case COPY:
                rect.set(8, 8, 19, 20);
                c.drawRoundRect(rect, 2, 2, s);
                path.reset();
                path.moveTo(15, 8); path.lineTo(15, 5); path.lineTo(5.5f, 5); path.lineTo(5.5f, 16); path.lineTo(8, 16);
                c.drawPath(path, s);
                break;
            case OPEN:
                path.reset();
                path.moveTo(18, 14); path.lineTo(18, 20); path.lineTo(4, 20); path.lineTo(4, 6); path.lineTo(10, 6);
                c.drawPath(path, s);
                c.drawLine(13, 4, 20, 4, s);
                c.drawLine(20, 4, 20, 11, s);
                c.drawLine(20, 4, 10.5f, 13.5f, s);
                break;
            case SORT:
                c.drawLine(4, 7, 20, 7, s);
                c.drawLine(7, 12, 17, 12, s);
                c.drawLine(10, 17, 14, 17, s);
                break;
            case FOLDER_PLUS:
                path.reset();
                path.moveTo(3.5f, 18.5f); path.lineTo(3.5f, 6); path.lineTo(9.5f, 6); path.lineTo(11.5f, 8.5f);
                path.lineTo(20.5f, 8.5f); path.lineTo(20.5f, 18.5f); path.close();
                c.drawPath(path, s);
                c.drawLine(12, 11.5f, 12, 16.5f, s);
                c.drawLine(9.5f, 14, 14.5f, 14, s);
                break;
            case READING:
                rect.set(4, 4, 20, 20);
                c.drawRoundRect(rect, 3, 3, s);
                c.drawCircle(8.5f, 9, 0.9f, f);
                c.drawCircle(8.5f, 12.5f, 0.9f, f);
                c.drawCircle(8.5f, 16, 0.9f, f);
                c.drawLine(11.5f, 9, 16.5f, 9, s);
                c.drawLine(11.5f, 12.5f, 16.5f, 12.5f, s);
                c.drawLine(11.5f, 16, 16.5f, 16, s);
                break;
            case PENCIL:
                path.reset();
                path.moveTo(4, 20); path.lineTo(4.8f, 15.8f); path.lineTo(16, 4.6f); path.lineTo(19.4f, 8);
                path.lineTo(8.2f, 19.2f); path.close();
                c.drawPath(path, s);
                c.drawLine(13.5f, 7, 17, 10.5f, s);
                break;
            case UNGROUP:
                rect.set(4, 9, 15, 20);
                c.drawRoundRect(rect, 2, 2, s);
                c.drawLine(13, 11, 20, 4, s);
                c.drawLine(14, 4, 20, 4, s);
                c.drawLine(20, 4, 20, 10, s);
                break;
            default:
                break;
        }
        c.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        stroke.setAlpha(alpha);
        fill.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        stroke.setColorFilter(cf);
        fill.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
