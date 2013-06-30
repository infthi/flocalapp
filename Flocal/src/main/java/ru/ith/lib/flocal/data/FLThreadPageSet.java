package ru.ith.lib.flocal.data;

/**
 * Created by adminfthi on 01.07.13.
 */
public class FLThreadPageSet {
	public final int offset;
	public final boolean hasMorePages;

	public FLThreadPageSet(int offset, boolean hasMorePages) {
		this.offset = offset;
		this.hasMorePages = hasMorePages;
	}
}
