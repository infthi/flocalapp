package ru.ith.android.flocal.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.engine.BoardListLoader;
import ru.ith.android.flocal.engine.SessionContainer;
import ru.ith.lib.flocal.FLException;
import ru.ith.lib.flocal.data.FLBoard;

public class BoardListActivity extends ForumActivity {
    ListView boardList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_list);
		SessionContainer.setPreferences(getPreferences(MODE_PRIVATE));
		refresh();
        boardList = (ListView) findViewById(R.id.boardListView);
        boardList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                FLBoard selectedBoard = (FLBoard) adapterView.getItemAtPosition(i);
                Intent intent = new Intent(BoardListActivity.this, ThreadListActivity.class);
                intent.putExtra(ThreadListActivity.KEY_BOARD, selectedBoard.boardURIName);
                intent.putExtra(ThreadListActivity.KEY_BOARD_NAME, selectedBoard.boardName);
                intent.putExtra(ThreadListActivity.KEY_BOARD_SRC, selectedBoard.src);
                startActivity(intent);
            }
        });
    }

	@Override
	void refresh() {
		BoardListLoader loader = new BoardListLoader(this);
		loader.execute();
	}
}
