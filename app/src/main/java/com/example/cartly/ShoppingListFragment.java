package com.example.cartly;

import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

// The shopping list, grouped by supermarket department. A tap ticks a product, which then slides down
// to "Nel carrello"; "Spesa fatta" moves the ticked ones to the history.
public class ShoppingListFragment extends Fragment {
    // the tick stays visible for a moment before the row moves to its new place
    private static final long MOVE_DELAY_MS = 450;

    private final RowAdapter adapter = new RowAdapter();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable reload = this::reload;
    private DBManager db;
    private TextInputLayout inputLayout;
    private EditText input;
    private RecyclerView recycler;
    private View emptyView;

    public ShoppingListFragment() {
        super(R.layout.fragment_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = new DBManager(requireContext());
        inputLayout = view.findViewById(R.id.input_layout);
        input = view.findViewById(R.id.input_product);
        recycler = view.findViewById(R.id.recycler);
        emptyView = view.findViewById(R.id.empty);

        inputLayout.setEndIconOnClickListener(v -> addProduct());
        input.setOnEditorActionListener((v, actionId, event) -> {
            addProduct();
            return true;  // keeps the keyboard open for the next product
        });
        // the "already on the list" error goes away as soon as the user types again
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0)
                    inputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        recycler.setAdapter(adapter);
        Ui.onSwipe(recycler, position -> removeWithUndo(adapter.getCurrentList().get(position).item));
        Ui.setEmptyView(emptyView, R.drawable.illus_list, R.string.empty_list_title, R.string.empty_list_text);
    }

    @Override
    public void onResume() {
        super.onResume();
        ExtendedFloatingActionButton fab = fab();
        fab.setIconResource(R.drawable.ic_cart);
        fab.setOnClickListener(v -> completeShopping());
        reload();
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(reload);
    }

    private ExtendedFloatingActionButton fab() {
        return ((MainActivity) requireActivity()).fab();
    }

    private void reload() {
        if (getView() == null)
            return;
        List<DBManager.CartItem> items = db.findCartItems();
        adapter.submitList(rows(items));
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);

