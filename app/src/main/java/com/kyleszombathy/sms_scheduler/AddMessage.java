/**
 * Copyright 2016 Kyle Szombathy

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
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

    // User input info
    private Message message = new Message();

    // ReminderDatePicker Library
    private ReminderDatePicker datePicker;
    private boolean dateTimeIsValid;

    // Contact Picker Field
    private RecipientEditTextView phoneRetv;
    private DrawableRecipientChip[] chips;
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
        setWindowTransitionAnimation();
        createView(savedInstanceState);
        getExtrasFromHome();
        setUpToolbar();
        createFragmentView();
        askForContactsReadPermission();
    }

    /**Set the animation from Home*/
    private void setWindowTransitionAnimation() {
        getWindow().setAllowEnterTransitionOverlap(true);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setEnterTransition(new Fade());
    }

    /**Create the Activity view (the view is not entirely created until after onResume)*/
    private void createView(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_message);
        Log.d(TAG, "Activity View Created");
    }

    /**Retrieve extras*/
    private void getExtrasFromHome() {
        // Get "Edit" extras from Home
        Bundle extras = getIntent().getExtras();
        editMessage = extras.getBoolean("EDIT_MESSAGE", false);
        message.setAlarmNumber(extras.getInt("alarmNumber", -1));
    }

    /**Set the top toolbar*/
    private void setUpToolbar() {
        Toolbar myChildToolbar = (Toolbar) findViewById(R.id.SMSScheduler_Toolbar);
        setSupportActionBar(myChildToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);
    }

    /**Set up the fragment view (this is the view that everything is displayed in)*/
    private void createFragmentView() {
        AddMessageFragment firstFragment = new AddMessageFragment();
        firstFragment.setArguments(getIntent().getExtras());
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.AddMessage_FragmentContainer, firstFragment);
        transaction.commit();
        Log.d(TAG, "Fragment view created");
    }

    @Override /** Create Toolbar buttons*/
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_message, menu);
        return true;
    }

    //============= Initialize data ================//


    @Override
    protected void onStart() {
        super.onStart();
        initializeViewFromXML();
        setupPhoneRetvLibrary();
        setupDateTimePickers();
        addListeners();

        // Remove any leftover error messages
        clearErrorMessages();

        /** If we are editing the message, pull values form sql database and insert them into the view*/
        if (editMessage) {
            retrieveEditMessageDataFromDB();
            setFieldData();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart called");
    }



    //============= Setup ===============//
    private void initializeViewFromXML() {
        // Get views from xml
        phoneRetv = (RecipientEditTextView) findViewById(R.id.AddMessage_PhoneRetv);
        phoneRetvErrorMessage = (TextView) findViewById(R.id.AddMessage_PhoneRetv_Error);
        messageContentEditText = (EditText) findViewById(R.id.AddMessage_Message_Content);
        messageContentErrorMessage = (TextView) findViewById(R.id.AddMessage_MessageContent_Error);
        counterTextView = (TextView) findViewById(R.id.AddMessage_MessageContent_Counter);
        datePicker = (ReminderDatePicker) findViewById(R.id.AddMessage_DatePicker);
    }

    private void setupPhoneRetvLibrary() {
        phoneRetv.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        phoneRetv.setAdapter(new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, this));
        phoneRetv.setImeOptions(EditorInfo.IME_ACTION_DONE);
    }

    //============= Date/Time Pickers ===============//
    private void setupDateTimePickers() {
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
    }

    /**Shows a date picker dialog fragment*/
    private void showDatePickerDialog() {
        DatePickerFragment datePicker = new DatePickerFragment();
        datePicker.show(getSupportFragmentManager(), "datePicker");
    }

    /**Shows a time picker dialog fragment*/
    private void showTimePickerDialog() {
        TimePickerFragment timePicker = new TimePickerFragment();
        timePicker.show(getSupportFragmentManager(), "timePicker");
    }

    /** Retrieves data from DatePickerFragment on completion*/
    @Override
    public void onComplete(int year, int month, int day) {
        GregorianCalendar calendar = new GregorianCalendar(year, month, day);
        datePicker.setSelectedDate(calendar);
        fixDateIfBeforeCurrentTime();
    }

    /** Retrieves data from TimePickerFragment on completion*/
    @Override
    public void onComplete(int hourOfDay, int minute) {
        datePicker.setSelectedTime(hourOfDay, minute);
        fixDateIfBeforeCurrentTime();
    }

    /**Sets date/time to current time if selected time is before the current time*/
    private void fixDateIfBeforeCurrentTime() {
        Calendar selDateTime = datePicker.getSelectedDate();

        Calendar oneMinuteAgo = Calendar.getInstance();
        oneMinuteAgo.add(Calendar.MINUTE, -1);

        if (selDateTime.before(oneMinuteAgo)) {
            fixDateTime();
            Toast.makeText(this, R.string.FixDateTimeToast, Toast.LENGTH_LONG).show();
        }
    }

    /**Set date/time to current time*/
    private void fixDateTime() {
        datePicker.setSelectedDate(Calendar.getInstance());
    }

    //====================== Add Listeners =======================//
    private void addListeners() {
        addPhoneRetvListeners();
        addMessageContentListeners();
    }

    //====================== PhoneRetvListeners =======================//

    private void addPhoneRetvListeners() {
        addPhoneRetvTextChangedListener();
        addPhoneRetvBackspaceListener();
        addPhoneRetvEnterKeyListener();
    }

    /*Begin phoneRetvEditTextWatcher*/
    /**Add a listener to all the text changed in phoneRetv in order to detect errors upon user typing*/
    private void addPhoneRetvTextChangedListener() {
        phoneRetv.addTextChangedListener(phoneRetvEditTextWatcher);
    }

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

    /**Add onkeyListener to clear the error message once backspace is pressed*/
    private void addPhoneRetvBackspaceListener() {
        phoneRetv.setOnKeyListener(new View.OnKeyListener() {
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
    }

    /**Detect if enter key on keyboard is pressed*/
    private void addPhoneRetvEnterKeyListener() {
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
    }


    /**If user enters a phone number and clicks 'Enter', 'comma', or 'space', try to detect if
     * that phone number is valid or if the phone number is a duplicate. If it's valid, add it
     * as a manual chip entry.*/
    private boolean tryAddChipFromTextEntered(boolean enterKeyPressed) {
        boolean isChipAddSuccessful = false;
        final String chipLastPhone, fieldText;
        String fieldLastPhone, lastTypedChar;

        // Get PhoneRetv Field Data
        fieldText = phoneRetv.getText().toString(); // Field text, including chips in a toString format
        lastTypedChar = getLastCharOfString(fieldText); // Gets the last character of the "typed" text
        fieldLastPhone = getFieldLastPhone(fieldText, lastTypedChar);  // Get the supposed phone number the user entered

        // Get chip data
        updateChips();
        chipLastPhone = getLastPhoneFromChip(chips);

        // Display some logging info
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: BEGINNING TO SEARCH ==============================================================");
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: lastCharPhoneRetvToString pressed - '" + lastTypedChar + "'");
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: phoneRetvToString - '" + fieldText + "'");
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: phoneRetvToStringLastArrayIndex - '" + fieldLastPhone + "'");
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: Arrays.toString(chips) - '" + Arrays.toString(chips) + "'");
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: lastChipPhoneString - " + chipLastPhone);
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: regex_strict.matcher(isPhoneValid(phoneRetvToStringLastArrayIndex) - " + isPhoneValid(fieldLastPhone));

        // Clear error upon user typing
        clearPhoneRetvError();

        // Fix duplicates after 1.Selecting contact 2.backspacing 3.clicking ","
        if (fieldText.endsWith(", ")) {
            displayErrorIfDuplicate(chips);
        }

        // First, check if the conditions are correct to add a new phone number, then check if it's valid and add it
        if (!doesPhoneEndWithBadKeys(fieldText) // Reject any endkeys that cause bugs
                && (enterKeyPressed || isLastCharValid(lastTypedChar)) // Check if valid endkey is pressed
                && !displayErrorIfDuplicate(chips) // Check for duplicates
                && isPhoneValid(fieldLastPhone)) { // Check if the entered number is valid

            addNewChip(fieldLastPhone, fieldLastPhone);
            displayErrorIfDuplicate(chips);
            Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: Created chip with phoneRetvToStringLastArrayIndex - '" + fieldLastPhone + " <------CHIP CREATED------");
            isChipAddSuccessful = true;
        }
        Log.d(TAG, "phoneRetvEditTextWatcher:onTextChanged: ENDING SEARCH ====================================================================");
        return isChipAddSuccessful;
    }

    private String getLastCharOfString(String str) {
        if (str.length()>0)
            return str.substring(str.length() - 1);
        else return EMPTY_STRING;
    }

    /**Try to retrieve the phone number that the user entered*/
    private String getFieldLastPhone(String fieldText, String lastTypedChar) {
        String[] fieldTextArray = getValidFieldTextArray(fieldText, lastTypedChar);
        return fieldTextArray[fieldTextArray.length - 1].trim();
    }

    /**Gets an array filled with "valid" information. It cleans or deletes invalid index's*/
    private String[] getValidFieldTextArray(String fieldText, String lastTypedChar){
        String[] fieldTextArray;

        // Split phoneRetvToString by commas and spaces depending on what it ends with
        if (lastTypedChar.equals(PHONERETV_CUSTOM_ENDKEYS[0])) {
            fieldTextArray = fieldText.split(PHONERETV_CUSTOM_ENDKEYS[0]);
        } else {
            fieldTextArray = fieldText.split(PHONERETV_CUSTOM_ENDKEYS[0] + PHONERETV_CUSTOM_ENDKEYS[1]);
        }

        // Remove all non-numericals from array
        for (int i =0; i < fieldTextArray.length; i++) {
            fieldTextArray[i] = deleteAllNonNumbericals(fieldTextArray[i]);
        }

        // Trim last array index if last index is an empty string
        if ((fieldTextArray[fieldTextArray.length - 1].trim().equals(EMPTY_STRING)
                || fieldTextArray[fieldTextArray.length - 1].trim().equals(",") )
                && fieldTextArray.length > 1) { // fieldTextArray.length is needed to avoid Exception
            ArrayList<String> fieldTextArrayList = new ArrayList<>(Arrays.asList(fieldTextArray));
            fieldTextArrayList.remove(fieldTextArray.length - 1);
            //noinspection ToArrayCallWithZeroLengthArrayArgument
            return fieldTextArrayList.toArray(new String[0]);
        } else {
            return fieldTextArray;
        }
    }

    /**Replace all non numericals with an empty string*/
    private String deleteAllNonNumbericals(String fieldText) {
        final String nonNumericalRegex = "[^0-9]";
        return fieldText.replaceAll(nonNumericalRegex,EMPTY_STRING);
    }

    /**Detects duplicates in a chips array. O(n^2) but it's a small array, so doesn't matter.*/
    private boolean displayErrorIfDuplicate(DrawableRecipientChip[] chips) {
        final int arrayLength = chips.length;
        String phones[] = new String[arrayLength];

        for (int i = 0; i < arrayLength; i++) {
            // Replace all non-numericals with an empty string
            phones[i] = deleteAllNonNumbericals(getPhoneNumbersFromChip(chips[i]));
            // If phone is has a preceding "1", remove it
            if (phones[i].startsWith("1")) phones[i] = phones[i].substring(1);
        }

        for (int i = 0; i < arrayLength; i++) {
            for (int j = i + 1; j < arrayLength; j++) {
                if (j != i && arePhoneNumbersDuplicate(phones[i], phones[j])) {
                    Log.e(TAG, "displayErrorIfDuplicate: " +
                            "Duplicates found for chipPhoneArray[j] - '" + phones[i] + "' " +
                            "chipPhoneArray[k] - '" + phones[j] + "' " + " <------DUPLICATES FOUND 1------");
                    errorPhoneWrong(getString(R.string.AddMessage_PhoneRetv_DuplicatePhone));
                    return true;
                }
                Log.d(TAG, "displayErrorIfDuplicate: " +
                        "Duplicates not found for chipPhoneArray[j] - '" + phones[i] + "' " +
                        "chipPhoneArray[k] - '" + phones[j] + "' " + " <------DUPLICATES NOT FOUND 1------");
            }
        }
        return false;
    }

    /**Detects if two phone numbers are duplicates*/
    private boolean arePhoneNumbersDuplicate(String phone1, String phone2) {
        return !phone1.equals(EMPTY_STRING) && !phone2.equals(EMPTY_STRING) && phone2.equals(phone1);
    }

    /**Detect if the last key entered is a valid key (e.g. comma)*/
    private boolean doesPhoneEndWithBadKeys(String phone) {
        for (String badKey : PHONERETV_CUSTOM_ENDKEYS_BAD) {
            if (phone.endsWith(badKey)) return true;
        }
        return false;
    }

    /**Check if the last character is a valid endkey*/
    private boolean isLastCharValid(String lastChar) {
        for (String endKey : PHONERETV_CUSTOM_ENDKEYS) if (lastChar.equalsIgnoreCase(endKey)) return true;
        return false;
    }

    /**Check if the entered phone number is in a valid form*/
    private boolean isPhoneValid(String phone) {
        final Pattern phone_regex_strict = Pattern.compile(PHONERETV_PHONE_REGEX_STRICT);
        return phone_regex_strict.matcher(phone).matches();
    }

    private String getLastPhoneFromChip(DrawableRecipientChip[] chips) {
        if (chips.length > 0)
            return getPhoneNumbersFromChip( chips[chips.length - 1].toString().trim() );
        else return null;
    }

    //============= Message Content Listeners ===============//

    private void addMessageContentListeners() {
        // Text change listener to message content
        messageContentEditText.addTextChangedListener(messageContentEditTextWatcher);
    }

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

    //============= Edit Mode data population ===============//

    /**Get Edit message data from SQL*/
    private void retrieveEditMessageDataFromDB() {
        try {
            retrieveAndUpdateMessageDataFromDB(getIntent().getExtras().getInt("OLD_ALARM", -1));
        } catch (Exception e) {
            // catch sql error on return to app because onResume is called again.
            Log.e(TAG, "EditMessage: While adding data to activity an error was encountered", e);
        }
    }

    /**Sets the field data with data in message object. Only call if fields are empty.*/
    private void setFieldData() {
        setPhoneRetvFieldData();
        setMessageContentFieldData();
        setDatePickerData();
    }

    /**Resubmit all chips that are present in message object. Make Sure phoneRetv is empty if calling this*/
    private void setPhoneRetvFieldData() {
        phoneRetv.requestFocus();
        // addOnGlobalLayoutListener is ran after phoneRetv view is created
        phoneRetv.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Loop through and add all chips manually
                        for (int i = 0; i < message.getNameList().size(); i++) {
                            Uri uri = message.getUriList().get(i);
                            addNewChip(message.getNameList().get(i), message.getPhoneList().get(i), uri);
                        }

                        // Close the tree observer
                        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            phoneRetv.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            phoneRetv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
    }

    /**Resubmit all chips that are present in message object. Make sure messageContentEditText is clear*/
    private void setMessageContentFieldData() {
        messageContentEditText.setText(message.getContent());
    }

    /**Sets date picker to the data present in message object.*/
    private void setDatePickerData() {
        // Wait until datePicker's view has been established:
        datePicker.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        datePicker.setSelectedDate(message.getYear(), message.getMonth(), message.getDay());
                        datePicker.setSelectedTime(message.getHour(), message.getMinute());

                        // Close the tree observer
                        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            datePicker.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            datePicker.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
    }

    //============= Finish ("Done") button pressed ================//
    @Override
    /** Called when user hits finish button*/
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.AddMessage_DoneButton:
                if (verifyData() && (allPermissionsGranted || askForSmsSendPermission())) {
                    finishAndReturn();
                } else return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**Verifies that user data is correct and makes error messages*/
    private boolean verifyData() {
        boolean allDataCorrect = true;
        clearArrayLists();
        clearPhoneRetvError();
        clearMessageContentError();

        //Retrieve data from fields
        messageContentEditText.requestFocus();
        message.setContent(messageContentEditText.getText().toString());
        phoneRetv.requestFocus();
        updateChips();

        // Get time from datePicker
        message.setDateTime(datePicker.getSelectedDate());

        if (allDataCorrect) allDataCorrect = validatePhoneRetv();
        if (allDataCorrect) allDataCorrect = validateMessageContent();
        if (allDataCorrect) {
            validateDateTime();
            allDataCorrect = dateTimeIsValid;
        }

        return allDataCorrect;
    }

    private boolean validatePhoneRetv() {
        Pattern regex = Pattern.compile(PHONERETV_FULL_REGEX);
        Pattern regexStrict = Pattern.compile(PHONERETV_PHONE_REGEX_STRICT);

        final String phoneRetvToString = phoneRetv.getText().toString();
        String lastTypedChar = getLastCharOfString(phoneRetvToString);
        String[] fieldTextArray = getValidFieldTextArray(phoneRetvToString, lastTypedChar);

        if (chips.length != fieldTextArray.length) {
            // Arrays should be the same length. If they are not, there is something wrong in getValidFieldTextArray()
            Log.e(TAG + "verifyData:", "Error: chips length is not equal to phoneRetv length");
            Log.e(TAG + "verifyData:", "chips is " + Arrays.toString(chips) + " fieldTextArray " + Arrays.toString(fieldTextArray));
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
                Log.d(TAG, "fieldTextArray[i] is '" + fieldTextArray[i] + "'");

                if (!regex.matcher(chipStr).matches() || !regexStrict.matcher(fieldTextArray[i]).matches()) {
                    Log.e(TAG, "verifyData: Error - Invalid Entry for chipStr - '" + chipStr + "' or fieldTextArray[i] - '" + fieldTextArray[i] + "'");
                    errorPhoneWrong(getResources().getString(R.string.AddMessage_PhoneRetv_InvalidEntries));
                    return false;
                }

                if (displayErrorIfDuplicate(chips)) {
                    return false;
                }

                //Result okay from here, add to final result
                message.addToNameList(getNameFromString(chipStr));
                message.addToPhoneList(getPhoneNumbersFromChip(chipStr));
                Uri uri = chip.getEntry().getPhotoThumbnailUri();
                if (uri != null) {
                    message.addToUriList(uri);
                } else {
                    message.addToUriList(null);
                }
            }
        }
        return true;
    }

    private boolean validateMessageContent() {
        if (message.getContent().length() == 0) {
            errorMessageContentWrong();
            return false;
        }
        return true;
    }

    private void validateDateTime() {
        Calendar selDateTime = datePicker.getSelectedDate();
        // Get time 5 minutes from now
        GregorianCalendar in5Mins = new GregorianCalendar();
        in5Mins.add(Calendar.MINUTE, 5);

        if ( selDateTime.before(new GregorianCalendar()) ) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.ValidateDateTimeDialogTitle1)
                    .setMessage(R.string.ValidateDateTimeDialogMessage1)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    }).show();
            dateTimeIsValid = false;
        } else if (selDateTime.before(in5Mins)) {
            showValidateDateTimeDialog();
            dateTimeIsValid = false;
        } else {
            dateTimeIsValid = true;
        }
    }

    private void showValidateDateTimeDialog() {
        ValidateDateTimeDialogFragment validateDateTimeDialogFragment = new ValidateDateTimeDialogFragment();
        validateDateTimeDialogFragment.show(getSupportFragmentManager(), "ValidateDateTimeDialog");
    }

    public void validateDateTimePositiveClick() {
        dateTimeIsValid = true;
        finishAndReturn();
    }

    /**Adds to sql, creates alarms, returns to Home*/
    private void finishAndReturn() {
        // Add to sql database and schedule the alarm
        SQLUtilities.addDataToSQL(AddMessage.this, message);
        scheduleMessage();
        hideKeyboard();
        createSnackBar(getString(R.string.AddMessage_Notifications_CreateSuccess));

        // Create bundle of extras to pass back to Home
        Intent returnIntent = new Intent();
        Bundle extras = new Bundle();
        extras.putInt("ALARM_EXTRA", message.getAlarmNumber());
        extras.putInt("OLD_ALARM", getIntent().getExtras().getInt("OLD_ALARM", -1));
        returnIntent.putExtras(extras);

        // Return to HOME
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    /**Utility method to schedule alarm*/
    private void scheduleMessage() {
        // Starts alarm
        new MessageAlarmReceiver().createAlarm(this, message);
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

    //================ Error Message Utility Methods ==================//

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

    private boolean isPhoneRetvInError() {
        return phoneRetvErrorMessage.getText() != "";
    }

    private void clearErrorMessages() {
        clearPhoneRetvError();
        clearMessageContentError();
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

    //================ PhoneRetv Utility Methods ==================//

    /**Updates chips object from the phoneRetv library*/
    private void updateChips() {
        chips = phoneRetv.getRecipients();
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

    //================Misc Methods==================//
    /**Clears all Arraylist Values*/
    private void clearArrayLists() {
        message.clearLists();
    }

    //===============Edit Message SQL Retrieval===============//
    /** Pulls values from sql db on editMessage*/
    private void retrieveAndUpdateMessageDataFromDB(int alarmNumber) {
        if (alarmNumber == -1) throw new IllegalArgumentException("alarmNumber cannot be -1");

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
                SQLContract.MessageEntry.PHOTO_URI
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
        message.setNameList(Tools.stringToArrayList(cursor.getString(cursor.getColumnIndexOrThrow
                (SQLContract.MessageEntry.NAME))));
        message.setPhoneList(Tools.stringToArrayList(cursor.getString(cursor.getColumnIndexOrThrow
                (SQLContract.MessageEntry.PHONE))));
        message.setDateTime(
                cursor.getInt(cursor.getColumnIndexOrThrow(SQLContract.MessageEntry.YEAR)),
                cursor.getInt(cursor.getColumnIndexOrThrow(SQLContract.MessageEntry.MONTH)),
                cursor.getInt(cursor.getColumnIndexOrThrow(SQLContract.MessageEntry.DAY)),
                cursor.getInt(cursor.getColumnIndexOrThrow(SQLContract.MessageEntry.HOUR)),
                cursor.getInt(cursor.getColumnIndexOrThrow(SQLContract.MessageEntry.MINUTE)));
        message.setPhotoUriString(Tools.stringToArrayList(cursor.getString(cursor.getColumnIndexOrThrow
                (SQLContract.MessageEntry.PHOTO_URI))));
        message.setContent(cursor.getString(cursor.getColumnIndexOrThrow
                (SQLContract.MessageEntry.MESSAGE)));

        // Close everything so android doesn't complain
        cursor.close();
        mDbHelper.close();

        Log.d(TAG, "retrieveAndUpdateMessageDataFromDB: Values retrieved");
    }

    //============= Permissions ================// TODO: Move to new class
    /**Checks if READ_CONTACTS permission exists and prompts user*/
    @TargetApi(Build.VERSION_CODES.M)
    private void askForContactsReadPermission() {
        int hasWriteContactsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);

        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                showMessageOKDeny(getString(R.string.AddMessage_Permissions_ReadContactsRationalle),
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
        List<String> permissionPromptsForPermissionsNeeded = new ArrayList<>();
        final List<String> permissionsList = new ArrayList<>();

        if (!addPermissionToPermissionListIfNeeded(permissionsList, Manifest.permission.SEND_SMS)) {
            permissionPromptsForPermissionsNeeded.add(getString(R.string.AddMessage_Permissions_SMSMessages));
        }
        if (!addPermissionToPermissionListIfNeeded(permissionsList, Manifest.permission.READ_PHONE_STATE)) {
            permissionPromptsForPermissionsNeeded.add(getString(R.string.AddMessage_Permissions_PhoneCalls));
        }

        if (permissionsList.size() > 0) {
            if (permissionPromptsForPermissionsNeeded.size() > 0) {
                StringBuilder totalMessagePrompt = new StringBuilder(getString(R.string.AddMessage_Permissions_PermissionsPrompt1));
                for(String permissionPrompt : permissionPromptsForPermissionsNeeded) {
                    totalMessagePrompt.append("\n");
                    totalMessagePrompt.append(permissionPrompt);
                }

                showMessageOKDeny(totalMessagePrompt.toString(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), MY_PERMISSIONS_REQUEST_MULTIPLE_PERMISSIONS);
                    }
                });
                return false;
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
    private boolean addPermissionToPermissionListIfNeeded(List<String> permissionsList, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }
    /** Shows a dialog box with OK/deny boxes*/
    private void showMessageOKDeny(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(AddMessage.this)
                .setMessage(message)
                .setPositiveButton(R.string.ok, okListener)
                .setNegativeButton(R.string.AddMessage_ButtonDeny, null)
                .create()
                .show();
    }
    /**Retrieves result of askForSmsSendPermission*/
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {
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
}