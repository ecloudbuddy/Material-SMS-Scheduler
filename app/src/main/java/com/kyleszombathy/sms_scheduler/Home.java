package com.kyleszombathy.sms_scheduler;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Fade;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toolbar;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

public class Home extends Activity {
/*    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;*/

    private static final int PICK_CONTACT_REQUEST = 1;
    private static final String EXTRA_MESSAGE = "imageUri";

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private String[] recyclerDataset = {};

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

        // Setting up RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new HomeRecyclerAdapter(recyclerDataset);
        mRecyclerView.setAdapter(mAdapter);


        // Floating action button start activity
        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Home.this, AddMessage.class),
                        ActivityOptions.makeSceneTransitionAnimation(Home.this).toBundle());
            }
        });

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


