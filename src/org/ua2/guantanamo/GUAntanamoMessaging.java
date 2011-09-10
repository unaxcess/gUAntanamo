package org.ua2.guantanamo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.json.JSONException;
import org.ua2.guantanamo.data.CacheFolders;
import org.ua2.guantanamo.data.CacheMessages;
import org.ua2.json.JSONFolder;
import org.ua2.json.JSONMessage;

import android.content.Context;
import android.util.Log;

public class GUAntanamoMessaging {
	
	private static class CurrentFolder {
		JSONFolder folder;
		List<InternalFolderThread> threads = new ArrayList<InternalFolderThread>();
		
		int location = 0;
		int index = 0;
	}
	
	public static class FolderThread {
		public JSONMessage topMessage;
		public int size;
	}
	
	private static class InternalFolderThread {
		Date mostRecentDate;
		
		List<JSONMessage> messages = new ArrayList<JSONMessage>();
	}

	private static Map<String, JSONFolder> folders;
	private static CurrentFolder currentFolder = null;
	private static Set<Integer> markedMessageIds = new HashSet<Integer>();

	private static final String TAG = GUAntanamoMessaging.class.getSimpleName();
	
	private static String getFolderKey(String name) {
		return name.toLowerCase();
	}

	private static boolean isThreadPosValid(int threadNum, int threadIndex) {
		if(threadNum < 0) {
			return false;
		} else if(threadNum >= currentFolder.threads.size()) {
			return false;
		}
		
		int threadSize = currentFolder.threads.get(threadNum).messages.size();
		if(threadIndex >= threadSize) {
			return false;
		}
		
		return true;
	}
	
	public static Collection<JSONFolder> getFolders() {
		if(folders == null) {
			return null;
		}
		
		return folders.values();
	}

	public static JSONFolder getFolder(String name) {
		if(folders == null) {
			return null;
		}
		
		return folders.get(name.toLowerCase());
	}

	public static JSONFolder getCurrentFolder() {
		if(currentFolder == null) {
			return null;
		}
		return currentFolder.folder;
	}

	public static int getFolderRefreshMinutes() {
		return 10;
	}

	public static void setFolders(Context context, boolean refresh) throws JSONException {
		Log.i(TAG, "Setting folders refresh=" + refresh);
		
		Map<String, JSONFolder> items = new TreeMap<String, JSONFolder>();

		CacheFolders cache = new CacheFolders(context);
		if(refresh) {
			cache.clear();
		}

		for(JSONFolder item : cache.getItem()) {
			items.put(getFolderKey(item.getName()), item);
			
			if(!refresh) {
				JSONFolder folder = getFolder(item.getName());
				if(folder != null) {
					item.setUnread(folder.getUnread());
				}
			}
		}
		
		cache.close();
		
		folders = items;
	}

	public static List<FolderThread> getCurrentThreads() {
		List<FolderThread> list = new ArrayList<FolderThread>();
		for(InternalFolderThread thread : currentFolder.threads) {
			FolderThread item = new FolderThread();
			item.topMessage = thread.messages.get(0);
			item.size = thread.messages.size();
			
			list.add(item);
		}
		
		return list;
	}

	/**
	 * Turn the flat message list into a list of lists.
	 * The outer list is sorted according to user preference,
	 * the inner list according to message position i.e. depth-first-order in the tree
	 * 
	 * Because the message list is cached there's the potential for the read counts to be out of date.
	 * As such a set of read IDs is used to "correct" messages. This may lead to unexpected results
	 * if messages have been held from elsewhere, but you can always force a refresh and the read IDs set
	 * will be cleared down.
	 * 
	 * @param name
	 * @param messages
	 * @throws JSONException 
	 */
	public static void setCurrentFolder(Context context, String name, boolean refresh) throws JSONException {
		int unread = 0;
		
		Log.i(TAG, "Setting current folder name=" + name + " refresh=" + refresh);
		
		currentFolder = new CurrentFolder();
		currentFolder.folder = getFolder(name);
		
		CacheMessages cache = new CacheMessages(context, name);
		if(refresh) {
			cache.clear();
		}
		
		// First group them into threads
		Map<Integer, InternalFolderThread> threads = new TreeMap<Integer, InternalFolderThread>();
		
		List<JSONMessage> messages = cache.getItem();
		refresh = !cache.isItemFromCache();
		for(JSONMessage message : messages) {
			if(markedMessageIds.contains(message.getId())) {
				if(refresh) {
					Log.i(TAG, "Removing message " + message.getId() + " from read set");
					markedMessageIds.remove(message.getId());
				} else {
					Log.i(TAG, "Overriding message " + message.getId() + " from read set");
					message.setRead(true);
				}
			}
			Log.d(TAG, "Message " + message.getId() + " read=" + message.isRead());
			if(GUAntanamo.getViewMode(false) != ViewMode.Unread || !message.isRead()) {
				InternalFolderThread thread = threads.get(message.getThread());
				if(thread == null) {
					Log.d(TAG, "Creating thread " + message.getThread());
					
					thread = new InternalFolderThread();
					thread.mostRecentDate = message.getDate();
					
					threads.put(message.getThread(), thread);
				}
				thread.messages.add(message);
			}
			if(!message.isRead()) {
				unread++;
			}
		}
		cache.close();
		
		Log.i(TAG, "Adding " + threads.size() + " threads to current folder");
		currentFolder.threads.addAll(threads.values());
		
		// TODO: Make this configurable (reverse-most-recent hard coded for now)
		Collections.sort(currentFolder.threads, new Comparator<InternalFolderThread>() {
			@Override
			public int compare(InternalFolderThread thread1, InternalFolderThread thread2) {
				return -1 * thread1.mostRecentDate.compareTo(thread2.mostRecentDate);
			}
			
		});
		
		getCurrentFolder().setUnread(unread);
	}
	
