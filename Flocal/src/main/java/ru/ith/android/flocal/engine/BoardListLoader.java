package ru.ith.android.flocal.engine;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.activities.BoardListActivity;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.FLException;
import ru.ith.lib.flocal.data.FLBoard;

/**
 * Created by infthi on 6/24/13.
 */
public class BoardListLoader extends AsyncTask<Void, Void, List<FLBoard>> {
    private final BoardListActivity listener;

    public BoardListLoader(BoardListActivity boardListActivity) {
    this.listener = boardListActivity;
    }

    @Override
    protected List<FLBoard> doInBackground(Void... voids) {
        try {
            return FLDataLoader.listBoards(SessionContainer.getSessionInstance());
        } catch (FLException e) {
            Log.e("FL", e.toString(), e);
        }
        return Collections.emptyList();
    }

    @Override
    protected void onPostExecute(final List<FLBoard> flBoards) {
        super.onPostExecute(flBoards);
        listener.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ListView list = (ListView) listener.findViewById(R.id.boardListView);
                list.setAdapter(new ArrayAdapter<FLBoard>(listener, R.layout.board_entry, flBoards){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View row = convertView;
                        if(row == null)
                        {
                            LayoutInflater inflater = listener.getLayoutInflater();
                            row = inflater.inflate(R.layout.board_entry, parent, false);
                        }
                        FLBoard item = getItem(position);
                        ((CheckBox)row.findViewById(R.id.boardEntryUnread)).setChecked(item.hasUnreadPosts);
                        ((TextView)row.findViewById(R.id.boardEntryText)).setText(item.boardName);
                        return row;
                    }
                });
            }
        });
    }
}
