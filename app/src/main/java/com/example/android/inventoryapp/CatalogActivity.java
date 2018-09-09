package com.example.android.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

public class CatalogActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int INVENTORY_LOADER = 0;

    // Setup an Adapter to create a list item for each row of inventory data in the Cursor
    InventoryCursorAdapter inventoryCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Set up FAB to open Editor Activity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the book data
        ListView bookListView = (ListView) findViewById(R.id.list);

        // Find the Sale button that will decrement the quantity by 1 when clicked.
        final Button saleButton = (Button) findViewById(R.id.sale_button);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        bookListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list for each row of inventory data in the Cursor.
        // There is no inventory data yet (until the loader finishes) so pass in null for the Cursor
        inventoryCursorAdapter = new InventoryCursorAdapter(this, null);
        bookListView.setAdapter(inventoryCursorAdapter);

        // Setup item click listener
        bookListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Create a new intent to open the {@link EditorActivity}
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);

                // Form the content URI that represents the specific book that was clicked on,
                // by appending the "id" (passed as input to this method) onto the
                // {@link InventoryEntry#CONTENT_URI}.
                // For example, the URI would be
                // "content://com.example.android.inventoryapp/inventory/2"
                // if the book with ID 2 was clicked on.
                Uri currentBookUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(currentBookUri);

                // Start the new activity
                startActivity(intent);
            }
        });

        // Kick off the loader
        getLoaderManager().initLoader(INVENTORY_LOADER, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu times to the app bar
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    /**
     * insert dummy data of a new book into database
     */

    private void insertData() {

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_PRODUCT_NAME, "The Fellowship of the Ring");
        values.put(InventoryEntry.COLUMN_PRICE, 19.99);
        values.put(InventoryEntry.COLUMN_QUANTITY, 20);
        values.put(InventoryEntry.COLUMN_SUPPLIER_NAME, "Best Fantasy Book Suppliers");
        values.put(InventoryEntry.COLUMN_SUPPLIER_PHONE_NUMBER, "555-555-5555");

        // Insert a new row for "The Fellowship of the Ring" into the provider using the ContentResolver.
        // Use the {@link InventoryEntry#CONTENT_URI} to indicate that we want to insert
        // into the inventory database table.
        // Receive the new content URI that will allow us to access Toto's data in the future.
        Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertData();
                return true;
            //Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllData();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns from the table we care about
        String[] projection = {
                InventoryEntry.COLUMN_ID,
                InventoryEntry.COLUMN_PRODUCT_NAME,
                InventoryEntry.COLUMN_PRICE,
                InventoryEntry.COLUMN_QUANTITY,
        };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   //Parent activity context
                InventoryEntry.CONTENT_URI,     // Provider content URI to query
                projection,                     // Columns to include in the resulting Cursor
                null,                  // No selection clause
                null,               // No selection arguments
                null);                 // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link InventoryCursorAdapter} with this new cursor containing updated inventory
        // data
        inventoryCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        inventoryCursorAdapter.swapCursor(null);
    }

    /**
     * Helper method to delete all data in the database
     */
    private void deleteAllData() {
        int rowsDeleted = getContentResolver().delete(InventoryEntry.CONTENT_URI, null,
                null);

        Log.v("CatalogActivity", rowsDeleted + " rows deleted from inventory database");

        // Show a toast message depending on whether or not the delete was successful.
        if (rowsDeleted == 0) {
            // If no rows were deleted, the there was an error with the delete.
            Toast.makeText(this, R.string.editor_delete_book_failed,
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the delete was successful and we can display a toast
            Toast.makeText(this, R.string.editor_delete_book_successful,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
