package ru.ith.android.flocal.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.engine.PostListAdapter;
import ru.ith.android.flocal.io.ImageFactory;
import ru.ith.android.flocal.views.OverScrollableList;
import ru.ith.android.flocal.views.PostView;
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
        String threadSrc = intent.getStringExtra(KEY_THREAD_SRC);
        FLThreadHeader readThread = new FLThreadHeader(threadName, null, 0, 0, threadID, threadUnreadID, false, threadSrc);

        {
            setTitle(threadName); //TODO: set scrollable
        }
        final OverScrollableList postList = (OverScrollableList) findViewById(R.id.postListView);
//        postList.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS); //to make post items receive click items
        adapter = new PostListAdapter(readThread, this, postList, new ImageFactory(this));
        postList.setAdapter(adapter);
        if (threadUnreadID >= 0)
            postList.setOverScrollListener(new overScrollListener() {
                @Override
                public void overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean touchEvent) {
                    if (deltaY < 0) {
                        adapter.upOverScroll();
                    }
                }
            });
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
    private final static int MENU_SHARE_TEXT = 31;
    private final static int MENU_SHARE_LINK = 32;
    private final static int MENU_MINIMIZE_POST = 4;

    private AdapterView.AdapterContextMenuInfo mainLevelMenuInfo = null;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        FLMessage message = adapter.getMessage(info.position);
        if (message == null)
            return;
        PostView messageView = (PostView) info.targetView;

        menu.add(0, MENU_REPLY, 0, R.string.post_option_reply);
        menu.add(0, MENU_EDIT, 1, R.string.post_option_edit);

        Menu shareMenu = menu.addSubMenu(0, MENU_SHARE, 2, R.string.post_option_share);
        shareMenu.add(0, MENU_SHARE_TEXT, 0, R.string.post_option_share_text);
        shareMenu.add(0, MENU_SHARE_LINK, 1, R.string.post_option_share_link);

        if (messageView.isCollapsable())
            menu.add(0, MENU_MINIMIZE_POST, 3, R.string.post_option_minimize);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (acmi == null)//http://code.google.com/p/android/issues/detail?id=7139*
            acmi = mainLevelMenuInfo;
        PostView messageView = (PostView) acmi.targetView;
        switch (item.getItemId()) {
            case MENU_REPLY:
                messageView.showReplyWindow();
                return true;
            case MENU_EDIT:
                notify("Not implemented yet");
                return true;
            case MENU_SHARE:
                mainLevelMenuInfo = acmi;
                break;
            case MENU_SHARE_LINK:
                shareText(messageView.getMessageUrl());
                return true;
            case MENU_SHARE_TEXT:
                shareText(messageView.getMessageText());
                return true;
            case MENU_MINIMIZE_POST:
                messageView.toggleExpansion();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void shareText(String text) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    public void dataLoaded() {
        hideLoadingProgressBar();
    }
}
