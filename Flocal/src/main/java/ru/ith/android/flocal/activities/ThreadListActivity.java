package ru.ith.android.flocal.activities;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

import ru.ith.android.flocal.R;

public class ThreadListActivity extends ForumActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_list);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.thread_list, menu);
        return true;
    }

    @Override
    void refresh() {

    }

}
