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
        postList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View sender, int position, long id) {
                registerForContextMenu(sender);
                openContextMenu(sender);
                unregisterForContextMenu(sender);
            }
        });
        registerForContextMenu(postList);
    }

    @Override
    void refreshImpl() {
        adapter.refresh();
    }

    @Override
    long getRefreshPeriod() {
        return 10000;
    }

    private final static int MENU_REPLY = 1;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, MENU_REPLY, 0, R.string.post_option_reply);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_REPLY){
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            Toast.makeText(this, "Not implemented yet", Toast.LENGTH_LONG).show();
            //adapter.getItem(acmi.position).toString()
            return true;
        }
        return super.onContextItemSelected(item);
    }
}
