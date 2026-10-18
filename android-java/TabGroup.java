package __APP_ID__;

import java.util.ArrayList;

/** مجموعة تبويبات: عنوان ولون، وعند «إغلاقها» تُحفظ روابط تبويباتها لاستعادتها لاحقًا. */
final class TabGroup {
    private static int counter = 0;

    static final int[] PALETTE = {0xFFDADCE0, 0xFF8AB4F8, 0xFFF28B82, 0xFFFDD663, 0xFF81C995, 0xFFFF8BCB, 0xFFC58AF9, 0xFF78D9EC, 0xFFFCAD70};
    static final String[] COLOR_NAMES_EN = {"Grey", "Blue", "Red", "Yellow", "Green", "Pink", "Purple", "Cyan", "Orange"};
    static final String[] COLOR_NAMES_AR = {"رمادي", "أزرق", "أحمر", "أصفر", "أخضر", "وردي", "بنفسجي", "سماوي", "برتقالي"};

    int id = ++counter;
    String title = "";
    int color = PALETTE[0];
    long updated = System.currentTimeMillis();
    boolean closed;
    final ArrayList<String[]> saved = new ArrayList<>(); // {العنوان، الرابط}

    static void bump(int seen) {
        if (seen > counter) counter = seen;
    }
}
