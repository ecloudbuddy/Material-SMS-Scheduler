package com.kyleszombathy.sms_scheduler;

import android.content.CursorLoader;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

public class AddMessage extends AppCompatActivity
{
    static final int PICK_CONTACT_REQUEST = 1;
    private static final String[] PROJECTION =
            {
                    ContactsContract.Data._ID,
                    ContactsContract.CommonDataKinds.Identity.IDENTITY,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Photo.PHOTO
            };
    private static final String SELECTION = ContactsContract.Data.LOOKUP_KEY + " = ?";
    String mSelectionClause = null;
    private String[] mSelectionArgs = { "" };
    private String mLookupKey;
    EditText et;

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

        // Setting up ContactsFragment
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
        // TODO: Doesn't work yet, works only from button
/*        et.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction()==KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    //et.setText("test", TextView.BufferType.EDITABLE);
                    Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
                    pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
                    startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
                    return true;
                }
                return false;
            }
        });*/
    }

    public void showTimePickerDialog(View v) {
        TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void showDatePickerDialog(View v) {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void showContactsFragment(View v) {
    // Create fragment and give it an argument specifying the article it should show
            Fragment newFragment = new ContactsFragment();
            Bundle args = new Bundle();
            //args.putInt(ContactsFragment.ARG_POSITION, position);
            newFragment.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

    // Replace whatever is in the fragment_container view with this fragment,
    // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);

    // Commit the transaction
            transaction.commit();
    }

    public void openContactSearch(View v) {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
        startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
                // A contact was picked.
                Uri contactUri = data.getData();
                System.out.println(contactUri);
                String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
                // TODO: CurorLoader Implementation https://developer.android.com/training/basics/intents/result.html
                CursorLoader loader = new CursorLoader(getApplicationContext(), contactUri, projection, null, null, null);


                System.out.println(loader.toString());

/*                String contactName = data.getStringExtra("android.intent.extra.shortcut.NAME");
                System.out.println(contactUri);
                et.setText(contactName, TextView.BufferType.EDITABLE);*/
            }
        }
    }

}
