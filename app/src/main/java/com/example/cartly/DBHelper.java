package com.example.cartly;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "CartlyDB";
    // v2: ISO dates ("yyyy-MM-dd") and times ("HH:mm")
    // v3: real foreign keys, unique names
    // v4: no user accounts (one list per phone), product names ignore case, ticks on the list are saved
    // v5: quantities ("2× latte") on the list and in past shoppings
    private static final int DATABASE_VERSION = 5;

    private static final String CREATE_SHOPPING_TABLE =
            "CREATE TABLE Shopping (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "description TEXT NOT NULL DEFAULT '', " +
            "date TEXT NOT NULL, " +
            "time TEXT NOT NULL, " +
            "type INTEGER NOT NULL);";

    private static final String CREATE_PRODUCT_TABLE =
            "CREATE TABLE Product (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL UNIQUE COLLATE NOCASE);";

    private static final String CREATE_PURCHASE_TABLE =
            "CREATE TABLE Purchase (" +
            "shopping INTEGER NOT NULL, " +
            "product INTEGER NOT NULL, " +
            "quantity INTEGER NOT NULL DEFAULT 1, " +
            "PRIMARY KEY (shopping, product), " +
            "FOREIGN KEY (shopping) REFERENCES Shopping(_id) ON DELETE CASCADE, " +
            "FOREIGN KEY (product) REFERENCES Product(_id));";

    // the implicit rowid keeps the order in which products were added
    private static final String CREATE_CART_TABLE =
            "CREATE TABLE Cart (" +
            "product INTEGER NOT NULL UNIQUE, " +
            "checked INTEGER NOT NULL DEFAULT 0, " +
            "quantity INTEGER NOT NULL DEFAULT 1, " +
            "FOREIGN KEY (product) REFERENCES Product(_id));";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SHOPPING_TABLE);
        db.execSQL(CREATE_PRODUCT_TABLE);
        db.execSQL(CREATE_PURCHASE_TABLE);
        db.execSQL(CREATE_CART_TABLE);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // SQLite leaves foreign keys off by default. Enabled here, after onCreate/onUpgrade,
        // so the table rebuild below runs without them.
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // SQLiteOpenHelper already wraps onUpgrade in a transaction
        if (oldVersion < 2)
            convertDatesToIso(db);
        if (oldVersion < 4) {
            rebuildWithoutUsers(db);  // creates today's tables, quantities included
        } else if (oldVersion < 5) {
            db.execSQL("ALTER TABLE Cart ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE Purchase ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1");
        }
    }

    // SQLite can't change the columns or constraints of an existing table, so copy every table into a new one.
    // Everything on the phone ends up in the single list: the shoppings and carts of every old account.
    private void rebuildWithoutUsers(SQLiteDatabase db) {
        String[] tables = {"Cart", "Purchase", "Shopping", "Product", "User"};
        for (String table : tables)
            db.execSQL("ALTER TABLE " + table + " RENAME TO old_" + table);
        onCreate(db);
        // OR IGNORE plus joining on the name: products that differ only in case become one
        db.execSQL("INSERT OR IGNORE INTO Product (_id, name) SELECT _id, name FROM old_Product ORDER BY _id");
        db.execSQL("INSERT INTO Shopping (_id, description, date, time, type) " +
                "SELECT _id, coalesce(description, ''), date, time, type FROM old_Shopping");
        db.execSQL("INSERT OR IGNORE INTO Purchase (shopping, product) SELECT pu.shopping, p._id FROM old_Purchase pu " +
                "JOIN old_Product op ON op._id = pu.product JOIN Product p ON p.name = op.name " +
                "WHERE pu.shopping IN (SELECT _id FROM Shopping)");
        db.execSQL("INSERT OR IGNORE INTO Cart (product) SELECT p._id FROM old_Cart c " +
                "JOIN old_Product op ON op._id = c.product JOIN Product p ON p.name = op.name ORDER BY c.rowid");
        for (String table : tables)
            db.execSQL("DROP TABLE old_" + table);
    }

    // v1 stored "d/M/yyyy" and "H:m", which do not sort or compare as strings
    private void convertDatesToIso(SQLiteDatabase db) {
        try (Cursor cursor = db.query("Shopping", new String[]{"_id", "date", "time"}, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                String date = cursor.getString(1);
                String time = cursor.getString(2);
                // skip anything unexpected instead of failing the upgrade and locking the user out of the DB
                if (!date.matches("\\d{1,2}/\\d{1,2}/\\d{4}") || !time.matches("\\d{1,2}:\\d{1,2}"))
                    continue;
                String[] d = date.split("/");
                String[] t = time.split(":");
                ContentValues values = new ContentValues();
                values.put("date", String.format(Locale.ROOT, "%s-%02d-%02d", d[2], Integer.parseInt(d[1]), Integer.parseInt(d[0])));
                values.put("time", String.format(Locale.ROOT, "%02d:%02d", Integer.parseInt(t[0]), Integer.parseInt(t[1])));
                db.update("Shopping", values, "_id=?", new String[]{String.valueOf(cursor.getInt(0))});
            }
        }
    }
}
