package org.ua2.guantanamo.data;

import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.json.JSONMessage;
import org.ua2.json.JSONWrapper;

import android.content.Context;

public class CacheMessage extends CacheTask<JSONMessage> {
	public CacheMessage(Context context, ItemProcessor<JSONMessage> processor, int id) {
		super(context, processor);
		
		load(id, false);
	}

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
	
	public void load(int id, boolean refresh) {
		super.load(Integer.toString(id), refresh);
	}
}
