package org.ua2.guantanamo.data;

import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.json.JSONMessage;
import org.ua2.json.JSONWrapper;

import android.content.Context;

public class CacheMessage extends CacheTask<JSONMessage> {
	@Override
	public String getType() {
		return "message";
	}

	@Override
	protected String getDescription() {
		return "message";
	}

	@Override
	public int getRefreshMinutes() {
		return -1;
	}

	@Override
	public JSONMessage loadItem(String id) throws JSONException {
		return new JSONMessage(GUAntanamo.getClient().getMessage(Integer.parseInt(id)));
	}

	@Override
	public JSONMessage convertDataToItem(String data) throws JSONException {
		return new JSONMessage(JSONWrapper.parse(data).getObject());
	}
	
	public void load(Context context, ItemProcessor<JSONMessage> processor, int id, boolean refresh) {
		super._load(context, processor, Integer.toString(id), refresh);
	}

	public void load(Context context, ItemProcessor<JSONMessage> processor, int id) {
		super._load(context, processor, Integer.toString(id), false);
	}
}
