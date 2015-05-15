package ru.ith.android.flocal.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * Created by infthi on 6/26/13.
 */
public class OverScrollableList extends ListView {
	private volatile overScrollListener listener = null;

	public OverScrollableList(Context context) {
		super(context);
	}

	public OverScrollableList(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public OverScrollableList(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setOverScrollListener(overScrollListener listener) {
		this.listener = listener;
	}

	@Override
	protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
		if (listener != null)
			listener.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
		return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
	}
}

