package ru.ith.android.flocal.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ListView;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.engine.PostListAdapter;
import ru.ith.lib.flocal.data.FLThreadHeader;

public class PostListActivity extends ForumActivity {

    public static final String KEY_THREAD = "thread";
    public static final String KEY_THREAD_NAME = "threadName";
    public static final String KEY_THREAD_SRC = "threadSrc";
    private PostListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);
        Intent intent = getIntent();

        long threadID = intent.getLongExtra(KEY_THREAD, -1);
        String threadName = intent.getStringExtra(KEY_THREAD_NAME);
        String threadSrc =  intent.getStringExtra(KEY_THREAD_SRC);
        FLThreadHeader readThread = new FLThreadHeader(threadName, null, 0, 0, threadID, false, threadSrc);
        setTitle(threadName); //TODO: set scrollable
        ListView postList = (ListView) findViewById(R.id.postListView);
        adapter = new PostListAdapter(readThread, this);
        postList.setAdapter(adapter);
        refresh();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    void refresh() {

    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopAppending();
    }
}
