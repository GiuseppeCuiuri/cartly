package com.example.cartly;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

// The only screen: a large title that shrinks on scroll, the three tabs (list, planned, history),
// the bottom bar to switch between them, and one FAB that each tab sets up for itself.
public class MainActivity extends AppCompatActivity {
    static final String EXTRA_TAB = "tab";
    static final String TAB_PLANNED = "planned";

    private BottomNavigationView navigation;
    private AppBarLayout appBar;
    private CollapsingToolbarLayout collapsing;
    private ExtendedFloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appBar = findViewById(R.id.app_bar);
        collapsing = findViewById(R.id.collapsing);
        fab = findViewById(R.id.fab);

        // Edge to edge: the app bar moves its content below the status bar.
        // The bottom bar does the same for the navigation bar on its own.
        ViewCompat.setOnApplyWindowInsetsListener(appBar, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.action_supermarkets)
                return false;
            openSupermarkets();
            return true;
        });

        navigation = findViewById(R.id.bottom_navigation);
        if (savedInstanceState == null) {
            int tab = tabFor(getIntent());
            navigation.setSelectedItemId(tab);
            showTab(tab);
            // alarms are lost on reboot and on "force stop"; setting them again is cheap
            NotificationReceiver.scheduleAll(this);
            if (OnboardingActivity.showOnce(this))
                startActivity(new Intent(this, OnboardingActivity.class));
        } else {
            collapsing.setTitle(titleFor(navigation.getSelectedItemId()));
        }
        navigation.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
    }

    // a tap on a reminder while the app is open
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        navigation.setSelectedItemId(tabFor(intent));
    }

    void showHistory() {
        navigation.setSelectedItemId(R.id.nav_history);
    }

    /** The one floating button; the tab on screen sets its text, icon and action. */
    ExtendedFloatingActionButton fab() {
        return fab;
    }

    private static int tabFor(Intent intent) {
        return TAB_PLANNED.equals(intent.getStringExtra(EXTRA_TAB)) ? R.id.nav_planned : R.id.nav_list;
    }

    private String titleFor(int itemId) {
        if (itemId == R.id.nav_planned)
            return getString(R.string.title_planned);
        if (itemId == R.id.nav_history)
            return getString(R.string.title_history);
        return getString(R.string.title_list);
    }

    private void showTab(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_planned)
            fragment = new PlannedFragment();
        else if (itemId == R.id.nav_history)
            fragment = new HistoryFragment();
        else
            fragment = new ShoppingListFragment();
        collapsing.setTitle(titleFor(itemId));
        appBar.setExpanded(true, false);
        fab.hide();  // each tab shows it again if it needs it
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    // Google Maps (or any maps app) searches around the user's position:
    // no location permission, no Maps API key and no map screen needed here.
    private void openSupermarkets() {
        Uri search = Uri.parse("geo:0,0?q=" + Uri.encode(getString(R.string.supermarket_query)));
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, search));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(fab, R.string.no_maps_app, Snackbar.LENGTH_LONG).show();
        }
    }
}
