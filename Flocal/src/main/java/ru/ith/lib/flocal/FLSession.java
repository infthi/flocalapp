package ru.ith.lib.flocal;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class FLSession {
	private final Map<String, String> sessionCookies;

	public static FLSession makeAnonymousSession() throws FLException {
		return new FLSession(null);
	}

	public FLSession(String login, String password) throws FLException {
		this(FLDataLoader.generateLoginData(login, password));
	}

    public FLSession() {
        this.sessionCookies = Collections.emptyMap();
    }

    public FLSession(String sKey) {
        Map<String, String> cookies = new TreeMap<String, String>();
        if (sKey != null) {
            String[] cookieData = sKey.split(":");
			cookies.put("w3t_w3t_key", cookieData[0]);
			cookies.put("w3t_w3t_mysess", cookieData[1]);
			cookies.put("w3t_w3t_myid", cookieData[2]);
		}
		this.sessionCookies = Collections.unmodifiableMap(cookies);
	}


	public boolean isAnonymous() {
		return sessionCookies.isEmpty();
	}

    public String getCurrentUser() {
        throw new RuntimeException("not implemented yet!");
    }

    public String getKey() {
		String key = sessionCookies.get("w3t_w3t_key");
		String mysess = sessionCookies.get("w3t_w3t_mysess");
		String myid = sessionCookies.get("w3t_w3t_myid");
		if ((key==null)||(mysess==null)||(myid==null))
			return null;
		return key+":"+mysess+":"+myid;
	}

    protected Map<String,String> getSessionCookies() {
        return sessionCookies;
    }
}
