package ru.ith.flocal.util;

/**
 * This class is responsible for app settings.
 *
 * Currently it uses hardcoded values, but in future we'll give a UI to set em.
 *
 * Created by infthi on 29.07.14.
 */
public class Settings {
	public static final Settings instance = new Settings();

	public int getPostCutLimit() {
		//500 pix is temporary enough. TODO: rely on screen size and DPI
		return 500;
	}

	public FULL_BOARD_LIST_TYPE getSpinnerBoardListType() {
		return FULL_BOARD_LIST_TYPE.DEFAULT;
	}

	public enum FULL_BOARD_LIST_TYPE {VISIBLE, DEFAULT, ALL}
}
