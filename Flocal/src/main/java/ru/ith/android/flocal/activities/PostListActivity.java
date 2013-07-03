package ru.ith.android.flocal.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import ru.ith.android.flocal.engine.MessageProcessor;
import ru.ith.android.flocal.io.ImageFactory;
import ru.ith.android.flocal.views.OverscrollableList;
import ru.ith.android.flocal.R;
import ru.ith.android.flocal.engine.PostListAdapter;
import ru.ith.android.flocal.views.overScrollListener;
import ru.ith.lib.flocal.data.FLThreadHeader;

public class PostListActivity extends ForumActivity {

    public static final String KEY_THREAD = "thread";
    public static final String KEY_THREAD_UNREAD = "threadUnreadID";
    public static final String KEY_THREAD_NAME = "threadName";
    public static final String KEY_THREAD_SRC = "threadSrc";
    private PostListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);
        Intent intent = getIntent();

        long threadID = intent.getLongExtra(KEY_THREAD, -1);
        long threadUnreadID = intent.getLongExtra(KEY_THREAD_UNREAD, -1);
        String threadName = intent.getStringExtra(KEY_THREAD_NAME);
        String threadSrc =  intent.getStringExtra(KEY_THREAD_SRC);
        FLThreadHeader readThread = new FLThreadHeader(threadName, null, 0, 0, threadID, threadUnreadID, false, threadSrc);
        setTitle(threadName); //TODO: set scrollable
        OverscrollableList postList = (OverscrollableList) findViewById(R.id.postListView);
        adapter = new PostListAdapter(readThread, this, postList, new ImageFactory(this), MessageProcessor.instance);
        postList.setAdapter(adapter);
        if (threadUnreadID>=0)
            postList.setOverScrollListener(new overScrollListener() {
                @Override
                public void overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean touchEvent) {
                    if (deltaY<0){
                        adapter.upOverScroll();
                    }
                }
            });
    }

    @Override
    void refresh() {
        adapter.refresh();
    }

    @Override
    long getRefreshPeriod() {
        return 10000;
    }
}
