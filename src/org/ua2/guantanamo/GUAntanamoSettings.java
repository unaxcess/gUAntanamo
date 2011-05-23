package org.ua2.guantanamo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GUAntanamoSettings extends Activity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
   
        setContentView(R.layout.settings);

        String url = GUAntanamo.getUrl();
        String username = GUAntanamo.getUsername();
        String password = GUAntanamo.getPassword();

        final TextView urlText = (TextView)findViewById(R.id.settingsUrlText);
        urlText.setText(url);
        
        final TextView usernameText = (TextView)findViewById(R.id.settingsUsernameText);
        usernameText.setText(username);
        
        final TextView passwordText = (TextView)findViewById(R.id.settingsPasswordText);
        passwordText.setText(password);
        
        Button saveButton = (Button)findViewById(R.id.settingsSaveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String url = urlText.getText().toString().trim();
				String username = usernameText.getText().toString().trim();
				String password = passwordText.getText().toString().trim();

		        if(!("".equals(url) && "".equals(username) && "".equals(password))) {
		        	GUAntanamo.setUrl(url);
		        	GUAntanamo.setUsername(username);
		        	GUAntanamo.setPassword(password);
					
		        	setResult(RESULT_OK);
		        	finish();
		        }
			}
        });

        Button cancelButton = (Button)findViewById(R.id.settingsCancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
        });
	}
}
