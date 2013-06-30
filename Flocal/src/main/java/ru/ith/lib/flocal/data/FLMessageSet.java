package ru.ith.lib.flocal.data;

import java.util.LinkedList;

/**
 * Created by infthi on 6/25/13.
 */
public class FLMessageSet {
    private final FLThreadHeader threadHeader;
    private final LinkedList<FLMessage> messages;
    private final FLThreadPageSet set;
	public volatile String URL = null;

    public FLMessageSet(FLThreadHeader threadHeader, LinkedList<FLMessage> messages, FLThreadPageSet set) {
        this.threadHeader = threadHeader;
        this.messages = messages;
		this.set = set;
    }

    public LinkedList<FLMessage> getPosts() {
        return messages;
    }


    public boolean hasMoreData() {
        return set.hasMorePages;
    }

    public int getEffectiveOffset() {
        return set.offset;
    }
}
