package com.example.cartly;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

// Done shoppings, latest first. A tap shows what was bought.
public class HistoryFragment extends Fragment {
    private final ArrayList<DBManager.Shopping> shoppings = new ArrayList<>();
    private ShoppingAdapter adapter;
    private DBManager db;
    private View emptyView;

    public HistoryFragment() {
        super(R.layout.fragment_shoppings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = new DBManager(requireContext());
        RecyclerView recycler = view.findViewById(R.id.recycler);
        adapter = new ShoppingAdapter(shoppings, R.drawable.ic_history,
                shopping -> ShoppingDoneDialogFragment.newInstance(shopping.id).show(getChildFragmentManager(), "products"));
        recycler.setAdapter(adapter);

        emptyView = view.findViewById(R.id.empty);
        Ui.setEmptyView(emptyView, R.drawable.illus_history, R.string.empty_history_title, R.string.empty_history_text);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) requireActivity()).fab().hide();
        shoppings.clear();
        shoppings.addAll(db.findShoppings(DBManager.DONE));
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(shoppings.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
