package ru.ith.android.flocal.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.LinkedList;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.engine.SessionContainer;
import ru.ith.android.flocal.engine.ThreadListAdapter;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.FLException;
import ru.ith.lib.flocal.data.FLBoard;
import ru.ith.lib.flocal.data.FLThreadHeader;

public class ThreadListActivity extends ForumActivity {

    public static final String KEY_BOARD = "board";
    public static final String KEY_BOARD_NAME = "boardName";
    public static final String KEY_BOARD_SRC = "boardSRC";
    private ThreadListAdapter adapter = null;
    private FLBoard board = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_list);
        Intent intent = getIntent();

        String boardURI = intent.getStringExtra(KEY_BOARD);
        String boardName = intent.getStringExtra(KEY_BOARD_NAME);
        String boardSrc = intent.getStringExtra(KEY_BOARD_SRC);
        board = new FLBoard(boardName, boardURI, false, boardSrc);
        setTitle(boardName);
        ListView threadList = (ListView) findViewById(R.id.threadListView);
        adapter = new ThreadListAdapter(board, this);
        threadList.setAdapter(adapter);
        threadList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                FLThreadHeader selectedThread = (FLThreadHeader) adapterView.getItemAtPosition(i);
                Intent intent = new Intent(ThreadListActivity.this, PostListActivity.class);
                intent.putExtra(PostListActivity.KEY_THREAD, selectedThread.getID());
                intent.putExtra(PostListActivity.KEY_THREAD_UNREAD, selectedThread.getUnreadID());
                intent.putExtra(PostListActivity.KEY_THREAD_NAME, selectedThread.getName());
                intent.putExtra(PostListActivity.KEY_THREAD_SRC, selectedThread.src);
                startActivity(intent);
            }
        });

    }

    @Override
    void refresh() {
        LinkedList<FLThreadHeader> firstPage = null;
        try {
            firstPage = FLDataLoader.listThreads(SessionContainer.getSessionInstance(), board, 0);
            adapter.applyUpdate(firstPage);
        } catch (FLException e) {
            //TODO: log
        }
    }

    @Override
    long getRefreshPeriod() {
        return 10000;
    }

}
