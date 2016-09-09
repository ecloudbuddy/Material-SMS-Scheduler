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
import java.util.Objects;

import jp.wasabeef.recyclerview.animators.SlideInDownAnimator;

public class Home extends Activity {
    private static final String TAG = "HOME";
    private static final String ALARM_EXTRA = "alarmNumber";
    private static final String EDIT_MESSAGE_EXTRA = "EDIT_MESSAGE";
    private final int circleImageViewWidth = 56;
    // Recyclerview adapter dataset
    private RelativeLayout mRecyclerEmptyState;
    private RecyclerView mRecyclerView;
    public RecyclerAdapter mAdapter;
    public RecyclerView.LayoutManager mLayoutManager;
    public ArrayList<String> nameDataset = new ArrayList<>();
    public ArrayList<String> messageContentDataset = new ArrayList<>();
    public ArrayList<String> phoneDataset = new ArrayList<>();
    public ArrayList<Calendar> dateTimeDataset = new ArrayList<>();
    public ArrayList<Integer> alarmNumberDataset = new ArrayList<>();
    public ArrayList<Uri> uriDataset = new ArrayList<>();
    public ArrayList<Bitmap> photoDataset= new ArrayList<>();
    // For random number retrieval
    private final int MAX_INT = Integer.MAX_VALUE ;
    private final int MIN_INT = 1;
    // For edit message function
    private static final int NEW_MESSAGE = 1;
    private static final int EDIT_MESSAGE = 0;
    private int tempSelectedPosition;
    private int oldAlarmNumber;

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
            phone = cursor.getString(cursor.getColumnIndexOrThrow
                    (SQLContract.MessageEntry.PHONE));

            // Add retrieved data to datasets
            name = extractName(name);
            dateTime = Tools.getNewCalendarInstance(year, month, day, hour, minute);

            //Extract URI
            uriArrayList = Tools.parseString(uriString.trim());
            if (uriArrayList.size() == 1 && !Objects.equals(uriArrayList.get(0).trim(), "null"))
                uri = Uri.parse(uriArrayList.get(0).trim());
            else uri = null;

            addValuesToDataSet(name, messageContent, phone, dateTime, alarmNumber, uri, null);

