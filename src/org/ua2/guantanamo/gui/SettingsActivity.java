package org.ua2.guantanamo.gui;

import org.ua2.guantanamo.GUAntanamo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SettingsActivity extends Activity {

	private static class State {
		String url;
		String username;
		String password;
	}
	
	private State state;
	
	TextView urlText;
	TextView usernameText;
	TextView passwordText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings);

		state = (State)getLastNonConfigurationInstance();
		if(state == null) {
			state = new State();

			state.url = GUAntanamo.getUrl();
			state.username = GUAntanamo.getUsername();
			state.password = GUAntanamo.getPassword();
		}

		urlText = (TextView)findViewById(R.id.settingsUrlText);
		urlText.setText(state.url);

		usernameText = (TextView)findViewById(R.id.settingsUsernameText);
		usernameText.setText(state.username);

		passwordText = (TextView)findViewById(R.id.settingsPasswordText);
		passwordText.setText(state.password);

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

	public Object onRetainNonConfigurationInstance() {
		return state;
	}
}
