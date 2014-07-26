package ru.ith.android.flocal.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;

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
        //TODO: fetch board name from fetched thread list, since it may not be set in the intent
        board = new FLBoard(boardName, boardURI, false, boardSrc);

        {
            setTitle(boardName);
            new AllBoardListLoader(board).execute();
        }

        ListView threadList = (ListView) findViewById(R.id.threadListView);
        adapter = new ThreadListAdapter(board, this);
        threadList.setAdapter(adapter);
        threadList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                FLThreadHeader selectedThread = (FLThreadHeader) adapterView.getItemAtPosition(i);
                Intent intent = new Intent(ThreadListActivity.this, PostListActivity.class);
                intent.putExtra(PostListActivity.KEY_THREAD, String.valueOf(selectedThread.getID()));
                intent.putExtra(PostListActivity.KEY_THREAD_UNREAD, String.valueOf(selectedThread.getUnreadID()));
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

    class AllBoardListLoader extends AsyncTask<Void, Void, FLBoard[]> {
        private final FLBoard currentBoard;
        int selectedItem = 0;

        AllBoardListLoader(FLBoard currentBoard) {
            this.currentBoard = currentBoard;
        }

        @Override
        protected FLBoard[] doInBackground(Void... params) {
            FLBoard[] result = new FLBoard[0];
            try {
                //TODO: cache such request. Also provide _entire_ board list, not just visible ones
                result = FLDataLoader.listBoards(SessionContainer.getSessionInstance()).toArray(result);
            } catch (FLException e) {
                return null;
            }
            for (FLBoard board : result) {
                if (board.equals(currentBoard)) {
                    break;
                }
                selectedItem++;
            }
            if (selectedItem == result.length)
                selectedItem = 0;
            return result;
        }

        @Override
        protected void onPostExecute(final FLBoard[] flBoards) {
            super.onPostExecute(flBoards);
            if (flBoards == null)
                return; //Failed to load boards list. Possibly notify user?
            ActionBar actions = getActionBar();
            //setting actionbar board selection
            SpinnerAdapter adapter = new ArrayAdapter(actions.getThemedContext(), android.R.layout.simple_spinner_dropdown_item, flBoards);
            ActionBar.OnNavigationListener callback = new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int position, final long id) {
                    final FLBoard toOpen = flBoards[(int) id];
                    if (toOpen.equals(currentBoard))
                        return false;
                    ThreadListActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ThreadListActivity.open(ThreadListActivity.this, toOpen);
                        }
                    });
                    return true;
                }
            };

            actions.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actions.setDisplayShowTitleEnabled(false);
            actions.setListNavigationCallbacks(adapter, callback);

            actions.setSelectedNavigationItem(selectedItem);
        }
    }

    public static void open(Activity context, FLBoard flBoard) {
        Intent intent = new Intent(context, ThreadListActivity.class);
        intent.putExtra(ThreadListActivity.KEY_BOARD, flBoard.boardURIName);
        intent.putExtra(ThreadListActivity.KEY_BOARD_NAME, flBoard.boardName);
        intent.putExtra(ThreadListActivity.KEY_BOARD_SRC, flBoard.src);
        context.startActivity(intent);
    }
}