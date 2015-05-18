package ru.ith.flocal.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by infthi on 05.08.14.
 */
public class BuildData {

	public static String getVersionName(Context ctxt) {
		try {
			return ctxt.getPackageManager().getPackageInfo(ctxt.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			return "Unknown";
		}
	}

	public static String getBuildDate(Context ctxt) {
		try {
			ApplicationInfo ai = ctxt.getPackageManager().getApplicationInfo(ctxt.getPackageName(), 0);
			ZipFile zf = new ZipFile(ai.sourceDir);
			ZipEntry ze = zf.getEntry("classes.dex");
			long time = ze.getTime();
			String s = SimpleDateFormat.getInstance().format(new java.util.Date(time));
			zf.close();
			return s;
		} catch (Exception e) {
			return "Unknown";
		}
	}
}
