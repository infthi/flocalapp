package ru.ith.lib.flocal.data;

public class FLThread {
	private final String name;
	private final String author;
	private final int numUnread;
	private final int numUnreadDisc;
	private final int ID;
	private final boolean isPinned;

	public FLThread(String name, String author, int numUnread, int numUnreadDisc, int ID, boolean isPinned) {
		this.name = name;
		this.author = author;
		this.numUnread = numUnread;
		this.numUnreadDisc = numUnreadDisc;
		this.ID = ID;
		this.isPinned = isPinned;
	}

	
	@Override
	public String toString() {
		return ((numUnread>0)?"("+numUnread+")":"")+((numUnreadDisc>0)?"{"+numUnreadDisc+"}":"")+(isPinned?"[x]":"")+name+" [@"+getID()+"] "+author;
	}


	public int getID() {
		return ID;
	}
}
