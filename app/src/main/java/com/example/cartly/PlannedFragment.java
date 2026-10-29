package com.example.cartly;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

// Planned shoppings, soonest first. Each one gets a reminder an hour before.
public class PlannedFragment extends Fragment {
    private final ArrayList<DBManager.Shopping> shoppings = new ArrayList<>();
    private ShoppingAdapter adapter;
    private DBManager db;
    private View emptyView;

    // asked when the first reminder is set, the moment the request makes sense to the user
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted && getView() != null)
                    Snackbar.make(fab(), R.string.notifications_denied, Snackbar.LENGTH_LONG).show();
            });

    public PlannedFragment() {
        super(R.layout.fragment_shoppings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = new DBManager(requireContext());
        RecyclerView recycler = view.findViewById(R.id.recycler);
        adapter = new ShoppingAdapter(shoppings, R.drawable.ic_event, null);
        recycler.setAdapter(adapter);
        Ui.onSwipe(recycler, this::deleteWithUndo);

        emptyView = view.findViewById(R.id.empty);
        Ui.setEmptyView(emptyView, R.drawable.illus_planned, R.string.empty_planned_title, R.string.empty_planned_text);

        getChildFragmentManager().setFragmentResultListener(NewShoppingDialogFragment.RESULT, getViewLifecycleOwner(),
                (key, result) -> {
                    reload();
                    askNotificationPermission();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        ExtendedFloatingActionButton fab = fab();
        fab.setText(R.string.new_shopping);
        fab.setIconResource(R.drawable.ic_add);
        fab.setOnClickListener(v -> new NewShoppingDialogFragment().show(getChildFragmentManager(), "new_shopping"));
        fab.extend();
        fab.show();
        reload();
    }

    private ExtendedFloatingActionButton fab() {
        return ((MainActivity) requireActivity()).fab();
    }

    private void reload() {
        shoppings.clear();
        shoppings.addAll(db.findShoppings(DBManager.PLANNED));
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(shoppings.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void deleteWithUndo(int position) {
        DBManager.Shopping shopping = shoppings.remove(position);
        adapter.notifyItemRemoved(position);
        // its reminder will find nothing to show and stay silent
        db.deleteShopping(shopping.id);
        emptyView.setVisibility(shoppings.isEmpty() ? View.VISIBLE : View.GONE);
        Snackbar.make(fab(), R.string.shopping_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> {
                    db.restoreShopping(shopping);
                    NotificationReceiver.schedule(requireContext(), shopping);
                    reload();
                })
                .show();
    }
}
