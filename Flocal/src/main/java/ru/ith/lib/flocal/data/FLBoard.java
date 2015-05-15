package ru.ith.lib.flocal.data;

public class FLBoard {
	public final String boardName;
	public final String boardURIName;
	public final boolean hasUnreadPosts;
	public final String src;


	public FLBoard(String name, String uRIName, boolean hasUnread, String src) {
		boardName = name;
		boardURIName = uRIName;
		hasUnreadPosts = hasUnread;
		this.src = (src == null ? "" : src);
	}

	@Override
	public String toString() {
		return boardName;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof FLBoard))
			return false;
		FLBoard bo = (FLBoard) o;
		return (src.equals(bo.src) && boardURIName.equals(bo.boardURIName));
	}
}
