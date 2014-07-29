package ru.ith.android.flocal.util;

/**
 * Created by infthi on 29.07.14.
 */
public class Settings {
    public static final Settings instance = new Settings();

    public int getPostCutLimit() {
        return 30;
    }

    public enum FULL_BOARD_LIST_TYPE {VISIBLE, DEFAULT, ALL}

    public FULL_BOARD_LIST_TYPE getSpinnerBoardListType() {
        return FULL_BOARD_LIST_TYPE.DEFAULT;
    }
}
