package org.ua2.guantanamo.data;

import java.util.List;

import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.ViewMode;
import org.ua2.json.JSONMessage;

import android.content.Context;

public class CacheFolderMessages extends CacheMessages {
	@Override
	protected String getType() {
		return "messages";
	}

	@Override
	protected String getDescription() {
		return "folder";
	}

	@Override
	protected List<JSONMessage> loadItem(String id) throws JSONException {
		return convert(GUAntanamo.getClient().getMessages(id, GUAntanamo.getViewMode(false) == ViewMode.Unread, false));
	}

	public void load(Context context, Processor<List<JSONMessage>> processor, final String id, final boolean forceRefresh) {
		super._load(context, processor, id, forceRefresh);
	}
}
