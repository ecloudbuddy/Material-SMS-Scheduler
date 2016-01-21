package com.kyleszombathy.sms_scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Class that listens for network connectivity changes
 * Interacts with MessageAlarmReceiver on fail to send message
 */
public class ConnectionChangeReceiver extends BroadcastReceiver
{
    String TAG = "ConnectionChangeReceiver";
    @Override
    public void onReceive( Context context, Intent intent )
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService( Context.CONNECTIVITY_SERVICE );
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        NetworkInfo mobNetInfo = connectivityManager.getNetworkInfo(     ConnectivityManager.TYPE_MOBILE );
        if ( activeNetInfo != null )
        {
            Log.i(TAG, "Active Network Type : " + activeNetInfo.getTypeName());
        }
        if( mobNetInfo != null )
        {
            Log.i(TAG, "Mobile Network Type : " + mobNetInfo.getTypeName());
        }
    }
}