package ru.ith.android.flocal.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.engine.SessionContainer;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.FLException;

/**
 * Basic activity with basic implementations
 * Created by adminfthi on 25.06.13.
 */
public abstract class ForumActivity extends Activity {


	private volatile Timer refresher = null;
	private volatile TimerTask refreshTask = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		try {
			if (SessionContainer.getInstance().isAnonymousSession())
				menu.add(0, 0, 0, getString(R.string.login_menu));
			else
				menu.add(0, 1, 1, getString(R.string.logout_menu));
		} catch (FLException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			menu.add(0, 1, 1, e.getMessage()).setEnabled(false);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getGroupId() == 0) {
			switch (item.getItemId()) {
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
		loginDialog.setTitle(getString(R.string.dialog_login_title));

		loginDialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
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

		loginDialog.findViewById(R.id.OKButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog progress = new ProgressDialog(loginDialog.getContext());
				progress.setMessage(getString(R.string.login_dialog_loginning));
				progress.setCancelable(false);
				progress.show();

				new AsyncTask<String, String, Boolean>() {
					String errorString = null;

					@Override
					protected Boolean doInBackground(String... params) {
						try {
							SessionContainer.getInstance().login(params[0], params[1]);
							return Boolean.TRUE;
						} catch (FLException e) {
							errorString = e.getMessage();
							return Boolean.FALSE;
						} finally {
							refreshWrap();
						}
					}

					@Override
					protected void onPostExecute(final Boolean success) {
						super.onPostExecute(success);
						progress.dismiss();
						if (success) {
							loginDialog.dismiss();
							invalidateOptionsMenu();
						} else
							Toast.makeText(loginDialog.getContext(), errorString, Toast.LENGTH_LONG).show();
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
					Log.wtf(FLDataLoader.FLOCAL_APP_SIGN, e.toString());
				} finally {
					refreshWrap();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							invalidateOptionsMenu();
							progress.dismiss();
						}
					});
				}
			}
		}).start();
	}

	protected final void refreshWrap() {
		if (refreshTask != null)
			refreshTask.cancel();
		try {
			refresh();
		} catch (final RuntimeException e) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					e.printStackTrace();
					View status = findViewById(R.id.firstLoadingStatus);
					if (status != null)
						((TextView) status).setText(e.getMessage());
					Toast.makeText(ForumActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
				}
			});
		} finally {
			makeRefreshTask(getRefreshPeriod());
		}
	}

	private synchronized void makeRefreshTask(long timeOut) {
		if (refresher == null) {
			refresher = new Timer("Thread_list_refresher", true);
		}
		refreshTask = new TimerTask() {
			@Override
			public void run() {
				refreshWrap();
			}
		};
		refresher.schedule(refreshTask, timeOut);
	}

	abstract void refresh();

	abstract long getRefreshPeriod();

	@Override
	protected synchronized void onResume() {
		super.onResume();
		makeRefreshTask(0);// == NOW
	}

	@Override
	protected synchronized void onPause() {
		super.onPause();
		refreshTask.cancel();
		refresher.purge();
		refresher = null;
	}

	public final void hideLoadingProgressBar() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				View progress = findViewById(R.id.firstLoadingProgressBar);
				if (progress != null)
					progress.setVisibility(View.GONE);
				View status = findViewById(R.id.firstLoadingStatus);
				if (status != null)
					status.setVisibility(View.GONE);
			}
		});
	}

	public final void notify(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(ForumActivity.this, text, Toast.LENGTH_SHORT).show();
			}
		});
	}
}
