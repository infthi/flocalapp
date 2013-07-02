package ru.ith.lib.flocal.data;

public class FLThreadHeader implements Comparable<FLThreadHeader>{
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

	@Override
	public int compareTo(FLThreadHeader another) {
		//threads are similar if:
		//1. they have similar author
		//2. they have similar startID
		int authorCompare = author.compareTo(another.author);
		if (authorCompare!=0)
			return authorCompare;
		if (getID()>=0){
			if (another.getID()>=0)
				return (int) (getID()-another.getID());
			else if (another.getUnreadID()<getID())
				return -1;
		} else {
			if (another.getID()>=getUnreadID())
				return 1;
		}
		//this threads are possibly equal. let's check their themes are equal enough.
		//TODO: check themes are equal enough (but same thread can have different themes from  time to tice 'cause of edits)
		return getName().compareTo(another.getName());
	}

    @Override
    public boolean equals(Object o) {
        if (o==null)
            return false;
        if (!(o instanceof FLThreadHeader))
            return false;
        return this.compareTo((FLThreadHeader) o)==0;
    }

    @Override
    public int hashCode() {
        return author.hashCode(); //the only immutable property of thread =[
    }
}
