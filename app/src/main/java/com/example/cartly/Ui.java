package com.example.cartly;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Small helpers shared by the screens.
final class Ui {
    private Ui() {}

    // --- products

    /** What the user typed in the "add a product" field, split into name and quantity. */
    static final class Entry {
        final String name;
        final int quantity;

        Entry(String name, int quantity) {
            this.name = name;
            this.quantity = quantity;
        }
    }

    // "2 latte", "2x latte" | "latte 2", "latte x2"; one or two digits only, so "Pasta 500" stays a name
    private static final Pattern QUANTITY_FIRST = Pattern.compile("^(\\d{1,2})\\s*[x×]?\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUANTITY_LAST = Pattern.compile("^(.+?)\\s+[x×]?\\s*(\\d{1,2})$", Pattern.CASE_INSENSITIVE);

    static Entry parseEntry(String typed) {
        String text = typed.trim();
        Matcher first = QUANTITY_FIRST.matcher(text);
        if (first.matches() && Integer.parseInt(first.group(1)) > 0)
            return new Entry(capitalized(first.group(2).trim()), Integer.parseInt(first.group(1)));
        Matcher last = QUANTITY_LAST.matcher(text);
        if (last.matches() && Integer.parseInt(last.group(2)) > 0)
            return new Entry(capitalized(last.group(1).trim()), Integer.parseInt(last.group(2)));
        return new Entry(capitalized(text), 1);
    }

    // "latte" → "Latte", so the list looks tidy however it was typed
    private static String capitalized(String name) {
        return name.isEmpty() ? name : name.substring(0, 1).toUpperCase(Locale.ITALIAN) + name.substring(1);
    }

    /** "2× Latte", or just "Latte" for one. */
    static String withQuantity(String name, int quantity) {
        return quantity > 1 ? quantity + "× " + name : name;
    }

    // --- dates

    /** "2026-10-29", "18:00" → "Domani · 18:00" or "Ven 31 ott · 18:00" */
    static String formatDateTime(Context context, String date, String time) {
        return formatDate(context, date) + " · " + time;
    }

    /** "2026-10-29" → "Oggi", "Domani", "Ven 31 ott", or "Ven 31 ott 2025" outside the current year */
    static String formatDate(Context context, String isoDate) {
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        if (isoDate.equals(DBManager.today()))
            return context.getString(R.string.today);
        if (isoDate.equals(iso.format(tomorrow.getTime())))
            return context.getString(R.string.tomorrow);
        try {
            String pattern = isoDate.startsWith(DBManager.today().substring(0, 5)) ? "EEE d MMM" : "EEE d MMM yyyy";
            String text = new SimpleDateFormat(pattern, Locale.ITALIAN).format(iso.parse(isoDate));
            return text.substring(0, 1).toUpperCase(Locale.ITALIAN) + text.substring(1);
        } catch (ParseException e) {
            return isoDate;
        }
    }

    // --- lists

    /** Fills in the "nothing here yet" view of a list: an illustration, a title and a hint. */
    static void setEmptyView(View empty, @DrawableRes int illustration, @StringRes int title, @StringRes int text) {
        ((ImageView) empty.findViewById(R.id.empty_icon)).setImageResource(illustration);
        ((TextView) empty.findViewById(R.id.empty_title)).setText(title);
        ((TextView) empty.findViewById(R.id.empty_text)).setText(text);
    }

    /** Marks the view holders that may be swiped away; section headers aren't. */
    interface Swipeable {}

    /**
     * Swiping a row left or right reports its position. While it moves, a red rounded background
     * with a bin shows behind it, so it is clear what letting go will do.
     */
    static void onSwipe(RecyclerView recycler, IntConsumer onSwiped) {
        Context context = recycler.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable background = new GradientDrawable();
        background.setColor(MaterialColors.getColor(recycler, com.google.android.material.R.attr.colorErrorContainer));
        background.setCornerRadius(16 * density);
        Drawable bin = ContextCompat.getDrawable(context, R.drawable.ic_delete).mutate();
        bin.setTint(MaterialColors.getColor(recycler, com.google.android.material.R.attr.colorOnErrorContainer));
        int binSize = (int) (24 * density);
        int binMargin = (int) (20 * density);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public int getSwipeDirs(@NonNull RecyclerView view, @NonNull RecyclerView.ViewHolder holder) {
                return holder instanceof Swipeable ? super.getSwipeDirs(view, holder) : 0;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView view, @NonNull RecyclerView.ViewHolder holder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION)
                    onSwiped.accept(position);
            }

            @Override
            public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView view, @NonNull RecyclerView.ViewHolder holder,
                                    float dX, float dY, int actionState, boolean active) {
                View row = holder.itemView;
                if (dX != 0) {
                    background.setBounds(row.getLeft(), row.getTop(), row.getRight(), row.getBottom());
                    background.draw(canvas);
                    int top = row.getTop() + (row.getHeight() - binSize) / 2;
                    int left = dX > 0 ? row.getLeft() + binMargin : row.getRight() - binMargin - binSize;
                    bin.setBounds(left, top, left + binSize, top + binSize);
                    bin.draw(canvas);
                }
                super.onChildDraw(canvas, view, holder, dX, dY, actionState, active);
            }
        }).attachToRecyclerView(recycler);
    }
}
