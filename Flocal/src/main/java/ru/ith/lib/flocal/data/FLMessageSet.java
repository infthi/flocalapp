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

    public FLMessageSet(FLThreadHeader threadHeader, LinkedList<FLMessage> messages, boolean hasMoreData) {
        this.threadHeader = threadHeader;
        this.messages = messages;
        this.hasMoreData = hasMoreData;
        this.offset = 0;
    }

    public LinkedList<FLMessage> getPosts() {
        return messages;
    }


    public boolean hasMoreData() {
        return hasMoreData;
    }

    public int getOffset() {
        return offset;
    }
}