	public static int getCurrentMessageId() {
		return currentFolder.threads.get(currentFolder.location).messages.get(currentFolder.index).getId();
	}

	/**
	 * Perform the required navigation and return current message
	 * If the navigation cannot be performed the current position
	 * remains unchanged
	 * 
	 * @param type
	 * @param normalise
	 * @return
	 */
	public static int getCurrentMessageId(NavType type) {
		int location = currentFolder.location;
		int index = currentFolder.index;

		// Perform the nav
		if(type == NavType.NextMessage) {
			index++;
		} else if(type == NavType.NextThread) {
			location++;
			index = 0;
		} else if(type == NavType.PrevMessage) {
			index--;
		} else if(type == NavType.PrevThread) {
			location--;
			if(location >= 0) {
				index = currentFolder.threads.get(location).messages.size() - 1;
			}
		}
		
		// Check it's possible
		if(!isThreadPosValid(location, index)) {
			return -1;
		}
		
		if(GUAntanamo.getViewMode(false) == ViewMode.Unread) {
			/*
			 * Only the initial state is guaranteed to match the view mode,
			 * messages marked as read stay in currentFolder so that
			 * location / index remain valid
			 */
			boolean loop = true;
			while(loop) {
				List<JSONMessage> messages = currentFolder.threads.get(location).messages;
				if(messages.get(index).isRead()) {
					if(type == NavType.NextMessage || type == NavType.NextThread) {
						index++;
						if(index == messages.size()) {
							location++;
							index = 0;
						}
					} else if(type == NavType.PrevMessage || type == NavType.PrevThread) {
						index--;
						if(index < 0) {
							location--;
						}
					}
					
					if(!isThreadPosValid(location, index)) {
						return -1;
					}
				} else {
					loop = false;
				}
			}
		}
		
		currentFolder.location = location;
		currentFolder.index = index;
		
		return getCurrentMessageId();
	}

	public static void setCurrentMessageId(int id) {
		Log.i(TAG, "Setting current message to " + id);
		
		int location = 0;
		int index = 0;
		
		InternalFolderThread thread = currentFolder.threads.get(location);
		boolean loop = true;
		do {
			JSONMessage message = thread.messages.get(index);
			Log.d(TAG, "Checking current folder location=" + location + " index=" + index + " message=" + message.getId() + ": " + message.getSubject());
			
			if(message.getId() == id) {
				Log.i(TAG, "Found message " + id + " in current folder");
				
				loop = false;
				currentFolder.location = location;
				currentFolder.index = index;
			} else {
				index++;
				if(index == thread.messages.size()) {
					location++;
					if(location < currentFolder.threads.size() ) {
						index = 0;
						thread = currentFolder.threads.get(location);
					} else {
						loop = false;
						Log.i(TAG, "Cannot find message " + id + " in current folder");
					}
				}
			}
		} while(loop);
	}

	public static void markCurrentMessageRead() throws JSONException {
		JSONMessage message = currentFolder.threads.get(currentFolder.location).messages.get(currentFolder.index);
		if(!message.isRead()) {
			
			
			message.setRead(true);
			markedMessageIds.add(message.getId());
			JSONFolder folder = getFolder(message.getFolder());
			if(folder != null) {
				folder.setUnread(folder.getUnread() - 1);
			}
	
			// TODO: Move to a worker thread or background service
			GUAntanamo.getClient().markMessage(message.getId(), true);
		}
	}

}
