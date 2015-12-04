package com.kyleszombathy.sms_scheduler;

import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.telephony.SmsManager;


import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import fr.ganfra.materialspinner.MaterialSpinner;

public class AddMessage extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener,
        DatePickerFragment.OnCompleteListener, TimePickerFragment.OnCompleteListener
{
    //=============Variables & Declarations================//
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
    private ArrayList<Integer> ID = new ArrayList<>();
    private int arrayListCount = 0;
    private int DETAILS_QUERY_ID = 0;

    private int year, month, day, hour, minute;
    private String fullTime;
    private String fullDate;
    private ArrayList<String> name = new ArrayList<>();
    private ArrayList<String> phone = new ArrayList<>();
    private ArrayList<String> fullChipString = new ArrayList<>();

    // Spinners
    private ArrayList<CharSequence> dateEntries;
    private ArrayList<CharSequence> timeEntries;
    private Spinner dateSpinner, timeSpinner;
    private String morningTime, afternoonTime, eveningTime, nightTime;
    private CharSequence today, tomorrow, nextWeek, pickDate,
            morning, afternoon, evening, night, pickTime;
    int morningInt = 9; int afternoonInt = 13;
    int eveningInt = 17; int nightInt = 20;

    // Contact Picker
    private RecipientEditTextView phoneRetv;
    private DrawableRecipientChip[] chips;
    private boolean phoneRetvSelected = false;

    // For getting character count
    private int smsLength = 160;
    private TextView counterTextView;
    private EditText messageContentEditText;

    // For getting current time
    private Calendar calendar = Calendar.getInstance();

    // Other
    private TextView phoneRetvErrorMessage;
    private TextView messageContentErrorMessage;
    private String messageContentString = "";

    //======================Listeners=======================//
    // Watches message content, makes a counter, and handles errors
    private final TextWatcher messageContentEditTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //This sets a textview to the current length
            int length = s.length();
            if (length == 1) {
                messageContentEditText.getBackground().setColorFilter(getResources().
                        getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
                messageContentErrorMessage.setText("");
            }
            if (length <= smsLength) {
                counterTextView.setText(String.valueOf(smsLength - length));
            } else {
                counterTextView.setText(String.valueOf(smsLength - length % smsLength)
                        + " / " + String.valueOf(1 + (length / smsLength)));
            }
        }
        public void afterTextChanged(Editable s) {}
    };

    // Removes error text from phoneRetv
    private final TextWatcher phoneRetvEditTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Because chips get innacurate count unless it's currently selected
            if (phoneRetvSelected) {
                chips = phoneRetv.getSortedRecipients();
            }
            phoneRetv.getBackground().setColorFilter(getResources().
                    getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
            phoneRetvErrorMessage.setText("");
        }
        public void afterTextChanged(Editable s) {
        }
    };

    // Listens for selection of phoneRetv
    private View.OnFocusChangeListener phoneRetvFocusListner = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus){
                phoneRetvSelected = true;
            } else {
                phoneRetvSelected = false;
            }
        }
    };


    //=============Activity Creation Methods================//
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
    // When fragment is made
    protected void onResume() {
        super.onResume();
        phoneRetvErrorMessage = (TextView) findViewById(R.id.phone_retv_error);
        messageContentErrorMessage = (TextView) findViewById(R.id.messageContentError);

        // Get character count
        counterTextView = (TextView) findViewById(R.id.count);
        messageContentEditText = (EditText) findViewById(R.id.messageContent);
        messageContentEditText.addTextChangedListener(messageContentEditTextWatcher);

        setUpAutocomplete();
        setCurrentDate();
        setCurrentTime();
        setUpSpinners();
    }

    @Override // Inserts menu send button
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_message, menu);
        return true;
    }

    //=============Dialog Fragments===============//

    public void showTimePickerDialog() {
        TimePickerFragment timePicker = new TimePickerFragment();
        timePicker.show(getSupportFragmentManager(), "timePicker");
    }

    public void showDatePickerDialog() {
        DatePickerFragment datePicker = new DatePickerFragment();
        datePicker.show(getSupportFragmentManager(), "datePicker");
    }

    // Retrieves data from DatePickerFragment on completion
    public void onComplete(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    @Override
    // Retrieves data from TimePickerFragment on completion
    public void onComplete(int hourOfDay, int minute) {
        this.hour = hourOfDay;
        this.minute = minute;
    }

    //===========Setter Uppers & Spinner methods==========//
    // Setup phoneRetv autocomplete contacts
    private void setUpAutocomplete() {
        phoneRetv = (RecipientEditTextView) findViewById(R.id.phone_retv);
        phoneRetv.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        phoneRetv.setAdapter(new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, this));
        phoneRetv.setOnFocusChangeListener(phoneRetvFocusListner);
        phoneRetv.addTextChangedListener(phoneRetvEditTextWatcher);
    }

    public void setUpSpinners() {
        setUpSpinners(null);
    }

    public void setUpSpinners(String date) {
        if (!DateFormat.is24HourFormat(this)) {
            morningTime = Integer.toString(morningInt) + ":00 AM";
            afternoonTime = Integer.toString(afternoonInt - 12) + ":00 PM";
            eveningTime = Integer.toString(eveningInt - 12) + ":00 PM";
            nightTime = Integer.toString(nightInt - 12) + ":00 PM";
        } else {
            morningTime = "0" + Integer.toString(morningInt) + ":00";
            afternoonTime = Integer.toString(afternoonInt) + ":00";
            eveningTime = Integer.toString(eveningInt) + ":00";
            nightTime = Integer.toString(nightInt) + ":00";
        }

        today = getString(R.string.today);
        tomorrow = getString(R.string.tomorrow);
        nextWeek = getString(R.string.next) + " " + getDayOfWeekString();
        pickDate = getString(R.string.pick_date);
        morning = getString(R.string.morning) + " " + morningTime;
        afternoon = getString(R.string.afternoon) + " " + afternoonTime;
        evening = getString(R.string.evening) + " " + eveningTime;
        night = getString(R.string.night) + " " + nightTime;
        pickTime = getString(R.string.pick_time);

        dateEntries = new ArrayList<CharSequence>(Arrays.<CharSequence>asList (
                today, tomorrow, nextWeek, pickDate));
        timeEntries = new ArrayList<CharSequence>(Arrays.<CharSequence>asList(
                morning, afternoon, evening, night, pickTime));
        if(date != null) {
            dateEntries.add(0, date);
        }
        ArrayAdapter<CharSequence> dateAdapter =
                new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, dateEntries);
        ArrayAdapter<CharSequence> timeAdapter =
                new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, timeEntries);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpinner = (MaterialSpinner) findViewById(R.id.date_spinner);
        timeSpinner = (MaterialSpinner) findViewById(R.id.time_spinner);
        dateSpinner.setAdapter(dateAdapter);
        timeSpinner.setAdapter(timeAdapter);
        dateSpinner.setOnItemSelectedListener(this);
        timeSpinner.setOnItemSelectedListener(this);
        dateSpinner.setSelection(getIndex(dateSpinner, tomorrow.toString()));
    }

    // Gets index of string in spinner
    private int getIndex(Spinner spinner, String myString){
        int index = 0;
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).equals(myString)){
                index = i;
            }
        }
        return index;
    }

    // Listens for item selected in spinners
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        hideKeyboard();
        // Couldn't get cases to work so just used else ifs, sorry lol
        if(parent.getItemAtPosition(pos).equals(tomorrow)) {
            setXDaysInFuture(1);
        } else if(parent.getItemAtPosition(pos).equals(today)) {
            setCurrentDate();
            updateDateSpinner();
            updateTimeSpinner();
        } else if(parent.getItemAtPosition(pos).equals(nextWeek)) {
            setXDaysInFuture(7);
        } else if(parent.getItemAtPosition(pos).equals(pickDate)) {
            showDatePickerDialog();
        } else if(parent.getItemAtPosition(pos).equals(morning)) {
            setHour(morningInt);
        } else if(parent.getItemAtPosition(pos).equals(afternoon)) {
            setHour(afternoonInt);
        } else if(parent.getItemAtPosition(pos).equals(evening)) {
            setHour(eveningInt);
        } else if(parent.getItemAtPosition(pos).equals(night)) {
            setHour(nightInt);
        } else if(parent.getItemAtPosition(pos).equals(pickTime)) {
            showTimePickerDialog();
        }
    }

    private void updateDateSpinner() {
        getCurrentDateString();
    }



    private void updateTimeSpinner() {

    }

    // If nothing selected on spinner
    public void onNothingSelected(AdapterView<?> parent) {
    }

    //=============Finishing and adding to SQL================//

    @Override
    // When user hits finish button
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send:
                messageContentString = messageContentEditText.getText().toString();

                if (verifyData()) {
                    addDataToSQL();
                    scheduleMessage();
                    hideKeyboard();
                    createSnackBar(getString(R.string.success));
                    Intent returnIntent = new Intent();
                    setResult(RESULT_OK, returnIntent);
                    finish();
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Verifys that user data is correct and makes error messages
    private boolean verifyData() {
        boolean result = true;
        // PhoneRetv error handling
        if (chips == null) {
            result = errorChipsEmpty();
        } else if (chips.length == 0) {
            result = errorChipsEmpty();
        } else if (chips.length > 0) {
            for (DrawableRecipientChip chip : chips) {
                String str = chip.toString();
                String nameTemp = getNameFromString(str);
                String phoneTemp = getPhoneNumberFromString(str);

                if (phoneTemp.length() != 0) {
                    // Adds values to global
                    phone.add(phoneTemp);
                    name.add(nameTemp);
                    fullChipString.add(str);
                } else {
                    // Invalid contact without number
                    phoneRetvErrorMessage.setText(getResources().getString(R.string.invalid_entry));
                    phoneRetv.getBackground().setColorFilter(getResources().
                            getColor(R.color.error_color), PorterDuff.Mode.SRC_ATOP);
                    YoYo.with(Techniques.Shake)
                            .duration(700)
                            .playOn(findViewById(R.id.phone_retv));
                    phoneRetv.addTextChangedListener(phoneRetvEditTextWatcher);
                    result = false;
                }
            }
        }
        // Message Content error handling
        if (messageContentString.length() == 0) {
            messageContentErrorMessage.setText(getResources().
                    getString(R.string.error_message_content));
            messageContentEditText.getBackground().setColorFilter(getResources().
                    getColor(R.color.error_color), PorterDuff.Mode.SRC_ATOP);
            YoYo.with(Techniques.Shake)
                    .duration(700)
                    .playOn(findViewById(R.id.messageContent));
            result = false;
        }

        return result;
    }

    private boolean errorChipsEmpty() {
        phoneRetvErrorMessage.setText(getResources().getString(R.string.error_recipient));
        phoneRetv.getBackground().setColorFilter(getResources().
                getColor(R.color.error_color), PorterDuff.Mode.SRC_ATOP);
        YoYo.with(Techniques.Shake)
                .duration(700)
                .playOn(findViewById(R.id.phone_retv));
        return false;
    }
    private void scheduleMessage() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone.toString(), null, messageContentString, null, null);

    }
    private void addDataToSQL() {
        // SQLite database accessor
        MessageDbHelper mDbHelper = new MessageDbHelper(AddMessage.this);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        System.out.println(name.toString());
        System.out.println(phone.toString());
        System.out.println(year);
        System.out.println(month);
        System.out.println(day);
        System.out.println(hour);
        System.out.println(minute);

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(MessageContract.MessageEntry.
                COLUMN_NAME_NAME, name.toString());
        values.put(MessageContract.MessageEntry.
                COLUMN_NAME_PHONE, phone.toString());
        values.put(MessageContract.MessageEntry.
                COLUMN_NAME_NAME_PHONE_FULL, fullChipString.toString());
        values.put(MessageContract.MessageEntry.
                COLUMN_NAME_MESSAGE, messageContentString);
        values.put(MessageContract.MessageEntry.
                COLUMN_NAME_YEAR, year);
        values.put(MessageContract.MessageEntry.
                COLUMN_NAME_MONTH, month);
        values.put(MessageContract.MessageEntry.
                COLUMN_NAME_DAY, day);
        values.put(MessageContract.MessageEntry.
                COLUMN_NAME_HOUR, hour);
        values.put(MessageContract.MessageEntry.
                COLUMN_NAME_MINUTE, minute);

        // Insert the new row, returning the primary key value of the new row
        long newRowId;
        newRowId = db.insert(
                MessageContract.MessageEntry.TABLE_NAME,
                MessageContract.MessageEntry.COLUMN_NAME_NULLABLE,
                values);
        System.out.println(newRowId);
    }
    //================Time&Date Methods================//
    private void getCurrentDateString() {

    }
    // Gets and sets current date
    public void setCurrentDate(){
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
    }
    // Gets and sets current time
    public void setCurrentTime() {
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);
    }

    public void setXDaysInFuture(int days) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.add(Calendar.DATE, days);
        year = gc.get(Calendar.YEAR);
        month = gc.get(Calendar.MONTH);
        day = gc.get(Calendar.DAY_OF_MONTH);
    }

    public void setHour(int hour) {
        this.hour = hour;
        minute = 0;
    }

    // Returns current day of the week in string
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

    //================Utility Methods==================//
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void createSnackBar(String str) {
        Snackbar snackbar = Snackbar
                .make(findViewById(android.R.id.content), str, Snackbar.LENGTH_LONG);
        snackbar.show();
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
        System.out.println(str);
        String temp = "";
        for (int i =0; i < str.length(); i++) {
            char c = str.charAt(i);
            char d = str.charAt(i + 1);
            temp += c;
            if (d == '<') {
                break;
            }
        } return temp;
    }

    public void setFullTime() {

    }

    //============Data Loaders for Contacts, Disabled================//
    // Disabled. for old contact picking
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

    // Disabled. For loaders of contacts
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, uri, PROJECTION, null, null, null);
    }

    @Override
    // Disabled. For loaders of contacts
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
        phone.add(Long.parseLong(temp));
        System.out.println("ID: " + Arrays.toString(ID.toArray()));
        System.out.println("Name: " + Arrays.toString(name.toArray()));
        System.out.println("Phone: " + Arrays.toString(phone.toArray()));

        // Add name to recipients list
        if (name.size() == 1) {
            //t.setText(t.getText() + " " + name.get(arrayListCount));
        } else {
            //t.setText(t.getText() + ", " + name.get(arrayListCount));
        }

        DETAILS_QUERY_ID++;
        arrayListCount++;*/
    }

    // Disabled. For loaders of contacts
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
    }
}
