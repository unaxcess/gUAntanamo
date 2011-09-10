package org.ua2.guantanamo.gui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.GUAntanamoMessaging;
import org.ua2.guantanamo.GUAntanamoMessaging.FolderThread;
import org.ua2.guantanamo.ViewMode;
import org.ua2.json.JSONFolder;
import org.ua2.json.JSONMessage;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FolderActivity extends ListActivity {

	private static final int ACTIVITY_MESSAGE = 1;
	private static final int ACTIVITY_POST = 2;

	private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM");
	private static final DateFormat DAY_FORMATTER = new SimpleDateFormat("E");
	private static final DateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm");

	private String folderName;
	
	private Calendar midnight;
	private Calendar threeDays;

	private static final String TAG = FolderActivity.class.getSimpleName();

	private class JSONDisplay {
		private JSONMessage object;
		private String subject;
		private String datetime;
		private int count;

		public JSONDisplay(JSONMessage object, String subject, String datetime, int count) {
			this.object = object;
			this.subject = subject;
			this.datetime = datetime;
			this.count = count;
		}

		public String toString() {
			return subject + " [" + datetime + ", " + count + "]";
		}
	}

	private void showFolder(final boolean refresh) throws JSONException {
		BackgroundCaller.run(this, "Getting messages...", new BackgroundWorker() {
			@Override
			public void during() throws Exception {
				GUAntanamoMessaging.setCurrentFolder(FolderActivity.this, folderName, refresh);
			}

			@Override
			public void after() {
				JSONFolder folder = GUAntanamoMessaging.getFolder(folderName);
				String title = folder.getName();
				int unread = 0;
				int count = folder.getCount();
				if(count > 0) {
					unread = folder.getUnread();
					if(unread > 0) {
						title += " (" + unread + " of " + count + ")";
					} else {
						title += " (" + count + ")";
					}
				}

				setTitle(title + " [" + GUAntanamo.getViewMode(false).name() + "]");

				List<JSONDisplay> list = new ArrayList<JSONDisplay>();
				for(FolderThread thread : GUAntanamoMessaging.getCurrentThreads()) {
					String dateStr = null;
					JSONMessage message = thread.topMessage;
					if(threeDays.getTime().after(message.getDate())) {
						dateStr = DATE_FORMATTER.format(message.getDate());
					} else if(midnight.getTime().after(message.getDate())) {
						dateStr = DAY_FORMATTER.format(message.getDate());
					} else {
						dateStr = TIME_FORMATTER.format(message.getDate());
					}
					list.add(new JSONDisplay(message, message.getSubject(), dateStr, thread.size));
				}

				setListAdapter(new ArrayAdapter<JSONDisplay>(FolderActivity.this, android.R.layout.simple_list_item_1, list));
			}

			@Override
			public String getError() {
				return "Cannot get messages";
			}
			
		});
	}

	private void setFolderView(ViewMode viewMode) {
		GUAntanamo.setViewMode(viewMode, false);

		try {
			showFolder(false);
		} catch(JSONException e) {
			GUAntanamo.handleException(this, "Cannot show folder", e);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.folder);

		FolderActivity retainedData = (FolderActivity)getLastNonConfigurationInstance();

		if(retainedData == null) {
			// No retained data from the running config change

			folderName = getIntent().getStringExtra("folder");
		} else {
			// Retrieve stored data from before the running config changed

			folderName = retainedData.folderName;
		}

		midnight = Calendar.getInstance();
		midnight.set(Calendar.HOUR_OF_DAY, 0);
		midnight.set(Calendar.MINUTE, 0);
		midnight.set(Calendar.SECOND, 0);
		midnight.set(Calendar.MILLISECOND, 0);

		threeDays = Calendar.getInstance();
		threeDays.add(Calendar.DATE, -3);

		try {
			showFolder(false);
		} catch(JSONException e) {
			GUAntanamo.handleException(this, "Cannot get folder", e);
		}
	}

	public Object onRetainNonConfigurationInstance() {
		// If the screen orientation, availability of keyboard, etc
		// changes, Android will kill and restart the Activity. This
		// stores its data so we can reuse it when the Activity
		// restarts

		return this;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Intent intent = new Intent(this, MessageViewActivity.class);

		JSONMessage message = ((JSONDisplay)getListAdapter().getItem(position)).object;

		Log.i(TAG, "Selected message is " + message.getId() + ": " + message.getSubject());
		intent.putExtra("message", message.getId());

		startActivityForResult(intent, ACTIVITY_MESSAGE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.folder, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.folderUnread) {
			setFolderView(ViewMode.Unread);

		} else if(item.getItemId() == R.id.folderAll) {
			setFolderView(ViewMode.All);

		} else if(item.getItemId() == R.id.folderRefresh) {
			try {
				showFolder(true);
			} catch(JSONException e) {
				GUAntanamo.handleException(this, "Cannot refresh folder", e);
			}

		} else if(item.getItemId() == R.id.folderPost) {
			Intent intent = new Intent(this, MessagePostActivity.class);
			startActivityForResult(intent, ACTIVITY_POST);
			
		} else if(item.getItemId() == R.id.folderPage) {
			Intent intent = new Intent(this, MessagePostActivity.class);
			intent.putExtra("pageMode", true);
			startActivityForResult(intent, ACTIVITY_POST);

		} else {
			return super.onContextItemSelected(item);

		}

		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if(requestCode == ACTIVITY_MESSAGE) {
			try {
				showFolder(false);
			} catch(JSONException e) {
				GUAntanamo.handleException(this, "Cannot show folder", e);
			}
		}
	}
}
