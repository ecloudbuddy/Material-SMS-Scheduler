/*
 Copyright 2016 Kyle Szombathy

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

import android.app.Application;
import android.content.Context;

/**
 * Created by Kyle on 12/7/2015.
 */
public class SMSScheduler extends Application {
    private static Context context;
    private boolean applicationInForeground = false;

    public void onCreate() {
        super.onCreate();
        SMSScheduler.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return SMSScheduler.context;
    }

    public boolean isApplicationInForeground() {
        return applicationInForeground;
    }

    public void setApplicationInForeground(boolean applicationInForeground) {
        this.applicationInForeground = applicationInForeground;
    }
}
