package ru.ith.lib.flocal.data;

public class FLThreadHeader {
	private final String name;
	private final String author;
	private final int numUnread;
	private final int numUnreadDisc;
	private final long ID, firstUnreadID;
	private final boolean isPinned;
    public final String src;

	public FLThreadHeader(String name, String author, int numUnread, int numUnreadDisc, long ID, long unreadID, boolean isPinned, String src) {
		this.name = name;
		this.author = author;
		this.numUnread = numUnread;
		this.numUnreadDisc = numUnreadDisc;
		this.ID = ID;
        this.firstUnreadID = unreadID;
		this.isPinned = isPinned;
        this.src = src;
	}

	
	@Override
	public String toString() {
		return ((numUnread>0)?"("+numUnread+")":"")+((numUnreadDisc>0)?"{"+numUnreadDisc+"}":"")+(isPinned?"[x]":"")+name+" [@"+getID()+"] "+author;
	}


	public long getID() {
		return ID;
	}

    public String getName() {
        return name;
    }

    public long getUnreadID() {
        return firstUnreadID;
    }
}
