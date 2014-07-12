package ru.ith.android.flocal.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.engine.SessionContainer;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.FLException;
import ru.ith.lib.flocal.data.FLBoard;

public class BoardListActivity extends ForumActivity {
    ListView boardList;
	public ArrayAdapter<FLBoard> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_list);
		SessionContainer.setPreferences(getPreferences(MODE_PRIVATE));
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
		adapter = new ArrayAdapter<FLBoard>(this, R.layout.board_entry){
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View row = convertView;
				if(row == null)
				{
					LayoutInflater inflater = getLayoutInflater();
					row = inflater.inflate(R.layout.board_entry, parent, false);
				}
				FLBoard item = getItem(position);
				((CheckBox)row.findViewById(R.id.boardEntryUnread)).setChecked(item.hasUnreadPosts);
				((TextView)row.findViewById(R.id.boardEntryText)).setText(item.boardName);
				return row;
			}
		};
		adapter.setNotifyOnChange(false);
		boardList.setAdapter(adapter);
    }

	@Override
	protected long getStartRefreshPeriod() {
		return 0;
	}

	@Override
	void refreshImpl() {
		List<FLBoard> data;
		try {
			data = FLDataLoader.listBoards(SessionContainer.getSessionInstance());
		} catch (FLException e) {
			Log.e(FLDataLoader.FLOCAL_APP_SIGN, e.toString(), e);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            data = Collections.emptyList();
		}
		final List<FLBoard> finalData = data;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adapter.clear();
				adapter.addAll(finalData);
				adapter.notifyDataSetChanged();
			}
		});

	}

    @Override
    long getRefreshPeriod() {
        return 10000;
    }
}
