package com.example.cartly;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DBManager {
    // values of Shopping.type
    public static final int PLANNED = 0;
    public static final int DONE = 1;

    /** A product on the shopping list. Never changed in place: the list is re-read after every change. */
    public static final class CartItem {
        final long rowId;
        final int productId;
        final String name;
        final boolean checked;
        final int quantity;

        CartItem(long rowId, int productId, String name, boolean checked, int quantity) {
            this.rowId = rowId;
            this.productId = productId;
            this.name = name;
            this.checked = checked;
            this.quantity = quantity;
        }
    }

    /** A planned or done shopping trip. date is "yyyy-MM-dd", time is "HH:mm". */
    public static final class Shopping {
        final int id;
        final String description;
        final String date;
        final String time;
        final int type;
        final int productCount;

        Shopping(int id, String description, String date, String time, int type, int productCount) {
            this.id = id;
            this.description = description;
            this.date = date;
            this.time = time;
            this.type = type;
            this.productCount = productCount;
        }
    }

    // One helper, so one connection, for the whole app. It is never closed: SQLiteOpenHelper caches it,
    // and reopening the database for every query was most of the cost of each call.
    // ponytail: queries still run on the main thread; fine for these few-row tables,
    // move them to an Executor (or Room) if lists grow or StrictMode complains
    private static DBHelper dbHelper;
    private final SQLiteDatabase db;

    public DBManager(Context context) {
        synchronized (DBManager.class) {
            if (dbHelper == null)
                dbHelper = new DBHelper(context.getApplicationContext());
        }
        db = dbHelper.getWritableDatabase();
    }

    // --- shopping list

    /** The shopping list, in the order the products were added. */
    public ArrayList<CartItem> findCartItems() {
        ArrayList<CartItem> items = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("SELECT c.rowid, c.product, p.name, c.checked, c.quantity FROM Cart c " +
                "JOIN Product p ON p._id = c.product ORDER BY c.rowid", null)) {
            while (cursor.moveToNext())
                items.add(new CartItem(cursor.getLong(0), cursor.getInt(1), cursor.getString(2),
                        cursor.getInt(3) != 0, cursor.getInt(4)));
        }
        return items;
    }

    /** Returns false if the product is already on the list (names ignore case). */
    public boolean addToCart(String name, int quantity) {
        db.execSQL("INSERT OR IGNORE INTO Product (name) VALUES (?)", new Object[]{name});
        try (SQLiteStatement insert = db.compileStatement(
                "INSERT OR IGNORE INTO Cart (product, quantity) SELECT _id, ? FROM Product WHERE name = ?")) {
            insert.bindLong(1, quantity);
            insert.bindString(2, name);
            return insert.executeInsert() != -1;
        }
    }

    public void setChecked(long rowId, boolean checked) {
        db.execSQL("UPDATE Cart SET checked = ? WHERE rowid = ?", new Object[]{checked ? 1 : 0, rowId});
    }

    public void setQuantity(long rowId, int quantity) {
        db.execSQL("UPDATE Cart SET quantity = ? WHERE rowid = ?", new Object[]{quantity, rowId});
    }

    public void removeFromCart(long rowId) {
        db.execSQL("DELETE FROM Cart WHERE rowid = ?", new Object[]{rowId});
    }

    /** Undoes removeFromCart: back in its old place, or at the end if a new product took that place meanwhile. */
    public void restoreToCart(CartItem item) {
        int checked = item.checked ? 1 : 0;
        db.execSQL("INSERT OR IGNORE INTO Cart (rowid, product, checked, quantity) VALUES (?, ?, ?, ?)",
                new Object[]{item.rowId, item.productId, checked, item.quantity});
        db.execSQL("INSERT OR IGNORE INTO Cart (product, checked, quantity) VALUES (?, ?, ?)",
                new Object[]{item.productId, checked, item.quantity});
    }

    /** Puts the products of a past shopping back on the list. */
    public void addPurchasesToCart(int shopping) {
        db.execSQL("INSERT OR IGNORE INTO Cart (product, quantity) SELECT pu.product, pu.quantity FROM Purchase pu " +
                "JOIN Product p ON p._id = pu.product WHERE pu.shopping = ? ORDER BY p.name", new Object[]{shopping});
    }

    /**
     * Moves the checked products into a shopping, all or nothing: the latest planned one up to today,
     * or a new one made now if nothing was planned.
     */
    public void completeShopping() {
        db.beginTransaction();
        try {
            int shopping = findLatestPlannedUntilToday();
            if (shopping < 0)
                shopping = (int) addShopping("", today(), now(), DONE);
            else
                db.execSQL("UPDATE Shopping SET type = ? WHERE _id = ?", new Object[]{DONE, shopping});
            db.execSQL("INSERT OR IGNORE INTO Purchase (shopping, product, quantity) " +
                    "SELECT ?, product, quantity FROM Cart WHERE checked = 1", new Object[]{shopping});
            db.execSQL("DELETE FROM Cart WHERE checked = 1");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private int findLatestPlannedUntilToday() {
        try (Cursor cursor = db.rawQuery("SELECT _id FROM Shopping WHERE type = ? AND date <= ? " +
                "ORDER BY date DESC, time DESC LIMIT 1", new String[]{String.valueOf(PLANNED), today()})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        }
    }

    // --- shoppings

    public long addShopping(String description, String date, String time, int type) {
        ContentValues values = new ContentValues();
        values.put("description", description);
        values.put("date", date);
        values.put("time", time);
        values.put("type", type);
        return db.insert("Shopping", null, values);
    }

    /** Planned shoppings soonest first, done ones latest first. */
    public ArrayList<Shopping> findShoppings(int type) {
        return findShoppings("type = ?", new String[]{String.valueOf(type)}, type == PLANNED ? "date, time" : "date DESC, time DESC");
    }

    /** Planned shoppings from today on: the ones that may still need a reminder. */
    public ArrayList<Shopping> findUpcomingShoppings() {
        return findShoppings("type = ? AND date >= ?", new String[]{String.valueOf(PLANNED), today()}, "date, time");
    }

    /** Returns null if there is no such shopping (any more). */
    public Shopping findShopping(int id) {
        ArrayList<Shopping> found = findShoppings("_id = ?", new String[]{String.valueOf(id)}, "_id");
        return found.isEmpty() ? null : found.get(0);
    }

    private ArrayList<Shopping> findShoppings(String where, String[] args, String orderBy) {
        ArrayList<Shopping> shoppings = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("SELECT _id, description, date, time, type, " +
                "(SELECT COUNT(*) FROM Purchase WHERE shopping = Shopping._id) FROM Shopping " +
                "WHERE " + where + " ORDER BY " + orderBy, args)) {
            while (cursor.moveToNext())
                shoppings.add(new Shopping(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getInt(4), cursor.getInt(5)));
        }
        return shoppings;
    }

    /** Its purchases go with it. */
    public void deleteShopping(int id) {
        db.delete("Shopping", "_id = ?", new String[]{String.valueOf(id)});
    }

    /** Undoes deleteShopping for a planned shopping, which has no purchases to bring back. */
    public void restoreShopping(Shopping shopping) {
        ContentValues values = new ContentValues();
        values.put("_id", shopping.id);
        values.put("description", shopping.description);
        values.put("date", shopping.date);
        values.put("time", shopping.time);
        values.put("type", shopping.type);
        db.insertWithOnConflict("Shopping", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /** "2× Latte", "Pane" … for the products bought in a shopping. */
    public ArrayList<String> findPurchasedProducts(int shopping) {
        ArrayList<String> names = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("SELECT p.name, pu.quantity FROM Purchase pu JOIN Product p ON p._id = pu.product " +
                "WHERE pu.shopping = ? ORDER BY p.name", new String[]{String.valueOf(shopping)})) {
            while (cursor.moveToNext())
                names.add(Ui.withQuantity(cursor.getString(0), cursor.getInt(1)));
        }
        return names;
    }

    // ISO formats, the stored ones: they compare and sort correctly as strings
    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
    }

    static String now() {
        return new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date());
    }
}
