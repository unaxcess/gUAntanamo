package org.ua2.guantanamo.gui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.GUAntanamoMessaging;
import org.ua2.guantanamo.GUAntanamoMessaging.MessagingThread;
import org.ua2.guantanamo.ViewMode;
import org.ua2.guantanamo.data.CacheFolderMessages;
import org.ua2.guantanamo.data.CacheMessages;
import org.ua2.guantanamo.data.CacheSavedMessages;
import org.ua2.guantanamo.data.CacheTask.Processor;
import org.ua2.json.JSONFolder;
import org.ua2.json.JSONMessage;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MessagesActivity extends ListActivity {

	private static final int ACTIVITY_MESSAGE = 1;
	private static final int ACTIVITY_POST = 2;

	private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM");
	private static final DateFormat DAY_FORMATTER = new SimpleDateFormat("E");
	private static final DateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm");

	private Calendar midnight;
	private Calendar threeDays;
	
	private GestureDetector detector;
	private OnTouchListener listener;

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
			return subject + " [" + datetime + (count > 0 ? ", " + count : "" ) + "]";
		}
	}
	
	private Processor<List<JSONMessage>> processor;

	private static class State {
		boolean saves;
		String folderName;
		
		CacheMessages caller;
	}
	
	private State state;
	
	private static final String TAG = MessagesActivity.class.getName();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.folder);

		DisplayMetrics metrics = new DisplayMetrics(); getWindowManager().getDefaultDisplay().getMetrics(metrics);
		detector = new GestureDetector(new NavGestureDetector(metrics.widthPixels / 2, -1) {
			@Override
			protected void performAction(NavType direction) {
				if(!state.saves) {
					showFolder(direction, false);
				}
			}
		});

		listener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(detector.onTouchEvent(event)) {
					return true;
				} else {
					return false;
				}
			}
		};
		getListView().setOnTouchListener(listener);
		
		midnight = Calendar.getInstance();
		midnight.set(Calendar.HOUR_OF_DAY, 0);
		midnight.set(Calendar.MINUTE, 0);
		midnight.set(Calendar.SECOND, 0);
		midnight.set(Calendar.MILLISECOND, 0);

		threeDays = Calendar.getInstance();
		threeDays.add(Calendar.DATE, -3);

		processor = new Processor<List<JSONMessage>>() {
			@Override
			public void processItem(List<JSONMessage> messages, boolean isNew) {
				if(state.saves) {
					GUAntanamoMessaging.setSavedMessages(messages);
				} else {
					GUAntanamoMessaging.setCurrentFolder(state.folderName, messages);
				}
				showFolder();
			}
		};
		
		state = (State)getLastNonConfigurationInstance();
		if(state == null) {
			state = new State();
		
			state.saves = getIntent().getBooleanExtra("saves", false);
			state.folderName = getIntent().getStringExtra("folder");
			
			if(state.saves) {
				state.caller = new CacheSavedMessages();
			} else {
				state.caller = new CacheFolderMessages();
			}

			loadMessages(state.folderName, false);
		}
	}
	
	private void loadMessages(String folderName, boolean refresh) {
		if(state.saves) {
			((CacheSavedMessages)state.caller).load(this, processor, refresh);
		} else {
			((CacheFolderMessages)state.caller).load(this, processor, folderName, refresh);
		}
	}

	public void onResume() {
		super.onResume();

		state.caller.attach(this, processor);
	}
	
	public void onStop() {
		super.onStop();

		state.caller.detatch();
	}

	public Object onRetainNonConfigurationInstance() {
		return state;
	}
	
	private void showFolder(NavType direction, boolean refresh) {
		JSONFolder folder = GUAntanamoMessaging.setCurrentFolder(direction);
		if(folder == null) {
			setResult(RESULT_OK);
			finish();
			return;
		}
		
		loadMessages(folder.getName(), refresh);
	}
	
	private void addMessageToList(JSONMessage message, List<JSONDisplay> list, int count) {
		String dateStr = null;
		if(threeDays.getTime().after(message.getDate())) {
			dateStr = DATE_FORMATTER.format(message.getDate());
		} else if(midnight.getTime().after(message.getDate())) {
			dateStr = DAY_FORMATTER.format(message.getDate());
		} else {
			dateStr = TIME_FORMATTER.format(message.getDate());
		}
		list.add(new JSONDisplay(message, message.getSubject(), dateStr, count));
	}

	private void showFolder() {
		if(state.saves) {
			setTitle("Saved Messages");
		} else {
			JSONFolder folder = GUAntanamoMessaging.getCurrentFolder();
			
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
		}
		
		List<JSONDisplay> list = new ArrayList<JSONDisplay>();
		if(state.saves) {
			for(JSONMessage message : GUAntanamoMessaging.getSavedMessages()) {
				addMessageToList(message, list, 0);
			}
		} else {
			for(MessagingThread thread : GUAntanamoMessaging.getCurrentThreads()) {
				for(JSONMessage message : thread.getMessages()) {
					addMessageToList(message, list, 0);
				}
			}
		}

		setListAdapter(new ArrayAdapter<JSONDisplay>(this, android.R.layout.simple_list_item_1, list));
	}

	private void setFolderView(ViewMode viewMode) {
		GUAntanamo.setViewMode(viewMode, false);

		showFolder(null, false);
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
			showFolder(null, true);

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
			showFolder(null, false);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return listener.onTouch(getListView(), event);
	}
}
