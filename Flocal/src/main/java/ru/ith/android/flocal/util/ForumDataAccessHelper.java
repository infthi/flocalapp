package ru.ith.android.flocal.util;

import java.util.LinkedList;

import ru.ith.android.flocal.engine.SessionContainer;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.FLException;
import ru.ith.lib.flocal.data.FLBoard;

/**
 * Created by infthi on 29.07.14.
 */
public class ForumDataAccessHelper {
	private static volatile Settings.FULL_BOARD_LIST_TYPE lastBoardListRequest = null;
	private static volatile LinkedList<FLBoard> lastBoardListResponse = null;

	public static LinkedList<FLBoard> getBoardList(Settings.FULL_BOARD_LIST_TYPE type) throws FLException {
		if (lastBoardListRequest == type)
			return lastBoardListResponse;
		LinkedList<FLBoard> result;
		switch (type) {
			//TODO: ALL: fetch list of all available alt threads etc
			case DEFAULT:
				result = FLDataLoader.listBoards(SessionContainer.getAnonymousSessionInstance());
				break;
			case VISIBLE:
			default:
				result = FLDataLoader.listBoards(SessionContainer.getSessionInstance());
		}
		lastBoardListRequest = type;
		lastBoardListResponse = result;
		return result;
	}
}
