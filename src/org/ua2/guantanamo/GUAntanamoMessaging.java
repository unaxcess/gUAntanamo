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
import org.ua2.guantanamo.gui.NavType;
import org.ua2.json.JSONFolder;
import org.ua2.json.JSONMessage;

import android.util.Log;

public class GUAntanamoMessaging {

	private static class MessagingFolder {
		private JSONFolder folder;
		private List<MessagingThread> threads = new ArrayList<MessagingThread>();

		public MessagingFolder(JSONFolder folder) {
			this.folder = folder;
		}
	}
	
	public static class MessagingThread {
		private int threadId;
		private Date mostRecentDate;
		
		private List<JSONMessage> messages = new ArrayList<JSONMessage>();
		
		public JSONMessage getTopMessage() {
			return messages.get(0);
		}
		
		public int getMessageCount() {
			return messages.size();
		}
	}
	
	private static List<JSONFolder> folderList = new ArrayList<JSONFolder>();
	private static Map<String, MessagingFolder> folderMap = new TreeMap<String, MessagingFolder>();

	private static MessagingFolder currentFolder = null;
	private static int threadInFolder = 0;
	private static int messageInThread = 0;
	private static JSONMessage currentMessage = null;

	private static Set<Integer> markedMessageIds = new HashSet<Integer>();

	private static final String TAG = GUAntanamoMessaging.class.getName();

	private static boolean isPosValid(int threadIndex, int messageIndex) {
		if(threadIndex < 0 || messageIndex < 0) {
			return false;
		} else if(threadIndex >= currentFolder.threads.size()) {
			return false;
		}
		
		int threadSize = currentFolder.threads.get(threadIndex).messages.size();
		if(messageIndex >= threadSize) {
			return false;
		}
		
		return true;
	}
	
	public static List<JSONFolder> getFolders() {
		return folderList;
	}

	public static JSONFolder getFolder(String name) {
		MessagingFolder folder = folderMap.get(name.toLowerCase());
		return folder != null ? folder.folder : null;
	}
	
	public static List<String> getFolderNames() {
		List<String> names = new ArrayList<String>();
		for(JSONFolder folder : folderList) {
			names.add(folder.getName());
		}
		return names;
	}
	
	public static JSONFolder getCurrentFolder() {
		if(currentFolder == null) {
			return null;
		}
		return currentFolder.folder;
	}
	
	public static JSONFolder setCurrentFolder(NavType direction) {
		Log.d(TAG, "Resetting current folder using nav type " + direction);
		if(currentFolder == null) {
			return null;
			
		} else if(direction == null) {
			return currentFolder.folder;
			
		}
		
		int index = folderList.indexOf(currentFolder.folder);
		// Perform the nav
		if(direction == NavType.NEXT_SIBLING) {
			index++;
			
		} else if(direction == NavType.PREV_SIBLING) {
			index--;
			
		}

		// Check it's possible
		if(index < 0 || index >= folderList.size()) {
			return null;
		}
		
		if(GUAntanamo.getViewMode(false) == ViewMode.Unread) {
			/*
			 * Only the initial state is guaranteed to match the view mode,
			 * messages marked as read stay in currentFolder so that
			 * the indexes remain valid
			 */
			boolean loop = true;
			while(loop) {
				JSONFolder item = folderList.get(index);
				if(!item.getSubscribed() || item.getUnread() == 0) {
					index++;
					
					if(index < 0 || index >= folderList.size()) {
						return currentFolder.folder;
					}
				} else {
					return null;
				}
			}
		}
		
		return folderList.get(index);
	}
	
	private static MessagingFolder getMessagingFolder(JSONFolder item) {
		MessagingFolder folder = folderMap.get(item.getName().toLowerCase());
		if(folder == null) {
			folder = new MessagingFolder(item);
			folderMap.put(item.getName().toLowerCase(), folder);
		}
		
		return folder;
	}
	
	public synchronized static void setFolders(Collection<JSONFolder> folders, boolean isNew) {
		if(folderList.size() != 0 && !isNew) {
			return;
		}
		
		Log.i(TAG, "Setting folders");
		
		folderList.clear();

		for(JSONFolder item : folders) {
			folderList.add(item);
			
			getMessagingFolder(item);
		}
		
		Collections.sort(folderList, new Comparator<JSONFolder>() {
			@Override
			public int compare(JSONFolder lhs, JSONFolder rhs) {
				return lhs.getName().toLowerCase().compareTo(rhs.getName().toLowerCase());
			}
		});
	}
	
