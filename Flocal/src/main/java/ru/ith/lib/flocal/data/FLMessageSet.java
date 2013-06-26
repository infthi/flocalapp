package ru.ith.lib.flocal.data;

import java.util.LinkedList;

/**
 * Created by infthi on 6/25/13.
 */
public class FLMessageSet {
    private final FLThreadHeader threadHeader;
    private final LinkedList<FLMessage> messages;
    private final boolean hasMoreData;
    private final int offset;
    public volatile String URL = null;

    public FLMessageSet(FLThreadHeader threadHeader, LinkedList<FLMessage> messages, boolean hasMoreData, int threadOffset) {
        this.threadHeader = threadHeader;
        this.messages = messages;
        this.hasMoreData = hasMoreData;
        this.offset = threadOffset;
    }

    public LinkedList<FLMessage> getPosts() {
        return messages;
    }


    public boolean hasMoreData() {
        return hasMoreData;
    }

    public int getEffectiveOffset() {
        return offset;
    }
}
