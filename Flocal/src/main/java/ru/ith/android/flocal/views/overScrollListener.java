package ru.ith.android.flocal.views;

/**
 * Created by infthi on 6/26/13.
 */

public interface overScrollListener {
   public void overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean touchEvent);
}