package tv.aurora.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(8, 13, 27);
    private final int white = Color.WHITE;
    private final List<ResolveInfo> apps = new ArrayList<>();
    private LinearLayout root;
    private LinearLayout rail;
    private TextView clock;
    private final Handler handler = new Handler();
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (clock != null) clock.setText(new SimpleDateFormat("EEE, d MMM   •   HH:mm", Locale.getDefault()).format(new Date()));
            handler.postDelayed(this, 30000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setNavigationBarColor(bg);
        loadApps();
        buildHome();
        handler.post(ticker);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent leanback = new Intent(Intent.ACTION_MAIN);
        leanback.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> tv = pm.queryIntentActivities(leanback, 0);
        Intent standard = new Intent(Intent.ACTION_MAIN);
        standard.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> normal = pm.queryIntentActivities(standard, 0);
        for (ResolveInfo r : tv) addUnique(r);
        for (ResolveInfo r : normal) addUnique(r);
        Collections.sort(apps, (a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
    }

    private void addUnique(ResolveInfo item) {
        for (ResolveInfo old : apps) {
            if (old.activityInfo.packageName.equals(item.activityInfo.packageName)) return;
        }
        if (!item.activityInfo.packageName.equals(getPackageName())) apps.add(item);
    }

    private GradientDrawable panel(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void buildHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(54, 28, 54, 34);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(15, 31, 61), Color.rgb(8, 13, 27), Color.rgb(20, 12, 37)}));
        setContentView(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("AURORA", 22, white, true);
        brand.setLetterSpacing(0.18f);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        clock = text("", 15, 0xFFCFD8EA, false);
        top.addView(clock);
        TextView profile = text("   ●  Guest", 15, 0xFFE2E8F5, false);
        top.addView(profile);
        root.addView(top);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(0, 34, 0, 22);
        for (String item : new String[]{"Home", "Movies", "TV Shows", "Apps"}) {
            TextView tab = text(item, 16, item.equals("Home") ? white : 0xFF9EABC5, item.equals("Home"));
            tab.setPadding(0, 0, 30, 0);
            nav.addView(tab);
        }
        root.addView(nav);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(32, 24, 32, 24);
        hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF233E70, 0xFF182849, 0xFF111A30}));
        TextView eyebrow = text("YOUR ENTERTAINMENT SPACE", 12, 0xFFB8C9E8, true);
        eyebrow.setLetterSpacing(0.15f);
        hero.addView(eyebrow);
        TextView headline = text("A world of entertainment.\nOne beautiful home.", 29, white, true);
        headline.setPadding(0, 14, 0, 10);
        hero.addView(headline);
        TextView sub = text("Open your installed apps with your remote.", 15, 0xFFD1D9E8, false);
        hero.addView(sub);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, 190);
        hp.bottomMargin = 24;
        root.addView(hero, hp);

        TextView label = text("YOUR APPS", 14, 0xFFB9C7DF, true);
        label.setLetterSpacing(0.14f);
        root.addView(label);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.HORIZONTAL);
        rail.setPadding(0, 16, 0, 14);
        for (ResolveInfo app : apps) rail.addView(appCard(app));
        hsv.addView(rail);
        root.addView(hsv, new LinearLayout.LayoutParams(-1, 145));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView library = action("App Library", () -> showLibrary());
        TextView refresh = action("Refresh apps", () -> { loadApps(); buildHome(); });
        TextView settings = action("Settings", () -> openSettings());
        actions.addView(library);
        actions.addView(refresh);
        actions.addView(settings);
        root.addView(actions);
        if (apps.isEmpty()) {
            TextView empty = text("No launchable apps found. Check your TV app installation.", 15, 0xFFD1D9E8, false);
            root.addView(empty);
        }
        if (rail.getChildCount() > 0) rail.getChildAt(0).requestFocus();
    }

    private TextView action(String label, Runnable run) {
        TextView v = text(label, 14, white, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(20, 13, 20, 13);
        v.setBackground(panel(0xFF263653, 24));
        v.setFocusable(true);
        v.setOnClickListener(w -> run.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.rightMargin = 12;
        v.setLayoutParams(p);
        return v;
    }

    private View appCard(ResolveInfo app) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setFocusable(true);
        card.setPadding(12, 10, 12, 8);
        card.setBackground(panel(0xFF1B2943, 18));
        android.graphics.drawable.Drawable icon = app.loadIcon(getPackageManager());
        android.widget.ImageView image = new android.widget.ImageView(this);
        image.setImageDrawable(icon);
        card.addView(image, new LinearLayout.LayoutParams(58, 58));
        TextView name = text(app.loadLabel(getPackageManager()).toString(), 12, white, true);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(1);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(112, -2);
        np.topMargin = 7;
        card.addView(name, np);
        card.setOnClickListener(v -> launch(app));
        card.setOnFocusChangeListener((v, focused) -> {
            v.animate().scaleX(focused ? 1.12f : 1f).scaleY(focused ? 1.12f : 1f).setDuration(140).start();
            v.setBackground(panel(focused ? 0xFF3C5F96 : 0xFF1B2943, 18));
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(132, 122);
        cp.rightMargin = 14;
        card.setLayoutParams(cp);
        return card;
    }

    private void launch(ResolveInfo app) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setClassName(app.activityInfo.packageName, app.activityInfo.name);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); } catch (Exception ignored) { }
    }

    private void showLibrary() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(28, 18, 28, 18);
        for (ResolveInfo app : apps) {
            TextView row = text("  " + app.loadLabel(getPackageManager()), 18, white, false);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(14, 15, 14, 15);
            row.setFocusable(true);
            row.setOnClickListener(v -> launch(app));
            list.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        scroll.addView(list);
        new AlertDialog.Builder(this).setTitle("App Library").setView(scroll)
                .setNegativeButton("Close", null).show();
    }

    private void openSettings() {
        Intent i = new Intent(android.provider.Settings.ACTION_SETTINGS);
        try { startActivity(i); } catch (Exception e) {
            new AlertDialog.Builder(this).setMessage("Settings are not available on this device.")
                    .setPositiveButton("OK", null).show();
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            new AlertDialog.Builder(this).setTitle("AURORA Control Center")
                    .setItems(new String[]{"Android Settings", "Refresh applications", "About AURORA TV"},
                            (d, which) -> {
                                if (which == 0) openSettings();
                                else if (which == 1) { loadApps(); buildHome(); }
                                else new AlertDialog.Builder(this).setMessage("AURORA TV 0.5.0\nIndependent Android TV launcher.")
                                        .setPositiveButton("OK", null).show();
                            }).show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
