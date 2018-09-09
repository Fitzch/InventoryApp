package com.example.android.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

public class InventoryProvider extends ContentProvider {

    /**
     * URI matcher code for the content URI for the inventory table
     */
    private static final int INVENTORY = 100;

    /**
     * URI matcher code for the content URI for a single book in the inventory table
     */
    private static final int INVENTORY_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case
     */
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this calss.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a coresponding code to return
        // when a match is found

        uriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY,
                InventoryContract.PATH_INVENTORY, INVENTORY);
        uriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY,
                InventoryContract.PATH_INVENTORY + "/#", INVENTORY_ID);
    }

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    /**
     * Database helper object
     */
    private InventoryDbHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = uriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                // For the INVENTORY code, query the inventory table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the inventory table.
                cursor = database.query(InventoryContract.InventoryEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case INVENTORY_ID:
                // For the INVENTORY_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.inventoryapp/inventory/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the inventory table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor, so we know what content URI the Cursor was
        // created for. If the data th this URI changes, then we know we need to update the Cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = uriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return insertInventory(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a book into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertInventory(Uri uri, ContentValues values) {

        // Check that the title is not null
        String title = values.getAsString(InventoryEntry.COLUMN_PRODUCT_NAME);
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Book requires a title");
        }

        // Check that Price is not null and is not negative
        Double price = values.getAsDouble(InventoryEntry.COLUMN_PRICE);
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Book requires a valid price");
        }

        // Check that Quantity is not null and is a positive value
        Integer quantity = values.getAsInteger(InventoryEntry.COLUMN_QUANTITY);
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Book requires valid quantity");
        }

        // Check that the supplier name is not null
        String supplierName = values.getAsString(InventoryEntry.COLUMN_SUPPLIER_NAME);
        if (supplierName == null || supplierName.isEmpty()) {
            throw new IllegalArgumentException("Book requires supplier name");
        }

        // Check that the supplier phone number is not null and is a valid phone number
        String supplierPhoneNumber = values.getAsString(InventoryEntry.COLUMN_SUPPLIER_PHONE_NUMBER);
        if (!isValidPhoneNumber(supplierPhoneNumber)) {
            throw new IllegalArgumentException("Book requires supplier phone number");
        }

        // gets the database in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // insert the values
        long id = db.insert(InventoryEntry.TABLE_NAME, null, values);

        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the inventory content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = uriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return updateInventory(uri, contentValues, selection, selectionArgs);
            case INVENTORY_ID:
                // For the INVENTORY_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateInventory(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update inventory in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more books).
     * Return the number of rows that were successfully updated.
     */
    private int updateInventory(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // If the {@link InventoryEntry#COLUMN_PRODUCT_NAME} key is present,
        // check that the name value is not null.
        if (values.containsKey(InventoryEntry.COLUMN_PRODUCT_NAME)) {

            // Check that the name is not null
            String title = values.getAsString(InventoryEntry.COLUMN_PRODUCT_NAME);

            if (title == null || title.isEmpty()) {
                throw new IllegalArgumentException("Book requires a title");
            }
        }

        // If the {@link InventoryEntry#COLUMN_PRICE} key is present,
        // check that the price value is valid.
        if (values.containsKey(InventoryEntry.COLUMN_PRICE)) {

            // Check that Price is not null and is a positive value
            Double price = values.getAsDouble(InventoryEntry.COLUMN_PRICE);
            if (price == null || price < 0) {
                throw new IllegalArgumentException("Book requires a valid price");
            }
        }

        // If the {@link InventoryEntry#COLUMN_QUANTITY} key is present,
        // check that the quantity value is valid.
        if (values.containsKey(InventoryEntry.COLUMN_QUANTITY)) {

            // Check that Quantity is not null and is a positive value
            Integer quantity = values.getAsInteger(InventoryEntry.COLUMN_QUANTITY);
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Book requires valid quantity");
            }
        }

        // If the {@link InventoryEntry#COLUMN_SUPPLIER_NAME} key is present,
        // check that the price value is valid.
        if (values.containsKey(InventoryEntry.COLUMN_SUPPLIER_NAME)) {

            // Check that the supplier name is not null
            String supplierName = values.getAsString(InventoryEntry.COLUMN_SUPPLIER_NAME);
            if (supplierName == null || supplierName.isEmpty()) {
                throw new IllegalArgumentException("Book requires supplier name");
            }
        }

        // If the {@link InventoryEntry#COLUMN_SUPPLIER_PHONE_NUMBER} key is present,
        // check that the price value is valid.
        if (values.containsKey(InventoryEntry.COLUMN_SUPPLIER_PHONE_NUMBER)) {

            // Check that the supplier phone number is not null and is a valid phone number
            String supplierPhoneNumber = values.getAsString(InventoryEntry.COLUMN_SUPPLIER_PHONE_NUMBER);
            if (!isValidPhoneNumber(supplierPhoneNumber)) {
                throw new IllegalArgumentException("Book requires supplier phone number");
            }
        }

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // gets the database in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // update the values
        int rowsUpdated = db.update(InventoryEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            // Notify all listeners that the data has changed for the inventory content URI
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        final int match = uriMatcher.match(uri);

        // Track the number of rows that were deleted
        int rowsDeleted;

        switch (match) {
            case INVENTORY:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case INVENTORY_ID:
                // Delete a single row given by the ID in the URI
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        final int match = uriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return InventoryEntry.CONTENT_LIST_TYPE;
            case INVENTORY_ID:
                return InventoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    /**
     * Validates that phone numbers are properly formatted
     * <p>
     * Method found at
     * https://stackoverflow.com/questions/5958665/validation-for-a-cell-number-in-android/5959341#5959341
     *
     * @param phoneNumber phone number of the current supplier
     */
    public static boolean isValidPhoneNumber(CharSequence phoneNumber) {
        if (phoneNumber == null || TextUtils.isEmpty(phoneNumber)) {
            return false;
        } else {
            return android.util.Patterns.PHONE.matcher(phoneNumber).matches();
        }
    }
}
