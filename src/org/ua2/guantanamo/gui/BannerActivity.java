package org.ua2.guantanamo.gui;

import org.json.JSONException;
import org.json.JSONObject;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.data.CacheItem;
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

	private static final long ONE_DAY = 24 * 60;
	private static final String PREFERENCE = "bannerHash";

	private class CacheBanner extends CacheItem<JSONObject> {

		public CacheBanner(Context context) throws JSONException {
			super(context, "system", null);
		}

		@Override
		protected long getStaleMinutes() {
			// Once per day should be enough for a banner
			return ONE_DAY;
		}

		@Override
		protected JSONObject refreshItem() throws JSONException {
			return GUAntanamo.getClient().get("/system").getObject();
		}

		@Override
		protected JSONObject toItem(String data) throws JSONException {
			return JSONWrapper.parse(data).getObject();
		}
	};
	
	private TextView bannerText;
	
	private BackgroundCaller caller;

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

		if(getLastNonConfigurationInstance() != null) {
			caller = ((BannerActivity)getLastNonConfigurationInstance()).caller;
		}
		showBanner();
	}

	public Object onRetainNonConfigurationInstance() {
		// If the screen orientation, availability of keyboard, etc
		// changes, Android will kill and restart the Activity. This
		// stores its data so we can reuse it when the Activity
		// restarts

		if(caller != null) {
			caller.detach();
		}

		return this;
	}
	
	private void showBanner() {
		if(caller != null) {
			caller.attach(this);
			return;
		}
		
		caller = BackgroundCaller.run(new BackgroundCaller(this, "Getting system info") {
			JSONObject system;
			
			@Override
			public void during() throws Exception {
				system = new CacheBanner(getContext()).getAndClose(false);
			}

			@Override
			public void after() {
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
		});
	}

	private void hide(String msg) {
		if(msg != null) {
			Log.d(TAG, msg);
		}
		
		setResult(RESULT_OK);
		finish();
	}
}
