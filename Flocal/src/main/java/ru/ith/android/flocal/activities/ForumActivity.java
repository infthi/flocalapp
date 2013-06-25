package ru.ith.android.flocal.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Semaphore;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.engine.SessionContainer;
import ru.ith.lib.flocal.FLException;

/**
 * Created by adminfthi on 25.06.13.
 */
public abstract class ForumActivity extends Activity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		try {
			if (SessionContainer.getInstance().isAnonymousSession())
				menu.add(0,0,0, getString(R.string.login_menu));
			else
				menu.add(0,1,1, getString(R.string.logout_menu));
		} catch (FLException e) {
			Toast.makeText(this, e.getMessage(), 3);
			menu.add(0,1,1, e.getMessage()).setEnabled(false);
			Log.e("FL", e.getMessage());
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getGroupId()==0){
			switch (item.getItemId()){
				case 0:
					doLogin();
					return true;
				case 1:
					doLogout();
					return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	private void doLogin() {
		final Dialog loginDialog = new Dialog(this);
		loginDialog.setContentView(R.layout.login_dialog);
		loginDialog.setTitle(getString(R.string.login_dialog_title));

		((Button)loginDialog.findViewById(R.id.cancelButton)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						loginDialog.dismiss();
					}
				});
			}
		});

		final EditText loginText = (EditText) loginDialog.findViewById(R.id.loginField);
		final EditText passText = (EditText) loginDialog.findViewById(R.id.passField);

		((Button) loginDialog.findViewById(R.id.OKButton)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog progress = new ProgressDialog(loginDialog.getContext());
				progress.setMessage(getString(R.string.login_dialog_loginning));
				progress.setCancelable(false);
				progress.show();

				new AsyncTask<String, String, Boolean>(){
					String errorString = null;

					@Override
					protected Boolean doInBackground(String... params) {
						try {
							SessionContainer.getInstance().login(params[0], params[1]);
							return Boolean.TRUE;
						} catch (FLException e) {
							errorString = e.getMessage();
							return Boolean.FALSE;
						}
					}

					@Override
					protected void onPostExecute(final Boolean success) {
						super.onPostExecute(success);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								progress.dismiss();
								if (success){
									loginDialog.dismiss();
									refresh();
									invalidateOptionsMenu();
								}
								else
									Toast.makeText(loginDialog.getContext(), errorString, 5);
							}
						});
					}
				}.execute(loginText.getText().toString(), passText.getText().toString());
			}
		});

		loginDialog.show();
	}

	private void doLogout() {
		final ProgressDialog progress = new ProgressDialog(this);
		progress.setMessage(getString(R.string.login_dialog_logging_out));
		progress.setCancelable(false);
		progress.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					SessionContainer.getInstance().logout();
				} catch (FLException e) {
					Log.wtf("FL", e.toString());
				} finally {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							invalidateOptionsMenu();
							refresh();
							progress.dismiss();
						}
					});
				}
			}
		}).start();
	}

	abstract void refresh();
}
