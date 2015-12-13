package com.kyleszombathy.sms_scheduler;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.transition.Fade;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toolbar;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.marshalchen.ultimaterecyclerview.UltimateRecyclerView;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Random;

public class Home extends Activity {
/*    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;*/

    private static final int PICK_CONTACT_REQUEST = 1;
    private static final String EXTRA_MESSAGE = "alarmNumber";

    private UltimateRecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<String> nameDataset = new ArrayList<>();
    private ArrayList<String> messageContentDataset = new ArrayList<>();
    private ArrayList<String> dateDataset = new ArrayList<>();
    private ArrayList<String> timeDataSet = new ArrayList<>();
    private int alarmNumber;
    private final int MAX_INT = Integer.MAX_VALUE ;
    private final int MIN_INT = 1;
    Random rand;
    private boolean multipleRecipients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Random alarm number
        alarmNumber = MIN_INT + (int)(Math.random() * ((MAX_INT - MIN_INT) + 1));

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
        
        // Floating action button start activity
        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(new Intent(Home.this, AddMessage.class));
                intent.putExtra(EXTRA_MESSAGE, alarmNumber);

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(Home.this);
                stackBuilder.addParentStack(AddMessage.class);
                stackBuilder.addNextIntent(intent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                startActivity(intent,
                        ActivityOptions.makeSceneTransitionAnimation(Home.this).toBundle());
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void readFromSQLDatabase() {
        MessageDbHelper mDbHelper = new MessageDbHelper(Home.this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
            MessageContract.MessageEntry.COLUMN_NAME_NAME,
            MessageContract.MessageEntry.COLUMN_NAME_MESSAGE,
            MessageContract.MessageEntry.COLUMN_NAME_YEAR,
            MessageContract.MessageEntry.COLUMN_NAME_MONTH,
            MessageContract.MessageEntry.COLUMN_NAME_DAY,
            MessageContract.MessageEntry.COLUMN_NAME_HOUR,
            MessageContract.MessageEntry.COLUMN_NAME_MINUTE
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                MessageContract.MessageEntry.COLUMN_NAME_NAME + " DESC";

        Cursor cursor = db.query(
                MessageContract.MessageEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                     // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        // Moves to first row
        cursor.moveToFirst();

        for (int i = 0; i < cursor.getCount(); i++) {
            extractName(cursor.getString(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.COLUMN_NAME_NAME)));
            int year = cursor.getInt(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.COLUMN_NAME_YEAR));
            int month = cursor.getInt(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.COLUMN_NAME_MONTH));
            int day = cursor.getInt(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.COLUMN_NAME_DAY));
            int hour = cursor.getInt(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.COLUMN_NAME_HOUR));
            int minute = cursor.getInt(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.COLUMN_NAME_MINUTE));
            messageContentDataset.add(cursor.getString(cursor.getColumnIndexOrThrow
                    (MessageContract.MessageEntry.COLUMN_NAME_MESSAGE)));

            setFullDateAndTime(year, month, day, hour, minute);
            cursor.moveToNext();
        }

        cursor.close();
        mDbHelper.close();
    }

    private void extractName(String names) {
        String nameCondensedString = "";
        ArrayList<String> nameList = new ArrayList<>();
        names = names.replace("[", "");
        names = names.replace("]", "");

        if(names.contains(",")) {
            multipleRecipients = true;
            for (String name: names.split(",")) {
                nameList.add(name);
            }
            nameCondensedString = nameList.remove(0) + ", " + nameList.remove(0);
        } else {
            nameCondensedString = names;
        }

        int nameListSize = nameList.size();
        if (nameListSize > 0) {
            nameCondensedString += " +" + (nameListSize);
        }
        nameDataset.add(nameCondensedString);
    }

    public void setFullDateAndTime(int year, int month, int day, int hour, int minute) {
        //Calendar date = new GregorianCalendar(year, month, day, hour, minute);
        GregorianCalendar date = new GregorianCalendar(year, month, day, hour, minute);

        String dateString = DateFormat.getDateFormat(getApplicationContext()).format(date.getTime());
        String timeString = DateFormat.getTimeFormat(getApplicationContext()).format(date.getTime());

        // Delete "20" in year
        StringBuilder sb = new StringBuilder(dateString);
        sb.deleteCharAt(sb.length() - 3);
        sb.deleteCharAt(sb.length() - 3);
        dateString = sb.toString();


        dateDataset.add(dateString);
        timeDataSet.add(timeString);
    }

    private void setUpRecyclerView() {
        // Setting up RecyclerView
        mRecyclerView = (UltimateRecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new HomeRecyclerAdapter(
                nameDataset, messageContentDataset, dateDataset, timeDataSet);
        mRecyclerView.setAdapter(mAdapter);

    }

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
    public void onRestart() {
        super.onRestart();
        //When BACK BUTTON is pressed, the activity on the stack is restarted
        //Do what you want on the refresh procedure here
        finish();
        startActivity(getIntent());
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
}


