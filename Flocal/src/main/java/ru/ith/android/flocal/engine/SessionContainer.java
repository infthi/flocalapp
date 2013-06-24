package ru.ith.android.flocal.engine;

import ru.ith.lib.flocal.FLException;
import ru.ith.lib.flocal.FLSession;

/**
 * Created by infthi on 6/24/13.
 */
public class SessionContainer {
    private static SessionContainer instance = null;
    private static final Object lock = new Object();
    private FLSession session;

    public static SessionContainer getInstance() throws FLException {
        if (instance==null){
            synchronized (lock) {
            if (instance==null)
                instance = new SessionContainer();
            }
        }
        return instance;
    }

    private SessionContainer() throws FLException {
        this.session = new FLSession(null);
    }

    public FLSession getSession(){
        return session;
    }


}
