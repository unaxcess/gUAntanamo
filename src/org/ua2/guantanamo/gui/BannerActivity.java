package org.ua2.guantanamo.gui;

import org.json.JSONException;
import org.json.JSONObject;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.data.CacheItem;
import org.ua2.json.JSONWrapper;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class BannerActivity extends Activity {

	private static final long ONE_DAY = 24 * 60;

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
	
	private JSONObject system;

	private static final String TAG = BannerActivity.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.banner);

		Button okButton = (Button)findViewById(R.id.bannerOKButton);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BannerActivity.this.setResult(RESULT_OK);
				finish();
			}
		});

		bannerText = (TextView)findViewById(R.id.bannerText);

		BackgroundCaller.run(this, "Getting system info...", new BackgroundWorker() {
			@Override
			public void during() throws Exception {
				CacheBanner cache = new CacheBanner(BannerActivity.this);
				
				system = cache.getItem();
				
				cache.close();
			}

			@Override
			public void after() {
				bannerText.setText(system.optString("banner", ""));
			}

			@Override
			public String getError() {
				return "Cannot get system info";
			}
			
		});
	}
}
