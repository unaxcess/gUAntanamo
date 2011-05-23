package org.ua2.guantanamo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.ua2.guantanamo.GUAntanamo.ViewType;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class GUAntanamoFolder extends ListActivity {
	
	private static final int ACTIVITY_MESSAGE = 1;
	private static final int ACTIVITY_POST = ACTIVITY_MESSAGE + 1;
	
	private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM");
	private static final DateFormat DAY_FORMATTER = new SimpleDateFormat("E");
	private static final DateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm");

	private static final String TAG = GUAntanamoFolder.class.getSimpleName();

	private String folder;
	private List<JSONDisplay> list;
	private boolean getting = false;

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
				return object.getString("id").compareToIgnoreCase(display.getObject().getString("id"));
			} catch(Exception e) {
				Log.e(TAG, "Cannot compare displays", e);
				return 0;
			}
		}
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setView(ViewType.Unread);
        
        setContentView(R.layout.folder);

        folder = getIntent().getStringExtra("folder");
		
		getFolder();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		if(!getting) {
			showFolder();
		}
	}

	private void getFolder() {
		getting = true;
		
		final ProgressDialog progress = ProgressDialog.show(this, "", "Loading messages...", true);
		
		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
				try {
					GUAntanamo.setMessages(folder, true);
		        	
		        	return null;
				} catch(JSONException e) {
					return e.getMessage();
				}
			}
			
			protected void onPostExecute(String error) {
				progress.dismiss();

				if(error == null) {
					showFolder();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(GUAntanamoFolder.this);
					builder
						.setMessage("Unable to get messages, " + error)
						.setCancelable(false)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
				
				getting = false;
			}
		}.execute();
	}
	
	private void showFolder() {
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        Calendar threeDays = Calendar.getInstance();
        threeDays.add(Calendar.DATE, -3);

        try {
        	ViewType view = getView();
        	setTitle(folder + " [" + view.name() + "]");
        	
			list = new ArrayList<JSONDisplay>();
			for(int messageNum = 0; messageNum < GUAntanamo.getMessages().length(); messageNum++) {
				JSONObject message = GUAntanamo.getMessages().getJSONObject(messageNum);
	
				boolean read = message.getBoolean("read");
				Date date = new Date(1000 * message.getLong("epoch"));
				String dateStr = null;
				if(threeDays.getTime().after(date)) {
					dateStr = DATE_FORMATTER.format(date);
				} else if(midnight.getTime().after(date)) {
					dateStr = DAY_FORMATTER.format(date);
				} else {
					dateStr = TIME_FORMATTER.format(date);
				}
				String string = message.getString("subject") + " [" + dateStr + "]";
				if(view == ViewType.All || (view == ViewType.Unread && !read)) {
					list.add(new JSONDisplay(message, string));
				}
			}
			Collections.sort(list);
	
			setListAdapter(new ArrayAdapter<JSONDisplay>(GUAntanamoFolder.this, android.R.layout.simple_list_item_1, list));
        } catch(JSONException e) {
        	Log.e(TAG, "Cannot show folder", e);
        }
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		try {
			Intent intent = new Intent(this, GUAntanamoViewMessage.class);
			
			JSONObject message = (JSONObject)list.get(position).object;
			int msgId = message.getInt("id");
			
			intent.putExtra("message", msgId);
			
			startActivityForResult(intent, ACTIVITY_MESSAGE);
		} catch(JSONException e) {
			Log.e(TAG, "Cannot get message", e);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.folder, menu);
		
		return true;
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.unread) {
			setView(ViewType.Unread);
			showFolder();

		} else if(item.getItemId() == R.id.all) {
			setView(ViewType.All);
			showFolder();
		
		} else if(item.getItemId() == R.id.refresh) {
			getFolder();

		} else if(item.getItemId() == R.id.post) {
    		Intent intent = new Intent(GUAntanamoFolder.this, GUAntanamoPostMessage.class);

			intent.putExtra("folder", folder);

			startActivityForResult(intent, ACTIVITY_POST);
			
		} else {
			return super.onContextItemSelected(item);
		}
		
		return true;
	}

}
