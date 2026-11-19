package tv.aurora.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.GridLayout;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(8, 13, 27);
    private final int white = Color.WHITE;
    private final List<ResolveInfo> apps = new ArrayList<>();
    private final Set<String> favorites = new HashSet<>();
    private static final String PREFS = "aurora_launcher";
    private static final String PROFILES_KEY = "profiles";
    private static final String ACTIVE_PROFILE_KEY = "active_profile";
    private String activeProfileId = "guest";
    private LinearLayout root;
    private LinearLayout rail;
    private TextView clock;
    private final Handler handler = new Handler();
    private Dialog screensaverDialog;
    private final Runnable screensaverTask = this::showScreensaver;
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
        initializeProfiles();
        loadFavorites();
        loadApps();
        buildHome();
        handler.post(ticker);
        resetScreensaverTimer();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        handler.removeCallbacks(screensaverTask);
        if (screensaverDialog != null && screensaverDialog.isShowing()) screensaverDialog.dismiss();
        super.onDestroy();
    }

    @Override protected void onResume() {
        super.onResume();
        resetScreensaverTimer();
    }

    @Override protected void onPause() {
        handler.removeCallbacks(screensaverTask);
        if (screensaverDialog != null && screensaverDialog.isShowing()) screensaverDialog.dismiss();
        super.onPause();
    }

    private void resetScreensaverTimer() {
        handler.removeCallbacks(screensaverTask);
        if (!isFinishing()) handler.postDelayed(screensaverTask, 120000);
    }

    private void showScreensaver() {
        if (isFinishing() || isDestroyed() || (screensaverDialog != null && screensaverDialog.isShowing())) return;
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setGravity(Gravity.CENTER);
        view.setPadding(40, 40, 40, 40);
        view.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF101D38, 0xFF080D1B, 0xFF21152F}));
        TextView logo = text("A U R O R A", 18, 0xFFB8C9E8, true);
        logo.setLetterSpacing(0.28f);
        view.addView(logo);
        TextView time = text(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()),
                76, white, false);
        time.setPadding(0, 18, 0, 4);
        view.addView(time);
        TextView date = text(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date()),
                17, 0xFFCFD8EA, false);
        view.addView(date);
        TextView hint = text("Press any button to continue", 13, 0xFF899AB8, false);
        hint.setPadding(0, 36, 0, 0);
        view.addView(hint);
        view.setFocusableInTouchMode(true);
        view.setOnClickListener(v -> dismissScreensaver());
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                dismissScreensaver();
                return true;
            }
            return true;
        });
        screensaverDialog = new Dialog(this);
        screensaverDialog.setContentView(view);
        screensaverDialog.setCancelable(false);
        screensaverDialog.show();
        view.requestFocus();
        if (screensaverDialog.getWindow() != null) {
            screensaverDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            screensaverDialog.getWindow().setLayout(-1, -1);
            WindowManager.LayoutParams lp = screensaverDialog.getWindow().getAttributes();
            lp.dimAmount = 0f;
            screensaverDialog.getWindow().setAttributes(lp);
        }
    }

    private void dismissScreensaver() {
        if (screensaverDialog != null && screensaverDialog.isShowing()) {
            screensaverDialog.dismiss();
            screensaverDialog = null;
            resetScreensaverTimer();
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (screensaverDialog != null && screensaverDialog.isShowing()) {
                dismissScreensaver();
                return true;
            }
            resetScreensaverTimer();
        }
        return super.dispatchKeyEvent(event);
    }

    private void initializeProfiles() {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        Set<String> ids = new HashSet<>(p.getStringSet(PROFILES_KEY, new HashSet<>()));
        if (ids.isEmpty()) {
            ids.add("guest");
            Set<String> legacyFavorites = new HashSet<>(p.getStringSet("favorites", new HashSet<>()));
            p.edit().putStringSet(PROFILES_KEY, ids).putString("profile_name_guest", "Guest")
                    .putStringSet("favorites_guest", legacyFavorites)
                    .putString(ACTIVE_PROFILE_KEY, "guest").apply();
        }
        activeProfileId = p.getString(ACTIVE_PROFILE_KEY, ids.iterator().next());
        if (!ids.contains(activeProfileId)) {
            activeProfileId = ids.iterator().next();
            p.edit().putString(ACTIVE_PROFILE_KEY, activeProfileId).apply();
        }
    }

    private String profileName(String id) {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getString("profile_name_" + id, "Profile");
    }

    private void loadFavorites() {
        favorites.clear();
        favorites.addAll(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getStringSet("favorites_" + activeProfileId, new HashSet<>()));
    }

    private void saveFavorites() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putStringSet("favorites_" + activeProfileId, new HashSet<>(favorites)).apply();
    }

    private void showProfiles() {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        Set<String> ids = new HashSet<>(p.getStringSet(PROFILES_KEY, new HashSet<>()));
        final List<String> profileIds = new ArrayList<>(ids);
        Collections.sort(profileIds, (a, b) -> profileName(a).compareToIgnoreCase(profileName(b)));
        String[] names = new String[profileIds.size()];
        for (int i = 0; i < profileIds.size(); i++) {
            String id = profileIds.get(i);
            names[i] = (id.equals(activeProfileId) ? "✓  " : "     ") + profileName(id);
        }
        new AlertDialog.Builder(this).setTitle("Choose profile")
                .setItems(names, (d, which) -> {
                    String selected = profileIds.get(which);
                    if (!selected.equals(activeProfileId)) {
                        activeProfileId = selected;
                        p.edit().putString(ACTIVE_PROFILE_KEY, activeProfileId).apply();
                        loadFavorites();
                        buildHome();
                    }
                })
                .setPositiveButton("Add profile", (d, w) -> showCreateProfile())
                .setNeutralButton("Delete active", (d, w) -> deleteActiveProfile())
                .setNegativeButton("Close", null).show();
    }

    private void showCreateProfile() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Profile name");
        input.setTextColor(white);
        input.setHintTextColor(0xFF9EABC5);
        new AlertDialog.Builder(this).setTitle("New profile").setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "Profile";
                    String id = UUID.randomUUID().toString();
                    android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
                    Set<String> ids = new HashSet<>(p.getStringSet(PROFILES_KEY, new HashSet<>()));
                    ids.add(id);
                    p.edit().putStringSet(PROFILES_KEY, ids).putString("profile_name_" + id, name)
                            .putString(ACTIVE_PROFILE_KEY, id).apply();
                    activeProfileId = id;
                    loadFavorites();
                    buildHome();
                }).show();
    }

    private void deleteActiveProfile() {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        Set<String> ids = new HashSet<>(p.getStringSet(PROFILES_KEY, new HashSet<>()));
        if (ids.size() <= 1) {
            new AlertDialog.Builder(this).setMessage("At least one profile must remain.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Delete " + profileName(activeProfileId) + "?")
                .setMessage("This removes this profile and its saved favorites from this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    String removed = activeProfileId;
                    ids.remove(removed);
                    String next = ids.iterator().next();
                    p.edit().putStringSet(PROFILES_KEY, ids).putString(ACTIVE_PROFILE_KEY, next)
                            .remove("profile_name_" + removed).remove("favorites_" + removed).apply();
                    activeProfileId = next;
                    loadFavorites();
                    buildHome();
                }).show();
    }

    private void toggleFavorite(ResolveInfo app) {
        String pkg = app.activityInfo.packageName;
        if (favorites.contains(pkg)) {
            favorites.remove(pkg);
            android.widget.Toast.makeText(this, "Removed from Favorites", android.widget.Toast.LENGTH_SHORT).show();
        } else {
            favorites.add(pkg);
            android.widget.Toast.makeText(this, "Added to Favorites", android.widget.Toast.LENGTH_SHORT).show();
        }
        saveFavorites();
        buildHome();
    }

    private void loadApps() {
        apps.clear();
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

    private ResolveInfo findApp(String packageName) {
        if (packageName == null) return null;
        for (ResolveInfo app : apps) {
            if (packageName.equals(app.activityInfo.packageName)) return app;
        }
        return null;
    }

    private TextView navAction(String label, Runnable action) {
        TextView tab = text(label, 17, 0xFFCFD8EA, true);
        tab.setFocusable(true);
        tab.setClickable(true);
        tab.setPadding(22, 14, 22, 14);
        tab.setElevation(3f);
        tab.setBackground(panel(0x001B2943, 24));
        tab.setOnClickListener(v -> action.run());
        tab.setOnFocusChangeListener((v, focused) -> {
            tab.setTextColor(focused ? white : 0xFF9EABC5);
            tab.setBackground(panel(focused ? 0xFF527DB8 : 0x332B3D5B, 28));
            tab.setElevation(focused ? 10f : 2f);
            tab.animate().scaleX(focused ? 1.06f : 1f).scaleY(focused ? 1.06f : 1f).setDuration(130).start();
        });
        return tab;
    }

    
    private int[] themeColors() {
        String theme = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString("theme_" + activeProfileId, "aurora");
        if ("midnight".equals(theme)) return new int[]{0xFF080B14, 0xFF111827, 0xFF090D18, 0xFF1A2235};
        if ("ocean".equals(theme)) return new int[]{0xFF061B2B, 0xFF0A3048, 0xFF071522, 0xFF123B56};
        return new int[]{0xFF0F1F3D, 0xFF080D1B, 0xFF140C25, 0xFF233E70};
    }

    private void showThemePicker() {
        String[] labels = {"Aurora", "Midnight", "Ocean"};
        String[] values = {"aurora", "midnight", "ocean"};
        String current = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString("theme_" + activeProfileId, "aurora");
        int selected = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) selected = i;
        new AlertDialog.Builder(this).setTitle("Choose your theme")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString("theme_" + activeProfileId, values[which]).apply();
                    dialog.dismiss();
                    buildHome();
                }).setNegativeButton("Close", null).show();
    }


    private void showDialogRestoringFocus(AlertDialog dialog, View fallback) {
        View previousFocus = getCurrentFocus();
        dialog.setOnDismissListener(d -> {
            View target = previousFocus != null && previousFocus.isShown() ? previousFocus : fallback;
            if (target != null && target.isShown()) target.requestFocus();
        });
        dialog.show();
    }

    private void showAppDetails(ResolveInfo app) {
        String name = app.loadLabel(getPackageManager()).toString();
        String pkg = app.activityInfo.packageName;
        String version = "Version information unavailable";
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(pkg, 0);
            version = "Version " + (info.versionName == null ? "Unknown" : info.versionName)
                    + "  ·  Build " + info.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) { }

        LinearLayout detailsView = new LinearLayout(this);
        detailsView.setOrientation(LinearLayout.HORIZONTAL);
        detailsView.setGravity(Gravity.CENTER_VERTICAL);
        detailsView.setPadding(8, 12, 8, 8);

        android.widget.ImageView appIcon = new android.widget.ImageView(this);
        appIcon.setImageDrawable(app.loadIcon(getPackageManager()));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(76, 76);
        iconParams.rightMargin = 18;
        detailsView.addView(appIcon, iconParams);

        LinearLayout metadata = new LinearLayout(this);
        metadata.setOrientation(LinearLayout.VERTICAL);
        TextView packageName = text(pkg, 14, white, true);
        packageName.setMaxLines(2);
        metadata.addView(packageName);
        TextView versionText = text(version, 12, 0xFFB8C7DF, false);
        versionText.setPadding(0, 8, 0, 0);
        metadata.addView(versionText);
        detailsView.addView(metadata, new LinearLayout.LayoutParams(0, -2, 1f));

        new AlertDialog.Builder(this).setTitle(name).setView(detailsView)
                .setPositiveButton("Open", (dialog, which) -> launch(app))
                .setNeutralButton("App settings", (dialog, which) -> {
                    try {
                        Intent details = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        details.setData(android.net.Uri.parse("package:" + pkg));
                        startActivity(details);
                    } catch (Exception ignored) { openSettings(); }
                }).setNegativeButton("Close", null).show();
    }

    private void buildHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(72, 34, 72, 42);
        int[] selectedTheme = themeColors();
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{selectedTheme[0], selectedTheme[1], selectedTheme[2]}));
        setContentView(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("AURORA", 22, white, true);
        brand.setLetterSpacing(0.18f);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        clock = text("", 15, 0xFFCFD8EA, false);
        top.addView(clock);
        TextView profile = text("   ●  " + profileName(activeProfileId) + "  ▾", 15, 0xFFE2E8F5, true);
        profile.setFocusable(true);
        profile.setPadding(14, 9, 14, 9);
        profile.setBackground(panel(0xFF263653, 22));
        profile.setOnClickListener(v -> showProfiles());
        profile.setOnFocusChangeListener((v, focused) ->
                v.setBackground(panel(focused ? 0xFF3C5F96 : 0xFF263653, 22)));
        top.addView(profile);
        root.addView(top);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(0, 26, 0, 28);
        nav.addView(navAction("Watch Now", () -> buildHome()));
        nav.addView(navAction("Apps", () -> showLibrary()));
        nav.addView(navAction("Search", () -> showLibrary()));
        nav.addView(navAction("Favorites", () -> showFavorites()));
        nav.addView(navAction("Profiles", () -> showProfiles()));
        nav.addView(navAction("Control Center", () -> showControlCenter()));
        root.addView(nav);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(62, 42, 62, 42);
        hero.setElevation(18f);
        GradientDrawable heroSurface = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF526F9E, selectedTheme[3], selectedTheme[1], 0xFF090F20});
        heroSurface.setCornerRadius(34f);
        hero.setBackground(heroSurface);
        TextView eyebrow = text("  AURORA TV  •  FEATURED", 14, 0xFFE0E9FA, true);
        eyebrow.setLetterSpacing(0.15f);
        hero.addView(eyebrow);
        TextView headline = text("The best of entertainment.\nAll in one place.", 36, white, true);
        headline.setPadding(0, 14, 0, 10);
        hero.addView(headline);
        TextView sub = text("Discover something great to watch.", 19, 0xFFE0E7F3, false);
        hero.addView(sub);
        LinearLayout heroActions = new LinearLayout(this);
        heroActions.setOrientation(LinearLayout.HORIZONTAL);
        heroActions.setPadding(0, 16, 0, 0);
        heroActions.addView(action("▶  Open App Library", () -> showLibrary()));
        heroActions.addView(action("Profiles", () -> showProfiles()));
        hero.addView(heroActions);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, 278);
        hp.bottomMargin = 30;
        root.addView(hero, hp);

        List<ResolveInfo> recentApps = recentApps();
        if (!recentApps.isEmpty()) {
            TextView continueLabel = text("Continue Watching", 20, 0xFFF2F5FC, true);
            continueLabel.setLetterSpacing(0.02f);
            continueLabel.setPadding(4, 4, 0, 0);
            root.addView(continueLabel);
            HorizontalScrollView continueScroll = new HorizontalScrollView(this);
            continueScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout continueRail = new LinearLayout(this);
            continueRail.setOrientation(LinearLayout.HORIZONTAL);
            continueRail.setPadding(0, 14, 0, 14);
            for (ResolveInfo recent : recentApps) continueRail.addView(appCard(recent));
            continueScroll.addView(continueRail);
            root.addView(continueScroll, new LinearLayout.LayoutParams(-1, 145));
        }

        if (!favorites.isEmpty()) {
            TextView favoriteLabel = text("Favorites", 20, 0xFFF2F5FC, true);
            favoriteLabel.setLetterSpacing(0.02f);
            favoriteLabel.setPadding(4, 4, 0, 0);
            root.addView(favoriteLabel);
            HorizontalScrollView favoriteScroll = new HorizontalScrollView(this);
            favoriteScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout favoriteRail = new LinearLayout(this);
            favoriteRail.setOrientation(LinearLayout.HORIZONTAL);
            favoriteRail.setPadding(0, 14, 0, 14);
            for (ResolveInfo app : apps) {
                if (favorites.contains(app.activityInfo.packageName)) favoriteRail.addView(appCard(app));
            }
            favoriteScroll.addView(favoriteRail);
            root.addView(favoriteScroll, new LinearLayout.LayoutParams(-1, 145));
        }

        TextView label = text("Your Apps", 20, 0xFFF2F5FC, true);
        label.setLetterSpacing(0.02f);
        label.setPadding(4, 4, 0, 0);
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
        card.setPadding(14, 12, 14, 10);
        card.setElevation(4f);
        card.setBackground(panel(0xFF202E49, 24));
        android.graphics.drawable.Drawable icon = app.loadIcon(getPackageManager());
        android.widget.ImageView image = new android.widget.ImageView(this);
        image.setImageDrawable(icon);
        card.addView(image, new LinearLayout.LayoutParams(76, 76));
        TextView name = text(app.loadLabel(getPackageManager()).toString(), 15, white, true);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(1);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(142, -2);
        np.topMargin = 7;
        card.addView(name, np);
        card.setOnClickListener(v -> launch(app));
        card.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_INFO) {
                showAppDetails(app);
                return true;
            }
            return false;
        });
        card.setOnLongClickListener(v -> {
            toggleFavorite(app);
            return true;
        });
        card.setOnFocusChangeListener((v, focused) -> {
            v.animate().scaleX(focused ? 1.16f : 1f).scaleY(focused ? 1.16f : 1f).setDuration(160).start();
            v.setBackground(panel(focused ? 0xFF527BB8 : 0xFF202E49, 24));
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(166, 150);
        cp.rightMargin = 14;
        card.setLayoutParams(cp);
        return card;
    }

    private List<ResolveInfo> recentApps() {
        String key = "recent_apps_" + activeProfileId;
        String stored = getSharedPreferences(PREFS, MODE_PRIVATE).getString(key, "");
        List<ResolveInfo> result = new ArrayList<>();
        if (stored.isEmpty()) {
            ResolveInfo legacy = findApp(getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getString("last_opened_package_" + activeProfileId, null));
            if (legacy != null) result.add(legacy);
            return result;
        }
        for (String pkg : stored.split("\\|")) {
            ResolveInfo app = findApp(pkg);
            if (app != null && result.size() < 8) result.add(app);
        }
        return result;
    }

    private void recordRecentApp(String packageName) {
        List<String> history = new ArrayList<>();
        history.add(packageName);
        for (ResolveInfo app : recentApps()) {
            String pkg = app.activityInfo.packageName;
            if (!pkg.equals(packageName) && history.size() < 8) history.add(pkg);
        }
        StringBuilder stored = new StringBuilder();
        for (String pkg : history) {
            if (stored.length() > 0) stored.append("|");
            stored.append(pkg);
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("last_opened_package_" + activeProfileId, packageName)
                .putString("recent_apps_" + activeProfileId, stored.toString()).apply();
    }

    private void launch(ResolveInfo app) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setClassName(app.activityInfo.packageName, app.activityInfo.name);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
            recordRecentApp(app.activityInfo.packageName);
        } catch (Exception error) {
            android.widget.Toast.makeText(this, "Unable to open " +
                    app.loadLabel(getPackageManager()), android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void showFavorites() {
        List<ResolveInfo> saved = new ArrayList<>();
        for (ResolveInfo app : apps) {
            if (favorites.contains(app.activityInfo.packageName)) saved.add(app);
        }
        if (saved.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Favorites")
                    .setMessage("No favorites yet. In the Apps shelf or App Library, long-press an app to add it here.")
                    .setPositiveButton("Open App Library", (d, w) -> showLibrary())
                    .setNegativeButton("Close", null).show();
            return;
        }
        ScrollView scroll = new ScrollView(this);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setPadding(12, 12, 12, 12);
        scroll.addView(grid);
        for (ResolveInfo app : saved) {
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setFocusable(true);
            tile.setClickable(true);
            tile.setPadding(8, 10, 8, 8);
            tile.setBackground(panel(0xFF1B2943, 16));
            android.widget.ImageView icon = new android.widget.ImageView(this);
            icon.setImageDrawable(app.loadIcon(getPackageManager()));
            tile.addView(icon, new LinearLayout.LayoutParams(58, 58));
            TextView name = text(app.loadLabel(getPackageManager()).toString(), 13, white, true);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            name.setPadding(4, 6, 4, 0);
            tile.addView(name, new LinearLayout.LayoutParams(-1, -2));
            tile.setOnClickListener(v -> launch(app));
            tile.setOnLongClickListener(v -> {
                toggleFavorite(app);
                return true;
            });
            tile.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_INFO) {
                    showAppDetails(app);
                    return true;
                }
                return false;
            });
            tile.setOnFocusChangeListener((v, focused) -> {
                v.animate().scaleX(focused ? 1.08f : 1f).scaleY(focused ? 1.08f : 1f).setDuration(120).start();
                v.setBackground(panel(focused ? 0xFF3C5F96 : 0xFF1B2943, 16));
            });
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.height = 120;
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            p.setMargins(6, 6, 6, 6);
            grid.addView(tile, p);
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Favorites")
                .setView(scroll).setNegativeButton("Close", null).create();
        showDialogRestoringFocus(dialog, rail);
        if (grid.getChildCount() > 0) grid.getChildAt(0).requestFocus();
    }

    private void showLibrary() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(20, 12, 20, 8);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(white);
        search.setHintTextColor(0xFF9EABC5);
        search.setHint("Search installed apps");
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        search.setBackground(panel(0xFF1B2943, 18));
        search.setPadding(18, 12, 18, 12);
        container.addView(search, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        GridLayout results = new GridLayout(this);
        results.setColumnCount(4);
        results.setUseDefaultMargins(false);
        results.setPadding(0, 12, 0, 8);
        scroll.addView(results);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, 0, 1);
        sp.topMargin = 10;
        container.addView(scroll, sp);

        TextView count = text("", 12, 0xFF9EABC5, false);
        count.setPadding(4, 8, 4, 0);
        container.addView(count);

        Runnable render = () -> {
            String query = search.getText().toString().trim().toLowerCase(Locale.getDefault());
            results.removeAllViews();
            int shown = 0;
            for (ResolveInfo app : apps) {
                String label = app.loadLabel(getPackageManager()).toString();
                if (!label.toLowerCase(Locale.getDefault()).contains(query)) continue;
                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER);
                tile.setPadding(8, 10, 8, 8);
                tile.setFocusable(true);
                tile.setClickable(true);
                tile.setBackground(panel(0xFF1B2943, 16));
                android.widget.ImageView icon = new android.widget.ImageView(this);
                icon.setImageDrawable(app.loadIcon(getPackageManager()));
                tile.addView(icon, new LinearLayout.LayoutParams(54, 54));
                TextView name = text(label, 13, white, true);
                name.setGravity(Gravity.CENTER);
                name.setMaxLines(2);
                name.setPadding(4, 6, 4, 0);
                tile.addView(name, new LinearLayout.LayoutParams(-1, -2));
                tile.setOnClickListener(v -> launch(app));
                tile.setOnLongClickListener(v -> {
                    toggleFavorite(app);
                    return true;
                });
                tile.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_INFO) {
                        showAppDetails(app);
                        return true;
                    }
                    return false;
                });
                tile.setOnFocusChangeListener((v, focused) -> {
                    v.animate().scaleX(focused ? 1.08f : 1f).scaleY(focused ? 1.08f : 1f).setDuration(120).start();
                    v.setBackground(panel(focused ? 0xFF3C5F96 : 0xFF1B2943, 16));
                });
                GridLayout.LayoutParams tp = new GridLayout.LayoutParams();
                tp.width = 0;
                tp.height = 112;
                tp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                tp.setMargins(6, 6, 6, 6);
                results.addView(tile, tp);
                shown++;
            }
            count.setText(shown + " APPLICATIONS");
            if (shown == 0) {
                TextView empty = text("No apps match your search.", 15, 0xFFD1D9E8, false);
                empty.setPadding(12, 20, 12, 20);
                GridLayout.LayoutParams ep = new GridLayout.LayoutParams();
                ep.width = -1;
                ep.height = -2;
                ep.columnSpec = GridLayout.spec(0, 4);
                results.addView(empty, ep);
            }
        };
        render.run();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) { render.run(); }
            @Override public void afterTextChanged(Editable e) { }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("App Library")
                .setView(container)
                .setNegativeButton("Close", null)
                .create();
        dialog.getWindow();
        showDialogRestoringFocus(dialog, rail);
        search.requestFocus();
    }

    private void openSettings() {
        Intent i = new Intent(android.provider.Settings.ACTION_SETTINGS);
        try { startActivity(i); } catch (Exception e) {
            new AlertDialog.Builder(this).setMessage("Settings are not available on this device.")
                    .setPositiveButton("OK", null).show();
        }
    }

    private void openSystemSettings(String action) {
        try { startActivity(new Intent(action)); }
        catch (Exception e) { openSettings(); }
    }

    private LinearLayout controlItem(String title, String subtitle, Runnable action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(18, 13, 18, 13);
        item.setFocusable(true);
        item.setClickable(true);
        item.setBackground(panel(0xFF1B2943, 18));
        item.addView(text(title, 16, white, true));
        TextView detail = text(subtitle, 12, 0xFFB8C7DF, false);
        detail.setPadding(0, 4, 0, 0);
        item.addView(detail);
        item.setOnClickListener(v -> action.run());
        item.setOnFocusChangeListener((v, focused) -> {
            v.animate().scaleX(focused ? 1.035f : 1f).scaleY(focused ? 1.035f : 1f).setDuration(110).start();
            v.setBackground(panel(focused ? 0xFF3C5F96 : 0xFF1B2943, 18));
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.setMargins(6, 6, 6, 6);
        item.setLayoutParams(p);
        return item;
    }

    private void showControlCenter() {
        LinearLayout panelView = new LinearLayout(this);
        panelView.setOrientation(LinearLayout.VERTICAL);
        panelView.setPadding(14, 10, 14, 8);
        panelView.setBackground(panel(0xFF101B30, 22));
        TextView heading = text("Quick Controls", 21, white, true);
        heading.setPadding(10, 4, 10, 12);
        panelView.addView(heading);

        LinearLayout rowOne = new LinearLayout(this);
        rowOne.setOrientation(LinearLayout.HORIZONTAL);
        rowOne.addView(controlItem("Network", "Wi-Fi and internet settings",
                () -> openSystemSettings(android.provider.Settings.ACTION_WIFI_SETTINGS)));
        rowOne.addView(controlItem("Bluetooth", "Connected devices and accessories",
                () -> openSystemSettings(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)));
        panelView.addView(rowOne);

        LinearLayout rowTwo = new LinearLayout(this);
        rowTwo.setOrientation(LinearLayout.HORIZONTAL);
        rowTwo.addView(controlItem("Display", "Screen and display preferences",
                () -> openSystemSettings(android.provider.Settings.ACTION_DISPLAY_SETTINGS)));
        rowTwo.addView(controlItem("Sound", "Audio and volume settings",
                () -> openSystemSettings(android.provider.Settings.ACTION_SOUND_SETTINGS)));
        panelView.addView(rowTwo);

        LinearLayout rowThree = new LinearLayout(this);
        rowThree.setOrientation(LinearLayout.HORIZONTAL);
        rowThree.addView(controlItem("Profiles", "Switch or create a profile", () -> showProfiles()));
        rowThree.addView(controlItem("Search", "Find an installed app by name", () -> showLibrary()));
        panelView.addView(rowThree);

        LinearLayout rowFour = new LinearLayout(this);
        rowFour.setOrientation(LinearLayout.HORIZONTAL);
        rowFour.addView(controlItem("Refresh apps", "Rescan installed applications",
                () -> { loadApps(); buildHome(); }));
        rowFour.addView(controlItem("Screen Saver", "Preview the idle display",
                () -> showScreensaver()));
        rowFour.addView(controlItem("About AURORA", "Version 0.5.0 · Independent project",
                () -> new AlertDialog.Builder(this)
                        .setMessage("AURORA TV 0.5.0\nAn independent Android TV launcher project.")
                        .setPositiveButton("OK", null).show()));
        panelView.addView(rowFour);

        LinearLayout rowFive = new LinearLayout(this);
        rowFive.setOrientation(LinearLayout.HORIZONTAL);
        rowFive.addView(controlItem("Appearance", "Choose this profile's background theme",
                () -> showThemePicker()));
        panelView.addView(rowFive);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(panelView)
                .setNegativeButton("Close", null).create();
        showDialogRestoringFocus(dialog, rail);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.78f),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        rowOne.getChildAt(0).requestFocus();
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            showLibrary();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            showControlCenter();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
