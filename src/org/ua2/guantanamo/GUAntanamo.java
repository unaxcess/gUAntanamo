package org.ua2.guantanamo;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Useful bits and pieces for use in the GUI
 * 
 * @author Techo
 * 
 */
public class GUAntanamo {

	private static GUAntanamoClient client;

	private static String url;
	private static String username;
	private static String password;

	private static ViewMode viewMode = ViewMode.Unread;
	
	private static final String TAG = GUAntanamo.class.getSimpleName();

	public static GUAntanamoClient getClient() {
		if(client == null) {
			Log.i(TAG, "Creating client using " + url + " " + username + " " + password);
			client = new GUAntanamoClient(url, username, password);
		}
		return client;
	}

	public static void handleException(Context context, String msg, Exception e) {
		Log.e(TAG, msg, e);
		Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	}

	public static String getUrl() {
		return url != null ? url : "";
	}

	public static String getUsername() {
		return username != null ? username : "";
	}

	public static String getPassword() {
		return password != null ? password : "";
	}

	public static void setPassword(String password) {
		GUAntanamo.password = password;
		GUAntanamo.client = null;
	}
	
	public static void setUrl(String url) {
		GUAntanamo.url = url;
		GUAntanamo.client = null;
	}
	
	public static void setUsername(String username) {
		GUAntanamo.username = username;
		GUAntanamo.client = null;
	}

	public static ViewMode getViewMode(boolean topLevel) {
		if(!topLevel && viewMode == ViewMode.Subscribed) {
			return ViewMode.All;
		}

		return viewMode;
	}
	
	public static void setViewMode(ViewMode viewMode, boolean topLevel) {
		if(!topLevel && GUAntanamo.viewMode == ViewMode.Subscribed && viewMode == ViewMode.All) {
			viewMode = ViewMode.All;
		}
	
		GUAntanamo.viewMode = viewMode;
	}
}
