package ru.ith.android.flocal.activities;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.engine.BoardListLoader;
import ru.ith.lib.flocal.data.FLBoard;

public class BoardListActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_list);
        BoardListLoader loader = new BoardListLoader(this);
        loader.execute();
    }
}
