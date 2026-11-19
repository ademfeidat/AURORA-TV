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
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
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

        if (!favorites.isEmpty()) {
            TextView favoriteLabel = text("FAVORITES", 14, 0xFFB9C7DF, true);
            favoriteLabel.setLetterSpacing(0.14f);
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
        card.setOnLongClickListener(v -> {
            toggleFavorite(app);
            return true;
        });
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
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("last_opened_package_" + activeProfileId, app.activityInfo.packageName).apply();
        try { startActivity(i); } catch (Exception ignored) { }
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
        dialog.show();
        search.requestFocus();
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
