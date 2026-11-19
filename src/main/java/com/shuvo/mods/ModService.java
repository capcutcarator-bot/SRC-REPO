package com.shuvo.mods;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.util.TypedValue;

public class ModService extends Service {

    interface ModToggle { void onToggle(boolean value); }

    static { System.loadLibrary("SHUVOxMODS"); }

    public static native void setGodMode(boolean v);
    public static native void setFreezeGranny(boolean v);
    public static native void setHighSpeed(boolean v);
    public static native void setNoGravity(boolean v);
    public static native void setNoBloodScreen(boolean v);
    public static native void setGrannyStun(boolean v);
    public static native void setEsp(boolean v);
    public static native void setSilentMove(boolean v);

    private WindowManager wm;
    private View menuView, toggleBtn;
    private boolean menuVisible = false;
    private float dX, dY;
    private int lastX, lastY;
    private WindowManager.LayoutParams menuParams, btnParams;

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildToggleButton();
        buildMenu();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        super.onDestroy();
        if (menuView  != null) try { wm.removeView(menuView);  } catch (Exception ignored) {}
        if (toggleBtn != null) try { wm.removeView(toggleBtn); } catch (Exception ignored) {}
    }

    private void buildToggleButton() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        btnParams = new WindowManager.LayoutParams(dp(52), dp(52), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        btnParams.gravity = Gravity.TOP | Gravity.START;
        btnParams.x = dp(16); btnParams.y = dp(80);
        TextView fab = new TextView(this);
        fab.setText("S"); fab.setTextColor(Color.WHITE);
        fab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        fab.setTypeface(null, Typeface.BOLD); fab.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL); bg.setColor(0xFF0A0A0A); bg.setStroke(dp(2), 0xFF00FFCC);
        fab.setBackground(bg);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (menuVisible) hideMenu(); else showMenu(); }
        });
        setupDrag(fab, btnParams);
        toggleBtn = fab; wm.addView(toggleBtn, btnParams);
    }

    private void buildMenu() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        menuParams = new WindowManager.LayoutParams(dp(280),
                WindowManager.LayoutParams.WRAP_CONTENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.TOP | Gravity.START;
        menuParams.x = dp(80); menuParams.y = dp(80);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(2), dp(2), dp(2), dp(2));
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(0xEE0A0A0A); rootBg.setCornerRadius(dp(14)); rootBg.setStroke(dp(1), 0xFF00FFCC);
        root.setBackground(rootBg);
        TextView header = new TextView(this);
        header.setText("⚡ SHUVOxMODS"); header.setTextColor(0xFF00FFCC);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        header.setTypeface(null, Typeface.BOLD); header.setGravity(Gravity.CENTER);
        header.setPadding(dp(10), dp(10), dp(10), dp(6)); root.addView(header);
        TextView sub = new TextView(this);
        sub.setText("Granny 3 • v1.3 • arm64"); sub.setTextColor(0xFF666666);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9); sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, dp(8)); root.addView(sub);
        root.addView(divider());
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout items = new LinearLayout(this);
        items.setOrientation(LinearLayout.VERTICAL);
        items.setPadding(dp(10), dp(6), dp(10), dp(10));
        items.addView(sectionLabel("⚔ PLAYER"));
        items.addView(modRow("God Mode",        "No damage, can't be caught",  new ModToggle() { @Override public void onToggle(boolean v) { setGodMode(v); } }));
        items.addView(modRow("High Speed",      "3x move speed",               new ModToggle() { @Override public void onToggle(boolean v) { setHighSpeed(v); } }));
        items.addView(modRow("No Gravity",      "Float freely",                new ModToggle() { @Override public void onToggle(boolean v) { setNoGravity(v); } }));
        items.addView(modRow("No Blood Screen", "Remove red vignette",         new ModToggle() { @Override public void onToggle(boolean v) { setNoBloodScreen(v); } }));
        items.addView(sectionLabel("👵 GRANNY"));
        items.addView(modRow("Freeze Granny",   "Lock Granny in place",        new ModToggle() { @Override public void onToggle(boolean v) { setFreezeGranny(v); } }));
        items.addView(modRow("Stun Granny",     "Keep her permanently stunned",new ModToggle() { @Override public void onToggle(boolean v) { setGrannyStun(v); } }));
        items.addView(modRow("Silent Move",     "Granny can't hear you",       new ModToggle() { @Override public void onToggle(boolean v) { setSilentMove(v); } }));
        items.addView(sectionLabel("🔍 UTILS"));
        items.addView(modRow("ESP / Logcat",    "Log enemy coords to logcat",  new ModToggle() { @Override public void onToggle(boolean v) { setEsp(v); } }));
        scroll.addView(items); root.addView(scroll);
        root.addView(divider());
        TextView close = new TextView(this);
        close.setText("✕  CLOSE"); close.setTextColor(0xFFFF4444);
        close.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        close.setTypeface(null, Typeface.BOLD); close.setGravity(Gravity.CENTER);
        close.setPadding(0, dp(8), 0, dp(10));
        close.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { hideMenu(); } });
        root.addView(close);
        setupDrag(root, menuParams);
        menuView = root; menuView.setVisibility(View.GONE);
        wm.addView(menuView, menuParams);
    }

    private LinearLayout modRow(String title, String desc, final ModToggle cb) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView tv = new TextView(this); tv.setText(title); tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); tv.setTypeface(null, Typeface.BOLD);
        textBlock.addView(tv);
        TextView dv = new TextView(this); dv.setText(desc); dv.setTextColor(0xFF888888);
        dv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9); textBlock.addView(dv);
        row.addView(textBlock);
        Switch sw = new Switch(this);
        sw.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFF00FFCC));
        sw.setTrackTintList(android.content.res.ColorStateList.valueOf(0xFF1A1A1A));
        sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean checked) { cb.onToggle(checked); }
        });
        row.addView(sw);
        return row;
    }

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this); tv.setText(text); tv.setTextColor(0xFF00FFCC);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10); tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dp(10), 0, dp(4)); return tv;
    }

    private View divider() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        v.setBackgroundColor(0xFF1A1A1A); return v;
    }

    private void setupDrag(View v, final WindowManager.LayoutParams p) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View view, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = e.getRawX() - p.x; dY = e.getRawY() - p.y;
                        lastX = p.x; lastY = p.y; return false;
                    case MotionEvent.ACTION_MOVE:
                        p.x = (int)(e.getRawX() - dX); p.y = (int)(e.getRawY() - dY);
                        wm.updateViewLayout(view, p); return true;
                    case MotionEvent.ACTION_UP:
                        if (Math.abs(p.x - lastX) < 5 && Math.abs(p.y - lastY) < 5) return false;
                        return true;
                }
                return false;
            }
        });
    }

    private void showMenu() { menuVisible = true;  menuView.setVisibility(View.VISIBLE); }
    private void hideMenu() { menuVisible = false; menuView.setVisibility(View.GONE); }
    private int dp(int px) { return (int)(px * getResources().getDisplayMetrics().density); }
}
