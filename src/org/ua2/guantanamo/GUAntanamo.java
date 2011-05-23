package org.ua2.guantanamo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class GUAntanamo extends ListActivity {

	private static final int ACTIVITY_SETTINGS = 1;
	private static final int ACTIVITY_MESSAGES = ACTIVITY_SETTINGS + 1;
	private static final int ACTIVITY_POST = ACTIVITY_MESSAGES + 1;
	
	private static final String TAG = GUAntanamo.class.getSimpleName();
	
	public enum ViewType { Unread, Subscribed, All };

	private static String url;
	private static String username;
	private static String password;
	
	private static GUAntanamoClient client;
	
	private static JSONArray folders;
	private static JSONObject folder;
	private static List<String> folderNames; 
	private static JSONArray messages;
	
	private static Map<String, JSONArray> messagesLookup = new HashMap<String, JSONArray>();
	
	private List<JSONDisplay> list;
	
	private class JSONDisplay implements Comparable<JSONDisplay> {
		private JSONObject object;
		private String string;
		
		public JSONDisplay(JSONObject object, String string) {
			this.object = object;
			this.string = string;
		}
		
		public JSONObject getObject() {
			return object;
		}
		
		public String toString() {
			return string;
		}

		@Override
		public int compareTo(JSONDisplay display) {
			try {
				return object.getString("folder").compareToIgnoreCase(display.getObject().getString("folder"));
			} catch(Exception e) {
				Log.e(TAG, "Cannot compare displays", e);
				return 0;
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getClass().getPackage().getName(), 0);
			GUAntanamoClient.setVersion(info.versionName);
		} catch(NameNotFoundException e) {
			Log.e(TAG, "Cannot get package info", e);
		}
		
		setContentView(R.layout.main);
		
        url = getPreferences(MODE_PRIVATE).getString("url", "http://ua2.org/uaJSON");
        username = getPreferences(MODE_PRIVATE).getString("username", "");
        password = getPreferences(MODE_PRIVATE).getString("password", "");

        if(!("".equals(username) && "".equals(password))) {
        	resetConnection();
        } else {
    		Intent intent = new Intent(this, GUAntanamoSettings.class);
    		startActivityForResult(intent, ACTIVITY_SETTINGS);
        }
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if(requestCode == ACTIVITY_SETTINGS && resultCode == RESULT_OK) {
			SharedPreferences settings = getPreferences(MODE_PRIVATE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("url", url);
			editor.putString("username", username);
			editor.putString("password", password);
			editor.commit();
			
			resetConnection();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		try {
			Intent intent = new Intent(this, GUAntanamoFolder.class);
			folder = (JSONObject)list.get(position).object;
			intent.putExtra("folder", folder.getString("folder"));
			startActivityForResult(intent, ACTIVITY_MESSAGES);
		} catch(JSONException e) {
			Log.e(TAG, "Cannot get folder", e);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ViewType view = getView();
		
		menu.findItem(R.id.unread).setChecked(ViewType.Unread == view);
		menu.findItem(R.id.subscribed).setChecked(ViewType.Subscribed == view);
		menu.findItem(R.id.all).setChecked(ViewType.All == view);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.unread) {
			setView(ViewType.Unread);
			showFolders();

		} else if(item.getItemId() == R.id.subscribed) {
			setView(ViewType.Subscribed);
			showFolders();
		
		} else if(item.getItemId() == R.id.all) {
			setView(ViewType.All);
			showFolders();

		} else if(item.getItemId() == R.id.settings) {
    		Intent intent = new Intent(this, GUAntanamoSettings.class);
    		startActivityForResult(intent, ACTIVITY_SETTINGS);

		} else if(item.getItemId() == R.id.refresh) {
			final ProgressDialog progress = ProgressDialog.show(this, "", "Loading folders...", true);
			
			new AsyncTask<String, Void, String>() {
				@Override
				protected String doInBackground(String... params) {
					try {
						folders = client.getFolders();
						return null;
					} catch(JSONException e) {
						return e.getMessage();
					}
				}
				
				protected void onPostExecute(String error) {
					progress.dismiss();

					if(error == null) {
						setView(ViewType.Unread);
						
						showFolders();
					} else {
						AlertDialog.Builder builder = new AlertDialog.Builder(GUAntanamo.this);
						builder
							.setMessage("Unable to refresh folders, " + error)
							.setCancelable(false)
							.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
						AlertDialog dialog = builder.create();
						dialog.show();
					}
				}
			}.execute();

		} else if(item.getItemId() == R.id.post) {
    		Intent intent = new Intent(this, GUAntanamoPostMessage.class);
    		startActivityForResult(intent, ACTIVITY_POST);
			
		} else {
			return super.onOptionsItemSelected(item);
		}
		
		return true;
	}
	
	private void resetConnection() {
		final ProgressDialog progress = ProgressDialog.show(this, "", "Loading data...", true);
		
		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
				try {
					Log.i(TAG, "Creating client using " + url + " " + username + " " + password);
			        client = new GUAntanamoClient(GUAntanamo.this, url, username, password);
					
			        folders = client.getFolders();
					folderNames = new ArrayList<String>();
					for(int folderNum = 0; folderNum < folders.length(); folderNum++) {
						JSONObject folder = (JSONObject)folders.get(folderNum);
						folderNames.add(folder.getString("folder"));
					}
					Collections.sort(folderNames);

		        	return null;
				} catch(Exception e) {
					return e.getMessage();
				}
			}
			
			protected void onPostExecute(String error) {
				progress.dismiss();

				if(error == null) {
					setView(ViewType.Unread);
					
					showFolders();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(GUAntanamo.this);
					builder
						.setMessage("Unable to connect, " + error)
						.setCancelable(false)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			}
		}.execute();
	}
	
	private void showFolders() {
		try {
			ViewType view = getView();
			setTitle("gUAntanamo [" + view.name() + "]");
			
			list = new ArrayList<JSONDisplay>();
			for(int folderNum = 0; folderNum < folders.length(); folderNum++) {
				JSONObject folder = (JSONObject)folders.get(folderNum);
				
				String string = folder.getString("folder");
				boolean subscribed = folder.getBoolean("subscribed");
				int unread = 0;
				int count = folder.getInt("count");
				if(count > 0) {
					unread = folder.getInt("unread");
					if(unread > 0) {
						string += " (" + unread + " of " + count + ")";
					} else {
						string += " (" + count + ")";
					}
				}
				if(view == ViewType.All ||
						(view == ViewType.Subscribed && subscribed) ||
						(view == ViewType.Unread && subscribed && unread > 0)) {
					list.add(new JSONDisplay(folder, string));
				}
			}
			Collections.sort(list);

			setListAdapter(new ArrayAdapter<JSONDisplay>(this, android.R.layout.simple_list_item_1, list));
		} catch(JSONException e) {
			Log.e(TAG, "Cannot set folder names", e);
		}
	}

	private ViewType getView() {
		String viewStr = getPreferences(MODE_PRIVATE).getString("view", ViewType.Unread.name());
		for(ViewType view : ViewType.values()) {
			if(view.name().equals(viewStr)) {
				return view;
			}
		}
		return null;
	}
	
	private void setView(ViewType view) {
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("view", view.name());
		editor.commit();
	}

	public static GUAntanamoClient getClient() {
		return client;
	}

	public static String getUrl() {
		return url;
	}

	public static void setUrl(String url) {
		GUAntanamo.url = url;
	}

	public static String getUsername() {
		return username;
	}

	public static void setUsername(String username) {
		GUAntanamo.username = username;
	}

	public static String getPassword() {
		return password;
	}

	public static void setPassword(String password) {
		GUAntanamo.password = password;
	}
	
	public static JSONArray getFolders() {
		return folders;
	}
	
	public static JSONObject getFolder() {
		return folder;
	}
	
	public static JSONArray getMessages() {
		return messages;
	}

	public static void setMessages(String folder, boolean refresh) throws JSONException {
		if(!refresh) {
			messages = messagesLookup.get(folder);
			if(messages != null) {
				return;
			}
		}
			
		messages = client.getMessages(folder);
		messagesLookup.put(folder, messages);
	}
	
	public static String getText(Activity activity, int id) {
        TextView view = (TextView)activity.findViewById(id);
        if(view == null) {
        	throw new RuntimeException("No widget " + id + " found in " + activity.getClass().getName());
        }
        
        return view.getText().toString();
	}
	
	public static void setText(Activity activity, int id, String text) {
        TextView view = (TextView)activity.findViewById(id);
        if(view == null) {
        	throw new RuntimeException("No widget " + id + " found in " + activity.getClass().getName());
        }
        
        view.setText(text);
	}

	public static List<String> getFolderNames() {
		return folderNames;
	}
}
