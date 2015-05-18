package ru.ith.flocal.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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

import ru.ith.flocal.R;
import ru.ith.flocal.engine.SessionContainer;
import ru.ith.flocal.util.BuildData;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.FLException;
import ru.ith.lib.flocal.data.FLBoard;

public class BoardListActivity extends ForumActivity {
	public ArrayAdapter<FLBoard> adapter;
	ListView boardList;

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
				ThreadListActivity.open(BoardListActivity.this, selectedBoard);
			}
		});
		adapter = new ArrayAdapter<FLBoard>(this, R.layout.board_entry) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View row = convertView;
				if (row == null) {
					LayoutInflater inflater = getLayoutInflater();
					row = inflater.inflate(R.layout.board_entry, parent, false);
				}
				FLBoard item = getItem(position);
				((CheckBox) row.findViewById(R.id.boardEntryUnread)).setChecked(item.hasUnreadPosts);
				((TextView) row.findViewById(R.id.boardEntryText)).setText(item.boardName);
				return row;
			}
		};
		adapter.setNotifyOnChange(false);
		boardList.setAdapter(adapter);
	}

	@Override
	void refresh() {
		List<FLBoard> data;
		try {
			data = FLDataLoader.listBoards(SessionContainer.getSessionInstance());
			hideLoadingProgressBar();
		} catch (final FLException e) {
			Log.e(FLDataLoader.FLOCAL_APP_SIGN, e.toString(), e);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(BoardListActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			});
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(1, 1025, 10, "About");
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getGroupId() == 1) {
			if (item.getItemId() == 1025) {
				Dialog versionDialog = new Dialog(this);
				versionDialog.setTitle("About");
				versionDialog.setContentView(R.layout.dialog_version);
				((TextView) versionDialog.findViewById(R.id.versionVersionDataTextView)).setText(BuildData.getVersionName(this));
				((TextView) versionDialog.findViewById(R.id.versionBuiltDataTextView)).setText(BuildData.getBuildDate(this));
				versionDialog.show();

			}
		}
		return super.onOptionsItemSelected(item);
	}
}
