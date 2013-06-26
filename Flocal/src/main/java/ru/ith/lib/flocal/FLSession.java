package ru.ith.lib.flocal;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import ru.ith.lib.flocal.data.FLBoard;
import ru.ith.lib.flocal.data.FLMessage;
import ru.ith.lib.flocal.data.FLMessageSet;
import ru.ith.lib.flocal.data.FLThreadHeader;
import ru.ith.lib.webcrawl.ConnectionFactory;
import ru.ith.lib.webcrawl.providers.HTMLResponce;
import ru.ith.lib.webcrawl.providers.ProviderEnum;

public class FLSession {
	private final Map<String, String> sessionCookies;

	public static FLSession makeAnonymousSession() throws FLException {
		return new FLSession(null);
	}

	public FLSession(String login, String password) throws FLException {
		this(FLDataLoader.generateLoginData(login, password));
	}

	public FLSession(String sKey) throws FLException {
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
