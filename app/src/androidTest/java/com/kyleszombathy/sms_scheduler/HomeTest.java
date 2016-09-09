package com.kyleszombathy.sms_scheduler;

import android.support.test.espresso.Root;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.ex.chips.RecipientEditTextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.DEFAULT;
import static android.support.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;


/**
 * UI Test for Add Message
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class HomeTest {
    public static final String TAG = "HomeTest";
    public static final String LONG_MESSAGE = "Lorem ipsum dolor sit amet, consectetur adipiscing " +
            "elit. Sed condimentum, augue id porta cursus, lorem magna auctor nulla, vel ultricies" +
            " eros sem id magna. Etiam sed feugiat risus. Aliquam eget dui risus. Phasellus " +
            "non mi consequat lacus rhoncus venenatis at ultricies lectus. Suspendisse vestibulum " +
            "erat risus, quis tempor ex ornare vitae. Integer non sapien nulla. Sed aliquet " +
            "euismod felis, eu blandit enim lacinia a. Etiam venenatis elit ut ultrices congue. " +
            "In mollis, felis nec egestas fermentum, diam enim dignissim tortor, in malesuada " +
            "purus lacus in diam. Suspendisse tincidunt, purus et vestibulum malesuada, lacus " +
            "quam tincidunt ante, vitae ultricies nisi libero a diam.";
    public static final String PHONE_RETV_1_SEARCH = "Alex";
    public static final String PHONE_RETV_1_RESULT = "Alex Valenze";
    public static final String PHONE_RETV_1__NUMBER[] = {"Alex Valenze <+19168656910>"};
    public static final String PHONE_RETV_INVALID = "InvalidContact";
    public static final String PHONE_RETV_2 = "Noelle";

    public static final int MESSAGE_CONTENT_1_LENGTH = 160;
    public static final int MESSAGE_CONTENT_2_LENGTH = MESSAGE_CONTENT_1_LENGTH * 2;
    public static final String MESSAGE_CONTENT_1 = LONG_MESSAGE.substring(0, MESSAGE_CONTENT_1_LENGTH);
    public static final String MESSAGE_CONTENT_2 = LONG_MESSAGE.substring(0, MESSAGE_CONTENT_2_LENGTH);
    public static final int MESSAGE_CONTENT_VALID_LENGTH = 20;
    public static final String MESSAGE_CONTENT_VALID = LONG_MESSAGE.substring(0,MESSAGE_CONTENT_VALID_LENGTH);



    @Rule
    public ActivityTestRule<Home> mActivityRule = new ActivityTestRule<>(Home.class);

    private void clickAddMessageButton() {
        onView(withId(R.id.Home_fab)).perform(click());
    }
    private void typeLoadPhoneRetv(int ID, String search, String result) {
        onView(withId(ID)).perform(typeText(search), closeSoftKeyboard());
        onView(withText(result)).inRoot(isPopupWindow()).perform(click());
    }
    private void typeTextGivenID(int ID, String text) {
        onView(withId(ID)).perform(typeText(text), closeSoftKeyboard());
    }
    private void verifyTextGivenID(int ID, String text) {
        onView(withId(ID)).check(matches(withText(text)));
    }

    private static Matcher<View> phoneRetvMatch(final String[] expected) {
        return new TypeSafeMatcher<View>() {

            @Override
            public boolean matchesSafely(View view) {
                RecipientEditTextView phoneRetv = (RecipientEditTextView) view;
                String str1 = Arrays.toString(phoneRetv.getSortedRecipients());
                String str2 = Arrays.toString(expected);
                Log.d(TAG, "phoneRetvMatch:matchesSafely: values are " + str1 + " and "  + str2);
                return str1.equals(str2);
            }

            @Override
            public void describeTo(Description description) {}
        };
    }

    private static Matcher<View> textMatch(final String expected) {
        return new TypeSafeMatcher<View>() {

            @Override
            public boolean matchesSafely(View view) {
                String str1= null;
                if (view instanceof EditText) {
                    EditText editText = (EditText) view;
                    str1 = editText.getText().toString();
                } else if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    str1 = textView.getText().toString();
                }

                Log.d(TAG, "editTextMatch: matchesSafely: values are " + str1 + " and "  + expected);
                return str1 != null && str1.equals(expected);
            }

            @Override
            public void describeTo(Description description) {}
        };
    }

    //@Test
    public void testPhoneRetvLoad1() {
        // Navigate to AddMessage
        clickAddMessageButton();
        // Type name and click result
        typeLoadPhoneRetv(R.id.AddMessage_PhoneRetv, PHONE_RETV_1_SEARCH, PHONE_RETV_1_RESULT);
        // Verify that the text was changed.
        onView(withId(R.id.AddMessage_PhoneRetv)).inRoot(DEFAULT).check(matches(phoneRetvMatch(PHONE_RETV_1__NUMBER)));
    }
    //@Test
    //Test PhoneRetv for invalid contact
    public void testPhoneRetvError1() {
        // Navigate to AddMessage
        clickAddMessageButton();
        // Type name and click result
        typeTextGivenID(R.id.AddMessage_PhoneRetv, PHONE_RETV_INVALID);
        onView(withId(R.id.AddMessage_Message_Content)).perform(typeText(MESSAGE_CONTENT_VALID));
        // Click done button
        onView(withId(R.id.AddMessage_DoneButton)).perform(click());
        // Verify error message
        //onView(withId(R.id.AddMessage_PhoneRetv_Error)).check(matches(withText(R.string.AddMessage_PhoneRetvError)));
    }
    //@Test
    public void testMessageContent1() {
        clickAddMessageButton();
        typeTextGivenID(R.id.AddMessage_Message_Content, MESSAGE_CONTENT_1);
        onView(withId(R.id.AddMessage_MessageContent_Counter)).check(matches(textMatch("0")));
    }
    //@Test
    public void testMessageContent2() {
        clickAddMessageButton();
        typeTextGivenID(R.id.AddMessage_Message_Content, MESSAGE_CONTENT_2);
        onView(withId(R.id.AddMessage_MessageContent_Counter)).check(matches(textMatch("1 / 2")));
    }
    //@Test
    // Test Message Content for blank field
    public void testMessageContentError1() {
        // Navigate to AddMessage
        clickAddMessageButton();
        // Type name and click result
        typeLoadPhoneRetv(R.id.AddMessage_PhoneRetv, PHONE_RETV_1_SEARCH, PHONE_RETV_1_RESULT);
        // Click done button
        onView(withId(R.id.AddMessage_DoneButton)).perform(click());
        onView(withId(R.id.AddMessage_MessageContent_Error)).check(matches(withText(R.string.AddMessage_MessageContentError)));
    }
    @Test
    // Test






    public static Matcher<Root> isPopupWindow() {
        return isPlatformPopup();
    }
}