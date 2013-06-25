package ru.ith.android.flocal.activities;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.AsyncTask;
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
        boardList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                FLBoard selectedBoard = (FLBoard)adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

	@Override
	void refresh() {
		BoardListLoader loader = new BoardListLoader(this);
		loader.execute();
	}
}
