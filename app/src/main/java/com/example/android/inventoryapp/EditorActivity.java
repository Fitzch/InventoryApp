package com.example.android.inventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;
import com.example.android.inventoryapp.data.InventoryProvider;


public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the inventory data loader
     */
    private static final int INVENTORY_LOADER = 0;

    /**
     * Content URI for the exiting book (null if it's a new book)
     */
    private Uri currentBookUri;

    /**
     * EditText field to enter the book title.
     */
    private EditText bookTitleEditText;

    /**
     * EditText field to enter the book price
     */
    private EditText bookPriceEditText;

    /**
     * EditText field to enter the book quantity
     */
    private EditText bookQuantityEditText;

    /**
     * EditText field to enter the supplier name
     */
    private EditText supplierNameEditText;

    /**
     * EditText field to enter the supplier phone number
     */
    private EditText supplierPhoneNumberEditText;

    /**
     * Tag for log messages
     */
    private static final String LOG_TAG = EditorActivity.class.getName();

    /**
     * Boolean flag that keeps track of whether the book has been edited (true) or not (false).
     */
    private boolean bookHasChanged = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity in order to figure out
        // if we're creating a new book or editing an existing one.
        Intent intent = getIntent();
        currentBookUri = intent.getData();

        try {
            Log.e(LOG_TAG, currentBookUri.toString());
        } catch (NullPointerException exception) {
            exception.printStackTrace();
        }

        // If the intent DOES NOT contain an inventory content URI, then we know that we are
        // creating a new book
        if (currentBookUri == null) {
            // This is a new book so change the app bar to say "Add a Book"
            setTitle(R.string.editor_activity_title_new_book);

            // Invalidate the options menu so that the "Delete" menu option can be hidden.
            // It doesn't make sense to delete a book that hasn't been created yet.
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing book so change teh app bar to say "Edit Book"
            setTitle(R.string.editor_activity_title_edit_book);

            // Initialize a loader to read the book data from the database and display the current
            // values in the editor
            getLoaderManager().initLoader(INVENTORY_LOADER, null, this);
        }

        // Find all relevant views that we will need from which to read user input
        bookTitleEditText = (EditText) findViewById(R.id.edit_book_title);
        bookPriceEditText = (EditText) findViewById(R.id.edit_book_price);
        bookQuantityEditText = (EditText) findViewById(R.id.edit_book_quantity);
        supplierNameEditText = (EditText) findViewById(R.id.edit_supplier_name);
        supplierPhoneNumberEditText = (EditText) findViewById(R.id.edit_supplier_phone);

        // Find buttons that user can interact with
        Button decrementQuantityButton = (Button) findViewById(R.id.decrement_quantity_button);
        Button incrementQuantityButton = (Button) findViewById(R.id.increment_quantity_button);
        Button orderBooksButton = (Button) findViewById(R.id.order_button);

        // Set listeners on all edit texts and on the increment/decrement buttons so that we can
        // determine if an edit has been made.
        bookTitleEditText.setOnTouchListener(touchListener);
        bookPriceEditText.setOnTouchListener(touchListener);
        bookQuantityEditText.setOnTouchListener(touchListener);
        supplierNameEditText.setOnTouchListener(touchListener);
        supplierPhoneNumberEditText.setOnTouchListener(touchListener);
        decrementQuantityButton.setOnTouchListener(touchListener);
        incrementQuantityButton.setOnTouchListener(touchListener);

        // Set click listener on decrement button
        decrementQuantityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the current quantity
                String bookQuantityString = bookQuantityEditText.getText().toString().trim();

                // Check if the book quantity is not empty.
                if (!bookQuantityString.isEmpty()) {

                    // Parse the quantity into an int so calculations can be performed on it.
                    int bookQuantity = Integer.parseInt(bookQuantityString);

                    // Decrement the quantity
                    bookQuantity = decrementQuantity(bookQuantity);

                    // Update the edit text for quantity with the new value
                    bookQuantityEditText.setText(String.valueOf(bookQuantity));
                }
            }
        });

        // Set click listener on increment button
        incrementQuantityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the current quantity
                String bookQuantityString = bookQuantityEditText.getText().toString().trim();

                if (!bookQuantityString.isEmpty()) {

                    // Parse the quantity into an int so calculations can be performed on it.
                    int bookQuantity = Integer.parseInt(bookQuantityString);

                    // Increment the quantity
                    bookQuantity = incrementQuantity(bookQuantity);

                    // Update the edit text for quantity with the new value.
                    bookQuantityEditText.setText(String.valueOf(bookQuantity));
                } else {
                    // Otherwise, go ahead and set the quantity edit text to 1.
                    bookQuantityEditText.setText("1");
                }
            }
        });

        // Set click listener on order button
        orderBooksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the phone number for the current book
                String uri = "tel:" +
                        supplierPhoneNumberEditText.getText().toString().trim();

                // Create new intent to dial supplier phone number
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse(uri));

                // Start the intent
                startActivity(intent);
            }
        });
    }

    /**
     * Get user input from the editor and save new book into database
     */
    private boolean saveBook() {
        String bookTitleString = bookTitleEditText.getText().toString().trim();
        String bookPriceString = bookPriceEditText.getText().toString().trim();
        String bookQuantityString = bookQuantityEditText.getText().toString().trim();
        String supplierNameString = supplierNameEditText.getText().toString().trim();
        String supplierPhoneNumberString = supplierPhoneNumberEditText.getText().toString().trim();

        // Check if this is supposed to be a new book and check if all the fields in the editor
        // are blank.
        if (currentBookUri == null &&
                bookTitleString.isEmpty() && bookPriceString.isEmpty() &&
                bookQuantityString.isEmpty() && supplierNameString.isEmpty() &&
                supplierPhoneNumberString.isEmpty()) {
            // Since no fields were modified, we can return early without creating a new book.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            // Finish Editor Activity and return to Catalog Activity
            return true;
        }

        // Check if fields are blank
        if (bookTitleString.isEmpty() || bookPriceString.isEmpty() ||
                bookQuantityString.isEmpty() || supplierNameString.isEmpty() ||
                supplierPhoneNumberString.isEmpty()) {
            // If it is blank, show toast informing user that all fields are required.
            Toast.makeText(this, R.string.all_fields_required, Toast.LENGTH_SHORT).show();

            // Return to the Editor activity. Allow user to continue editing.
            return false;

            // Else check if the phone number is valid
        } else if (!InventoryProvider.isValidPhoneNumber(supplierPhoneNumberString)) {
            Toast.makeText(this, R.string.incorrectly_formatted_phone_number,
                    Toast.LENGTH_SHORT).show();

            // Return to the Editor activity. Allow user to continue editing.
            return false;

        } else {

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(InventoryEntry.COLUMN_PRODUCT_NAME, bookTitleString);

            // If the price or quantity are not provided by the user, don't try to parse the string
            // into an integer values. Use 0 by default.
            double price = 0;
            int quantity = 0;
            if (!bookPriceString.isEmpty()) {
                price = Double.parseDouble(bookPriceString);
            }
            values.put(InventoryEntry.COLUMN_PRICE, price);

            if (!bookQuantityString.isEmpty()) {
                quantity = Integer.parseInt(bookQuantityString);
            }

            values.put(InventoryEntry.COLUMN_QUANTITY, quantity);
            values.put(InventoryEntry.COLUMN_SUPPLIER_NAME, supplierNameString);
            values.put(InventoryEntry.COLUMN_SUPPLIER_PHONE_NUMBER, supplierPhoneNumberString);

            // Determine if this is a new or existing book by checking if currentBookUri is null or not
            if (currentBookUri == null) {

                // Insert a new book into the provider returning the content URI for the new book
                Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

                // Show a toast message depending on whether or not the insertion was successful
                if (newUri == null) {
                    // If the new content URI is null, then there was an error with insertion
                    Toast.makeText(this, R.string.insert_failed, Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise the insertion was successful, and we can display a toast
                    Toast.makeText(this, R.string.insert_successful, Toast.LENGTH_SHORT).show();
                }
            } else {
                // Otherwise this is an existing book, so update the book with the content URI:
                // currentBookUri and pass in the new ContentValues. Pass in null for the selection and
                // selection args because currentBookUri will already identify the correct row in the
                // database that we want to modify.

                int rowsAffected = getContentResolver().update(
                        currentBookUri,
                        values,
                        null,
                        null);

                // Show a toast message depending on whether or not the update was successful.
                if (rowsAffected == 0) {
                    // If no rows were affected, then there was an error with the update.
                    Toast.makeText(this, R.string.update_failed, Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the update was successful and we can display a toast.
                    Toast.makeText(this, R.string.update_successful, Toast.LENGTH_SHORT).show();
                }
            }

            // Finish Editor Activity and return to Catalog Activity
            return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the /res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu options in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // If the save book function returns true, save the book and finish
                // the editor activity
                if (saveBook()) {
                    // Exit Activity
                    finish();
                    return true;
                } else {
                    // Allow the user to continue editing.
                    return false;
                }
                // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the book hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!bookHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that the changes
                // should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user that they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new book, hide the "Delete" menu item.
        if (currentBookUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all book attributes, define a projection that contains
        // all columns from the inventory table
        String[] projection = {
                InventoryEntry.COLUMN_ID,
                InventoryEntry.COLUMN_PRODUCT_NAME,
                InventoryEntry.COLUMN_PRICE,
                InventoryEntry.COLUMN_QUANTITY,
                InventoryEntry.COLUMN_SUPPLIER_NAME,
                InventoryEntry.COLUMN_SUPPLIER_PHONE_NUMBER
        };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                currentBookUri,                 // Query the content URI for the current book
                projection,                     // Columns to include in the resulting Cursor
                null,                  // No selection clause
                null,               // No selection arguments
                null);                 // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of the book attributes that we're interested in
            int bookTitleIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME);
            int bookPriceIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE);
            int bookQuantityIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_QUANTITY);
            int supplierNameIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_SUPPLIER_NAME);
            int supplierPhoneNumberIndex = cursor.getColumnIndex(
                    InventoryEntry.COLUMN_SUPPLIER_PHONE_NUMBER);

            // Extract out the value from the Cursor for the given column index.
            String bookTitle = cursor.getString(bookTitleIndex);
            double bookPrice = cursor.getDouble(bookPriceIndex);
            int bookQuantity = cursor.getInt(bookQuantityIndex);
            String supplierName = cursor.getString(supplierNameIndex);
            String supplierPhoneNumber = cursor.getString(supplierPhoneNumberIndex);

            // Update the view on the screen with the values from the database
            bookTitleEditText.setText(bookTitle);
            bookPriceEditText.setText(Double.toString(bookPrice));
            bookQuantityEditText.setText(Integer.toString(bookQuantity));
            supplierNameEditText.setText(supplierName);
            supplierPhoneNumberEditText.setText(supplierPhoneNumber);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from tne input fields.
        bookTitleEditText.setText("");
        bookPriceEditText.setText("");
        bookQuantityEditText.setText("");
        supplierNameEditText.setText("");
        supplierPhoneNumberEditText.setText("");

    }

    // OnTouchListener that listens for any user touches on a View, implying that they are modifying
    // the view, and we change the bookHasChanged boolean to true.
    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            bookHasChanged = true;
            return false;
        }
    };

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message and click listeners for the
        // positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing button, so dismiss the dialog and
                // continue editing the book.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        // If the book hasn't changed, continue with handling back button press
        if (!bookHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }


    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners for the positive
        // and negative buttons on the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the book.
                deleteBook();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog and continue
                // editing the book.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the book in the database
     */
    private void deleteBook() {
        // Only perform the delete if this is an existing book.
        if (currentBookUri != null) {
            // Call the ContentResolver to delete the book at the given content URI.
            // Pass in null for the selection and selection args because the currentBookUri
            // content URI already identifies teh poet that we want.
            int rowsDeleted = getContentResolver().delete(currentBookUri, null,
                    null);

            // Show a toast message depending on whether or not the delete was successful
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, R.string.editor_delete_book_failed,
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise the delte was successful, and we can display a toast
                Toast.makeText(this, R.string.editor_delete_book_successful,
                        Toast.LENGTH_SHORT).show();
            }
            // Close the activity
            finish();
        }
    }

    /**
     * The helper method to decrement the quantity of books in stock.
     *
     * @param quantity the current quantity of books.
     * @return quantity
     */

    private int decrementQuantity(int quantity) {
        if (quantity == 0) {
            Toast.makeText(this, R.string.quantity_less_than_zero, Toast.LENGTH_SHORT).show();
            return quantity;
        } else {
            quantity--;
            return quantity;
        }
    }

    /**
     * The helper method to increment the quantity of books in stock.
     *
     * @param quantity the current quantity of books in stock.
     * @return quantity
     */

    private int incrementQuantity(int quantity) {
        if (quantity == 100) {
            Toast.makeText(this, R.string.too_many_books, Toast.LENGTH_SHORT).show();
            return quantity;
        } else {
            quantity++;
            return quantity;
        }
    }
}
