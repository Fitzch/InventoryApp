package com.example.android.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * {@link InventoryCursorAdapter} is an adapter for a list or grid view that uses a {@link Cursor}
 * of inventory data as its data source. This adapter knows how to create list items for each row of
 * inventory data in the {@link Cursor}
 */
public class InventoryCursorAdapter extends CursorAdapter{

    /**
     * Constructs a new {@link InventoryCursorAdapter}
     *
     * @param context   The context
     * @param cursor    The cursor from which to get the data
     */
    public InventoryCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the inventory data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current book can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views tha we want to modify in the list item layout
        TextView bookTitleTextView = (TextView) view.findViewById(R.id.bookTitle);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);

        // Find sale button
        Button saleButton = (Button) view.findViewById(R.id.sale_button);

        // Find the columns of book attributes that we're interested in
        int bookTitleColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_QUANTITY);
        int idColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ID);

        // Read the book attributes from the Cursor for the current book
        String bookTitle = cursor.getString(bookTitleColumnIndex);
        double bookPrice = cursor.getDouble(priceColumnIndex);
        final String bookQuantity = cursor.getString(quantityColumnIndex);
        long id = cursor.getLong(idColumnIndex);

        // Update the TextViews with the attributes for the current book
        bookTitleTextView.setText(bookTitle);
        priceTextView.setText(formatPrice(bookPrice));
        quantityTextView.setText(bookQuantity);

        // Build Uri for current book
        final Uri currentBookUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);

        // Set click listener on Sale button
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Decrease quantity by 1. Using new variable for quantity since bookQuantity
                // must be declared final in order to be used in OnClickListener
                String bookQuantityAfterSale = sellBook(Integer.parseInt(bookQuantity), context);

                // Set the new quantity on the Quantity Text View.
                quantityTextView.setText(bookQuantityAfterSale);

                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();
                values.put(InventoryEntry.COLUMN_QUANTITY, Integer.parseInt(bookQuantityAfterSale));

                //Update the database with the new value
                int rowsAffected = context.getContentResolver().update(
                        currentBookUri,
                        values,
                        null,
                        null
                );

                // Show a toast message depending on whether or not the update was successful.
                if (rowsAffected == 0) {
                    // If no rows were affected, then there was an error with the update.
                    Toast.makeText(context, R.string.update_failed, Toast.LENGTH_SHORT).show();
                } else if (Integer.parseInt(bookQuantity) != 0){
                    // Otherwise, the update was successful and we can display a toast, but only
                    // when the bookQuantity is not equal to 0.
                    Toast.makeText(context, R.string.sale_successful, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     *  Helper method to format book price as currency especially when user inputs trailing zeros
     *  example: 8.50
     *
     * @param price of the current book
     * @return the formatted price.
     */

    private String formatPrice (double price){
        DecimalFormat priceFormat = new DecimalFormat("0.00");
        return priceFormat.format(price);
    }

    /**
     * Helper method to decrement book quantity when Sale button is pressed.
     *
     * @param quantity current quantity in stock of current book.
     * @param context the context of the activity where update is happening.
     * @return the quantity of books after the sale is complete.
     */

    private String sellBook (int quantity, Context context) {
        if (quantity == 0){
            // If quantity is already 0, do not decrement. Show toast informing user that quantity
            // cannot be less than 0.
            Toast.makeText(context, R.string.quantity_less_than_zero, Toast.LENGTH_SHORT).show();
            return String.valueOf(quantity);

            // Otherwise decrement the quantity by 1.
        } else {
            quantity--;
            return String.valueOf(quantity);
        }
    }
}
