package ru.ith.android.flocal.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.engine.MessageProcessor;
import ru.ith.android.flocal.engine.PostListAdapter;
import ru.ith.android.flocal.io.ImageFactory;
import ru.ith.android.flocal.views.OverscrollableList;
import ru.ith.android.flocal.views.overScrollListener;
import ru.ith.lib.flocal.data.FLMessage;
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

        String threadIDS = intent.getStringExtra(KEY_THREAD);
        if (threadIDS == null)
            threadIDS = "-1";
        //TODO: separate view of thread with unread messages from the direct link to the message
        //TODO: fetch thread name from post list
        String threadUnreadIDS = intent.getStringExtra(KEY_THREAD_UNREAD);
        if (threadUnreadIDS == null)
            threadUnreadIDS = "-1";

        final long threadID = Long.valueOf(threadIDS);
        final long threadUnreadID = Long.valueOf(threadUnreadIDS);
        //TODO: trycatch

        String threadName = intent.getStringExtra(KEY_THREAD_NAME);
        String threadSrc =  intent.getStringExtra(KEY_THREAD_SRC);
        FLThreadHeader readThread = new FLThreadHeader(threadName, null, 0, 0, threadID, threadUnreadID, false, threadSrc);

        {
            setTitle(threadName); //TODO: set scrollable
        }
        final OverscrollableList postList = (OverscrollableList) findViewById(R.id.postListView);
//        postList.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS); //to make post items receive click items
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
        postList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final FLMessage msg = adapter.getMessage(position);
                return true;
            }
        });
//        postList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View sender, int position, long id) {
//                registerForContextMenu(sender);
//                openContextMenu(sender);
//                unregisterForContextMenu(sender);
//            }
//        });
        registerForContextMenu(postList);
    }

    @Override
    void refresh() {
        adapter.refresh();
    }

    @Override
    long getRefreshPeriod() {
        return 10000;
    }

    private final static int MENU_REPLY = 1;
    private final static int MENU_EDIT = 2;
    private final static int MENU_SHARE = 3;
    private final static int MENU_MINIMIZE_POST = 4;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, MENU_REPLY, 0, R.string.post_option_reply);
        menu.add(0, MENU_EDIT, 1, R.string.post_option_edit);
        menu.add(0, MENU_SHARE, 2, R.string.post_option_share);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        //adapter.getItem(acmi.position).toString()
        switch (item.getItemId()) {
            case MENU_REPLY:
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_LONG).show();
                return true;
            case MENU_EDIT:
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_LONG).show();
                return true;
            case MENU_SHARE:
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_LONG).show();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    public void dataLoaded() {
        hideLoadingProgressBar();
    }
}
