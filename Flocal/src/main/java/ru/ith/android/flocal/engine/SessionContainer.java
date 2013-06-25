package ru.ith.android.flocal.engine;

import android.content.SharedPreferences;
import android.text.Editable;

import java.util.prefs.Preferences;

import ru.ith.lib.flocal.FLException;
import ru.ith.lib.flocal.FLSession;

/**
 * Created by infthi on 6/24/13.
 */
public class SessionContainer {
	private static final String SESSION_STORE_KEY = "flocal.session";

	private static final Object lock = new Object();
	private static SessionContainer instance = null;
	private FLSession session;
	private static SharedPreferences preferences;

	private SessionContainer() throws FLException {
		String key = null;
		if (preferences!=null)
			key = preferences.getString(SESSION_STORE_KEY, null);
		this.session = new FLSession(key);
	}

	public static void setPreferences(SharedPreferences prefs){
		preferences = prefs;
	}

	public static SessionContainer getInstance() throws FLException {
		if (instance == null) {
			synchronized (lock) {
				if (instance == null){
					instance = new SessionContainer();
				}
			}
		}
		return instance;
	}

	public FLSession getSession() {
		return session;
	}

	public boolean isAnonymousSession() {
		return session.isAnonymous();
	}

	public void login(String user, String pass) throws FLException {
		logout();
		setSession(new FLSession(user, pass));
	}

	public void logout(){
		if (!isAnonymousSession())
			try {
				session.logout();
				setSession(FLSession.makeAnonymousSession());
			} catch (FLException e) {
			}
	}

	private void setSession(FLSession session){
		this.session = session;
		if (preferences!=null){
			SharedPreferences.Editor edit = preferences.edit();
			edit.putString(SESSION_STORE_KEY, session.getKey());
			edit.commit();
		}
	}
}
