package ru.ith.android.flocal;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import ru.ith.android.flocal.activities.BoardListActivity;

public class LinkHandlerActivity extends Activity {
    TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_handler);
        statusView = (TextView) findViewById(R.id.linkStatusText);
        Uri data = getIntent().getData();
        new FLUriParser().execute(data);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.link_handler, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class FLUriParser extends AsyncTask<Uri, Void, Class> {
        String diagnosis;

        @Override
        protected Class doInBackground(Uri... params) {
            String handler = params[0].getLastPathSegment();
            if (handler.equals("ubbthreads.php")) {
                diagnosis = "root";
                return BoardListActivity.class;
            }
            diagnosis = "unknown :( " + handler;
            return null;
        }

        @Override
        protected void onPostExecute(Class s) {
            super.onPostExecute(s);
            if (s != null) {
                Intent intent = new Intent(LinkHandlerActivity.this, s);
                LinkHandlerActivity.this.startActivity(intent);
                return;
            } else {
                statusView.setText(diagnosis);
            }
        }
    }
}

