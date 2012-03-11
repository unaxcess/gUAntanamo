package org.ua2.guantanamo.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.GUAntanamoMessaging;
import org.ua2.guantanamo.ViewMode;
import org.ua2.json.JSONItem;
import org.ua2.json.JSONMessage;
import org.ua2.json.JSONWrapper;

public class CacheMessages extends CacheItem<List<JSONMessage>> {
	
	private String folderName;
	
	public CacheMessages(String folderName, boolean refresh) throws JSONException {
		super("folder", folderName + "/" + GUAntanamo.getViewMode(false).name(), refresh);
		
		this.folderName = folderName;
	}

	private List<JSONMessage> toList(JSONArray array) throws JSONException {
		Map<Integer, JSONMessage> map = new TreeMap<Integer, JSONMessage>();
		for(int index = 0; index < array.length(); index++) {
			JSONMessage item = new JSONMessage(array.getJSONObject(index));
			map.put(item.getId(), item);
		}

		List<JSONMessage> messages = new ArrayList<JSONMessage>(map.values());
		return messages;
	}
	
	@Override
	protected long getStaleMinutes() {
		return GUAntanamoMessaging.getFolderRefreshMinutes();
	}

	@Override
	protected List<JSONMessage> refreshItem() throws JSONException {
		return toList(GUAntanamo.getClient().getMessages(folderName, GUAntanamo.getViewMode(false) == ViewMode.Unread, false));
	}

	@Override
	protected List<JSONMessage> toItem(String data) throws JSONException {
		return toList(JSONWrapper.parse(data).getArray());
	}

	@Override
	protected String toData(List<JSONMessage> messages) {
		return JSONItem.collectionToString(messages);
	}
}