            // Move to next row
            cursor.moveToNext();
        }
        cursor.close();
        mDbHelper.close();
        // Get bitmaps
        setContactImage();
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

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                SQLContract.MessageEntry.DATETIME+ " ASC";
        String selection = SQLContract.MessageEntry.ARCHIVED + " LIKE ?";
        String[] selectionArgs = { String.valueOf(0) };

        try {
            cursor =  db.query(
                    SQLContract.MessageEntry.TABLE_NAME,  // The table to query
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

    /** Add values to the end of the dataset*/
    private void addValuesToDataSet(String name, String content, String phone,
                                    Calendar dateTime, Integer alarm,
                                    Uri uri, Bitmap photo) {
        addValuesToDataSet(nameDataset.size(), name, content, phone, dateTime, alarm, uri, photo);
    }

    /** Add values to a @param position in the dataset*/
    private void addValuesToDataSet(int position, String name, String content, String phone,
                                    Calendar dateTime, Integer alarm,
                                    Uri uri, Bitmap photo) {
        nameDataset.add(position, name);
        messageContentDataset.add(position, content);
        phoneDataset.add(position, phone);
        dateTimeDataset.add(position, dateTime);
        alarmNumberDataset.add(position, alarm);
        uriDataset.add(position, uri);
        photoDataset.add(position, photo);
    }

    private void clearDatasets() {
        nameDataset.clear();
        messageContentDataset.clear();
        phoneDataset.clear();
        dateTimeDataset.clear();
        alarmNumberDataset.clear();
        uriDataset.clear();
        photoDataset.clear();
    }

    /**Retreieves bitmap from database*/
    // TODO: Move this off the UI thread
    private void setContactImage() {
        for (int i = 0; i < uriDataset.size(); i++) {
            if (uriDataset.get(i)!= null) {
                photoDataset.set(i, retrieveContactImage(uriDataset.get(i)));
            } else {
                // Set custom contact image based off first letter of contact name
                Character nameFirstLetter = nameDataset.get(i).charAt(0);
                if (Character.isLetter(nameFirstLetter)) {
                    // Get color
                    ColorGenerator generator = ColorGenerator.MATERIAL;
                    int color = generator.getColor(nameDataset.get(i));
                    TextDrawable drawable = TextDrawable.builder(this)
                            .beginConfig()
                            .useFont(Typeface.DEFAULT_BOLD)
                            .fontSize(60)
                            .height(circleImageViewWidth * 2)
                            .width(circleImageViewWidth * 2)
                            .endConfig()
                            .buildRound(nameFirstLetter.toString().toUpperCase(), color);
                    Bitmap bitmap = Tools.drawableToBitmap(drawable);
                    photoDataset.set(i, bitmap);
                    Log.i(TAG, "setContactImage: Created custom image based off first letter: " + nameFirstLetter);
                } else {
                    // TODO: Write code for handling messages to multiple people
                }

            }
        }
    }

    private Bitmap retrieveContactImage(Uri uri) {
        InputStream arrayInputStream =SQLUtilities.getPhoto(Home.this, uri);
        return BitmapFactory.decodeStream(arrayInputStream);
    }

    /**Extracts name from given string */
    private String extractName(String names) {
        String nameCondensedString;
        ArrayList<String> nameList = new ArrayList<>();
        // Trim brackets
        names = names.replace("[", "");
        names = names.replace("]", "");

        if(names.contains(",")) {
            for (String name: names.split(",")) {
                name = name.trim();
                nameList.add(name);
            }
            nameCondensedString = nameList.remove(0) + ", " + nameList.remove(0);
        } else {
            nameCondensedString = names;
        }

        int nameListSize = nameList.size();
        if (nameListSize > 0) {
            nameCondensedString += ", +" + (nameListSize);
        }
        return nameCondensedString.trim();
    }

    public String getFullTime(){
        // Don't know why I left this code
        return "";
    }

    /**Cancels alarm given alarm service number
     * @param alarmNumb alarmNumber to delete*/
    private void cancelAlarm(int alarmNumb) {
        Intent intent = new Intent(this, MessageAlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(this, alarmNumb, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(sender);
        Log.i(TAG, "cancelAlarm: Alarm number " + alarmNumb + " Canceled");
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

        updateRecyclerViewAdapter();

        // Item touch listener for editing message
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(Home.this,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                tempSelectedPosition = position;
                                oldAlarmNumber = alarmNumberDataset.get(position);
                                int newAlarmNumber = getRandomInt(MIN_INT, MAX_INT);

                                // Update Adapter with new alarm number
                                alarmNumberDataset.set(position, newAlarmNumber);

                                // Create new intent to AddMessage with data from item in position
                                Intent intent = new Intent(new Intent(Home.this, AddMessage.class));
                                Bundle extras = new Bundle();
                                // Older alarm. For SQL retrieval
                                extras.putInt(ALARM_EXTRA, oldAlarmNumber);
                                // New alarm number
                                extras.putInt("NEW_ALARM", newAlarmNumber);
                                // Designates that we're editing the message
                                extras.putBoolean(EDIT_MESSAGE_EXTRA, true);
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
    private void updateRecyclerViewAdapter() {
        mAdapter = new RecyclerAdapter(nameDataset, messageContentDataset, dateTimeDataset, photoDataset);
        mRecyclerView.setAdapter(mAdapter);

        updateRecyclerState();
        Log.i(TAG, "updateRecyclerViewAdapter: Successfully updated recycler view adapter");
    }

    /**Removes a ghost recycler row if needed*/
    private void updateRecyclerState() {
        if (nameDataset.isEmpty()) {
            mRecyclerEmptyState.setX(0);
            YoYo.with(Techniques.FadeIn)
                    .duration(700)
                    .playOn(mRecyclerEmptyState);
        } else {
            // Moves view out of way. If turned off, creates weird bug on swipe
            mRecyclerEmptyState.setX(10000);
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
                    final String TEMP_NAME = nameDataset.remove(position);
                    final String TEMP_CONTENT = messageContentDataset.remove(position);
                    final String TEMP_PHONE = phoneDataset.remove(position);
                    final Calendar TEMP_DATE_TIME = dateTimeDataset.remove(position);
                    final Integer TEMP_ALARM = alarmNumberDataset.remove(position);
                    final Uri TEMP_URI = uriDataset.remove(position);
                    final Bitmap TEMP_PHOTO = photoDataset.remove(position);
                    mAdapter.notifyItemRemoved(position);

                    // Solves ghost issue and insert empty state
                    if (nameDataset.size() == 0) {
                        updateRecyclerViewAdapter();
                    }

                    // Deletes alarm and sets as archived
                    if (alarmNumberDataset.indexOf(TEMP_ALARM) == -1) {
                        cancelAlarm(TEMP_ALARM);
                        SQLUtilities.setAsArchived(Home.this, TEMP_ALARM);
                    }

                    // Update Recyclerview
                    setRecyclerStateToDefault();

                    // Makes snackbar with undo button
                    Snackbar.make(findViewById(R.id.Home_coordLayout),"1 "+ getString(R.string.Home_Notifications_archived),
                            Snackbar.LENGTH_LONG).setAction(R.string.Home_Notifications_Undo, new View.OnClickListener() {
                        // When Undo button is pressed
                        @Override
                        public void onClick(View v) {
                            // Add back temp values
                            addValuesToDataSet(position, TEMP_NAME, TEMP_CONTENT, TEMP_PHONE, TEMP_DATE_TIME, TEMP_ALARM, TEMP_URI, TEMP_PHOTO);
                            // Add back alarm
                            new MessageAlarmReceiver().createAlarm(
                                    Home.this,
                                    TEMP_DATE_TIME,
                                    Tools.parseString(TEMP_PHONE),
                                    TEMP_CONTENT,
                                    TEMP_ALARM,
                                    Tools.parseString(TEMP_NAME));
                            // Remove empty state
                            updateRecyclerState();
                            // Return to default position
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
        // Check if able to send sms
        PackageManager pm = this.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
                !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA)) {
            Toast.makeText(this, R.string.Home_Notifications_CantSendMessages, Toast.LENGTH_SHORT).show();
        }

        if (resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();

            // Delete old stuff
            if (requestCode == EDIT_MESSAGE) {
                cancelAlarm(oldAlarmNumber);
                SQLUtilities.deleteAlarmNumberFromDatabase(Home.this, oldAlarmNumber);
            }

            // Update all items from db
            populateDatasets();

            // Get new alarm number position
            int position = alarmNumberDataset.indexOf(extras.getInt("ALARM_EXTRA"));

            // Remove empty state
            updateRecyclerState();

            // Remove previous position if edit message
            // Deisgnates that user did not cancel edit message function
            if (requestCode == EDIT_MESSAGE) {
                mAdapter.notifyItemChanged(position);
                mRecyclerView.scrollToPosition(position);
            }

            if (requestCode == NEW_MESSAGE) {
                mAdapter.notifyItemInserted(position);
                mRecyclerView.scrollToPosition(position);
            }
        } else {
            if (requestCode == EDIT_MESSAGE) {
                // Edit message altered the alarm number. Return it to the old alarm number now
                alarmNumberDataset.set(tempSelectedPosition, oldAlarmNumber);
            }
        }
    }

    /** Removes given position from dataset*/
    private void removeFromDataset(int position) {
        // Remove from dataset
        nameDataset.remove(position);
        messageContentDataset.remove(position);
        phoneDataset.remove(position);
        dateTimeDataset.remove(position);
        alarmNumberDataset.remove(position);
        uriDataset.remove(position);
        photoDataset.remove(position);
        // Show empty state if necessary
        if (nameDataset.isEmpty()) {
            updateRecyclerViewAdapter();
        }
    }

    //=============== Broadcast Receiver for Sent Message ===============//
    /**Broadcast receiver that receives broadcast on message send to delete message in adapter*/
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String notificationMessage = intent.getStringExtra("notificationMessage");
            int alarmNumber = intent.getIntExtra("alarmNumber", -1);
            int position = alarmNumberDataset.indexOf(alarmNumber);
            if(position != -1) {
                removeFromDataset(position);
                mAdapter.notifyItemRemoved(position);
            }
            Snackbar snackbar = Snackbar
                    .make(findViewById(R.id.Home_coordLayout), notificationMessage, Snackbar.LENGTH_LONG);
            snackbar.show();
        }
    };

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
}


