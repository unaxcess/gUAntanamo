package org.ua2.guantanamo.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.json.JSONItem;
import org.ua2.json.JSONMessage;
import org.ua2.json.JSONWrapper;

public abstract class CacheMessages extends CacheTask<List<JSONMessage>> {
	protected List<JSONMessage> convert(JSONArray array) throws JSONException {
		Map<Integer, JSONMessage> map = new TreeMap<Integer, JSONMessage>();
		for(int index = 0; index < array.length(); index++) {
			JSONMessage item = new JSONMessage(array.getJSONObject(index));
			map.put(item.getId(), item);
		}

		List<JSONMessage> messages = new ArrayList<JSONMessage>(map.values());
		return messages;
	}
	
	@Override
	protected int getRefreshMinutes() {
		return GUAntanamo.MESSAGING_REFRESH_MINUTES;
	}

	@Override
	protected List<JSONMessage> convertDataToItem(String data) throws JSONException {
		return convert(JSONWrapper.parse(data).getArray());
	}

	@Override
	protected String convertItemToData(List<JSONMessage> messages) {
		return JSONItem.collectionToString(messages);
	}
}
