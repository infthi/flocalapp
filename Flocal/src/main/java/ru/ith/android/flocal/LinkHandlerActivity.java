package ru.ith.android.flocal;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.protocol.HTTP;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.ith.android.flocal.activities.BoardListActivity;
import ru.ith.android.flocal.activities.PostListActivity;
import ru.ith.android.flocal.activities.ThreadListActivity;

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
		Map<String, String> intentData = new HashMap<String, String>();

		@Override
		protected Class doInBackground(Uri... params) {
			Uri data = params[0];
			String handler = data.getLastPathSegment();
			Map<String, String> getParams = new HashMap<String, String>();
			data.getEncodedQuery();
			try {
				List<NameValuePair> parameters = URLEncodedUtils.parse(new URI(data.toString()), HTTP.UTF_8);//TODO: possibly nicknames can be in 1251?
				for (NameValuePair parameter : parameters)
					getParams.put(parameter.getName(), parameter.getValue());
			} catch (URISyntaxException e) {
				diagnosis = "failed to parse URI";
				return null;
			}
			if (!handler.endsWith(".php")) {
				//not a forum page; possibly an upload...
				diagnosis = "upload? :( " + handler;
				return null;
			} else if (handler.equals("ubbthreads.php")) {
				diagnosis = "root";
				return BoardListActivity.class;
			} else if (handler.equals("postlist.php")) {
				diagnosis = "board";
				intentData.put(ThreadListActivity.KEY_BOARD_SRC, getParams.get("src"));
				intentData.put(ThreadListActivity.KEY_BOARD, getParams.get("Board"));
				return ThreadListActivity.class;
			} else if (handler.equals("showflat.php")) {
				diagnosis = "thread";
				intentData.put(PostListActivity.KEY_THREAD, getParams.get("Number"));
				intentData.put(PostListActivity.KEY_THREAD_UNREAD, getParams.get("Number"));//TODO: not unread but selected
				intentData.put(PostListActivity.KEY_THREAD_SRC, getParams.get("src"));

				return PostListActivity.class;
			}
			diagnosis = "unknown :( " + handler;
			return null;
		}

		@Override
		protected void onPostExecute(Class s) {
			super.onPostExecute(s);
			if (s != null) {
				Intent intent = new Intent(LinkHandlerActivity.this, s);
				for (Map.Entry<String, String> data : intentData.entrySet()) {
					intent.putExtra(data.getKey(), data.getValue());
				}
				LinkHandlerActivity.this.startActivity(intent);
				return;
			} else {
				statusView.setText(diagnosis);
			}
		}
	}
}

