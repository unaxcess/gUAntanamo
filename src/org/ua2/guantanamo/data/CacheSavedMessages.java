package org.ua2.guantanamo.data;

import java.util.List;

import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.json.JSONItem;
import org.ua2.json.JSONMessage;

import android.content.Context;

public class CacheSavedMessages extends CacheMessages {
	@Override
	protected String getType() {
		return "saves";
	}

	@Override
	protected String getDescription() {
		return "saved messages";
	}

	@Override
	protected List<JSONMessage> loadItem(String id) throws JSONException {
		return convert(GUAntanamo.getClient().getSavedMessages(false));
	}

	@Override
	protected String convertItemToData(List<JSONMessage> messages) {
		return JSONItem.collectionToString(messages);
	}

	public void load(Context context, Processor<List<JSONMessage>> processor, final boolean forceRefresh) {
		super._load(context, processor, null, forceRefresh);
	}
}
