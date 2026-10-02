package com.bossxor.scrollbox;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Lock settings: choose PIN+biometric OR pattern+biometric (not both). */
public final class LockUi {
    private LockUi() {}

    private static View v(Activity a, String n) {
        int id = a.getResources().getIdentifier(n, "id", a.getPackageName());
        return id == 0 ? null : a.findViewById(id);
    }

    public static void setup(final Activity a) {
        try {
            View pinEdit = v(a, "editPassword");
            final View delBtn = v(a, "btnSetPatternDelete");
            final EditText edit = (EditText) pinEdit;
            final View pinRow = (View) pinEdit.getParent();
            final LinearLayout col = (LinearLayout) pinRow.getParent();
            final int base = col.indexOfChild(pinRow);
            final View[] pat = new View[4]; // label, info, pattern view, button row
            for (int i = 0; i < 4; i++) pat[i] = col.getChildAt(base + 1 + i);

            String line = LockStore.readLockLine(a);
            String pin = LockStore.pinFromLine(line);
            boolean patternMode = LockStore.getPattern(a).length() > 0
                    && (pin == null || pin.length() == 0 || "9999".equals(pin));

            final TextView bPin = tab(a, "비밀번호 (PIN)");
            final TextView bPat = tab(a, "패턴");
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(a, 20), dp(a, 20), dp(a, 20), 0);
            row.addView(bPin, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            row.addView(bPat, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView hint = new TextView(a);
            hint.setText("잠금 방식 선택 (생체 인증은 선택한 방식에 추가로 사용)");
            hint.setTextColor(0xffcccccc);
            hint.setTextSize(14);
            hint.setPadding(dp(a, 20), dp(a, 20), 0, 0);
            col.addView(hint, base);
            col.addView(row, base + 1);

            final boolean[] mode = {patternMode};
            Runnable apply = new Runnable() {
                public void run() {
                    ((TextView) bPin).setBackgroundColor(mode[0] ? 0xff333333 : 0xff2f6f9f);
                    ((TextView) bPat).setBackgroundColor(mode[0] ? 0xff2f6f9f : 0xff333333);
                    pinRow.setVisibility(mode[0] ? View.GONE : View.VISIBLE);
                    for (View p : pat) p.setVisibility(mode[0] ? View.VISIBLE : View.GONE);
                }
            };
            bPin.setOnClickListener(new View.OnClickListener() {
                public void onClick(View x) {
                    if (mode[0] && delBtn != null) delBtn.performClick();
                    mode[0] = false;
                    apply.run();
                }
            });
            bPat.setOnClickListener(new View.OnClickListener() {
                public void onClick(View x) {
                    mode[0] = true;
                    edit.setText(""); // PIN cleared -> saved as pattern-only
                    apply.run();
                }
            });
            apply.run();

            CheckBox chk = (CheckBox) v(a, "chkBiometric");
            if (chk != null) chk.setText("생체 인증 사용 (지문 성공 시 바로 입장, 실패 시 PIN/패턴)");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static TextView tab(Context c, String s) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(16);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(c, 12), 0, dp(c, 12));
        t.setClickable(true);
        return t;
    }

    private static int dp(Context c, int d) {
        return (int) (d * c.getResources().getDisplayMetrics().density + 0.5f);
    }
}
