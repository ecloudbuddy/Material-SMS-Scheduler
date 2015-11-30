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
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.transition.Fade;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class AddMessage extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener,
        DatePickerFragment.OnCompleteListener, TimePickerFragment.OnCompleteListener
{
    // For contact info retrieval. Disabled
    private Uri uri;
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
    private int DETAILS_QUERY_ID = 0;

    // For storing current time and user input time
    private int curYear, curMonth, curDay, curHourOfDay, curMinute;
    private int year, month, day, hourOfDay, minute;

    // Spinners
    ArrayList<CharSequence> dateEntries;
    ArrayList<CharSequence> timeEntries;
    String morning, afternoon, evening, night;

    // Contact Picker
    RecipientEditTextView phoneRetv;
    DrawableRecipientChip[] chips;

    // Other
    private TextView errorMessageContent;
    private String messageContentString = "";

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
            if (length == 1) {

            } if (length <= smsLength) {
                counterTextView.setText(String.valueOf(smsLength - length));
            } else {
                counterTextView.setText(String.valueOf(smsLength - length % smsLength)
                        + " / " + String.valueOf(1 + (length / smsLength)));
            }
        }
        public void afterTextChanged(Editable s) {
        }
    };

    // For removing error text
    private final TextWatcher phoneTextEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            DrawableRecipientChip[] chips = phoneRetv.getSortedRecipients();
            if (chips.length == 1) {

            }
        }
        public void afterTextChanged(Editable s) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setAllowEnterTransitionOverlap(true);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setEnterTransition(new Fade());

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
        // Pass extras to fragment - not necessary in our case but leaving in for future
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

        setUpAutocomplete();
        setCurrentDate();
        setCurrentTime();
        setUpSpinners();
    }

    private void setUpAutocomplete() {
        phoneRetv = (RecipientEditTextView) findViewById(R.id.phone_retv);
        phoneRetv.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        phoneRetv.setAdapter(new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send:
                messageContentString = messageContentEditText.getText().toString();
                chips = phoneRetv.getSortedRecipients();

                if (verifyData()) {
                    addDataToSQL();
                    createSnackBar(getString(R.string.success));
                    //finish();
                    return true;
                }
            default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void createSnackBar(String str) {

        // Create snackbar
        Snackbar snackbar = Snackbar
                .make(findViewById(android.R.id.content), str, Snackbar.LENGTH_LONG);

        snackbar.show();
    }

    private void addDataToSQL() {
        // Insert phone numbers and names into arraylist
        for (DrawableRecipientChip chip : chips) {
            String str = chip.toString();
            String tempName = getNameFromString(str);
            String tempPhone = getPhoneNumberFromString(str);
            phoneNum.add(tempPhone);
            // Adds phone number as name if no name exists
            if(tempName.length() == 0) {
                name.add(tempPhone);
            } else {
                name.add(tempName);
            }
        }
        //TODO: CREATE SQL TABLE HERE
    }

    private boolean verifyData() {
        boolean result = true;
        //Error handling
        if (chips.length == 0) {
/*                    rtil.setErrorEnabled(true);
            rtil.setError(getResources().getString(R.string.error_recipient));*/
            YoYo.with(Techniques.Shake)
                    .duration(700)
                    .playOn(findViewById(R.id.phone_retv));
            phoneRetv.addTextChangedListener(phoneTextEditorWatcher);
            result = false;
        }
        if (messageContentString.length() == 0) {
/*                    mtil.setErrorEnabled(true);
            mtil.setError(getResources().getString(R.string.error_message_content));*/
            YoYo.with(Techniques.Shake)
                    .duration(700)
                    .playOn(findViewById(R.id.messageContent));
            result = false;
        }
        return result;
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
    public void setCurrentDate(){
        Calendar calendar = Calendar.getInstance();
        year = curYear = calendar.get(Calendar.YEAR);
        month = curMonth = calendar.get(Calendar.MONTH);
        day = curDay = calendar.get(Calendar.DAY_OF_MONTH);
    }

    public void setCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        hourOfDay = curHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        minute = curMinute = calendar.get(Calendar.MINUTE);
    }

    // Item selected from spinner
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // Couldn't get cases to work
        if(parent.getItemAtPosition(pos).equals(dateEntries.get(0))) {
            year = curYear;
            month = curMonth;
            day = curDay;
        }
        else if(parent.getItemAtPosition(pos).equals(dateEntries.get(1))) {
            year = curYear;
            month = curMonth;
            day = curDay + 1;
        }
        else if(parent.getItemAtPosition(pos).equals(dateEntries.get(2))) {
            year = curYear;
            month = curMonth;
            day = curDay + 7;
        }
        else if(parent.getItemAtPosition(pos).equals(dateEntries.get(3))) {
            showDatePickerDialog();
        }
        else if(parent.getItemAtPosition(pos).equals(timeEntries.get(0))) {
            hourOfDay = 9;
            minute = 0;
        }
        else if(parent.getItemAtPosition(pos).equals(timeEntries.get(1))) {
            hourOfDay = 13;
            minute = 0;
        }
        else if(parent.getItemAtPosition(pos).equals(timeEntries.get(2))) {
            hourOfDay = 17;
            minute = 0;
        }
        else if(parent.getItemAtPosition(pos).equals(timeEntries.get(3))) {
            hourOfDay = 20;
            minute = 0;
        }
        else if(parent.getItemAtPosition(pos).equals(timeEntries.get(4))) {
            showTimePickerDialog();
        }
    }

    // If nothing selected on spinner
    public void onNothingSelected(AdapterView<?> parent) {
        setCurrentDate();
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

    public void setUpSpinners() {
        Calendar mCalendar = Calendar.getInstance();

        if (!DateFormat.is24HourFormat(this)) {
            morning = "9:00 AM";
            afternoon = "1:00 PM";
            evening = "5:00 PM";
            night = "8:00 PM";
        } else {
            morning = "09:00";
            afternoon = "13:00";
            evening = "17:00";
            night = "20:00";
        }
        dateEntries = new ArrayList<CharSequence>(Arrays.<CharSequence>asList(
                getString(R.string.tomorrow),
                getString(R.string.today),
                getString(R.string.next) + " " + getDayOfWeekString(),
                getString(R.string.pick_date)));
        timeEntries = new ArrayList<CharSequence>(Arrays.<CharSequence>asList(
                getString(R.string.morning) + morning,
                getString(R.string.afternoon) + afternoon,
                getString(R.string.evening) + evening,
                getString(R.string.night) + night,
                getString(R.string.pick_time)));
        ArrayAdapter<CharSequence> dateAdapter =
                new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, dateEntries);
        ArrayAdapter<CharSequence> timeAdapter =
                new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, timeEntries);
        Spinner dateSpinner = (Spinner) findViewById(R.id.date_spinner);
        Spinner timeSpinner = (Spinner) findViewById(R.id.time_spinner);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpinner.setAdapter(dateAdapter);
        timeSpinner.setAdapter(timeAdapter);
        dateSpinner.setOnItemSelectedListener(this);
        timeSpinner.setOnItemSelectedListener(this);
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
}
