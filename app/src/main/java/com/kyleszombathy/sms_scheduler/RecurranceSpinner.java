package com.kyleszombathy.sms_scheduler;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.codetroopers.betterpickers.recurrencepicker.EventRecurrence;
import com.codetroopers.betterpickers.recurrencepicker.EventRecurrenceFormatter;
import com.codetroopers.betterpickers.recurrencepicker.RecurrencePickerDialogFragment;
import com.simplicityapks.reminderdatepicker.lib.DateItem;
import com.simplicityapks.reminderdatepicker.lib.OnDateSelectedListener;
import com.simplicityapks.reminderdatepicker.lib.PickerSpinner;
import com.simplicityapks.reminderdatepicker.lib.PickerSpinnerAdapter;
import com.simplicityapks.reminderdatepicker.lib.ReminderDatePicker;
import com.simplicityapks.reminderdatepicker.lib.TwinTextItem;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by Kyle on 1/23/2016.
 */
public class RecurranceSpinner extends PickerSpinner implements RecurrencePickerDialogFragment.OnRecurrenceSetListener, AdapterView.OnItemSelectedListener{
    private static final String FRAG_TAG_RECUR_PICKER = "recurrencePickerDialogFragment";
    private EventRecurrence mEventRecurrence = new EventRecurrence();
    private String mRrule;
    public String displayText;

    public static final String XML_TAG_DATEITEM = "DateItem";

    public static final String XML_ATTR_ABSDAYOFYEAR = "absDayOfYear";
    public static final String XML_ATTR_ABSDAYOFMONTH = "absDayOfMonth";
    public static final String XML_ATTR_ABSMONTH = "absMonth";
    public static final String XML_ATTR_ABSYEAR = "absYear";

    public static final String XML_ATTR_RELDAY = "relDay";
    public static final String XML_ATTR_RELMONTH = "relMonth";
    public static final String XML_ATTR_RELYEAR = "relYear";


    // These listeners don't have to be implemented, if null just ignore
    private OnDateSelectedListener dateListener = null;

    // The default DatePicker dialog to show if customDatePicker has not been set
    private FragmentManager fragmentManager;

    private boolean showMonthItem = false;
    private boolean showWeekdayNames = false;
    private boolean showNumbersInView = false;

    private String[] weekDays = null;

    // To catch twice selecting the same date:
    private Calendar lastSelectedDate = null;

    // The custom DateFormat used to convert Calendars into displayable Strings:
    private java.text.DateFormat customDateFormat = null;
    private java.text.DateFormat secondaryDateFormat = null;

    /**
     * Construct a new RecurranceSpinner with the given context's theme.
     * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
     */
    public RecurranceSpinner(Context context){
        this(context, null, 0);
    }

    /**
     * Construct a new RecurranceSpinner with the given context's theme and the supplied attribute set.
     * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view. May contain a flags attribute.
     */
    public RecurranceSpinner(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }

