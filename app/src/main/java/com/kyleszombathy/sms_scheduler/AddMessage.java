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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static android.view.KeyEvent.KEYCODE_DEL;

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
    private static final String PHONERETV_FULL_REGEX = "^.*<[^\n\ra-zA-Z]+>$";
    private static final String PHONERETV_PHONE_REGEX_STRICT = "^[^,<>a-zA-Z]{4,}$";
    private static final String PHONERETV_PHONE_REGEX_LOOSE = "^[^a-zA-Z]+$";
    private static final String[] PHONERETV_CUSTOM_ENDKEYS = {",", " "};
    private static final String[] PHONERETV_CUSTOM_ENDKEYS_BAD = {", ", ",,", ",  "};

    // Permissions Request
    final private int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
    final private int MY_PERMISSIONS_REQUEST_MULTIPLE_PERMISSIONS = 1;
    private boolean allPermissionsGranted = false;

    // Random
    private static final String EMPTY_STRING = "";

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
        phoneRetv.setImeOptions(EditorInfo.IME_ACTION_DONE);
        phoneRetv.addTextChangedListener(phoneRetvEditTextWatcher);
        phoneRetv.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KEYCODE_DEL) {
                    Log.d(TAG, "PhoneRetv:OnKeyListener: Backspace pressed");
                    clearPhoneRetvError();
                }
                Log.d(TAG, "PhoneRetv:OnKeyListener: Key pressed - " + keyCode);
                return false;
            }
        });
        phoneRetv.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean result = false;
                Log.d(TAG, "PhoneRetv:onEditorAction: actionId key pressed - " + actionId);
                if (tryAddChipFromTextEntered(true)) {
                    result = true;
                } else if (!isPhoneRetvInError()){
                    // Go to next field
                    phoneRetv.clearFocus();
                    messageContentEditText.requestFocus();
                }
                return result;
            }
        });

        // Remove any ghost errors
        clearPhoneRetvError();
        clearMessageContentError();

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
                                    addNewChip(name.get(i), phone.get(i), uri);
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
    private boolean verifyData() throws ArrayIndexOutOfBoundsException {
        boolean result = true;
        Pattern regex = Pattern.compile(PHONERETV_FULL_REGEX);
        Pattern regexStrict = Pattern.compile(PHONERETV_PHONE_REGEX_STRICT);

        final String phoneRetvToString = phoneRetv.getText().toString();
        String lastCharPhoneRetvToString = getLastCharOfString(phoneRetvToString);
        String[] phoneRetvToStringArray = getValidFieldTextArray(phoneRetvToString, lastCharPhoneRetvToString);

        clearArrayLists();
        clearPhoneRetvError();
        clearMessageContentError();

        //Retrieve data from fields
        messageContentEditText.requestFocus();
        messageContentString = messageContentEditText.getText().toString();
        phoneRetv.requestFocus();
        updateChips();

        // Get time from datePicker
        Calendar cal = datePicker.getSelectedDate();
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        minute = cal.get(Calendar.MINUTE);

        if (chips.length != phoneRetvToStringArray.length) {
            // Arrays should be the same length. If they are not, there is something wrong in getValidFieldTextArray()
            Log.e(TAG + "verifyData:", "Error: chips length is not equal to phoneRetv length");
            throw new ArrayIndexOutOfBoundsException(); //TODO: In Prod, change this to an Error code so it does not impact user experience
        }

        // PhoneRetv error handling
        if (chips.length == 0) {
            errorPhoneWrong(getResources().getString(R.string.AddMessage_PhoneRetv_ErrorMustHaveRecipient));
            return false;
        } else if (chips.length > 0) {
            for (int i = 0; i < chips.length; i++) {
                DrawableRecipientChip chip = chips[i];

                String chipStr = chip.toString().trim();
                Log.d(TAG, "chipStr from chip is '" + chipStr + "'");
                Log.d(TAG, "phoneRetvToStringArray[i] is '" + phoneRetvToStringArray[i] + "'");

                if (!regex.matcher(chipStr).matches() || !regexStrict.matcher(phoneRetvToStringArray[i]).matches()) {
                    Log.e(TAG, "verifyData: Error - Invalid Entry for chipStr - '" + chipStr + "' or phoneRetvToStringArray[i] - '" + phoneRetvToStringArray[i] + "'");
                    errorPhoneWrong(getResources().getString(R.string.AddMessage_PhoneRetv_InvalidEntries));
                    return false;
                }

                if (throwErrorIfDuplicate(chips)) {
                    return false;
                }

                //Result okay from here, add to final result
                fullChipString.add(chipStr);
                name.add(getNameFromString(chipStr));
                phone.add(getPhoneNumbersFromChip(chipStr));
                Uri uri = chip.getEntry().getPhotoThumbnailUri();
                if (uri != null) {
                    photoUri.add(uri.toString().trim());
                } else {
                    photoUri.add(null);
                }
            }
        }

        // Message Content error handling
        if (messageContentString.length() == 0) {
            errorMessageContentWrong();
            return false;
        }

        return true;
    }

    private void updateChips() {
        chips = phoneRetv.getRecipients();
    }

    /**Creates error message if phone number is wrong*/
    private void errorPhoneWrong(String errorMessage) {
        // Invalid contact without number
        phoneRetvErrorMessage.setText(errorMessage);
        phoneRetv.getBackground().setColorFilter(
                ContextCompat.getColor(this, R.color.error_primary), PorterDuff.Mode.SRC_ATOP);
        YoYo.with(Techniques.Shake)
                .duration(ERROR_ANIMATION_DURATION)
                .playOn(findViewById(R.id.AddMessage_PhoneRetv));
        phoneRetv.requestFocus();
    }

    /**Creates error message if messageContent is wrong*/
    private void errorMessageContentWrong() {
        messageContentErrorMessage.setText(getResources().
                getString(R.string.AddMessage_MessageContentError));
        messageContentEditText.getBackground().setColorFilter(
                ContextCompat.getColor(this, R.color.error_primary), PorterDuff.Mode.SRC_ATOP);
        YoYo.with(Techniques.Shake)
                .duration(ERROR_ANIMATION_DURATION)
                .playOn(findViewById(R.id.AddMessage_Message_Content));
        messageContentEditText.requestFocus();
    }

    /**Utility method to schedule alarm*/
    private void scheduleMessage() {
        // Create calendar with class values
        Calendar cal = Tools.getNewCalendarInstance(year, month, day, hour, minute);
        // Starts alarm
        new MessageAlarmReceiver().createAlarm(this, cal, phone, messageContentString, alarmNumber, name);
    }

    // Detects duplicates in an array. O(n^2) but it's a small array, so doesn't matter.
    private boolean throwErrorIfDuplicate(DrawableRecipientChip[] chips) {
        final String nonNumericalRegex = "[^0-9]";
        final int arrayLength = chips.length;
        String phones[] = new String[arrayLength];

        for (int i = 0; i < arrayLength; i++) {
            // Replace all non-numericals with an empty string
            phones[i] = getPhoneNumbersFromChip(chips[i]).replaceAll(nonNumericalRegex,EMPTY_STRING);
            // If phone is has a preceding "1", remove it
            if (phones[i].startsWith("1")) phones[i] = phones[i].substring(1);
        }

        for (int i = 0; i < arrayLength; i++) {
            for (int j = i + 1; j < arrayLength; j++) {
                if (j != i && arePhonesDuplicate(phones[i], phones[j])) {
                    Log.e(TAG, "throwErrorIfDuplicate: " +
                            "Duplicates found for chipPhoneArray[j] - '" + phones[i] + "' " +
                            "chipPhoneArray[k] - '" + phones[j] + "' " + " <------DUPLICATES FOUND 1------");
                    errorPhoneWrong(getString(R.string.AddMessage_PhoneRetv_DuplicatePhone));
                    return true;
                }
                Log.d(TAG, "throwErrorIfDuplicate: " +
                        "Duplicates not found for chipPhoneArray[j] - '" + phones[i] + "' " +
                        "chipPhoneArray[k] - '" + phones[j] + "' " + " <------DUPLICATES NOT FOUND 1------");
            }
        }
        return false;
    }

    private boolean arePhonesDuplicate(String phone1, String phone2) {
        return !phone1.equals(EMPTY_STRING) && !phone2.equals(EMPTY_STRING) && phone2.equals(phone1);
    }

    private void clearPhoneRetvError() {
        phoneRetv.getBackground().setColorFilter(getResources().
                getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
        phoneRetvErrorMessage.setText(EMPTY_STRING);
    }
    private void clearMessageContentError() {
        messageContentEditText.getBackground().setColorFilter(getResources().
                getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
        messageContentErrorMessage.setText(EMPTY_STRING);
    }

    private boolean isPhoneRetvInError() {
        return phoneRetvErrorMessage.getText() != "";
    }

    //======================PhoneRetvListeners & Utility =======================//

    /** Watches phoneRetv and removes error text*/
    private final TextWatcher phoneRetvEditTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence charactersPressed, int start, int before, int count) {
            if (count == 1) {
                Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: Count is " + count + "charactersPressed: " + charactersPressed + "start: " + start + "before: " + before);
                tryAddChipFromTextEntered(false);
            } else {
                Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: Count is " + count);
            }
        }
        public void afterTextChanged(Editable s) {
        }
    };


    private boolean tryAddChipFromTextEntered(boolean enterKeyPressed) {
        boolean isChipAddSuccessful = false;
        final String chipLastPhone, fieldText;
        String fieldLastPhone, lastTypedChar;

        // PhoneRetv Field data
        fieldText = phoneRetv.getText().toString();
        lastTypedChar = getLastCharOfString(fieldText);
        fieldLastPhone = getFieldLastPhone(fieldText, lastTypedChar);

        updateChips();
        chipLastPhone = getLastPhoneFromChip(chips);
        clearPhoneRetvError(); // Clear error upon user typing

        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: BEGINNING TO SEARCH ==============================================================");
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: lastCharPhoneRetvToString pressed - '" + lastTypedChar + "'");
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: phoneRetvToString - '" + fieldText + "'");
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: phoneRetvToStringLastArrayIndex - '" + fieldLastPhone + "'");
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: Arrays.toString(chips) - '" + Arrays.toString(chips) + "'");
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: lastChipPhoneString - " + chipLastPhone);
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: regex_strict.matcher(isPhoneValid(phoneRetvToStringLastArrayIndex) - " + isPhoneValid(fieldLastPhone));

        if (fieldText.endsWith(", ")) {
            // Fixes duplicates after 1.Selecting contact 2.backspacing 3.clicking ","
            throwErrorIfDuplicate(chips);
        }

        if (!doesPhoneEndWithBadKeys(fieldText)
                && (enterKeyPressed || isLastCharValid(lastTypedChar))
                && !throwErrorIfDuplicate(chips)
                && isPhoneValid(fieldLastPhone)) {
            addNewChip(fieldLastPhone, fieldLastPhone);
            throwErrorIfDuplicate(chips);
            Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: Created chip with phoneRetvToStringLastArrayIndex - '" + fieldLastPhone + " <------CHIP CREATED------");
            isChipAddSuccessful = true;
        }
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: ENDING SEARCH ====================================================================");
        return isChipAddSuccessful;
    }

    private void addNewChip(String name, String phone) {
        addNewChip(name, phone, null);
    }

    private void addNewChip(String name, String phone, Uri uri) {
        if (uri == null) {
            phoneRetv.submitItem(name, phone);
        } else {
            phoneRetv.submitItem(name, phone, uri);
        }
        updateChips();
    }

    private boolean isPhoneValid(String phone) {
        final Pattern phone_regex_strict = Pattern.compile(PHONERETV_PHONE_REGEX_STRICT);
        return phone_regex_strict.matcher(phone).matches();
    }

    private String getFieldLastPhone(String fieldText, String lastTypedChar) {
        String[] fieldTextArray = getValidFieldTextArray(fieldText, lastTypedChar);
        return fieldTextArray[fieldTextArray.length - 1].trim();
    }

    /**Gets an array of phoneRetvToString, trimming empty array indexs on the end*/
    private String[] getValidFieldTextArray(String fieldText, String lastTypedChar){
        String[] fieldTextArray;
        ArrayList<String> fieldTextArrayList;

        // Replace < and > with blank chars
        fieldText = fieldText.replace("<", "");
        fieldText = fieldText.replace(">", "");

        // Split phoneRetvToString
        if (lastTypedChar.equals(PHONERETV_CUSTOM_ENDKEYS[0])) {
            fieldTextArray = fieldText.split(PHONERETV_CUSTOM_ENDKEYS[0]);
        } else {
            fieldTextArray = fieldText.split(PHONERETV_CUSTOM_ENDKEYS[0] + PHONERETV_CUSTOM_ENDKEYS[1]);
        }

        // Trim last array index if last index is an empty string
        if ((fieldTextArray[fieldTextArray.length - 1].trim().equals(EMPTY_STRING)
                || fieldTextArray[fieldTextArray.length - 1].trim().equals(",") )
                && fieldTextArray.length > 1) { // fieldTextArray.length is needed to avoid Exception
            fieldTextArrayList = new ArrayList<>(Arrays.asList(fieldTextArray));
            fieldTextArrayList.remove(fieldTextArray.length - 1);
            //noinspection ToArrayCallWithZeroLengthArrayArgument
            return fieldTextArrayList.toArray(new String[0]);
        }

        return fieldTextArray;

    }

    private String getFieldLastPhone(String[] phoneArray) {
        String lastPhone = phoneArray[phoneArray.length - 1].trim();
        if (lastPhone.equals(EMPTY_STRING) && phoneArray.length >= 2) {
            lastPhone = phoneArray[phoneArray.length - 2].trim();
        }
        return lastPhone;
    }

    private String getLastPhoneFromChip(DrawableRecipientChip[] chips) {
        if (chips.length > 0)
            return getPhoneNumbersFromChip( chips[chips.length - 1].toString().trim() );
        else return null;
    }

    private String getLastCharOfString(String str) {
        if (str.length()>0)
            return str.substring(str.length() - 1);
        else return EMPTY_STRING;
    }

    private boolean isLastCharValid(String lastChar) {
        for (String endKey : PHONERETV_CUSTOM_ENDKEYS) if (lastChar.equalsIgnoreCase(endKey)) return true;
        return false;
    }

    private boolean doesPhoneEndWithBadKeys(String phone) {
        for (String badKey : PHONERETV_CUSTOM_ENDKEYS_BAD) {
            if (phone.endsWith(badKey)) return true;
        }
        return false;
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
                clearMessageContentError();
            }
            // If length exceeds 1 message, shows user
            if (length <= SMS_MAX_LENGTH && length >= SMS_MAX_LENGTH_BEFORE_SHOWING_WARNING) {
                counterTextView.setText(String.valueOf(SMS_MAX_LENGTH - length));
            } else if (length > SMS_MAX_LENGTH){
                counterTextView.setText(String.valueOf(SMS_MAX_LENGTH - length % SMS_MAX_LENGTH)
                        + " / " + String.valueOf(1 + (length / SMS_MAX_LENGTH)));
            } else {
                counterTextView.setText(EMPTY_STRING);
            }
        }
        public void afterTextChanged(Editable s) {}
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


    /**Gets phone number from chip Overloaded*/
    private String getPhoneNumbersFromChip(DrawableRecipientChip chip) {
        return getPhoneNumbersFromChip(chip.toString());
    }
    /**Gets phone number from phoneRetv string*/
    private String getPhoneNumbersFromChip(String chipStr) {
        // Extracts number within <> brackets
        String[] retval = chipStr.split("<|>");
        return retval[1].trim();
    }

    /**Gets name from phoneRetv string*/
    private String getNameFromString(String chipStr) {
        String temp = EMPTY_STRING;
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


