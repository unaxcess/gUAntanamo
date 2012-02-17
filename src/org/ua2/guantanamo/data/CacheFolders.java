package org.ua2.guantanamo.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.GUAntanamoMessaging;
import org.ua2.json.JSONFolder;
import org.ua2.json.JSONItem;
import org.ua2.json.JSONWrapper;

import android.content.Context;

/**
 * Reusable cache for folders as it's useful to several classes
 * 
 * @author Techno
 * 
 */
public class CacheFolders extends CacheItem<Collection<JSONFolder>> {

	public CacheFolders(Context context) {
		super(context, "folders", null);
	}

	private Collection<JSONFolder> convert(JSONArray array) throws JSONException {
		List<JSONFolder> folders = new ArrayList<JSONFolder>();
		for(int pos = 0; pos < array.length(); pos++) {
			folders.add(new JSONFolder(array.getJSONObject(pos)));
		}
		return folders;
	}

	@Override
	protected long getStaleMinutes() {
		return GUAntanamoMessaging.getFolderRefreshMinutes();
	}

	@Override
	protected Collection<JSONFolder> refreshItem() throws JSONException {
		return convert(GUAntanamo.getClient().getFolders());
	}

	@Override
	protected Collection<JSONFolder> toItem(String data) throws JSONException {
		return convert(JSONWrapper.parse(data).getArray());
	}

	@Override
	protected String toData(Collection<JSONFolder> folders) {
		return JSONItem.collectionToString(folders);
	}

}
