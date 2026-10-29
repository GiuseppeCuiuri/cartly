package com.example.cartly;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

// Three pages shown once, on the first launch: what Cartly does, in its own illustrations.
public class OnboardingActivity extends AppCompatActivity {
    private static final String PREFS = "cartly";
    private static final String SHOWN = "onboarding_shown";

    // {illustration, title, text}
    private static final int[][] PAGES = {
            {R.drawable.illus_list, R.string.onboarding_title_1, R.string.onboarding_text_1},
            {R.drawable.illus_history, R.string.onboarding_title_2, R.string.onboarding_text_2},
            {R.drawable.illus_planned, R.string.onboarding_title_3, R.string.onboarding_text_3},
    };

    /** True only the first time: it is marked as shown right away, so skipping or leaving it also counts. */
    static boolean showOnce(Context context) {
        if (context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(SHOWN, false))
            return false;
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(SHOWN, true).apply();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        ViewPager2 pager = findViewById(R.id.pager);
        ViewGroup dots = findViewById(R.id.dots);
        MaterialButton next = findViewById(R.id.button_next);
        pager.setAdapter(new PageAdapter());
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dots.getChildCount(); i++)
                    dots.getChildAt(i).setSelected(i == position);
                next.setText(position == PAGES.length - 1 ? R.string.onboarding_start : R.string.onboarding_next);
            }
        });
        next.setOnClickListener(v -> {
            if (pager.getCurrentItem() == PAGES.length - 1)
                finish();
            else
                pager.setCurrentItem(pager.getCurrentItem() + 1);
        });
        findViewById(R.id.button_skip).setOnClickListener(v -> finish());
    }

    private static class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {
        static class Holder extends RecyclerView.ViewHolder {
            final ImageView illustration;
            final TextView title;
            final TextView text;

            Holder(View view) {
                super(view);
                illustration = view.findViewById(R.id.illustration);
                title = view.findViewById(R.id.title);
                text = view.findViewById(R.id.text);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_page, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.illustration.setImageResource(PAGES[position][0]);
            holder.title.setText(PAGES[position][1]);
            holder.text.setText(PAGES[position][2]);
        }

        @Override
        public int getItemCount() {
            return PAGES.length;
        }
    }
}
