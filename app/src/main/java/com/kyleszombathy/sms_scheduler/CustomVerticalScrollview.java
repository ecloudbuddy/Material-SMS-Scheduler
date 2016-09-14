package com.kyleszombathy.sms_scheduler;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/** Custom Scrollview in order to allow nested scrolling */
public class CustomVerticalScrollview extends ScrollView{

    public CustomVerticalScrollview(Context context) {
        super(context);
    }

    public CustomVerticalScrollview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVerticalScrollview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                super.onTouchEvent(event);
                break;

            case MotionEvent.ACTION_MOVE:
                return false;

            case MotionEvent.ACTION_CANCEL:
                super.onTouchEvent(event);
                break;

            case MotionEvent.ACTION_UP:
                return false;

            default: break;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return true;
    }
}