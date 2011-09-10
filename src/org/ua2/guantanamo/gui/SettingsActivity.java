package org.ua2.guantanamo.gui;

import org.ua2.guantanamo.GUAntanamo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SettingsActivity extends Activity {

	private String url;
	private String username;
	private String password;

	TextView urlText;
	TextView usernameText;
	TextView passwordText;

	public Object onRetainNonConfigurationInstance() {
		// If the screen orientation, availability of keyboard, etc
		// changes, Android will kill and restart the Activity. This
		// stores its data so we can reuse it when the Activity
		// restarts

		return this;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings);

		String url = GUAntanamo.getUrl();
		String username = GUAntanamo.getUsername();
		String password = GUAntanamo.getPassword();

		SettingsActivity retainedData = (SettingsActivity)getLastNonConfigurationInstance();
		if(retainedData != null) {
			url = retainedData.url;
			username = retainedData.username;
			password = retainedData.password;
		}

		urlText = (TextView)findViewById(R.id.settingsUrlText);
		urlText.setText(url);

		usernameText = (TextView)findViewById(R.id.settingsUsernameText);
		usernameText.setText(username);

		passwordText = (TextView)findViewById(R.id.settingsPasswordText);
		passwordText.setText(password);

		Button saveButton = (Button)findViewById(R.id.saveButton);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				GUAntanamo.setUrl(urlText.getText().toString().trim());
				GUAntanamo.setUsername(usernameText.getText().toString().trim());
				GUAntanamo.setPassword(passwordText.getText().toString());

				setResult(RESULT_OK);
				finish();
			}
		});
	}
}
