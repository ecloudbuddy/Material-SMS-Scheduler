package com.kyleszombathy.sms_scheduler;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
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

    // Edit Message Toggle
    private boolean editMessage = false;
    private int editMessageNewAlarmNumber;

    // User input info
    private int year, month, day, hour, minute;
    private ArrayList<String> name = new ArrayList<>();
    private ArrayList<String> phone = new ArrayList<>();
    private ArrayList<String> fullChipString = new ArrayList<>();
    private String messageContentString = "";
    private int alarmNumber;

    // ReminderDatePicker Library
    private ReminderDatePicker datePicker;

    // Contact Picker Field
    private RecipientEditTextView phoneRetv;
    private DrawableRecipientChip[] chips;
    private ArrayList<String> photoUri = new ArrayList<>();
    private TextView phoneRetvErrorMessage;

    // Message Content Field
    private EditText messageContentEditText;
    private TextView messageContentErrorMessage;
    private TextView counterTextView;
    private static final int SMS_MAX_LENGTH = 160;
    private static final int SMS_MAX_LENGTH_BEFORE_SHOWING_WARNING = 150;

    // Error Messages/ Validation
    private static final int ERROR_ANIMATION_DURATION = 700;

    // Permissions Request
    final private int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
    final private int MY_PERMISSIONS_REQUEST_MULTIPLE_PERMISSIONS = 1;
    private boolean allPermissionsGranted = false;

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
        Log.d(TAG, "Activity View Created");

        // Get "Edit" extras from Home
        Intent i = getIntent();
        Bundle extras = i.getExtras();
        alarmNumber = extras.getInt("alarmNumber", -1);
        editMessageNewAlarmNumber = extras.getInt("NEW_ALARM", -1);
        editMessage = extras.getBoolean("EDIT_MESSAGE", false);

        // Setting up toolbar
        Toolbar myChildToolbar = (Toolbar) findViewById(R.id.SMSScheduler_Toolbar);
        setSupportActionBar(myChildToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        // Create fragment view
        AddMessageFragment firstFragment = new AddMessageFragment();
        firstFragment.setArguments(getIntent().getExtras());
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.AddMessage_FragmentContainer, firstFragment);
        transaction.commit();

        Log.d(TAG, "Fragment view created");

        // Ask for contact permissions
        askForContactsReadPermission();
    }

    @Override /** Create Toolbar buttons*/
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_message, menu);
        return true;
    }

    /** Called when fragment view is created*/
    @Override
    protected void onResume() {
        super.onResume();

        // Get views from xml
        phoneRetv = (RecipientEditTextView) findViewById(R.id.AddMessage_PhoneRetv);
        phoneRetvErrorMessage = (TextView) findViewById(R.id.AddMessage_PhoneRetv_Error);
        messageContentEditText = (EditText) findViewById(R.id.AddMessage_Message_Content);
        messageContentErrorMessage = (TextView) findViewById(R.id.AddMessage_MessageContent_Error);
        counterTextView = (TextView) findViewById(R.id.AddMessage_MessageContent_Counter);
        datePicker = (ReminderDatePicker) findViewById(R.id.AddMessage_DatePicker);

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
                                    Uri uri = Uri.parse(photoUri.get(i).trim());
                                    phoneRetv.submitItem(name.get(i), phone.get(i), uri);
                                }

                                clearArrayLists();
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
                Log.e(TAG, "EditMessage: While adding data to activity an error was encountered", e);
            }
        }
    }

    //=============Date/Time Picker Fragments===============//
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

    //============="Done" button press actions================//
    @Override
    /** Called when user hits finish button*/
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.AddMessage_DoneButton:
                if (verifyData() && askForSmsSendPermission()) {
                    finishAndReturn();
                } else return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**Adds to sql, creates alarms, returns to Home*/
    private void finishAndReturn() {
        // Add to sql database and schedule the alarm
        SQLUtilities.addDataToSQL(AddMessage.this, name, phone, fullChipString,
                messageContentString, year, month, day, hour, minute, alarmNumber, photoUri);
        scheduleMessage();
        hideKeyboard();
        createSnackBar(getString(R.string.AddMessage_Notifications_CreateSuccess));

        // Create bundle of extras to pass back to Home
        Intent returnIntent = new Intent();
        Bundle extras = new Bundle();
        extras.putInt("ALARM_EXTRA", alarmNumber);
        returnIntent.putExtras(extras);

        // Return to HOME
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    /**Verifies that user data is correct and makes error messages*/
    private boolean verifyData() {
        boolean result = true;

        clearArrayLists();

        //Retrieve data from fields
        messageContentEditText.requestFocus();
        messageContentString = messageContentEditText.getText().toString();
        phoneRetv.requestFocus();
        chips = phoneRetv.getSortedRecipients();

        // Get time from datePicker
        Calendar cal = datePicker.getSelectedDate();
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        minute = cal.get(Calendar.MINUTE);

        // PhoneRetv error handling
        if (chips == null || chips.length == 0) {
            result = errorChipsEmpty();
        } else if (chips.length > 0) {
            for (DrawableRecipientChip chip : chips) {
                String chipStr = chip.toString().trim();
                String phoneNum = getPhoneNumberFromChip(chipStr);
                Log.d(TAG, "phoneNum from chip is '" + phoneNum + "'");

                if (phoneNum.length() == 0) {
                    result = errorPhoneWrong();
                } else {
                    // Checks if phone contains letters
                    for (char c: phoneNum.toCharArray()) {
                        if (Character.isLetter(c) || c == '<' || c == '>' || c == ',') {
                            result = errorPhoneWrong();
                            break;
                        }
                    }
                }

                //Result okay from here, add to final result
                if (result) {
                    fullChipString.add(chipStr);
                    name.add(getNameFromString(chipStr));
                    phone.add(getPhoneNumberFromChip(chipStr));
                    Uri uri = chip.getEntry().getPhotoThumbnailUri();
                    if (uri != null) {
                        photoUri.add(uri.toString().trim());
                    } else {
                        photoUri.add(null);
                    }
                }
            }
        }

        // Message Content error handling
        if (messageContentString.length() == 0) {
            result = errorMessageContentWrong();
        }

        return result;
    }

    /**Creates error message if phone number is wrong*/
    private boolean errorPhoneWrong() {
        // Invalid contact without number
        phoneRetvErrorMessage.setText(getResources().getString(R.string.AddMessage_PhoneRetv_InvalidEntries));
        phoneRetv.getBackground().setColorFilter(getResources().
                getColor(R.color.error_primary), PorterDuff.Mode.SRC_ATOP);
        YoYo.with(Techniques.Shake)
                .duration(ERROR_ANIMATION_DURATION)
                .playOn(findViewById(R.id.AddMessage_PhoneRetv));
        phoneRetv.addTextChangedListener(phoneRetvEditTextWatcher);
        return false;
    }

    /**Creates error message if phoneRetv is empty*/
    private boolean errorChipsEmpty() {
        // Sets error message
        phoneRetvErrorMessage.setText(getResources().getString(R.string.AddMessage_PhoneRetv_ErrorMustHaveRecipient));
        phoneRetv.getBackground().setColorFilter(getResources().
                getColor(R.color.error_primary), PorterDuff.Mode.SRC_ATOP);
        YoYo.with(Techniques.Shake)
                .duration(ERROR_ANIMATION_DURATION)
                .playOn(findViewById(R.id.AddMessage_PhoneRetv));
        return false;
    }

    /**Creates error message if messageContent is wrong*/
    private boolean errorMessageContentWrong() {
        messageContentErrorMessage.setText(getResources().
                getString(R.string.AddMessage_MessageContentError));
        messageContentEditText.getBackground().setColorFilter(getResources().
                getColor(R.color.error_primary), PorterDuff.Mode.SRC_ATOP);
        YoYo.with(Techniques.Shake)
                .duration(ERROR_ANIMATION_DURATION)
                .playOn(findViewById(R.id.AddMessage_Message_Content));
        return false;
    }

    /**Utility method to schedule alarm*/
    private void scheduleMessage() {
        // Create calendar with class values
        Calendar cal = Tools.getNewCalendarInstance(year, month, day, hour, minute);
        // Starts alarm
        new MessageAlarmReceiver().createAlarm(this, cal, phone, messageContentString, alarmNumber, name);
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
            if (length <= SMS_MAX_LENGTH && length >= SMS_MAX_LENGTH_BEFORE_SHOWING_WARNING) {
                counterTextView.setText(String.valueOf(SMS_MAX_LENGTH - length));
            } else if (length > SMS_MAX_LENGTH){
                counterTextView.setText(String.valueOf(SMS_MAX_LENGTH - length % SMS_MAX_LENGTH)
                        + " / " + String.valueOf(1 + (length / SMS_MAX_LENGTH)));
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
                showMessageOKCancel(getString(R.string.AddMessage_Permissions_ReadContactsRationalle),
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
            permissionsNeeded.add(getString(R.string.AddMessage_Permissions_SMSMessages));
        }
        if (!addPermission(permissionsList, Manifest.permission.READ_PHONE_STATE)) {
            permissionsNeeded.add(getString(R.string.AddMessage_Permissions_PhoneCalls));
        }

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = getString(R.string.AddMessage_Permissions_PermissionsPrompt1) + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);

                message = message + getString(R.string.AddMessage_Permissions_PermissionPromt2);
                showMessageOKCancel(message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), MY_PERMISSIONS_REQUEST_MULTIPLE_PERMISSIONS);
                    }
                });
            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), MY_PERMISSIONS_REQUEST_MULTIPLE_PERMISSIONS);

            Log.i(TAG, "askForSmsSendPermission: allPermissionsGranted value is " + allPermissionsGranted);
            return allPermissionsGranted;
        } else {
            Log.i(TAG, "askForSmsSendPermission: All permissions are granted");
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
                .setNegativeButton(R.string.AddMessage_ButtonDeny, null)
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
                    Log.i(TAG, "onRequestPermissionsResult: All Permissions Granted");
                    allPermissionsGranted = true;
                } else {
                    // Permission Denied
                    Log.i(TAG, "onRequestPermissionsResult: Permissions were denied");
                    Toast.makeText(AddMessage.this, R.string.AddMessage_Permissions_SomePermissionDenied, Toast.LENGTH_SHORT).show();
                    allPermissionsGranted = false;
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                allPermissionsGranted = false;
        }
    }

    //================Utility Methods==================//
    /**Clears all Arraylist Values*/
    private void clearArrayLists() {
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
    private void createSnackBar(String snackbarText) {
        Snackbar snackbar = Snackbar
                .make(findViewById(android.R.id.content), snackbarText, Snackbar.LENGTH_LONG);
        snackbar.show();
        Log.i(TAG, "createSnackBar: Snackbar Created with string " + snackbarText);
    }

    /**Gets phone number from phoneRetv string*/
    public String getPhoneNumberFromChip(String chipStr) {
        // Extracts number within <> brackets
        String[] retval = chipStr.split("<|>");
        return retval[1].trim();
    }

    /**Gets name from phoneRetv string*/
    public String getNameFromString(String chipStr) {
        String temp = "";
        for (int i =0; i < chipStr.length(); i++) {
            char c = chipStr.charAt(i);
            char d = chipStr.charAt(i + 1);
            temp += c;
            if (d == '<') {
                break;
            }
        } return temp.trim();
    }

    //===============Edit Message SQL Retrieval===============//
    /** Pulls values from sql db on editMessage*/
    private void getValuesFromSQL() {
        SQLDbHelper mDbHelper = new SQLDbHelper(AddMessage.this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Which row to update, based on the ID
        String selection = SQLContract.MessageEntry.ALARM_NUMBER + " LIKE ?";
        String[] selectionArgs = { String.valueOf(alarmNumber) };
        String[] projection = {
                SQLContract.MessageEntry.NAME,
                SQLContract.MessageEntry.MESSAGE,
                SQLContract.MessageEntry.PHONE,
                SQLContract.MessageEntry.YEAR,
                SQLContract.MessageEntry.MONTH,
                SQLContract.MessageEntry.DAY,
                SQLContract.MessageEntry.HOUR,
                SQLContract.MessageEntry.MINUTE,
                SQLContract.MessageEntry.PHOTO_URI,
                SQLContract.MessageEntry.ALARM_NUMBER
        };

        Cursor cursor = db.query(
                SQLContract.MessageEntry.TABLE_NAME,  // The table to query
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
                (SQLContract.MessageEntry.NAME)));
        String phoneNumbTempString = cursor.getString(cursor.getColumnIndexOrThrow
                (SQLContract.MessageEntry.PHONE));
        phone = Tools.parseString(phoneNumbTempString);
        year = cursor.getInt(cursor.getColumnIndexOrThrow
                (SQLContract.MessageEntry.YEAR));
        month = cursor.getInt(cursor.getColumnIndexOrThrow
                (SQLContract.MessageEntry.MONTH));
        day = cursor.getInt(cursor.getColumnIndexOrThrow
                (SQLContract.MessageEntry.DAY));
        hour = cursor.getInt(cursor.getColumnIndexOrThrow
                (SQLContract.MessageEntry.HOUR));
        minute = cursor.getInt(cursor.getColumnIndexOrThrow
                (SQLContract.MessageEntry.MINUTE));
        photoUri = Tools.parseString(cursor.getString(cursor.getColumnIndexOrThrow
                (SQLContract.MessageEntry.PHOTO_URI)));
        messageContentString = cursor.getString(cursor.getColumnIndexOrThrow
                (SQLContract.MessageEntry.MESSAGE));

        // Close everything so android doesn't complain
        cursor.close();
        mDbHelper.close();

        Log.d(TAG, "getValuesFromSQL: Values retrieved");
    }
}


