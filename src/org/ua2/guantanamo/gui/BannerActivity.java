package org.ua2.guantanamo.gui;

import org.json.JSONException;
import org.json.JSONObject;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.data.CacheTask;
import org.ua2.guantanamo.data.CacheTask.ItemProcessor;
import org.ua2.json.JSONWrapper;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class BannerActivity extends Activity {

	private static final int ONE_DAY = 24 * 60;
	private static final String PREFERENCE = "bannerHash";

	private static class CacheSystem extends CacheTask<JSONObject> {
		public CacheSystem(Context context, ItemProcessor<JSONObject> processor) {
			super(context, processor);
			
			load(null, false);
		}

		@Override
		protected String getType() {
			return "system";
		}
		
		@Override
		protected String getDescription() {
			return "system info";
		}
		
		@Override
		protected int getRefreshMinutes() {
			// Once per day should be enough for a banner
			return ONE_DAY;
		}

		@Override
		protected JSONObject loadItem(String id) throws JSONException {
			return GUAntanamo.getClient().get("/system").getObject();
		}
		
		@Override
		protected JSONObject convertDataToItem(String data) throws JSONException {
			return JSONWrapper.parse(data).getObject();
		}
	};
	
	private TextView bannerText;

	private ItemProcessor<JSONObject> processor = null;

	private static class State {
		CacheSystem caller;
	}	
	private State state;

	private static final String TAG = BannerActivity.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.banner);

		Button okButton = (Button)findViewById(R.id.bannerOKButton);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				hide(null);
			}
		});

		bannerText = (TextView)findViewById(R.id.bannerText);
		
		processor = new ItemProcessor<JSONObject>() {
			@Override
			public void processItem(JSONObject system, boolean isNew) {
				showBanner(system);
			}
		};

		state = (State)getLastNonConfigurationInstance();
		if(state == null) {
			state = new State();
			
			state.caller = new CacheSystem(this, processor);
		}
	}
	
	public void onStart() {
		super.onStart();

		state.caller.attach(this, processor);
	}
	
	public void onStop() {
		super.onStop();

		state.caller.detatch();
	}

	public Object onRetainNonConfigurationInstance() {
		return state;
	}
	
	private void showBanner(JSONObject system) {
		String banner = system.optString("banner");
		if(banner != null) {
			int hash = banner.hashCode();
			if(hash != getPreferences(MODE_PRIVATE).getInt(PREFERENCE, 0)) {
				bannerText.setText(system.optString("banner", ""));

				SharedPreferences settings = getPreferences(MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt(PREFERENCE, hash);
				editor.commit();

			} else {
				hide("Banner hash same as last time");
			}
		}
	}

	private void hide(String msg) {
		if(msg != null) {
			Log.d(TAG, msg);
		}
		
		setResult(RESULT_OK);
		finish();
	}
}