        int checked = 0;
        for (DBManager.CartItem item : items)
            if (item.checked)
                checked++;
        ExtendedFloatingActionButton fab = fab();
        if (checked == 0) {
            fab.hide();
            return;
        }
        fab.setText(getString(R.string.shopping_done, checked));
        fab.extend();
        fab.show();
    }

    // departments in supermarket order, then what is already in the trolley
    private static List<Row> rows(List<DBManager.CartItem> items) {
        EnumMap<Catalog.Department, List<Row>> byDepartment = new EnumMap<>(Catalog.Department.class);
        List<Row> inTrolley = new ArrayList<>();
        for (DBManager.CartItem item : items) {
            Catalog.Match match = Catalog.classify(item.name);
            Row row = Row.product(item, match.emoji);
            if (item.checked)
                inTrolley.add(row);
            else
                byDepartment.computeIfAbsent(match.department, d -> new ArrayList<>()).add(row);
        }
        List<Row> rows = new ArrayList<>();
        for (Catalog.Department department : byDepartment.keySet()) {
            rows.add(Row.header(department, 0));
            rows.addAll(byDepartment.get(department));
        }
        if (!inTrolley.isEmpty()) {
            rows.add(Row.header(null, inTrolley.size()));
            rows.addAll(inTrolley);
        }
        return rows;
    }

    private void addProduct() {
        Ui.Entry entry = Ui.parseEntry(input.getText().toString());
        if (entry.name.isEmpty())
            return;
        input.setText("");
        // shown on the field, not in a Snackbar: the keyboard would hide that
        if (!db.addToCart(entry.name, entry.quantity)) {
            inputLayout.setError(getString(R.string.already_in_list, entry.name));
            return;
        }
        reload();
    }

    private void toggle(DBManager.CartItem item, ProductHolder holder) {
        // from the box, not from item: a second tap may come before the reload brings a fresh item
        boolean checked = !holder.check.isChecked();
        db.setChecked(item.rowId, checked);
        holder.showChecked(checked);
        handler.removeCallbacks(reload);
        handler.postDelayed(reload, MOVE_DELAY_MS);
    }

    private void editQuantity(DBManager.CartItem item) {
        NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(1);
        picker.setMaxValue(99);
        picker.setValue(item.quantity);
        FrameLayout frame = new FrameLayout(requireContext());
        frame.addView(picker, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.quantity_of, item.name))
                .setView(frame)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    db.setQuantity(item.rowId, picker.getValue());
                    reload();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void removeWithUndo(DBManager.CartItem item) {
        db.removeFromCart(item.rowId);
        reload();
        // made on the FAB so the Snackbar lands in the screen's CoordinatorLayout and pushes the FAB up
        Snackbar.make(fab(), getString(R.string.removed, item.name), Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> {
                    db.restoreToCart(item);
                    reload();
                })
                .show();
    }

    private void completeShopping() {
        handler.removeCallbacks(reload);
        db.completeShopping();
        reload();
        Snackbar.make(fab(), R.string.shopping_saved, Snackbar.LENGTH_LONG)
                .setAction(R.string.show, v -> ((MainActivity) requireActivity()).showHistory())
                .show();
    }

    // --- rows: a department header, or a product

    private static final class Row {
        // header rows: the department, or null for "Nel carrello" with its count
        final Catalog.Department department;
        final int count;
        // product rows
        final DBManager.CartItem item;
        final String emoji;

        private Row(Catalog.Department department, int count, DBManager.CartItem item, String emoji) {
            this.department = department;
            this.count = count;
            this.item = item;
            this.emoji = emoji;
        }

        static Row header(Catalog.Department department, int count) {
            return new Row(department, count, null, null);
        }

        static Row product(DBManager.CartItem item, String emoji) {
            return new Row(null, 0, item, emoji);
        }

        boolean isHeader() {
            return item == null;
        }
    }

    private static final DiffUtil.ItemCallback<Row> DIFF = new DiffUtil.ItemCallback<Row>() {
        @Override
        public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
            if (a.isHeader() || b.isHeader())
                return a.isHeader() && b.isHeader() && a.department == b.department;
            return a.item.rowId == b.item.rowId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
            if (a.isHeader())
                return a.count == b.count;
            return a.item.name.equals(b.item.name) && a.item.checked == b.item.checked
                    && a.item.quantity == b.item.quantity && Objects.equals(a.emoji, b.emoji);
        }
    };

    private static final int HEADER = 0;
    private static final int PRODUCT = 1;

    private class RowAdapter extends ListAdapter<Row, RecyclerView.ViewHolder> {
        RowAdapter() {
            super(DIFF);
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).isHeader() ? HEADER : PRODUCT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == HEADER)
                return new HeaderHolder(inflater.inflate(R.layout.item_section, parent, false));
            return new ProductHolder(inflater.inflate(R.layout.item_product, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = getItem(position);
            if (holder instanceof HeaderHolder) {
                TextView title = ((HeaderHolder) holder).title;
                title.setText(row.department == null
                        ? getString(R.string.in_trolley, row.count)
                        : row.department.emoji + "  " + getString(row.department.label));
            } else {
                ((ProductHolder) holder).bind(row);
            }
        }
    }

    private static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderHolder(View view) {
            super(view);
            title = (TextView) view;
        }
    }

    private class ProductHolder extends RecyclerView.ViewHolder implements Ui.Swipeable {
        final MaterialCardView card;
        final TextView emoji;
        final TextView name;
        final TextView quantity;
        final MaterialCheckBox check;
        // ids of the TalkBack actions added in bind(), to replace them on the next bind
        private int removeAction = View.NO_ID;
        private int quantityAction = View.NO_ID;

        ProductHolder(View view) {
            super(view);
            card = (MaterialCardView) view;
            emoji = view.findViewById(R.id.emoji);
            name = view.findViewById(R.id.name);
            quantity = view.findViewById(R.id.quantity);
            check = view.findViewById(R.id.check);
        }

        void bind(Row row) {
            DBManager.CartItem item = row.item;
            emoji.setText(row.emoji);
            name.setText(item.name);
            quantity.setText(getString(R.string.quantity_badge, item.quantity));
            quantity.setVisibility(item.quantity > 1 ? View.VISIBLE : View.GONE);
            showChecked(item.checked);
            card.setOnClickListener(v -> toggle(item, this));
            card.setOnLongClickListener(v -> {
                editQuantity(item);
                return true;
            });
            // for TalkBack, which can't swipe or long-press: the same actions as named buttons
            ViewCompat.removeAccessibilityAction(card, removeAction);
            ViewCompat.removeAccessibilityAction(card, quantityAction);
            removeAction = ViewCompat.addAccessibilityAction(card, getString(R.string.remove), (v, args) -> {
                removeWithUndo(item);
                return true;
            });
            quantityAction = ViewCompat.addAccessibilityAction(card, getString(R.string.change_quantity), (v, args) -> {
                editQuantity(item);
                return true;
            });
        }

        void showChecked(boolean checked) {
            check.setChecked(checked);
            int flags = name.getPaintFlags();
            name.setPaintFlags(checked ? flags | Paint.STRIKE_THRU_TEXT_FLAG : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
            card.setAlpha(checked ? 0.55f : 1f);
            ViewCompat.setStateDescription(card, getString(checked ? R.string.state_in_trolley : R.string.state_to_buy));
        }
    }
}
