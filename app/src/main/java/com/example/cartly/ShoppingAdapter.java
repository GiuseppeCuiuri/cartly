package com.example.cartly;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Cards of the "Programmate" and "Storico" tabs: date and time, then the description (and, once done, the product count).
class ShoppingAdapter extends RecyclerView.Adapter<ShoppingAdapter.Holder> {
    interface OnClick {
        void onClick(DBManager.Shopping shopping);
    }

    private final List<DBManager.Shopping> shoppings;
    @DrawableRes
    private final int icon;
    @Nullable
    private final OnClick onClick;

    ShoppingAdapter(List<DBManager.Shopping> shoppings, @DrawableRes int icon, @Nullable OnClick onClick) {
        this.shoppings = shoppings;
        this.icon = icon;
        this.onClick = onClick;
    }

    static class Holder extends RecyclerView.ViewHolder implements Ui.Swipeable {
        final ImageView icon;
        final TextView title;
        final TextView subtitle;

        Holder(View view) {
            super(view);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shopping, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        DBManager.Shopping shopping = shoppings.get(position);
        Context context = holder.itemView.getContext();
        holder.icon.setImageResource(icon);
        holder.title.setText(Ui.formatDateTime(context, shopping.date, shopping.time));
        holder.subtitle.setText(subtitle(context, shopping));
        if (onClick == null) {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
        } else {
            holder.itemView.setOnClickListener(v -> onClick.onClick(shopping));
        }
    }

    @Override
    public int getItemCount() {
        return shoppings.size();
    }

    private static String subtitle(Context context, DBManager.Shopping shopping) {
        if (shopping.type == DBManager.DONE) {
            String count = context.getResources().getQuantityString(R.plurals.products, shopping.productCount, shopping.productCount);
            return shopping.description.isEmpty() ? count : count + " · " + shopping.description;
        }
        return shopping.description.isEmpty() ? context.getString(R.string.no_description) : shopping.description;
    }
}
