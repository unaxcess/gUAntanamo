package org.ua2.guantanamo.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.GUAntanamoClient;
import org.ua2.guantanamo.GUAntanamoMessaging;
import org.ua2.guantanamo.ViewMode;
import org.ua2.guantanamo.data.CacheFolders;
import org.ua2.guantanamo.data.CacheTask.Processor;
import org.ua2.json.JSONFolder;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends ListActivity {

	private static final int ACTIVITY_SETTINGS = 1;
	private static final int ACTIVITY_BANNER = 2;
	private static final int ACTIVITY_FOLDER = 3;
	private static final int ACTIVITY_POST = 4;

	private class JSONDisplay {
		private JSONFolder object;
		private String string;

		public JSONDisplay(JSONFolder object, String string) {
			this.object = object;
			this.string = string;
		}

		public String toString() {
			return string;
		}
	}
	
	private Processor<Collection<JSONFolder>> processor;

	private static class State {
		CacheFolders caller;
	}
	
	private State state;
	
	private static final String TAG = MainActivity.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			PackageInfo info = getPackageManager().getPackageInfo(getClass().getPackage().getName(), 0);
			GUAntanamoClient.setVersion(info.versionName);
		} catch(NameNotFoundException e) {
			GUAntanamo.handleException(this, "Cannot get package info", e);
		}

		setContentView(R.layout.main);

		processor = new Processor<Collection<JSONFolder>>() {
			@Override
			public void processItem(Collection<JSONFolder> folders, boolean isNew) {
				GUAntanamoMessaging.setFolders(folders, isNew);
				
				showFolders();
			}
		};
		
		state = (State)getLastNonConfigurationInstance();
		if(state == null) {
			state = new State();
			
			GUAntanamo.setUrl(getPreferences(MODE_PRIVATE).getString("url", "http://ua2.org/uaJSON"));
			GUAntanamo.setUsername(getPreferences(MODE_PRIVATE).getString("username", ""));
			GUAntanamo.setPassword(getPreferences(MODE_PRIVATE).getString("password", ""));

			if(!"".equals(GUAntanamo.getUrl()) && !"".equals(GUAntanamo.getUsername()) && !"".equals(GUAntanamo.getPassword())) {
				showBanner();
			} else {
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivityForResult(intent, ACTIVITY_SETTINGS);
			}

			state.caller = new CacheFolders();
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
	
	private void showFolders(boolean refresh) {
		state.caller.load(this, processor, refresh);
	}
	
	private void showFolders() {
		setTitle("gUAntanamo [" + GUAntanamo.getViewMode(true).name() + "]");
		
		List<JSONDisplay> list = new ArrayList<JSONDisplay>();

		for(JSONFolder folder : GUAntanamoMessaging.getFolders()) {
			String string = folder.getName();
			boolean subscribed = folder.getSubscribed();
			int unread = 0;
			int count = folder.getCount();
			if(count > 0) {
				unread = folder.getUnread();
				if(unread > 0) {
					string += " (" + unread + " of " + count + ")";
				} else {
					string += " (" + count + ")";
				}
			}
			if(GUAntanamo.getViewMode(true) == ViewMode.All || (GUAntanamo.getViewMode(true) == ViewMode.Subscribed && subscribed)
					|| (GUAntanamo.getViewMode(true) == ViewMode.Unread && subscribed && unread > 0)) {
				list.add(new JSONDisplay(folder, string));
			}
		}

		setListAdapter(new ArrayAdapter<JSONDisplay>(this, android.R.layout.simple_list_item_1, list));

		JSONFolder folder = GUAntanamoMessaging.getCurrentFolder();
		if(folder != null) {
			for(int index = 0; index < list.size(); index++) {
				if(folder.getName().compareTo(list.get(index).object.getName()) <= 0) {
					Log.d(TAG, "Setting folder selection to " + index);
					getListView().setSelection(index); 
					break;
				}
			}
		} else {
			Log.d(TAG, "Setting folder selection to top");
			getListView().setSelection(0);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if(requestCode == ACTIVITY_SETTINGS && resultCode == RESULT_OK) {
			SharedPreferences settings = getPreferences(MODE_PRIVATE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("url", GUAntanamo.getUrl());
			editor.putString("username", GUAntanamo.getUsername());
			editor.putString("password", GUAntanamo.getPassword());
			editor.commit();

			showBanner();

		} else if(requestCode == ACTIVITY_BANNER) {
			showFolders(false);

		} else if(requestCode == ACTIVITY_FOLDER) {
			showFolders(false);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		JSONDisplay item = (JSONDisplay)getListAdapter().getItem(position);
		JSONFolder folder = item.object;

		Intent intent = new Intent(this, FolderActivity.class);
		intent.putExtra("folder", folder.getName());
		startActivityForResult(intent, ACTIVITY_FOLDER);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		return true;
	}

	private void showBanner() {
		Intent intent = new Intent(this, BannerActivity.class);
		startActivityForResult(intent, ACTIVITY_BANNER);
	}

	private void setFoldersView(ViewMode viewMode) {
		GUAntanamo.setViewMode(viewMode, true);
		showFolders(false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.mainUnread) {
			setFoldersView(ViewMode.Unread);

		} else if(item.getItemId() == R.id.mainSubscribed) {
			setFoldersView(ViewMode.Subscribed);

		} else if(item.getItemId() == R.id.mainAll) {
			setFoldersView(ViewMode.All);

		} else if(item.getItemId() == R.id.mainSettings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivityForResult(intent, ACTIVITY_SETTINGS);

		} else if(item.getItemId() == R.id.mainRefresh) {
			showFolders(true);

		} else if(item.getItemId() == R.id.mainPost) {
			Intent intent = new Intent(this, MessagePostActivity.class);
			startActivityForResult(intent, ACTIVITY_POST);

		} else {
			return super.onOptionsItemSelected(item);
		}

		return true;
	}
}
