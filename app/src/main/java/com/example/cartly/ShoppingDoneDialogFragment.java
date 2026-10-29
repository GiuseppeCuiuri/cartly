package com.example.cartly;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

// What was bought in a past shopping, with a button to put it all back on the list.
public class ShoppingDoneDialogFragment extends DialogFragment {

    // Fragments are re-created with the no-arg constructor, so the id travels in the arguments Bundle
    public static ShoppingDoneDialogFragment newInstance(int id) {
        Bundle args = new Bundle();
        args.putInt("id", id);
        ShoppingDoneDialogFragment fragment = new ShoppingDoneDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        int id = requireArguments().getInt("id");
        Context context = requireContext();
        DBManager db = new DBManager(context);
        DBManager.Shopping shopping = db.findShopping(id);
        ArrayList<String> products = db.findPurchasedProducts(id);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(shopping == null ? getString(R.string.bought_products) : Ui.formatDateTime(context, shopping.date, shopping.time))
                .setPositiveButton(R.string.close, null);
        if (products.isEmpty())
            return builder.setMessage(R.string.no_products).create();
        return builder
                .setItems(products.toArray(new String[0]), null)
                .setNeutralButton(R.string.add_again, (dialog, which) -> {
                    db.addPurchasesToCart(id);
                    Toast.makeText(context, R.string.added_again, Toast.LENGTH_SHORT).show();
                })
                .create();
    }
}
