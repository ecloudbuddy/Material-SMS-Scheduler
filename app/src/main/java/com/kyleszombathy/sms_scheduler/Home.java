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
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Objects;

import jp.wasabeef.recyclerview.animators.SlideInDownAnimator;

public class Home extends Activity {
    private static final String TAG = "HOME";
    private static final String ALARM_EXTRA = "alarmNumber";
    private static final String EDIT_MESSAGE_EXTRA = "EDIT_MESSAGE";
    private final int circleImageViewWidth = 56;
    private SwipeRefreshLayout swipeContainer;
    // Recyclerview adapter dataset
    private RelativeLayout mRecyclerEmptyState;
    private RecyclerView mRecyclerView;
    public RecyclerAdapter mAdapter;
    public RecyclerView.LayoutManager mLayoutManager;
    public ArrayList<String> nameDataset = new ArrayList<>();
    public ArrayList<String> messageContentDataset = new ArrayList<>();
    public ArrayList<String> dateDataset = new ArrayList<>();
    public ArrayList<String> timeDataSet = new ArrayList<>();
    public ArrayList<Integer> alarmNumberDataset = new ArrayList<>();
    public ArrayList<String> uriDataset = new ArrayList<>();
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

        // Setting up transitions
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Fade());

        setContentView(R.layout.activity_home);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);

        readFromSQLDatabase();
        setUpRecyclerView();

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
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

    /** Returns random int between min and max*/
    private int getRandomInt(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    //=============== SQL Retrieval ===============//
    /**Retrieves values from sql Database*/
    private void readFromSQLDatabase() {
        nameDataset.clear();
        messageContentDataset.clear();
        dateDataset.clear();
        timeDataSet.clear();
        alarmNumberDataset.clear();
        uriDataset.clear();
        photoDataset.clear();

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
            MessageContract.MessageEntry.ALARM_NUMBER,
            MessageContract.MessageEntry.PHOTO_URI
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
                sortOrder                                 // The sort order
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
            ArrayList<String> tempUri = Tools.parseString(cursor.getString(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.PHOTO_URI)));

            if (tempUri.size() == 1 && !Objects.equals(tempUri.get(0).trim(), "null")) {
                uriDataset.add(tempUri.get(0).trim());
            } else {
                uriDataset.add(null);
            }
            // Get date in a format for viewing by user
            setFullDateAndTime(year, month, day, hour, minute);
            // Move to next row
            cursor.moveToNext();
        }
        // Close everything so android doesn't complain
        cursor.close();
        db.close();
        mDbHelper.close();

        // Get bitmaps
        getPhotoBytes();
    }

    /**Retreieves bitmap from database*/
    private void getPhotoBytes() {
        for (int i = 0; i < uriDataset.size(); i++) {
            if (uriDataset.get(i)!= null) {
                // Get byte array
                byte[] byteArray = Tools.getPhotoValuesFromSQL(Home.this, uriDataset.get(i));
                // Convert to bitmap and drawable
                ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(byteArray);
                Bitmap bitmap = BitmapFactory.decodeStream(arrayInputStream);
                photoDataset.add(bitmap);
            } else {
                // Get first letter
                Character nameFirstLetter = nameDataset.get(i).charAt(0);
                // Get color
                ColorGenerator generator = ColorGenerator.MATERIAL;
                int color = generator.getColor(nameDataset.get(i));
                TextDrawable drawable = TextDrawable.builder()
                        .beginConfig()
                        .useFont(Typeface.DEFAULT_BOLD)
                        .fontSize(60)
                        .height(circleImageViewWidth * 2)
                        .width(circleImageViewWidth * 2)
                        .endConfig()
                        .buildRound(nameFirstLetter.toString().toUpperCase(), color);
                Bitmap bitmap = Tools.drawableToBitmap(drawable);
                photoDataset.add(bitmap);
            }
        }
    }

    /**Extracts name from given string */
    private void extractName(String names) {
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
        nameDataset.add(nameCondensedString.trim());
    }

    /**Adds date and time string as relative time to now to database*/
    private void setFullDateAndTime(int year, int month, int day, int hour, int minute) {
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

    /**Cancels alarm given alarm service number
     * @param alarmNumb alarmNumber to delete*/
    private void cancelAlarm(int alarmNumb) {
        Intent intent = new Intent(this, MessageAlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(this, alarmNumb, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(sender);
        Log.i(TAG, "Alarm Canceled");
    }


    //=============== Recyclerview edit and delete ===============//
    /**Sets up recycler view and adapter*/
    private void setUpRecyclerView() {
        // Empty state
        mRecyclerEmptyState = (RelativeLayout) findViewById(R.id.recycler_empty_state);
        // Setting up RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
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
    }

    /**Initializes the Recycler Adapter*/
    private void updateRecyclerViewAdapter() {
        mAdapter = new RecyclerAdapter(
                nameDataset, messageContentDataset, dateDataset, timeDataSet, uriDataset, photoDataset);
        mRecyclerView.setAdapter(mAdapter);

        enableDisableEmptyStateIfNeeded();
    }

    private void enableDisableEmptyStateIfNeeded() {
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
                    final String TEMP_DATE = dateDataset.remove(position);
                    final String TEMP_TIME = timeDataSet.remove(position);
                    final Integer TEMP_ALARM = alarmNumberDataset.remove(position);
                    final String TEMP_URI = uriDataset.remove(position);
                    final Bitmap TEMP_PHOTO = photoDataset.remove(position);
                    mAdapter.notifyItemRemoved(position);

                    // Solves ghost issue and insert empty state
                    if (nameDataset.size() == 0) {
                        updateRecyclerViewAdapter();
                    }

                    // Deletes alarm and sets as archived
                    if (alarmNumberDataset.indexOf(TEMP_ALARM) == -1) {
                        cancelAlarm(TEMP_ALARM);
                        Tools.setAsArchived(Home.this, TEMP_ALARM);
                    }
                    // Update Recyclerview
                    returnToDefaultPosition();

                    // Makes snackbar with undo button
                    Snackbar.make(findViewById(R.id.coordLayout),
                            "1 "+ getString(R.string.archived),
                            Snackbar.LENGTH_LONG).setAction("Undo", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Add back temp values
                            nameDataset.add(position, TEMP_NAME);
                            messageContentDataset.add(position, TEMP_CONTENT);
                            dateDataset.add(position, TEMP_DATE);
                            timeDataSet.add(position, TEMP_TIME);
                            alarmNumberDataset.add(position, TEMP_ALARM);
                            uriDataset.add(position, TEMP_URI);
                            photoDataset.add(position, TEMP_PHOTO);
                            // Remove empty state
                            enableDisableEmptyStateIfNeeded();
                            // Return to default position
                            returnToDefaultPosition();
                            //mAdapter.notifyItemRangeChanged(position,nameDataset.size());
                            mAdapter.notifyItemInserted(position);
                            mRecyclerView.scrollToPosition(position);
                            returnToDefaultPosition();
                        }
                    }).show();
                }

                /** Returns selected view to default position*/
                private void returnToDefaultPosition() {
                    getDefaultUIUtil().onDraw(c, recyclerView, ((RecyclerAdapter.ViewHolder) viewHolder).getSwipableView(), 0, 0,    actionState, isCurrentlyActive);
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
    /**Showcase on first run giving a tutorial*/
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
    /** Retrieves results on return from AddMessage and creates animations*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check if able to send sms
        PackageManager pm = this.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
                !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA)) {
            Toast.makeText(this, R.string.home_cantSendMessages, Toast.LENGTH_SHORT).show();
        }

        if (resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();

            // Delete old stuff
            if (requestCode == EDIT_MESSAGE) {
                cancelAlarm(oldAlarmNumber);
                Tools.deleteAlarmNumberFromDatabase(Home.this, oldAlarmNumber);
            }

            // Update all items from db
            readFromSQLDatabase();

            // Get new alarm number position
            int position = alarmNumberDataset.indexOf(extras.getInt("ALARM_EXTRA"));

            // Remove empty state
            enableDisableEmptyStateIfNeeded();

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
        dateDataset.remove(position);
        timeDataSet.remove(position);
        alarmNumberDataset.remove(position);
        uriDataset.remove(position);
        photoDataset.remove(position);
        // Show empty state if necessary
        if (nameDataset.isEmpty()) {
            updateRecyclerViewAdapter();
        }
    }

    //=============== Broadcast Receiver ===============//
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
                    .make(findViewById(R.id.coordLayout), notificationMessage, Snackbar.LENGTH_LONG);
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


