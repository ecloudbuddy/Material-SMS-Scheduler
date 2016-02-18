package com.kyleszombathy.sms_scheduler;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Fade;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.simplicityapks.reminderdatepicker.lib.ReminderDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddMessage extends AppCompatActivity
        implements
        DatePickerFragment.OnCompleteListener, TimePickerFragment.OnCompleteListener
{
    // Debug Tag
    private static final String TAG = "AddMessage";

    // User input info
    private int year, month, day, hour, minute;
    private ArrayList<String> name = new ArrayList<>();
    private ArrayList<String> phone = new ArrayList<>();
    private ArrayList<String> fullChipString = new ArrayList<>();

    // ReminderDatePicker Libarary
    private ReminderDatePicker datePicker;

    // Contact Picker
    private RecipientEditTextView phoneRetv;
    private DrawableRecipientChip[] chips;
    private ArrayList<String> photoUri = new ArrayList<>();

    // For getting character count
    private int smsMaxLength = 140;
    private int smsMaxLengthBeforeShowingWarning = 120;
    private TextView counterTextView;
    private EditText messageContentEditText;

    // Alarm Manager
    private int alarmNumber;
    private int editMessageNewAlarmNumber;

    // For getting current time
    private Calendar calendar = Calendar.getInstance();

    // SQL info
    private long sqlRowId;

    // Permissions Request
    final private int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
    final private int MY_PERMISSIONS_REQUEST_MULTIPLE_PERMISSIONS = 1;
    private boolean allPermissionsGranted = false;

    // Other
    private TextView phoneRetvErrorMessage;
    private TextView messageContentErrorMessage;
    private String messageContentString = "";
    private boolean editMessage = false;
    private String phoneNumbTempString = "";


    //=============Activity Creation Methods================//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Sets window transition animations
        getWindow().setAllowEnterTransitionOverlap(true);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setEnterTransition(new Fade());

        // Create view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_message);

        // Get extras
        Intent i = getIntent();
        Bundle extras = i.getExtras();
        alarmNumber = extras.getInt("alarmNumber", -1);
        editMessageNewAlarmNumber = extras.getInt("NEW_ALARM", -1);
        editMessage = extras.getBoolean("EDIT_MESSAGE", false);

        // Setting up toolbar
        Toolbar myChildToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myChildToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        // Ask for contact permissions
        askForContactsReadPermission();

        // Create fragment view
        AddMessageFragment firstFragment = new AddMessageFragment();
        firstFragment.setArguments(getIntent().getExtras());
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, firstFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }



    /** Called when fragment view is created*/
    @Override
    protected void onResume() {
        super.onResume();

        // Get views from xml
        counterTextView = (TextView) findViewById(R.id.count);
        messageContentEditText = (EditText) findViewById(R.id.messageContent);
        messageContentErrorMessage = (TextView) findViewById(R.id.messageContentError);
        phoneRetv = (RecipientEditTextView) findViewById(R.id.phone_retv);
        datePicker = (ReminderDatePicker) findViewById(R.id.date_picker);

        // Text change listener to message content
        messageContentEditText.addTextChangedListener(messageContentEditTextWatcher);

        // Setup phoneRetv autocomplete contacts and listeners
        phoneRetv.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        phoneRetv.setAdapter(new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, this));
        phoneRetv.addTextChangedListener(phoneRetvEditTextWatcher);

        // Remove any ghost errors
        messageContentEditText.getBackground().setColorFilter(getResources().
                getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
        messageContentErrorMessage.setText("");
        phoneRetvErrorMessage = (TextView) findViewById(R.id.phone_retv_error);
        phoneRetv.getBackground().setColorFilter(getResources().
                getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
        phoneRetvErrorMessage.setText("");

        // Set up Date and Time pickers
        datePicker.setCustomDatePicker(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog();
            }
        });
        datePicker.setCustomTimePicker(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePickerDialog();
            }
        });


        /** If we are editing the message, pull values form sql database and insert them into the view*/
        if (editMessage) {
            try {
                getValuesFromSQL();
                phoneRetv.requestFocus();
                // Ran after view is created
                phoneRetv.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                // Submit chips to phoneRetv while pulling icons from database
                                for (int i = 0; i < name.size(); i++) {
                                    byte[] byteArray = Tools.getPhotoValuesFromSQL(
                                            AddMessage.this, photoUri.get(i).trim());
                                    phoneRetv.submitItem(name.get(i), phone.get(i),
                                            Uri.parse(photoUri.get(i).trim()), byteArray);
                                }

                                clearAll();
                                alarmNumber = editMessageNewAlarmNumber;
                                phoneRetv.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }
                });
                // Set new data to view
                messageContentEditText.setText(messageContentString);
                datePicker.setSelectedDate(new GregorianCalendar(year, month, day));
                datePicker.setSelectedTime(hour, minute);
            } catch (Exception e) {
                // To catch sql error on return to app because onResume is called again.
                // It's okay if this happens
            }
        }
    }

    @Override /** Create Toolbar buttons*/
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_message, menu);
        return true;
    }

    //===============Edit Message SQL Retrieval===============//
    /** Pulls values from sql db on editMessage*/
    private void getValuesFromSQL() {
        MessageDbHelper mDbHelper = new MessageDbHelper(AddMessage.this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Which row to update, based on the ID
        String selection = MessageContract.MessageEntry.ALARM_NUMBER + " LIKE ?";
        String[] selectionArgs = { String.valueOf(alarmNumber) };
        String[] projection = {
                MessageContract.MessageEntry.NAME,
                MessageContract.MessageEntry.MESSAGE,
                MessageContract.MessageEntry.PHONE,
                MessageContract.MessageEntry.YEAR,
                MessageContract.MessageEntry.MONTH,
                MessageContract.MessageEntry.DAY,
                MessageContract.MessageEntry.HOUR,
                MessageContract.MessageEntry.MINUTE,
                MessageContract.MessageEntry.PHOTO_URI,
                MessageContract.MessageEntry.ALARM_NUMBER
        };

        Cursor cursor = db.query(
                MessageContract.MessageEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        // Moves to first row
        cursor.moveToFirst();
        name = Tools.parseString(cursor.getString(cursor.getColumnIndexOrThrow
                (MessageContract.MessageEntry.NAME)));
        phoneNumbTempString = cursor.getString(cursor.getColumnIndexOrThrow
                (MessageContract.MessageEntry.PHONE));
        phone = Tools.parseString(phoneNumbTempString);
        year = cursor.getInt(cursor.getColumnIndexOrThrow
                (MessageContract.MessageEntry.YEAR));
        month = cursor.getInt(cursor.getColumnIndexOrThrow
                (MessageContract.MessageEntry.MONTH));
        day = cursor.getInt(cursor.getColumnIndexOrThrow
                (MessageContract.MessageEntry.DAY));
        hour = cursor.getInt(cursor.getColumnIndexOrThrow
                (MessageContract.MessageEntry.HOUR));
        minute = cursor.getInt(cursor.getColumnIndexOrThrow
                (MessageContract.MessageEntry.MINUTE));
        photoUri = Tools.parseString(cursor.getString(cursor.getColumnIndexOrThrow
                (MessageContract.MessageEntry.PHOTO_URI)));
        messageContentString = cursor.getString(cursor.getColumnIndexOrThrow
                (MessageContract.MessageEntry.MESSAGE));

        // Close everything so android doesn't complain
        cursor.close();
        mDbHelper.close();
    }

    //=============Dialog Fragments===============//
    /**Shows a time picker dialog fragment*/
    private void showTimePickerDialog() {
        TimePickerFragment timePicker = new TimePickerFragment();
        timePicker.show(getSupportFragmentManager(), "timePicker");
    }
    /**Shows a date picker dialog fragment*/
    private void showDatePickerDialog() {
        DatePickerFragment datePicker = new DatePickerFragment();
        datePicker.show(getSupportFragmentManager(), "datePicker");
    }

    /** Retrieves data from DatePickerFragment on completion*/
    public void onComplete(int year, int month, int day) {
        GregorianCalendar calendar = new GregorianCalendar(year, month, day);
        datePicker.setSelectedDate(calendar);
    }

    @Override
    /** Retrieves data from TimePickerFragment on completion*/
    public void onComplete(int hourOfDay, int minute) {
        datePicker.setSelectedTime(hourOfDay, minute);
    }

    //=============Finishing and adding to SQL================//

    @Override
    /** Called when user hits finish button*/
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send:
                messageContentString = messageContentEditText.getText().toString();
                // Hack to get phoeRetv to display the correct contants updated
                messageContentEditText.requestFocus();
                phoneRetv.requestFocus();
                chips = phoneRetv.getSortedRecipients();
                // Get time from datePicker
                Calendar cal = datePicker.getSelectedDate();
                year = cal.get(Calendar.YEAR);
                month = cal.get(Calendar.MONTH);
                day = cal.get(Calendar.DAY_OF_MONTH);
                hour = cal.get(Calendar.HOUR_OF_DAY);
                minute = cal.get(Calendar.MINUTE);

                if (verifyData()) {
                    if (askForSmsSendPermission()) {
                        finishAndReturn();
                    } else {
                        return false;
                    }

                } else {
                    return false;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**Adds to sql, creates alarms, returns to Home*/
    private void finishAndReturn() {
        // Add to sql database and schedule the alarm
        addDataToSQL();
        addPhotoDataToSQL();
        scheduleMessage();
        hideKeyboard();
        createSnackBar(getString(R.string.success));

        // Create bundle of extras to pass back to Home
        Intent returnIntent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("NAME_EXTRA", name.toString());
        extras.putString("CONTENT_EXTRA", messageContentString);
        extras.putInt("ALARM_EXTRA", alarmNumber);
        extras.putInt("YEAR_EXTRA", year);
        extras.putInt("MONTH_EXTRA", month);
        extras.putInt("DAY_EXTRA", day);
        extras.putInt("HOUR_EXTRA", hour);
        extras.putInt("MINUTE_EXTRA", minute);
        returnIntent.putExtras(extras);

        // Return to HOME
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    /**Verifies that user data is correct and makes error messages*/
    private boolean verifyData() {
        clearAll();
        boolean result = true;
        // PhoneRetv error handling
        if (chips == null) {
            result = errorChipsEmpty();
        } else if (chips.length == 0) {
            result = errorChipsEmpty();
        } else if (chips.length > 0) {
            for (DrawableRecipientChip chip : chips) {
                String str = chip.toString();
                String phoneTemp = getPhoneNumberFromString(str);
                Log.i(TAG, "phoneTemp: " + phoneTemp);

                if (phoneTemp.length() == 0) {
                    result = errorPhoneWrong();
                } else {
                    // Checks if phone contains letters
                    for (char c: phoneTemp.toCharArray()) {
                        if (Character.isLetter(c) || c == '<' || c == '>' || c == ',') {
                            result = errorPhoneWrong();
                            break;
                        }
                    }
                }
            }
        }

        // Message Content error handling
        if (messageContentString.length() == 0) {
            messageContentErrorMessage.setText(getResources().
                    getString(R.string.error_message_content));
            messageContentEditText.getBackground().setColorFilter(getResources().
                    getColor(R.color.error_primary), PorterDuff.Mode.SRC_ATOP);
            YoYo.with(Techniques.Shake)
                    .duration(700)
                    .playOn(findViewById(R.id.messageContent));
            result = false;
        }

        // If okay from here, add results to global
        if (result) {
            for (DrawableRecipientChip chip : chips) {
                String str = chip.toString().trim();
                String nameTemp = getNameFromString(str);
                String phoneTemp = getPhoneNumberFromString(str);

                // Adds values to data sets
                phone.add(phoneTemp);
                name.add(nameTemp);
                fullChipString.add(str);

                Uri uri = chip.getEntry().getPhotoThumbnailUri();
                if (uri != null) {
                    photoUri.add(uri.toString().trim());
                } else {
                    photoUri.add(null);
                }
            }
        }

        return result;
    }

    /**Creates error message if phone number is wrong*/
    private boolean errorPhoneWrong() {
        // Invalid contact without number
        phoneRetvErrorMessage.setText(getResources().getString(R.string.invalid_entry));
        phoneRetv.getBackground().setColorFilter(getResources().
                getColor(R.color.error_primary), PorterDuff.Mode.SRC_ATOP);
        YoYo.with(Techniques.Shake)
                .duration(700)
                .playOn(findViewById(R.id.phone_retv));
        phoneRetv.addTextChangedListener(phoneRetvEditTextWatcher);
        return false;
    }

    /**Creates error message if phoneRetv is empty*/
    private boolean errorChipsEmpty() {
        // Sets error message
        phoneRetvErrorMessage.setText(getResources().getString(R.string.error_recipient));
        phoneRetv.getBackground().setColorFilter(getResources().
                getColor(R.color.error_primary), PorterDuff.Mode.SRC_ATOP);
        YoYo.with(Techniques.Shake)
                .duration(700)
                .playOn(findViewById(R.id.phone_retv));
        return false;
    }

    /**Utility method to schedule alarm*/
    private void scheduleMessage() {
        // Create calendar with class values
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);

        // Starts alarm
        MessageAlarmReceiver receiver = new MessageAlarmReceiver();
        receiver.setAlarm(this, cal, phone, messageContentString, alarmNumber, name);
    }

    /** Adds data to sql db*/
    private void addDataToSQL() {
        // SQLite database accessor
        MessageDbHelper mDbHelper = new MessageDbHelper(AddMessage.this);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        System.out.println(name.toString());
        System.out.println(phone.toString());
        System.out.println(photoUri.toString());
        System.out.println(alarmNumber);

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(MessageContract.MessageEntry.NAME, name.toString());
        values.put(MessageContract.MessageEntry.PHONE, phone.toString());
        values.put(MessageContract.MessageEntry.NAME_PHONE_FULL, fullChipString.toString());
        values.put(MessageContract.MessageEntry.MESSAGE, messageContentString);
        values.put(MessageContract.MessageEntry.YEAR, year);
        values.put(MessageContract.MessageEntry.MONTH, month);
        values.put(MessageContract.MessageEntry.DAY, day);
        values.put(MessageContract.MessageEntry.HOUR, hour);
        values.put(MessageContract.MessageEntry.MINUTE, minute);
        values.put(MessageContract.MessageEntry.ALARM_NUMBER, alarmNumber);
        values.put(MessageContract.MessageEntry.PHOTO_URI, photoUri.toString());
        values.put(MessageContract.MessageEntry.ARCHIVED, 0);
        values.put(MessageContract.MessageEntry.DATETIME, getFullDateString());

        // Insert the new row, returning the primary key value of the new row
        sqlRowId = db.insert(
                MessageContract.MessageEntry.TABLE_NAME,
                MessageContract.MessageEntry.NULLABLE,
                values);

        mDbHelper.close();
        db.close();
    }

    /** Adds contact icon photobytes (bitmap bytes) to separate db.
     * Checks if specific contact exists before adding*/
    private void addPhotoDataToSQL() {
        MessageDbHelper mDbHelper = new MessageDbHelper(AddMessage.this);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        for (int i = 0; i < phoneRetv.getRecipients().length; i++) {
            // Check if data already exists
            if (photoUri.get(i) != null) {
                if (Tools.getPhotoValuesFromSQL(AddMessage.this, photoUri.get(i)) == null) {
                    byte[] photoBytes = chips[i].getEntry().getPhotoBytes();

                    // Create a new map of values, where column names are the keys
                    ContentValues values = new ContentValues();
                    values.put(MessageContract.MessageEntry.
                            PHOTO_URI_1, photoUri.get(i));
                    values.put(MessageContract.MessageEntry.
                            PHOTO_BYTES, photoBytes);

                    // Insert the new row, returning the primary key value of the new row
                    sqlRowId = db.insert(
                            MessageContract.MessageEntry.TABLE_PHOTO,
                            MessageContract.MessageEntry.NULLABLE,
                            values);
                    Log.i(TAG, "New Row Created. Row ID: " + String.valueOf(sqlRowId) + " URI: " + photoUri.get(i));
                } else {
                    Log.i(TAG, "Row not created. Value already exists for " + name.get(i));
                }
            }
        }
        // Close everything
        mDbHelper.close();
        db.close();
    }

    //======================Listeners=======================//
    /**Watches message content, makes a counter, and handles errors*/
    private final TextWatcher messageContentEditTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //This sets a textview to the current length
            int length = s.length();
            // Begins counting length of message
            if (length == 1) {
                messageContentEditText.getBackground().setColorFilter(getResources().
                        getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
                messageContentErrorMessage.setText("");
            }
            // If length exceeds 1 message, shows user
            if (length <= smsMaxLength && length >= smsMaxLengthBeforeShowingWarning) {
                counterTextView.setText(String.valueOf(smsMaxLength - length));
            } else if (length > smsMaxLength){
                counterTextView.setText(String.valueOf(smsMaxLength - length % smsMaxLength)
                        + " / " + String.valueOf(1 + (length / smsMaxLength)));
            } else {
                counterTextView.setText("");
            }
        }
        public void afterTextChanged(Editable s) {}
    };

    /** Watches phoneRetv and removes error text*/
    private final TextWatcher phoneRetvEditTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //askForContactsReadPermission();
        }
        public void afterTextChanged(Editable s) {
            // Removes error message once user adds a contact
            phoneRetv.getBackground().setColorFilter(getResources().
                    getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
            phoneRetvErrorMessage.setText("");
        }
    };

    /**Checks if READ_CONTACTS permission exists and prompts user*/
    @TargetApi(Build.VERSION_CODES.M)
    private void askForContactsReadPermission() {
        int hasWriteContactsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);

        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                showMessageOKCancel(getString(R.string.permission_read_contacts_rationalle),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(new String[] {Manifest.permission.READ_CONTACTS},
                                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                            }
                        });
                return;
            }
            requestPermissions(new String[] {Manifest.permission.READ_CONTACTS},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }
    }

    /**Checks if other permissions exists and prompts user*/
    @TargetApi(Build.VERSION_CODES.M)
    private boolean askForSmsSendPermission() {
        List<String> permissionsNeeded = new ArrayList<>();
        final List<String> permissionsList = new ArrayList<>();

        if (!addPermission(permissionsList, Manifest.permission.SEND_SMS)) {
            permissionsNeeded.add("'Send and view SMS messages'");
        }
        if (!addPermission(permissionsList, Manifest.permission.READ_PHONE_STATE)) {
            permissionsNeeded.add("'make and manage phone calls'");
        }

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);
                message = message + " for vital app functions.";
                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(permissionsList.toArray(
                                                new String[permissionsList.size()]),
                                        MY_PERMISSIONS_REQUEST_MULTIPLE_PERMISSIONS);
                            }
                        });

            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    MY_PERMISSIONS_REQUEST_MULTIPLE_PERMISSIONS);
            return allPermissionsGranted;
        } else {
            return true;
        }
    }

    /**Utility method for askForSmsSendPermission*/
    @TargetApi(Build.VERSION_CODES.M)
    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }

    /** Shows a dialog box with OK/cancel boxes*/
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(AddMessage.this)
                .setMessage(message)
                .setPositiveButton(R.string.ok, okListener)
                .setNegativeButton(R.string.button_deny, null)
                .create()
                .show();
    }

    /**Retrieves result of askForSmsSendPermission*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_MULTIPLE_PERMISSIONS:
            {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.SEND_SMS, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_PHONE_STATE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    allPermissionsGranted = true;
                } else {
                    // Permission Denied
                    Toast.makeText(AddMessage.this, R.string.error_permission_some_permission_denied, Toast.LENGTH_SHORT).show();
                    allPermissionsGranted = false;
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                allPermissionsGranted = false;
        }
    }

    //================Time&Date Methods================//
    /**Creates a full date string in a format for sorting in Home*/
    private String getFullDateString() {
        GregorianCalendar date = new GregorianCalendar(year, month, day, hour, minute);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return format.format(date.getTime());
    }

    //================Utility Methods==================//
    /**Clears all Arraylist Values*/
    private void clearAll() {
        name.clear();
        phone.clear();
        photoUri.clear();
        fullChipString.clear();
    }

    /**Puts away keyboard*/
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**Makes a snackbar with given string*/
    private void createSnackBar(String str) {
        Snackbar snackbar = Snackbar
                .make(findViewById(android.R.id.content), str, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    /**Gets phone number from phoneRetv string*/
    public String getPhoneNumberFromString(String str) {
        // Extracts number within <> brackets
        String[] retval = str.split("<|>");
        return retval[1].trim();
    }

    /**Gets name from phoneRetv string*/
    public String getNameFromString(String str) {
        String temp = "";
        for (int i =0; i < str.length(); i++) {
            char c = str.charAt(i);
            char d = str.charAt(i + 1);
            temp += c;
            if (d == '<') {
                break;
            }
        } return temp.trim();
    }
}
