package com.kyleszombathy.sms_scheduler;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.format.DateUtils;
import android.transition.Fade;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toolbar;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

public class Home extends Activity {
    private static final String ALARM_EXTRA = "alarmNumber";
    private static final String EDIT_MESSAGE_EXTRA = "EDIT_MESSAGE";
    private SwipeRefreshLayout swipeContainer;
    // Recyclerview adapter dataset
    private RecyclerView mRecyclerView;
    public RecyclerView.Adapter mAdapter;
    public RecyclerView.LayoutManager mLayoutManager;
    public ArrayList<String> nameDataset = new ArrayList<>();
    public ArrayList<String> messageContentDataset = new ArrayList<>();
    public ArrayList<String> dateDataset = new ArrayList<>();
    public ArrayList<String> timeDataSet = new ArrayList<>();
    public ArrayList<Integer> alarmNumberDataset = new ArrayList<>();
    // For random number retrieval
    private final int MAX_INT = Integer.MAX_VALUE ;
    private final int MIN_INT = 1;
    // For edit message function
    private static final int NEW_MESSAGE = 1;
    private static final int EDIT_MESSAGE = 0;
    private int tempSelectedPosition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setting up transitions
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Fade());

        setContentView(R.layout.activity_home);
        ObjectAnimator mAnimator;
        //mAnimator = ObjectAnimator.ofFloat(Home.this, View.X, View.Y, path);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);

        // Showcase TODO
        //showcase();

        readFromSQLDatabase();
        setUpRecyclerView();

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override public void run() {
                        readFromSQLDatabase();
                        mAdapter.notifyDataSetChanged();
                        swipeContainer.setRefreshing(false);
                    }
                }, 1000);
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("custom-event-name"));


        // Floating action button start activity
        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
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

                startActivityForResult(intent, NEW_MESSAGE,
                        ActivityOptions.makeSceneTransitionAnimation(Home.this).toBundle());
            }
        });
    }


    private int getRandomInt(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    //=============== SQL Retrieval ===============//
    private void readFromSQLDatabase() {
        nameDataset.clear();
        messageContentDataset.clear();
        dateDataset.clear();
        timeDataSet.clear();
        alarmNumberDataset.clear();

        MessageDbHelper mDbHelper = new MessageDbHelper(Home.this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
            MessageContract.MessageEntry.NAME,
            MessageContract.MessageEntry.MESSAGE,
            MessageContract.MessageEntry.YEAR,
            MessageContract.MessageEntry.MONTH,
            MessageContract.MessageEntry.DAY,
            MessageContract.MessageEntry.HOUR,
            MessageContract.MessageEntry.MINUTE,
            MessageContract.MessageEntry.ALARM_NUMBER
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                MessageContract.MessageEntry.DATETIME+ " ASC";
        String selection = MessageContract.MessageEntry.ARCHIVED + " LIKE ?";
        String[] selectionArgs = { String.valueOf(0) };

        Cursor cursor = db.query(
                MessageContract.MessageEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                      // The sort order
        );

        // Moves to first row
        cursor.moveToFirst();

        for (int i = 0; i < cursor.getCount(); i++) {
            extractName(cursor.getString(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.NAME)));
            int year = cursor.getInt(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.YEAR));
            int month = cursor.getInt(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.MONTH));
            int day = cursor.getInt(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.DAY));
            int hour = cursor.getInt(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.HOUR));
            int minute = cursor.getInt(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.MINUTE));
            messageContentDataset.add(cursor.getString(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.MESSAGE)));
            alarmNumberDataset.add(cursor.getInt(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.ALARM_NUMBER)));
            // Get date in a format for viewing by user
            setFullDateAndTime(year, month, day, hour, minute);
            // Move to next row
            cursor.moveToNext();
        }
        // Close everything so android doesn't complain
        cursor.close();
        db.close();
        mDbHelper.close();
    }

    private void extractName(String names) {
        String nameCondensedString;
        ArrayList<String> nameList = new ArrayList<>();
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
        nameDataset.add(nameCondensedString);
    }

    public void setFullDateAndTime(int year, int month, int day, int hour, int minute) {
        //Calendar date = new GregorianCalendar(year, month, day, hour, minute);
        GregorianCalendar date = new GregorianCalendar(year, month, day, hour, minute);
        GregorianCalendar dateNow = new GregorianCalendar();

        long time = date.getTimeInMillis();
        long timeNow = dateNow.getTimeInMillis();
        CharSequence dateString;
        String timeString = "";

        dateString = DateUtils.getRelativeTimeSpanString(time, timeNow, DateUtils.MINUTE_IN_MILLIS);

        dateDataset.add(dateString.toString());
        timeDataSet.add(timeString);
    }

    /**
     * Sets given item as archived in database
     */
    private void setAsArchived(int alarmNumb) {
        MessageDbHelper mDbHelper = new MessageDbHelper(Home.this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(MessageContract.MessageEntry.
                ARCHIVED, 1);

        // Which row to update, based on the ID
        String selection = MessageContract.MessageEntry.ALARM_NUMBER + " LIKE ?";
        String[] selectionArgs = { String.valueOf(alarmNumb) };

        int count = db.update(
                MessageContract.MessageEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        db.close();
        mDbHelper.close();
    }
    /**
     * Removes Item given alarm Number from database
     */
    private void deleteFromDatabase(int alarmNumb) {
        MessageDbHelper mDbHelper = new MessageDbHelper(Home.this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Which row to delete, based on the ID
        String selection = MessageContract.MessageEntry.ALARM_NUMBER + " LIKE ?";
        String[] selectionArgs = { String.valueOf(alarmNumb) };

        db.delete(
                MessageContract.MessageEntry.TABLE_NAME,
                selection,
                selectionArgs);
        db.close();
        mDbHelper.close();
    }

    private void cancelAlarm(int alarmNumb) {
        Intent intent = new Intent(this, MessageAlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(this, alarmNumb, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        alarmManager.cancel(sender);
    }



    //=============== Recyclerview edit and delete ===============//
    private void setUpRecyclerView() {
        // Setting up RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(Home.this,
                DividerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new SlideInLeftAnimator());

        mAdapter = new RecyclerAdapter(
                nameDataset, messageContentDataset, dateDataset, timeDataSet);
        mRecyclerView.setAdapter(mAdapter);


        // Item touch listener for editing message
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(Home.this,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                tempSelectedPosition = position;

                                // Create new intent to AddMessage with data from item in position
                                Intent intent = new Intent(new Intent(Home.this, AddMessage.class));
                                Bundle extras = new Bundle();
                                // Older alarm. For SQL retrieval
                                extras.putInt(ALARM_EXTRA, alarmNumberDataset.get(position));
                                // New alarm number
                                extras.putInt("NEW_ALARM", getRandomInt(MIN_INT, MAX_INT));
                                // Designates that we're editing the message
                                extras.putBoolean(EDIT_MESSAGE_EXTRA, true);
                                intent.putExtras(extras);

                                // Go to addMessage
                                startActivityForResult(intent, EDIT_MESSAGE,
                                        ActivityOptions.makeSceneTransitionAnimation(Home.this).toBundle());
                            }
                        })
        );
    }

    /**
     * Swipe to delete function
     */
    ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
            new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(RecyclerView recyclerView,
                                      RecyclerView.ViewHolder viewHolder,
                                      RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                    // Remove values from dataset and store them in temp values
                    final int position = viewHolder.getAdapterPosition();
                    final String TEMP_NAME = nameDataset.remove(position);
                    final String TEMP_CONTENT = messageContentDataset.remove(position);
                    final String TEMP_DATE = dateDataset.remove(position);
                    final String TEMP_TIME = timeDataSet.remove(position);
                    final Integer TEMP_ALARM = alarmNumberDataset.remove(position);
                    mAdapter.notifyItemRemoved(position);

                    // Makes snackbar with undo button
                    Snackbar.make(findViewById(R.id.coordLayout),
                            "1 "+ getString(R.string.archived),
                            Snackbar.LENGTH_LONG).setCallback( new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            switch(event) {
                                // Case for all events when the dataset needs to be deleted
                                case Snackbar.Callback.DISMISS_EVENT_TIMEOUT:
                                case Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE:
                                case Snackbar.Callback.DISMISS_EVENT_SWIPE:
                                    cancelAlarm(TEMP_ALARM);
                                    setAsArchived(TEMP_ALARM);
                                    // Update Recyclerview
                                    mAdapter.notifyItemRangeChanged(position,nameDataset.size());
                                    break;
                            }
                        }
                        @Override
                        public void onShown(Snackbar snackbar) {
                        }
                    }).setAction("Undo", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Add back temp values
                            nameDataset.add(position, TEMP_NAME);
                            messageContentDataset.add(position, TEMP_CONTENT);
                            dateDataset.add(position, TEMP_DATE);
                            timeDataSet.add(position, TEMP_TIME);
                            alarmNumberDataset.add(position, TEMP_ALARM);
                            // UpdateRecyclerview
                            mAdapter.notifyItemInserted(position);
                            mRecyclerView.scrollToPosition(position);

                        }
                    }).show();
                }
            };
    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);


    //=============== Other ===============//
    private void showcase() {
        Target viewTarget = new ViewTarget(R.id.fab, this);
        new ShowcaseView.Builder(this)
                .withMaterialShowcase()
                .setTarget(viewTarget)
                .setContentTitle("Welcome to SMS Scheduler")
                .setContentText("Click here to add a message")
                .singleShot(42)
                .build();
    }

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
        if (id == R.id.action_settings) {
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
        //readFromSQLDatabase();
    }

    // Receives result from AddMessage
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {

            // Delete old stuff
            if (requestCode == EDIT_MESSAGE) {
                int oldAlarmNumber = alarmNumberDataset.get(tempSelectedPosition);
                cancelAlarm(oldAlarmNumber);
                deleteFromDatabase(oldAlarmNumber);
            }

            // Update all items
            readFromSQLDatabase();
            // Get new alarm number position
            Bundle extras = data.getExtras();
            int position = alarmNumberDataset.indexOf(extras.getInt("ALARM_EXTRA"));


            // Remove previous position if edit message
            // Deisgnates that user did not cancel edit message function
            if (requestCode == EDIT_MESSAGE) {
                mAdapter.notifyItemChanged(position);
                mRecyclerView.scrollToPosition(position);
            }

/*            setFullDateAndTime(
                    extras.getInt("YEAR_EXTRA"),
                    extras.getInt("MONTH_EXTRA"),
                    extras.getInt("DAY_EXTRA"),
                    extras.getInt("HOUR_EXTRA"),
                    extras.getInt("MINUTE_EXTRA"));
            extractName(extras.getString("NAME_EXTRA"));
            messageContentDataset.add(extras.getString("CONTENT_EXTRA"));
            alarmNumberDataset.add(extras.getInt("ALARM_EXTRA"));*/

            if (requestCode == NEW_MESSAGE) {
                mAdapter.notifyItemInserted(position);
                mRecyclerView.scrollToPosition(position);
            }
        }
    }

    private void removeFromDataset(int position) {
        // Remove from dataset
        nameDataset.remove(position);
        messageContentDataset.remove(position);
        dateDataset.remove(position);
        timeDataSet.remove(position);
        alarmNumberDataset.remove(position);
    }

    //=============== Broadcast Receiver ===============//
    /**
     * Broadcast receiver that receives broadcast on message send to update adapter
     * */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int alarmNumber = intent.getIntExtra("alarmNumber", -1);
            setAsArchived(alarmNumber);
            int position = alarmNumberDataset.indexOf(alarmNumber);
            removeFromDataset(position);
            mAdapter.notifyItemRemoved(position);
        }
    };

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
}


