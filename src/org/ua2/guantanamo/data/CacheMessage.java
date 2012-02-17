package org.ua2.guantanamo.data;

import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.json.JSONMessage;
import org.ua2.json.JSONWrapper;

import android.content.Context;

public class CacheMessage extends CacheItem<JSONMessage> {
	
	private int id;

	public CacheMessage(Context context, int id) throws JSONException {
		super(context, "message", Integer.toString(id));
		
		this.id = id;
	}

	@Override
	protected long getStaleMinutes() {
		return 0;
	}

	@Override
	protected JSONMessage refreshItem() throws JSONException {
		return new JSONMessage(GUAntanamo.getClient().getMessage(id));
	}

	protected boolean shouldRefresh() {
		return false;
	}

	@Override
	protected JSONMessage toItem(String data) throws JSONException {
		return new JSONMessage(JSONWrapper.parse(data).getObject());
	}
}
