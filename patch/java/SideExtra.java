package com.bossxor.scrollbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Sidebar extras: folder favorites, font choice; file list read-progress; font lookup for viewer. */
public final class SideExtra {
    private SideExtra() {}

    private static final String[] FONT_KEYS = {"monospace", "sans-serif", "serif", "sans-serif-light", "cursive", "casual"};
    private static final String[] FONT_NAMES = {"기본 (고정폭)", "고딕", "명조", "얇은 고딕", "필기체", "캐주얼"};

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("scrollbox_ui", 0);
    }

    // ---- viewer font (called from ji constructor) ----
    public static Typeface font() {
        try {
            Context c = SbUx.appCtx();
            String k = c == null ? "monospace" : sp(c).getString("font", "monospace");
            return Typeface.create(k, Typeface.NORMAL);
        } catch (Throwable t) {
            return Typeface.create("monospace", Typeface.NORMAL);
        }
    }

    // ---- file list: append read progress to the date text ----
    private static Field fDir, fArr, fCnt;

    public static String prog(String date, String name) {
        try {
            if (fDir == null) {
                fDir = TIVFileListActivity.class.getDeclaredField("m");
                fDir.setAccessible(true);
                Class<?> ij = Class.forName("com.bossxor.scrollbox.ij");
                fArr = ij.getDeclaredField("p");
                fArr.setAccessible(true);
                fCnt = ij.getDeclaredField("q");
                fCnt.setAccessible(true);
            }
            String path = fDir.get(null) + name;
            Object arr = fArr.get(null);
            int n = Math.min(fCnt.getInt(null), Array.getLength(arr));
            for (int i = 0; i < n; i++) {
                Object e = Array.get(arr, i);
                if (e == null) continue;
                Class<?> k = e.getClass();
                if (path.equals(k.getField("c").get(e))) {
                    int tot = k.getField("f").getInt(e), cur = k.getField("g").getInt(e);
                    if (tot > 0 && cur > 0) return date + "  ·  읽음 " + Math.min(100, cur * 100 / tot) + "%";
                    break;
                }
            }
        } catch (Throwable t) { /* no progress shown */ }
        return date;
    }

    // ---- sidebar ----
    private static String[] favs(Context c) {
        String s = sp(c).getString("favs", "");
        return s.length() == 0 ? new String[0] : s.split("\n");
    }

    private static void saveFavs(Context c, java.util.List<String> l) {
        StringBuilder b = new StringBuilder();
        for (String s : l) { if (b.length() > 0) b.append('\n'); b.append(s); }
        sp(c).edit().putString("favs", b.toString()).commit();
    }

    private static String cwd() {
        try {
            Field f = TIVFileListActivity.class.getDeclaredField("m");
            f.setAccessible(true);
            return (String) f.get(null);
        } catch (Throwable t) { return null; }
    }

    private static int dp(Context c, int d) {
        return (int) (d * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static TextView row(final Context c, String s, int size, int color, int padV) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(dp(c, 24), dp(c, padV), dp(c, 24), dp(c, padV));
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        return t;
    }

    private static View id(Activity a, String n) {
        int i = a.getResources().getIdentifier(n, "id", a.getPackageName());
        return i == 0 ? null : a.findViewById(i);
    }

    public static void setup(final Activity a) {
        try {
            View anchor = id(a, "btn_menu_setting");
            if (anchor == null) return;
            final LinearLayout list = (LinearLayout) anchor.getParent();
            final LinearLayout box = new LinearLayout(a);
            box.setOrientation(LinearLayout.VERTICAL);
            list.addView(box, 0);
            refresh(a, box);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void refresh(final Activity a, final LinearLayout box) {
        box.removeAllViews();
        box.addView(row(a, "즐겨찾기 폴더", 13, 0x99ffffff, 14));
        final java.util.ArrayList<String> fl = new java.util.ArrayList<String>(java.util.Arrays.asList(favs(a)));
        for (final String p : fl) {
            String nm = new File(p).getName();
            TextView t = row(a, "▸  " + (nm.length() == 0 ? p : nm), 17, 0xffffffff, 14);
            t.setClickable(true);
            t.setBackgroundResource(sel(a));
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { open(a, p); }
            });
            t.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    fl.remove(p);
                    saveFavs(a, fl);
                    refresh(a, box);
                    Toast.makeText(a, "즐겨찾기 해제", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            box.addView(t);
        }
        TextView add = row(a, "＋ 현재 폴더 즐겨찾기 추가", 15, 0xff7fc4ff, 14);
        add.setClickable(true);
        add.setBackgroundResource(sel(a));
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String d = cwd();
                if (d == null || d.length() == 0) return;
                if (fl.contains(d)) { Toast.makeText(a, "이미 추가됨", Toast.LENGTH_SHORT).show(); return; }
                fl.add(d);
                saveFavs(a, fl);
                refresh(a, box);
            }
        });
        box.addView(add);
        box.addView(row(a, "길게 누르면 해제", 12, 0x66ffffff, 4));
        box.addView(line(a));

        String cur = sp(a).getString("font", "monospace");
        String curName = FONT_NAMES[0];
        for (int i = 0; i < FONT_KEYS.length; i++) if (FONT_KEYS[i].equals(cur)) curName = FONT_NAMES[i];
        TextView font = row(a, "뷰어 글꼴  ·  " + curName, 17, 0xffffffff, 17);
        font.setClickable(true);
        font.setBackgroundResource(sel(a));
        font.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(a).setTitle("뷰어 글꼴")
                        .setItems(FONT_NAMES, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                sp(a).edit().putString("font", FONT_KEYS[w]).commit();
                                refresh(a, box);
                                Toast.makeText(a, "다음에 여는 글부터 적용됩니다", Toast.LENGTH_SHORT).show();
                            }
                        }).show();
            }
        });
        box.addView(font);
        box.addView(line(a));
    }

    private static View line(Context c) {
        View v = new View(c);
        v.setBackgroundColor(0x22ffffff);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    private static int sel(Context c) {
        android.util.TypedValue tv = new android.util.TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        return tv.resourceId;
    }

    private static void open(Activity a, String path) {
        try {
            if (!new File(path).isDirectory()) { Toast.makeText(a, "폴더가 없습니다", Toast.LENGTH_SHORT).show(); return; }
            if (!path.endsWith("/")) path = path + "/";
            View tab = id(a, "tab_explorer");
            if (tab != null) tab.performClick();
            Method m = a.getClass().getMethod("goDir", String.class);
            m.invoke(a, path);
            View dl = id(a, "dl_activity_main_drawer");
            if (dl != null) {
                try { dl.getClass().getMethod("b").invoke(dl); } catch (Throwable t) { /* drawer stays open */ }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
