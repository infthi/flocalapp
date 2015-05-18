package ru.ith.flocal.engine;

import android.content.SharedPreferences;

import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.FLException;
import ru.ith.lib.flocal.FLSession;

/**
 * Created by infthi on 6/24/13.
 */
public class SessionContainer {
	private static final String SESSION_STORE_KEY = "flocal.session";

	private static final Object lock = new Object();
	private static SessionContainer instance = null;
	private static FLSession anonymousSession = new FLSession();
	private static SharedPreferences preferences;
	private FLSession session;

	private SessionContainer() throws FLException {
		String key = null;
		if (preferences != null)
			key = preferences.getString(SESSION_STORE_KEY, null);
		this.session = new FLSession(key);
	}

	public static void setPreferences(SharedPreferences prefs) {
		preferences = prefs;
	}

	public static SessionContainer getInstance() throws FLException {
		if (instance == null) {
			synchronized (lock) {
				if (instance == null) {
					instance = new SessionContainer();
				}
			}
		}
		return instance;
	}

	public static FLSession getSessionInstance() throws FLException {
		return getInstance().getSession();
	}

	public static FLSession getAnonymousSessionInstance() throws FLException {
		return anonymousSession;
	}

	public synchronized FLSession getSession() {
		return session;
	}

	private void setSession(FLSession session) {
		this.session = session;
		if (preferences != null) {
			SharedPreferences.Editor edit = preferences.edit();
			edit.putString(SESSION_STORE_KEY, session.getKey());
			edit.commit();
		}
	}

	public boolean isAnonymousSession() {
		return session.isAnonymous();
	}

	public void login(String user, String pass) throws FLException {
		logout();
		setSession(new FLSession(user, pass));
	}

	public synchronized void logout() {
		if (!isAnonymousSession())
			try {
				FLDataLoader.logout(session);
				setSession(FLSession.makeAnonymousSession());
			} catch (FLException e) {
			}
	}
}