    /**
     * Construct a new TimeSpinner with the given context's theme, the supplied attribute set, and default style.
     * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view. May contain a flags attribute.
     * @param defStyle The default style to apply to this view. If 0, no style will be applied (beyond
     *                 what is included in the theme). This may either be an attribute resource, whose
     *                 value will be retrieved from the current theme, or an explicit style resource.
     */
    public RecurranceSpinner(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        // check if the parent activity has our dateSelectedListener, automatically enable it:
        if(context instanceof OnDateSelectedListener)
            setOnDateSelectedListener((OnDateSelectedListener) context);
        setOnItemSelectedListener(this);

        // get the FragmentManager:
        try{
            fragmentManager = ((FragmentActivity) context).getSupportFragmentManager();
        } catch (ClassCastException e) {
            Log.d(getClass().getSimpleName(), "Can't get fragment manager from context");
        }

        if(attrs != null) {
            // get our flags from xml, if set:
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ReminderDatePicker);
            int flags = a.getInt(R.styleable.ReminderDatePicker_flags, ReminderDatePicker.MODE_GOOGLE);
            setFlags(flags);
            a.recycle();
        }
    }

    private boolean hasVibratePermission(Context context) {
        final String permission = "android.permission.VIBRATE";
        final int res = context.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public List<TwinTextItem> getSpinnerItems() {
        try {
            return getItemsFromXml(R.xml.date_items);
        } catch (Exception e) {
            Log.d("RecurranceSpinner", "Error parsing date items from xml");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected @Nullable TwinTextItem parseItemFromXmlTag(@NonNull XmlResourceParser parser) {
        final Resources res = getResources();
        final String packageName = getContext().getPackageName();

        if(!parser.getName().equals(XML_TAG_DATEITEM)) {
            Log.d("RecurranceSpinner", "Unknown xml tag name: " + parser.getName());
            return null;
        }

        // parse the DateItem, possible values are
        String text = null;
        @StringRes int textResource = NO_ID, id = NO_ID;
        Calendar date = Calendar.getInstance();
        for(int i=parser.getAttributeCount()-1; i>=0; i--) {
            String attrName = parser.getAttributeName(i);
            switch (attrName) {
                case XML_ATTR_ID:
                    id = parser.getIdAttributeResourceValue(NO_ID);
                    break;
                case XML_ATTR_TEXT:
                    text = parser.getAttributeValue(i);
                    if(text != null && text.startsWith("@string/"))
                        textResource = res.getIdentifier(text, "string", packageName);
                    break;

                case XML_ATTR_ABSDAYOFYEAR:
                    final int absDayOfYear = parser.getAttributeIntValue(i, -1);
                    if(absDayOfYear > 0)
                        date.set(Calendar.DAY_OF_YEAR, absDayOfYear);
                    break;
                case XML_ATTR_ABSDAYOFMONTH:
                    final int absDayOfMonth = parser.getAttributeIntValue(i, -1);
                    if(absDayOfMonth > 0)
                        date.set(Calendar.DAY_OF_MONTH, absDayOfMonth);
                    break;
                case XML_ATTR_ABSMONTH:
                    final int absMonth = parser.getAttributeIntValue(i, -1);
                    if(absMonth >= 0)
                        date.set(Calendar.MONTH, absMonth);
                    break;
                case XML_ATTR_ABSYEAR:
                    final int absYear = parser.getAttributeIntValue(i, -1);
                    if(absYear >= 0)
                        date.set(Calendar.YEAR, absYear);
                    break;

                case XML_ATTR_RELDAY:
                    final int relDay = parser.getAttributeIntValue(i, 0);
                    date.add(Calendar.DAY_OF_YEAR, relDay);
                    break;
                case XML_ATTR_RELMONTH:
                    final int relMonth = parser.getAttributeIntValue(i, 0);
                    date.add(Calendar.MONTH, relMonth);
                    break;
                case XML_ATTR_RELYEAR:
                    final int relYear = parser.getAttributeIntValue(i, 0);
                    date.add(Calendar.YEAR, relYear);
                    break;
                default:
                    Log.d("RecurranceSpinner", "Skipping unknown attribute tag parsing xml resource: "
                            + attrName + ", maybe a typo?");
            }
        }// end for attr

        // now construct the date item from the attributes

        // check if we got a textResource earlier and parse that string together with the weekday
        if(textResource != NO_ID)
            text = getWeekDay(date.get(Calendar.DAY_OF_WEEK), textResource);

        // when no text is given, format the date to have at least something to show
        if(text == null || text.equals(""))
            text = formatDate(date);

        return new DateItem(text, date, id);
    }

    private String getWeekDay(int weekDay, @StringRes int stringRes) {
        if(weekDays == null) weekDays = new DateFormatSymbols().getWeekdays();
        // use a separate string for Saturday and Sunday because of gender variation in Portuguese
        if(weekDay==7 || weekDay==1) {
            if(stringRes == R.string.date_next_weekday)
                stringRes = R.string.date_next_weekday_weekend;
            else if(stringRes == R.string.date_last_weekday)
                stringRes = R.string.date_last_weekday_weekend;
        }
        String result = getResources().getString(stringRes, weekDays[weekDay]);
        // in some translations (French for instance), the weekday is the first word but is not capitalized, so we'll do that
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    /**
     * Gets the currently selected date (that the Spinner is showing)
     * @return The selected date as Calendar, or null if there is none.
     */
    public Calendar getSelectedDate() {
        final Object selectedItem = getSelectedItem();
        if(!(selectedItem instanceof DateItem))
            return null;
        return ((DateItem) selectedItem).getDate();
    }

    /**
     * Sets the Spinner's selection as date. If the date was not in the possible selections, a temporary
     * item is created and passed to selectTemporary().
     * @param date The date to be selected.
     */
    public void setSelectedDate(@NonNull Calendar date) {
        final int count = getAdapter().getCount() - 1;
        int itemPosition = -1;
        for(int i=0; i<count; i++) {
            if(getAdapter().getItem(i).equals(date)) { // because DateItem deeply compares to calendar
                itemPosition = i;
                break;
            }
        }
        if(itemPosition >= 0)
            setSelection(itemPosition);
        else if(showWeekdayNames) {
            final long MILLIS_IN_DAY = 1000*60*60*24;
            final long dateDifference = (date.getTimeInMillis()/MILLIS_IN_DAY)
                    - (Calendar.getInstance().getTimeInMillis()/MILLIS_IN_DAY);
            if(dateDifference>0 && dateDifference<7) { // if the date is within the next week:
                // construct a temporary DateItem to select:
                final int day = date.get(Calendar.DAY_OF_WEEK);

                // Because these items are always temporarily selected, we can safely assume that
                // they will never appear in the spinner dropdown. When a FLAG_NUMBERS is set, we
                // want these items to have the date as secondary text in a short format.
                selectTemporary(new DateItem(getWeekDay(day, R.string.date_only_weekday), formatSecondaryDate(date), date, NO_ID));
            } else {
                // show the date as a full text, using the current DateFormat:
                selectTemporary(new DateItem(formatDate(date), date, NO_ID));
            }
        }
        else {
            // show the date as a full text, using the current DateFormat:
            selectTemporary(new DateItem(formatDate(date), date, NO_ID));
        }
    }

    private String formatDate(@NonNull Calendar date) {
        if(customDateFormat == null)
            return DateUtils.formatDateTime(getContext(), date.getTimeInMillis(), DateUtils.FORMAT_SHOW_DATE);
        else
            return customDateFormat.format(date.getTime());
    }

    // only to be used when FLAG_NUMBERS and FLAG_WEEKDAY_NAMES have been set
    private String formatSecondaryDate(@NonNull Calendar date) {
        if(secondaryDateFormat == null)
            return DateUtils.formatDateTime(getContext(), date.getTimeInMillis(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE);
        else
            return secondaryDateFormat.format(date.getTime());
    }

    /**
     * Gets the custom DateFormat currently used to format Calendar strings.
     * If {@link #setDateFormat(java.text.DateFormat)} has not been called yet, it will return null.
     * @return The date format, or null if the Spinner is using the default date format.
     */
    public java.text.DateFormat getCustomDateFormat() {
        return customDateFormat;
    }

    /**
     * Sets the custom date format to use for formatting Calendar objects to displayable strings.
     * @param dateFormat The new DateFormat, or null to use the default format.
     */
    public void setDateFormat(java.text.DateFormat dateFormat) {
        setDateFormat(dateFormat, null);
    }

    /**
     * Sets the custom date format to use for formatting Calendar objects to displayable strings.
     * @param dateFormat The new DateFormat, or null to use the default format.
     * @param numbersDateFormat The DateFormat for formatting the secondary date when both FLAG_NUMBERS
     *                          and FLAG_WEEKDAY_NAMES are set, or null to use the default format.
     */
    public void setDateFormat(java.text.DateFormat dateFormat, java.text.DateFormat numbersDateFormat) {
        this.customDateFormat = dateFormat;
        this.secondaryDateFormat = numbersDateFormat;
        // update the spinner with the new date format:

        // the only spinner item that will be affected is the month item, so just toggle the flag twice
        // instead of rebuilding the whole adapter
        if(showMonthItem) {
            int monthPosition = getAdapterItemPosition(4);
            boolean reselectMonthItem = getSelectedItemPosition() == monthPosition;
            setShowMonthItem(false);
            setShowMonthItem(true);
            if(reselectMonthItem) setSelection(monthPosition);
        }

        // if we have a temporary date item selected, update that as well
        if(getSelectedItemPosition() == getAdapter().getCount())
            setSelectedDate(getSelectedDate());
    }

    /**
     * Implement this interface if you want to be notified whenever the selected date changes.
     */
    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.dateListener = listener;
    }

    /**
     * Toggles showing the month item. Month mode an item in exactly one month from now.
     * @param enable True to enable, false to disable month mode.
     */
    public void setShowMonthItem(boolean enable) {
        if(enable && !showMonthItem) {
            // create the in 1 month item
            final Calendar date = Calendar.getInstance();
            date.add(Calendar.MONTH, 1);
            addAdapterItem(new DateItem(formatDate(date), date, R.id.date_month));
        }
        else if(!enable && showMonthItem) {
            removeAdapterItemById(R.id.date_month);
        }
        showMonthItem = enable;
    }

    /**
     * Toggles showing the weekday names instead of dates for the next week. Turning this on will
     * display e.g. "Sunday" for the day after tomorrow, otherwise it'll be January 1.
     * @param enable True to enable, false to disable weekday names.
     */
    public void setShowWeekdayNames(boolean enable) {
        if(showWeekdayNames != enable) {
            showWeekdayNames = enable;
            // if FLAG_NUMBERS has been set, toggle the secondary text in the adapter
            if(showNumbersInView)
                setShowNumbersInViewInt(enable);
            // reselect the current item so it will use the new setting:
            setSelectedDate(getSelectedDate());
        }
    }

    /**
     * Toggles showing numeric dates for the weekday items in the spinner view. This will only apply
     * when a day within the next week is selected and FLAG_WEEKDAY_NAMES has been set, not in the dropdown.
     * @param enable True to enable, false to disable numeric mode.
     */
    public void setShowNumbersInView(boolean enable) {
        showNumbersInView = enable;
        // only enable the adapter when FLAG_WEEKDAY_NAMES has been set as well
        if(!enable || showWeekdayNames)
            setShowNumbersInViewInt(enable);
    }

    private void setShowNumbersInViewInt(boolean enable) {
        PickerSpinnerAdapter adapter = (PickerSpinnerAdapter) getAdapter();
        // workaround for now. See GitHub issue #2
        if (enable != adapter.isShowingSecondaryTextInView() && adapter.getCount() == getSelectedItemPosition())
            setSelection(0);
        adapter.setShowSecondaryTextInView(enable);
    }

    /**
     * Set the flags to use for this date spinner.
     * @param modeOrFlags A mode of ReminderDatePicker.MODE_... or multiple ReminderDatePicker.FLAG_...
     *                    combined with the | operator.
     */
    public void setFlags(int modeOrFlags) {
        setShowMonthItem((modeOrFlags & ReminderDatePicker.FLAG_MONTH) != 0);
        setShowWeekdayNames((modeOrFlags & ReminderDatePicker.FLAG_WEEKDAY_NAMES) != 0);
        setShowNumbersInView((modeOrFlags & ReminderDatePicker.FLAG_NUMBERS) != 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAdapterItemAt(int index) {
        if(index == getSelectedItemPosition()) {
            Calendar date = getSelectedDate();
            selectTemporary(new DateItem(formatDate(date), date, NO_ID));
        }
        super.removeAdapterItemAt(index);
    }

    @Override
    public CharSequence getFooter() {
        return getResources().getString(R.string.spinner_date_footer);
    }

    @Override
    public void onFooterClick() {
        Bundle bundle = new Bundle();
        Calendar cal = new GregorianCalendar();

        bundle.putLong(RecurrencePickerDialogFragment.BUNDLE_START_TIME_MILLIS, cal.getTimeInMillis());
        bundle.putString(RecurrencePickerDialogFragment.BUNDLE_TIME_ZONE, cal.getTimeZone().toString());
        bundle.putBoolean(RecurrencePickerDialogFragment.BUNDLE_HIDE_SWITCH_BUTTON, true);

        // may be more efficient to serialize and pass in EventRecurrence
        //bundle.putString(RecurrencePickerDialogFragment.BUNDLE_RRULE, mRrule);

        RecurrencePickerDialogFragment dialogFragment = (RecurrencePickerDialogFragment) fragmentManager.findFragmentByTag(FRAG_TAG_RECUR_PICKER);
        if (dialogFragment != null) {
            dialogFragment.dismiss();
        }
        dialogFragment = new RecurrencePickerDialogFragment();
        dialogFragment.setArguments(bundle);
        dialogFragment.setOnRecurrenceSetListener(RecurranceSpinner.this);
        dialogFragment.show(fragmentManager, FRAG_TAG_RECUR_PICKER);
    }

    @Override
    protected void restoreTemporarySelection(String codeString) {
        selectTemporary(DateItem.fromString(codeString));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(dateListener != null) {
            // catch selecting same date twice
            Calendar date = getSelectedDate();
            if(date != null && !date.equals(lastSelectedDate)) {
                dateListener.onDateSelected(date);
                lastSelectedDate = date;
            }
        }
    }

    // unused
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onRecurrenceSet(String rrule) {
        mRrule = rrule;
        if (mRrule != null) {
            mEventRecurrence.parse(mRrule);
        }
        populateRepeats();
    }

    private void populateRepeats() {
        Resources r = getResources();
        String repeatString = "";
        boolean enabled;
        if (!TextUtils.isEmpty(mRrule)) {
            repeatString = EventRecurrenceFormatter.getRepeatString(getContext(), r, mEventRecurrence, true);
        }
        displayText = mRrule + "\n" + repeatString;
    }
}