	public static List<MessagingThread> getCurrentThreads() {
		if(currentFolder == null) {
			Log.e(TAG, "Current folder is null, this is likely to end badly", new Throwable());
		}
		return currentFolder.threads;
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
	 */
	public static JSONFolder setCurrentFolder(String name, List<JSONMessage> messages) {
		int unread = 0;
		
		currentFolder = folderMap.get(name.toLowerCase());
		if(currentFolder == null) {
			JSONFolder folder = new JSONFolder(name);
			currentFolder = getMessagingFolder(folder);
		}
		
		// First group them into threads
		Map<Integer, MessagingThread> threads = new TreeMap<Integer, MessagingThread>();
		
		for(JSONMessage message : messages) {
			if(markedMessageIds.contains(message.getId())) {
				Log.i(TAG, "Removing message " + message.getId() + " from read set");
				markedMessageIds.remove(message.getId());
			}
			Log.d(TAG, "Message " + message.getId() + " read=" + message.isRead());
			if(GUAntanamo.getViewMode(false) != ViewMode.Unread || !message.isRead()) {
				MessagingThread thread = threads.get(message.getThread());
				if(thread == null) {
					Log.d(TAG, "Creating thread " + message.getThread());
					
					thread = new MessagingThread();
					thread.threadId = message.getThread();
					thread.mostRecentDate = message.getDate();
					
					threads.put(message.getThread(), thread);
				}
				thread.messages.add(message);
			}
			if(!message.isRead()) {
				unread++;
			}
		}
		
		Log.i(TAG, "Setting " + threads.size() + " threads in current folder");
		currentFolder.threads.clear();
		currentFolder.threads.addAll(threads.values());
		
		// TODO: Make this configurable (reverse-most-recent hard coded for now)
		Collections.sort(currentFolder.threads, new Comparator<MessagingThread>() {
			@Override
			public int compare(MessagingThread thread1, MessagingThread thread2) {
				return -1 * thread1.mostRecentDate.compareTo(thread2.mostRecentDate);
			}
			
		});
		
		currentFolder.folder.setUnread(unread);
		
		return currentFolder.folder;
	}
	
	public static JSONMessage getCurrentMessage() {
		if(currentMessage == null) {
			Log.e(TAG, "Current message is null, this is likely to end badly", new Throwable());
			return null;
		}
		return currentMessage;
	}

	/**
	 * Perform the required navigation and return current message
	 * If the navigation cannot be performed the current position
	 * remains unchanged
	 * 
	 * @param direction
	 * @param normalise
	 * @return
	 */
	public static JSONMessage getMessage(NavType direction) {
		if(currentFolder == null) {
			return null;
		}
		
		int threadIndex = threadInFolder;
		int messageIndex = messageInThread;

		// Perform the nav
		if(direction == NavType.NEXT_SIBLING) {
			messageIndex++;

		} else if(direction == NavType.PREV_SIBLING) {
			messageIndex--;

		} else if(direction == NavType.NEXT_PARENT) {
			threadIndex++;
			messageIndex = 0;
			
		} else if(direction == NavType.PREV_PARENT) {
			threadIndex--;
			if(threadIndex >= 0) {
				messageIndex = currentFolder.threads.get(threadIndex).messages.size() - 1;
			}
			
		}
		
		// Check it's possible
		if(!isPosValid(threadIndex, messageIndex)) {
			return null;
		}
		
		if(GUAntanamo.getViewMode(false) == ViewMode.Unread) {
			/*
			 * Only the initial state is guaranteed to match the view mode,
			 * messages marked as read stay in currentFolder so that
			 * location / index remain valid
			 */
			boolean loop = true;
			while(loop) {
				List<JSONMessage> messages = currentFolder.threads.get(threadIndex).messages;
				if(messages.get(messageIndex).isRead()) {
					if(direction == NavType.NEXT_SIBLING || direction == NavType.NEXT_PARENT) {
						messageIndex++;
						if(messageIndex == messages.size()) {
							threadIndex++;
							messageIndex = 0;
						}
					} else if(direction == NavType.PREV_SIBLING || direction == NavType.PREV_PARENT) {
						messageIndex--;
						if(messageIndex < 0) {
							threadIndex--;
						}
					}
					
					if(!isPosValid(threadIndex, messageIndex)) {
						return null;
					}
				} else {
					loop = false;
				}
			}
		}
		
		return currentFolder.threads.get(threadIndex).messages.get(messageIndex);
	}

	public static JSONMessage setCurrentMessage(JSONMessage message) {
		Log.i(TAG, "Setting current message to " + message.getId());
		
		currentMessage = message;
		
		if(currentFolder == null) {
			return message;
		}
		
		int threadIndex = 0;
		int messageIndex = 0;
		
		MessagingThread thread = currentFolder.threads.get(threadIndex);
		boolean loop = true;
		do {
			JSONMessage item = thread.messages.get(messageIndex);
			Log.d(TAG, "Checking current folder location=" + threadIndex + " index=" + messageIndex + " message=" + item.getId() + ": " + item.getSubject());
			
			if(item.getId() == message.getId()) {
				Log.i(TAG, "Found message " + message.getId() + " in current folder");
				
				loop = false;
				threadInFolder = threadIndex;
				messageInThread = messageIndex;
			} else {
				messageIndex++;
				if(messageIndex == thread.messages.size()) {
					threadIndex++;
					if(threadIndex < currentFolder.threads.size() ) {
						messageIndex = 0;
						thread = currentFolder.threads.get(threadIndex);
					} else {
						loop = false;
						Log.w(TAG, "Cannot find message " + message.getId() + " in current folder");
					}
				}
			}
		} while(loop);
		
		return message;
	}
	
	private static void markMessageReadInThread() {
		if(currentFolder == null) {
			return;
		}
		
		for(MessagingThread thread : currentFolder.threads) {
			for(JSONMessage message : thread.messages) {
				if(message.getId() == currentMessage.getId()) {
					message.setRead(true);
					return;
				}
			}
		}
	}

	public static void markCurrentMessageRead() throws JSONException {
		if(!currentMessage.isRead()) {
			currentMessage.setRead(true);
			markMessageReadInThread();
			
			markedMessageIds.add(currentMessage.getId());
			JSONFolder folder = getFolder(currentMessage.getFolder());
			if(folder != null) {
				folder.setUnread(folder.getUnread() - 1);
			}
	
			// TODO: Move to a worker thread or background service
			GUAntanamo.getClient().markMessage(currentMessage.getId(), true);
		}
	}
	
	public static void markThreadRead(int id) {
		if(currentFolder == null) {
			return;
		}
		
		for(MessagingThread thread : currentFolder.threads) {
			if(thread.threadId == id) {
				for(JSONMessage message : thread.messages) {
					message.setRead(true);
					markedMessageIds.add(currentMessage.getId());
				}
			}
		}
	}
}
