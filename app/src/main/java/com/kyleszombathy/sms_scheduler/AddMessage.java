package com.kyleszombathy.sms_scheduler;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import java.util.Arrays;
import java.util.LinkedList;

public class AddMessage extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>
{
    // These are the Contacts rows that we will retrieve.
    private static final String[] PROJECTION = new String[] {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER/*,
            ContactsContract.CommonDataKinds.Photo.PHOTO*/
    };
    private static final int PICK_CONTACT_REQUEST = 0;

    private static final String SELECTION = ContactsContract.Data.LOOKUP_KEY + " = ?";
    private String[] mSelectionArgs = { "" };
    private static final String SORT_ORDER = Data.MIMETYPE;

    LinkedList <Integer> ID = new LinkedList<>();
    LinkedList <String> name = new LinkedList<>();
    LinkedList <Long> phoneNum = new LinkedList<>();

    private Uri uri;
    private EditText et;
    private final int DETAILS_QUERY_ID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_message);

        // Setting up toolbar
        Toolbar myChildToolbar =
                (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myChildToolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button on toolbar
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        // Setting up default fragment
        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
            // Creates a new Fragment to be placed in the activity layout
            AddMessageFragment firstFragment = new AddMessageFragment();
            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            firstFragment.setArguments(getIntent().getExtras());
            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment).commit();
        }

        // Setting editText field
        et =(EditText)findViewById(R.id.phoneNumber);
    }

    public void showTimePickerDialog(View v) {
        TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void showDatePickerDialog(View v) {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void openContactSearch(View v) {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
        startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }

    public void showContactsFragment(View v) {
    // Create fragment and give it an argument specifying the article it should show
            Fragment newFragment = new ContactsFragment();
            Bundle args = new Bundle();
            //args.putInt(ContactsFragment., position);
            newFragment.setArguments(args);

            FragmentTransaction transaction = getFragmentManager().beginTransaction();

    // Replace whatever is in the fragment_container view with this fragment,
    // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);

    // Commit the transaction
            transaction.commit();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
                // A contact was picked.
                uri = data.getData();
                System.out.println("\n\n\n\n");

                getLoaderManager().initLoader(DETAILS_QUERY_ID, null, this);
            }
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, uri, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        data.moveToPosition(0);
        ID.add(data.getInt(0));
        name.add(data.getString(1));
        String str = data.getString(2);
        String temp = "";
        // Gets int from string of phone number
        for (int i =0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isDigit(c)) {
                temp += c;
            }
        }
        phoneNum.add(Long.parseLong(temp));
        System.out.println("ID: " + Arrays.toString(ID.toArray()));
        System.out.println("Name: " + Arrays.toString(name.toArray()));
        System.out.println("Phone: " + Arrays.toString(phoneNum.toArray()));
        data.close();
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        //mAdapter.swapCursor(null);
    }

}
