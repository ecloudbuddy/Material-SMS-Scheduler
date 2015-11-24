package com.kyleszombathy.sms_scheduler;

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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class AddMessage extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener,
        DatePickerFragment.OnCompleteListener, TimePickerFragment.OnCompleteListener
{
    // These are the Contacts rows that we will retrieve.
    private static final String[] PROJECTION = new String[] {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER/*,
            ContactsContract.CommonDataKinds.Photo.PHOTO*/
    };
    private static final int PICK_CONTACT_REQUEST = 1;

    private static final String SELECTION = ContactsContract.Data.LOOKUP_KEY + " = ?";
    private String[] mSelectionArgs = { "" };
    private static final String SORT_ORDER = Data.MIMETYPE;

    ArrayList<Integer> ID = new ArrayList<>();
    ArrayList<String> name = new ArrayList<>();
    ArrayList<String> phoneNum = new ArrayList<>();
    private int arrayListCount = 0;
    private int curYear, curMonth, curDay, curHourOfDay, curMinute;
    private int year, month, day, hourOfDay, minute;
    private String messageContentString = "";

    private Uri uri;

    // For getting character count
    private int smsLength = 160;
    private TextView counterTextView;
    private EditText messageContentEditText;
    private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //This sets a textview to the current length
            int length = s.length();
            if (length <= smsLength) {
                counterTextView.setText(String.valueOf(smsLength - length));
            }
            else {
                counterTextView.setText(String.valueOf(length / smsLength) + " / "
                        + String.valueOf(smsLength - length % smsLength));
            }

        }
        public void afterTextChanged(Editable s) {
        }
    };


    Button saveButton;
    private int DETAILS_QUERY_ID = 0;

    // Contact Picker
    RecipientEditTextView phoneRetv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_message);

        // Setting up toolbar
        Toolbar myChildToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myChildToolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button on toolbar
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);


        // Set up initial fragment to display
        AddMessageFragment firstFragment = new AddMessageFragment();
        // Pass extras to fragment
        firstFragment.setArguments(getIntent().getExtras());
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, firstFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Get character count
        counterTextView = (TextView) findViewById(R.id.count);
        messageContentEditText = (EditText) findViewById(R.id.messageContent);
        messageContentEditText.addTextChangedListener(mTextEditorWatcher);


        // Creates an autocomplete for phone number contacts
        phoneRetv = (RecipientEditTextView) findViewById(R.id.phone_retv);
        phoneRetv.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        phoneRetv.setAdapter(new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, this));

        // Get current date/time
        setCurrentTime();

        // Set up date spinner
        ArrayList<CharSequence> dateEntries =
                new ArrayList<CharSequence>(Arrays.<CharSequence>asList(
                        getString(R.string.today),
                        getString(R.string.tomorrow),
                        getString(R.string.next) + " " + getDayOfWeekString(),
                        getString(R.string.pick_date)));
        ArrayAdapter<CharSequence> dateAdapter =
                new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, dateEntries);
        Spinner dateSpinner = (Spinner) findViewById(R.id.date_spinner);
        // Specify the layout to use when the list of choices appears
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        dateSpinner.setAdapter(dateAdapter);
        dateSpinner.setOnItemSelectedListener(this);

        // Set up time spinner
        ArrayList<CharSequence> timeEntries =
                new ArrayList<CharSequence>(Arrays.<CharSequence>asList(
                        getString(R.string.morning),
                        getString(R.string.afternoon),
                        getString(R.string.evening),
                        getString(R.string.night),
                        getString(R.string.pick_time)));
        ArrayAdapter<CharSequence> timeAdapter =
                new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, timeEntries);
        Spinner timeSpinner = (Spinner) findViewById(R.id.time_spinner);
        // Specify the layout to use when the list of choices appears
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        timeSpinner.setAdapter(timeAdapter);
        timeSpinner.setOnItemSelectedListener(this);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send:
                messageContentString = messageContentEditText.getText().toString();
                DrawableRecipientChip[] chips = phoneRetv.getSortedRecipients();
                //Error handling
                if (chips.length == 0) {
                    phoneRetv.setHint(R.string.error_recipient);
                    phoneRetv.setHintTextColor(getResources().getColor(R.color.error_primary));
                }
                if (messageContentString.length() == 0) {
                    messageContentEditText.setHint(R.string.error_message_content);
                    messageContentEditText.setHintTextColor(getResources().getColor(R.color.error_primary));
                }
                for (int i = 0; i < chips.length; i++) {
                    String str = chips[i].toString();
                    phoneNum.add(getPhoneNumberFromString(str));
                    name.add(getNameFromString(str));
                }

                //TODO: CREATE SQL TABLE HERE

                // Return to Home
                //finish();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override // Inserts menu send button
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_message, menu);
        return true;
    }

    public void showTimePickerDialog() {
        TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void showDatePickerDialog() {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
/*        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
                // A contact was picked.
                uri = data.getData();
                // Initiate loader
                getLoaderManager().initLoader(DETAILS_QUERY_ID, null, this);
            }
        }*/
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, uri, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
/*        data.moveToPosition(0);
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

        // Add name to recipients list
        if (name.size() == 1) {
            //t.setText(t.getText() + " " + name.get(arrayListCount));
        } else {
            //t.setText(t.getText() + ", " + name.get(arrayListCount));
        }

        DETAILS_QUERY_ID++;
        arrayListCount++;*/
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
    }

    public String getDayOfWeekString() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.SUNDAY:
                 return getString(R.string.sunday);
            case Calendar.MONDAY:
                return getString(R.string.monday);
            case Calendar.TUESDAY:
                return getString(R.string.tuesday);
            case Calendar.WEDNESDAY:
                return getString(R.string.wednesday);
            case Calendar.THURSDAY:
                return getString(R.string.thursday);
            case Calendar.FRIDAY:
                return getString(R.string.friday);
            case Calendar.SATURDAY:
                return getString(R.string.saturday);
            default:
                return "";
        }
    }

    public String getPhoneNumberFromString(String str) {
        String temp = "";
        for (int i =0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isDigit(c)) {
                temp += c;
            }
        } return temp;
    }

    public String getNameFromString(String str) {
        String temp = "";
        for (int i =0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isAlphabetic(c) ) {
                temp += c;
            }
        } return temp;
    }

    //Gets current time
    public void setCurrentTime(){
        Calendar calendar = Calendar.getInstance();
        year = curYear = calendar.get(Calendar.YEAR);
        month = curMonth = calendar.get(Calendar.MONTH);
        day = curDay = calendar.get(Calendar.DAY_OF_MONTH);
        hourOfDay = curHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        minute = curMinute = calendar.get(Calendar.MINUTE);
    }

    // Item selected from spinner
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        System.out.println(parent.toString());
        if(parent.getItemAtPosition(pos).equals(getString(R.string.today))) {
            year = curYear;
            month = curMonth;
            day = curDay;
        }
        else if(parent.getItemAtPosition(pos).equals(getString(R.string.tomorrow))) {
            year = curYear;
            month = curMonth;
            day = curDay + 1;
        }
        else if(parent.getItemAtPosition(pos).equals(getString(R.string.next) + " " + getDayOfWeekString())) {
            year = curYear;
            month = curMonth;
            day = curDay + 7;
        }
        else if(parent.getItemAtPosition(pos).equals(getString(R.string.pick_date))) {
            showDatePickerDialog();
        }
        else if(parent.getItemAtPosition(pos).equals(getString(R.string.morning))) {
            hourOfDay = 9;
            minute = 0;
        }
        else if(parent.getItemAtPosition(pos).equals(getString(R.string.afternoon))) {
            hourOfDay = 13;
            minute = 0;
        }
        else if(parent.getItemAtPosition(pos).equals(getString(R.string.evening))) {
            hourOfDay = 17;
            minute = 0;
        }
        else if(parent.getItemAtPosition(pos).equals(getString(R.string.night))) {
            hourOfDay = 20;
            minute = 0;
        }
        else if(parent.getItemAtPosition(pos).equals(getString(R.string.pick_time))) {
            showTimePickerDialog();
        }
    }

    // If nothing selected on spinner
    public void onNothingSelected(AdapterView<?> parent) {
        year = curYear;
        month = curMonth;
        day = curDay;
    }

    // Retrieves data from DatePickerFragment
    public void onComplete(int year, int month, int day) {
        year = this.year;
        month = this.month;
        day = this.day;
    }

    @Override
    public void onComplete(int hourOfDay, int minute) {
        hourOfDay = this.hourOfDay;
        minute = this.minute;
    }
}
