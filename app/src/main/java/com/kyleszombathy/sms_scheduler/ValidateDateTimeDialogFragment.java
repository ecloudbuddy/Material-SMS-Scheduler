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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

/**
 * Created by Kyle on 10/16/2016.
 */

public class ValidateDateTimeDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.ValidateDateTimeDialogTitle2)
                .setMessage(R.string.ValidateDateTimeDialogMessage2)
                .setPositiveButton(R.string.YES,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((AddMessage)getActivity()).validateDateTimePositiveClick();
                            }
                        }
                )
                .setNegativeButton(R.string.picker_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing
                            }
                        }
                )
                .create();
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
    }


}
