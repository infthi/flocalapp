package ru.ith.flocal.views.util.message;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

import ru.ith.flocal.engine.SessionContainer;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.data.FLMessage;

public class PostPostTask extends AsyncTask<String, Void, Boolean> {
	private final ProgressDialog progress;
	private final Dialog reply;
	private final FLMessage parent;
	private volatile String problem = "Unknown problem";

	public PostPostTask(ProgressDialog progress, Dialog reply, FLMessage parent) {
		this.progress = progress;
		this.reply = reply;
		this.parent = parent;
	}

	@Override
	protected Boolean doInBackground(String... params) {
		try {
			FLDataLoader.sendMessage(SessionContainer.getSessionInstance(), parent, params[0]);
			return true;
		} catch (Exception e) {
			problem = e.getMessage();
			return false;
		}
	}


	@Override
	protected void onPostExecute(Boolean aBoolean) {
		super.onPostExecute(aBoolean);
		progress.hide();
		if (aBoolean)
			reply.hide();
		else
			Toast.makeText(reply.getContext(), problem, Toast.LENGTH_SHORT).show();
	}
}
