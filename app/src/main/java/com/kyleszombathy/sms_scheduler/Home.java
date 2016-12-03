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

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.transition.Fade;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.Toolbar;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

import jp.wasabeef.recyclerview.animators.SlideInDownAnimator;

public class Home extends Activity {
    private static final String TAG = "HOME";
    private static final String ALARM_EXTRA = "alarmNumber";
    private static final String EDIT_MESSAGE_EXTRA = "EDIT_MESSAGE";
    private final int circleImageViewWidth = 112;
    private final int circleImageViewTextSize = 60;
    private final int screenFadeDuration = 700;
    private final int offScreenRecyclerDistance = 10000;
    private View parentView;
    // Recyclerview adapter dataset
    private RelativeLayout mRecyclerEmptyState;
    private RecyclerView mRecyclerView;
    private RecyclerAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private MessagesArrayList messages = new MessagesArrayList();
    // For random number retrieval
    private final int MAX_INT = Integer.MAX_VALUE ;
    private final int MIN_INT = 1;
    // For edit message function
    private static final int NEW_MESSAGE = 1;
    private static final int EDIT_MESSAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Activity View Created");

        // Setting up transitions
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Fade());

        setContentView(R.layout.activity_home);

        Toolbar toolbar = (Toolbar) findViewById(R.id.SMSScheduler_Toolbar);
        setActionBar(toolbar);

        populateDatasets();
        setUpRecyclerView();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("custom-event-name"));

        parentView = findViewById(R.id.Home_coordLayout);


        // Floating action button start activity
        findViewById(R.id.Home_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Random alarm number
                int alarmNumber = getRandomInt(MIN_INT, MAX_INT);

                Intent intent = new Intent(new Intent(Home.this, AddMessage.class));
                Bundle extras = new Bundle();
                extras.putInt(ALARM_EXTRA, alarmNumber);
                extras.putBoolean(EDIT_MESSAGE_EXTRA, false);
                intent.putExtras(extras);

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(Home.this);
                stackBuilder.addParentStack(AddMessage.class);
                stackBuilder.addNextIntent(intent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                Log.d(TAG, "Starting Activity AddMessage");

                startActivityForResult(intent, NEW_MESSAGE,
                        ActivityOptions.makeSceneTransitionAnimation(Home.this).toBundle());
            }
        });
    }

    /** Returns random int between min and max*/
    private int getRandomInt(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "OnResume called");
    }

    //=============== Data Retrieval and Initialization ===============//
    private void populateDatasets() {
        clearDatasets();
        SQLDbHelper mDbHelper = new SQLDbHelper(Home.this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor = dbRetrieveContactData(mDbHelper, db);

        // Moves to first row
        cursor.moveToFirst();

        // Loop through db
        for (int i = 0; i < cursor.getCount(); i++) {
            int year, month, day, hour, minute, alarmNumber;
            String name, messageContent, uriString, phone;
            Calendar dateTime;
            ArrayList<String> uriArrayList;
            Uri uri;

            // Retriever data from cursor
            name = cursor.getString(cursor.getColumnIndexOrThrow
                    (SQLContract.MessageEntry.NAME));
            phone = cursor.getString(cursor.getColumnIndexOrThrow
                    (SQLContract.MessageEntry.PHONE));
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
            alarmNumber = cursor.getInt(cursor.getColumnIndexOrThrow
                    (SQLContract.MessageEntry.ALARM_NUMBER));
            messageContent = cursor.getString(cursor.getColumnIndexOrThrow
                    (SQLContract.MessageEntry.MESSAGE));
            uriString = cursor.getString(cursor.getColumnIndexOrThrow
                    (SQLContract.MessageEntry.PHOTO_URI));

            //Extract URI
            uriArrayList = Tools.stringToArrayList(uriString.trim());

            Message message = new Message(Tools.stringToArrayList(name),
                    Tools.stringToArrayList(phone),
                    year, month, day, hour, minute,
                    messageContent, alarmNumber, Message.stringListToUriList(uriArrayList));
            messages.add(message);

            // Move to next row
            cursor.moveToNext();
        }
        cursor.close();
        mDbHelper.close();
        // Get bitmaps
        setContactImages();
    }

    /**Retrieves values from sql Database and store locally*/
    private Cursor dbRetrieveContactData(SQLDbHelper mDbHelper, SQLiteDatabase db) {
        Cursor cursor = null;

        String[] projection = {
                SQLContract.MessageEntry.NAME,
                SQLContract.MessageEntry.MESSAGE,
                SQLContract.MessageEntry.YEAR,
                SQLContract.MessageEntry.MONTH,
                SQLContract.MessageEntry.DAY,
                SQLContract.MessageEntry.HOUR,
                SQLContract.MessageEntry.MINUTE,
                SQLContract.MessageEntry.ALARM_NUMBER,
                SQLContract.MessageEntry.PHOTO_URI,
                SQLContract.MessageEntry.PHONE
        };

        // Sort the contact data by date/time, then by Name, then by Message Content
        String sortOrder = SQLContract.MessageEntry.DATETIME+ " ASC, " + SQLContract.MessageEntry.NAME + " ASC, " + SQLContract.MessageEntry.MESSAGE + " ASC";
        String selection = SQLContract.MessageEntry.ARCHIVED + " LIKE ?";
        String[] selectionArgs = { String.valueOf(0) };

        try {
            cursor =  db.query(
                    SQLContract.MessageEntry.TABLE_NAME,      // The table to query
                    projection,                               // The columns to return
                    selection,                                // The columns for the WHERE clause
                    selectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    sortOrder                                 // The sort order
            );
        } catch(Exception e) {
            Log.e(TAG, "dbRetrieveContactData: Retrieve encountered exception", e);
        } if (cursor != null) {
            Log.i(TAG, "dbRetrieveContactData: Retrieve successful. Found " + cursor.getCount() + " contact entries");
        }

        return cursor;
    }

    private void clearDatasets() {
        messages.clear();
    }

    /**Retreieves bitmap from database*/
    // TODO: Move this off the UI thread
    private void setContactImages() {
        for (int msgIndex = 0; msgIndex < messages.size(); msgIndex++) {
            Message message = messages.get(msgIndex);
            ArrayList<Uri> uriList = message.getUriList();
            Bitmap contactPhoto = null;

            if (uriList != null) {
                contactPhoto = retrieveContactImage(uriList.get(0));
            } else {
                // Set custom contact image based off first letter of contact name
                String firstName = message.getNameList().get(0);
                // Ensure character is not a number
                if (Character.isLetter(firstName.charAt(0))) {
                    contactPhoto = createCircleImageFromFirstLetterOfName(firstName);
                    Log.i(TAG, "setContactImages: Created custom image based off first letter: " + firstName.charAt(0));
                } else {
                    // TODO: Write code for handling messages to multiple people
                }

            }
            message.setContactPhoto(contactPhoto);
            messages.setPhotoDataset(msgIndex);
        }

    }

    private Bitmap createCircleImageFromFirstLetterOfName(String firstName) {
        // Get color
        ColorGenerator generator = ColorGenerator.MATERIAL;
        int color = generator.getColor(firstName); //Always use same color for a specific person
        TextDrawable drawable = TextDrawable.builder(this)
                .beginConfig()
                .useFont(Typeface.DEFAULT_BOLD)
                .fontSize(circleImageViewTextSize)
                .height(circleImageViewWidth)
                .width(circleImageViewWidth)
                .endConfig()
                .buildRound(Character.toString(firstName.charAt(0)).toUpperCase(), color);
        return Tools.drawableToBitmap(drawable); // Convert to bitmap
    }

    private Bitmap retrieveContactImage(Uri uri) {
        InputStream arrayInputStream =SQLUtilities.getPhoto(Home.this, uri);
        return BitmapFactory.decodeStream(arrayInputStream);
    }


    /**Cancels alarm given alarm service number
     * @param alarmNumb alarmNumber to delete*/
    private void cancelAlarmInAndroidSystem(int alarmNumb) {
        Intent intent = new Intent(this, MessageAlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(this, alarmNumb, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(sender);
        Log.i(TAG, "cancelAlarmInAndroidSystem: Alarm number " + alarmNumb + " Canceled");
    }


    //=============== Recycler View ===============//
    /**Sets up recycler view and adapter*/
    private void setUpRecyclerView() {
        // Empty state
        mRecyclerEmptyState = (RelativeLayout) findViewById(R.id.Home_recycler_empty_state);
        // Setting up RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.Home_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new SlideInDownAnimator());

        initializeRecyclerAdapter();

        // Item touch listener for editing message
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(Home.this,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int clickedPosition) {
                                // Tie to global values for use after AddMessage return
                                int oldAlarmNumber = messages.get(clickedPosition).getAlarmNumber();
                                if (oldAlarmNumber == -1) throw new ArithmeticException("oldAlarmNumber cannot be -1 if passing to AddMessage");
                                int newAlarmNumber = getRandomInt(MIN_INT, MAX_INT);

                                // Bundle extras
                                Bundle extras = new Bundle();
                                extras.putInt(ALARM_EXTRA, newAlarmNumber);
                                extras.putInt("OLD_ALARM", oldAlarmNumber);
                                extras.putBoolean(EDIT_MESSAGE_EXTRA, true);

                                // Create new intent to AddMessage with data from item in position
                                Intent intent = new Intent(new Intent(Home.this, AddMessage.class));
                                intent.putExtras(extras);

                                // Go to AddMessage
                                startActivityForResult(intent, EDIT_MESSAGE,
                                        ActivityOptions.makeSceneTransitionAnimation(Home.this).toBundle());
                            }
                        })
        );
        Log.i(TAG, "setUpRecyclerView: Successfully set up recycler view");
    }

    /**Initializes the Recycler Adapter*/
    private void initializeRecyclerAdapter() {
       mAdapter = new RecyclerAdapter(messages);
       mRecyclerView.setAdapter(mAdapter);

        updateRecyclerEmptyState();
        Log.i(TAG, "initializeRecyclerAdapter: Successfully updated recycler view adapter");
    }

    /**Removes a ghost recycler row if needed*/
    private void updateRecyclerEmptyState() {
        if (messages.isEmpty()) {
            mRecyclerEmptyState.setX(0);
            YoYo.with(Techniques.FadeIn)
                    .duration(screenFadeDuration)
                    .playOn(mRecyclerEmptyState);
        } else {
            // Moves view out of way. If turned off, creates weird bug on swipe
            mRecyclerEmptyState.setX(offScreenRecyclerDistance);
        }
    }

    /**Swipe to delete function*/
    ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
            new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                Canvas c;
                RecyclerView recyclerView;
                RecyclerView.ViewHolder viewHolder;
                int actionState;
                boolean isCurrentlyActive;

                @Override
                public boolean onMove(RecyclerView recyclerView,
                                      RecyclerView.ViewHolder viewHolder,
                                      RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) {
                    // Remove values from dataset and store them in temp values
                    final int position = viewHolder.getAdapterPosition();
                    final Message swipedMessage = removeFromDataset(position);
                    if (swipedMessage == null) throw new NullPointerException("swipedMessage should not be null");
                    final int swipedAlarm = swipedMessage.getAlarmNumber();
                    mAdapter.notifyItemRemoved(position);

                    // Solves ghost issue and insert empty state
                    updateRecyclerEmptyState();

                    // Archive and Delete alarm
                    if (messages.getAlarmIndex(swipedAlarm) == -1) {
                        archiveAndDeleteAlarm(swipedAlarm);
                    }

                    // Makes snackbar with undo button
                    Snackbar.make(parentView,"1 "+ getString(R.string.Home_Notifications_archived), Snackbar.LENGTH_LONG).setAction(R.string.Home_Notifications_Undo, new View.OnClickListener() {
                        // When Undo button is pressed
                        @Override
                        public void onClick(View v) {
                            // Add back temp values
                            messages.add(position, swipedMessage);

                            // Add back alarm
                            new MessageAlarmReceiver().createAlarm(Home.this, swipedMessage);

                            // Add to sql
                            SQLUtilities.addDataToSQL(Home.this, swipedMessage);

                            // Remove zero state screen
                            updateRecyclerEmptyState();

                            // Re-add to adapter
                            setRecyclerStateToDefault();
                            mAdapter.notifyItemInserted(position);
                            mRecyclerView.scrollToPosition(position);
                            setRecyclerStateToDefault();
                        }
                    }).show();
                }

                /** Returns selected view to default position*/
                private void setRecyclerStateToDefault() {
                    getDefaultUIUtil().onDraw(c, recyclerView, ((RecyclerAdapter.ViewHolder) viewHolder).getSwipableView(), 0, 0, actionState, isCurrentlyActive);
                }

                @Override
                public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                    if (viewHolder instanceof RecyclerAdapter.ViewHolder) {
                        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                        return makeMovementFlags(0, swipeFlags);
                    } else
                        return 0;
                }

                @Override
                public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                    if (viewHolder != null) {
                        getDefaultUIUtil().onSelected(((RecyclerAdapter.ViewHolder) viewHolder).getSwipableView());
                        this.viewHolder = viewHolder;
                    }
                }

                public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                    this.c =c;
                    this.recyclerView = recyclerView;
                    this.viewHolder = viewHolder;
                    this.actionState = actionState;
                    this.isCurrentlyActive = isCurrentlyActive;
                    getDefaultUIUtil().onDraw(c, recyclerView, ((RecyclerAdapter.ViewHolder) viewHolder).getSwipableView(), dX, dY,    actionState, isCurrentlyActive);
                }

                public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                    getDefaultUIUtil().onDrawOver(c, recyclerView, ((RecyclerAdapter.ViewHolder) viewHolder).getSwipableView(), dX, dY,    actionState, isCurrentlyActive);
                }
            };

    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);


    //=============== Other ===============//
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.Home_action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //=============== Return from AddMessage ===============//
    @Override
    public void onRestart() {
        super.onRestart();
        //When BACK BUTTON is pressed, the activity on the stack is restarted
        //Do what you want on the refresh procedure here
        //dbRetrieveContactData();
    }
    /** Retrieves results on return from AddMessage and creates animations*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        displayErrorMessageIfDeviceCannotSendMessages();
        Log.i(TAG, "onActivityResult: resultCode = " + resultCode);

        if (resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            int oldAlarmNumber = extras.getInt("OLD_ALARM");
            int newPosition = messages.getAlarmIndex(extras.getInt("ALARM_EXTRA"));

            // Remove adapter position and db entry if edit message
            if (requestCode == EDIT_MESSAGE) cancelAndDeleteAlarm(oldAlarmNumber);

            updateAllMessagesAndRecyclerAdapter();

            // do recyclerview animations
            if (requestCode == EDIT_MESSAGE) mAdapter.notifyItemChanged(newPosition);
            if (requestCode == NEW_MESSAGE) mAdapter.notifyItemInserted(newPosition);
            mRecyclerView.scrollToPosition(newPosition);
        }
    }

    private void updateAllMessagesAndRecyclerAdapter() {
        // Update all items from db.
        populateDatasets();
        // Remove empty state
        initializeRecyclerAdapter();
    }

    /**Cancel alarm in system and delete alarm from DB*/
    private void cancelAndDeleteAlarm(int oldAlarmNumber) {
        cancelAlarmInAndroidSystem(oldAlarmNumber);
        SQLUtilities.deleteAlarmFromDB(Home.this, oldAlarmNumber);
    }

    /**Cancel alrm in system and Archive DB entry*/
    private void archiveAndDeleteAlarm(int alarmNumber) {
        cancelAlarmInAndroidSystem(alarmNumber);
        SQLUtilities.setAsArchived(Home.this, alarmNumber);
    }

    private void displayErrorMessageIfDeviceCannotSendMessages() {
        PackageManager pm = this.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
                !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA)) {
            Toast.makeText(this, R.string.Home_Notifications_CantSendMessages, Toast.LENGTH_SHORT).show();
        }
    }

    /** Removes given position from dataset*/
    private Message removeFromDataset(int position) {
        if (position >= 0) {
            // Remove from dataset
            Message returnMsg = messages.remove(position);
            // Show empty state if necessary
            if (messages.isEmpty()) {
                initializeRecyclerAdapter();
            }
            return returnMsg;
        }
        return null;
    }

    //=============== Broadcast Receiver for Sent Message ===============//
    /**Broadcast receiver that receives broadcast on message send to delete message in adapter*/
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String notificationMessage = intent.getStringExtra("notificationMessage");
            int alarmNumber = intent.getIntExtra("alarmNumber", -1);
            int sentMessageDatasetPosition = messages.getAlarmIndex(alarmNumber);
            if(sentMessageDatasetPosition != -1) {
                removeFromDataset(sentMessageDatasetPosition);
                mAdapter.notifyItemRemoved(sentMessageDatasetPosition);
            }
            createSnackbar(notificationMessage);
        }
    };

    private void createSnackbar(String messsageToShow) {
        Snackbar snackbar = Snackbar.make(parentView, messsageToShow, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
}


