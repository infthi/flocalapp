package ru.ith.lib.flocal.data;

public class FLBoard {
	public final String boardName;
	public final String boardURIName;
	public final boolean hasUnreadPosts;
	

	public FLBoard(String name, String uRIName, boolean hasUnread) {
		boardName = name;
		boardURIName = uRIName;
		hasUnreadPosts = hasUnread;
	}
	
	@Override
	public String toString() {
		return (hasUnreadPosts?"(*)":"")+boardName+" [@"+boardURIName+"]";
	}

}
