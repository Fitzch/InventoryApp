package com.example.android.inventoryapp.data;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

public class InventoryDbHelper extends SQLiteOpenHelper {

    private static final String LOG_TAG = InventoryDbHelper.class.getSimpleName();

    /**
     * name of the database file
     */
    public static final String DATABASE_NAME = "storeInventory.db";

    /**
     * Database version. If the database schema is changed, the version must be incremented.
     */

    public static final int DATABASE_VERSION = 1;

    /**
     * SQL Statement to CREATE TABLE inventory
     */

    public static final String SQL_CREATE_INVENTORY_TABLE = "CREATE TABLE " +
            InventoryEntry.TABLE_NAME + " ("
            + InventoryEntry.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + InventoryEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, "
            + InventoryEntry.COLUMN_PRICE + " INTEGER NOT NULL, "
            + InventoryEntry.COLUMN_QUANTITY + " REAL NOT NULL DEFAULT 0, "
            + InventoryEntry.COLUMN_SUPPLIER_NAME + " TEXT, "
            + InventoryEntry.COLUMN_SUPPLIER_PHONE_NUMBER + " TEXT);";

    // Public constructor of InventoryDbHelper
    public InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_INVENTORY_TABLE);
        Log.i(LOG_TAG, SQL_CREATE_INVENTORY_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
