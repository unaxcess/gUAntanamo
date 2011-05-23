package org.ua2.guantanamo;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class GUAntanamoPostMessage extends Activity {

	private int replyId;

	private static final String TAG = GUAntanamoPostMessage.class.getSimpleName();
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.postmessage);
        
        Spinner folderList = (Spinner)findViewById(R.id.messageFolderList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, GUAntanamo.getFolderNames());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        folderList.setAdapter(adapter);
        folderList.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
        
        TextView bodyText = (TextView)findViewById(R.id.messageBodyText);
        bodyText.setText("");
        bodyText.setMovementMethod(new ScrollingMovementMethod());
        
        replyId = getIntent().getIntExtra("reply", 0);
        
        String folder = getIntent().getStringExtra("folder");
        if(folder != null) {
        	for(int pos = 0; pos < GUAntanamo.getFolderNames().size(); pos++) {
        		String name = GUAntanamo.getFolderNames().get(pos);
        		Log.i(TAG, "Checking " + name + " -vs- " + folder);
        		if(name.equals(folder)) {
        			folderList.setSelection(pos);
        			break;
        		}
        	}
        }
        
        String to = getIntent().getStringExtra("to");
        if(to != null) {
        	GUAntanamo.setText(this, R.id.messageToText, to);
        }
        
        String subject = getIntent().getStringExtra("subject");
        if(subject != null) {
        	GUAntanamo.setText(this, R.id.messageSubjectText, subject);
        }
        
        String body = getIntent().getStringExtra("body");
        if(body != null) {
	        // TODO: Maybe split the body and put chevrons in front?
	        // GUAntanamo.setText(this, R.id.messageSubjectText, body);
        }
        
        Button cancelButton = (Button)findViewById(R.id.messageCancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				GUAntanamoPostMessage.this.setResult(RESULT_CANCELED);
				finish();
			}
        });

        Button actionButton = (Button)findViewById(R.id.messageActionButton);
        if(replyId > 0) {
        	actionButton.setText("Reply");
        } else {
        	actionButton.setText("Post");
        }
        actionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				postMessage();
			}
        });
	}

	private void postMessage() {
		final ProgressDialog progress = ProgressDialog.show(this, "", "Posting message...", true);
		
		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
		        try {
		        	Spinner folderList = (Spinner)findViewById(R.id.messageFolderList);
		        	String folder = (String)folderList.getSelectedItem();
			        String to = GUAntanamo.getText(GUAntanamoPostMessage.this, R.id.messageToText);
			        String subject = GUAntanamo.getText(GUAntanamoPostMessage.this, R.id.messageSubjectText);
			        String body = GUAntanamo.getText(GUAntanamoPostMessage.this, R.id.messageBodyText);
		
			        Log.i(TAG, "Posting message to " + replyId + " / " + folder + ", " + to + " " + subject + " " + body);
			        GUAntanamo.getClient().postMessage(replyId, folder, to, subject, body);
					
					return null;
		        } catch(JSONException e) {
		        	return e.getMessage();
		        }
			}
			
			protected void onPostExecute(String error) {
				progress.dismiss();

				if(error == null) {
					setResult(RESULT_OK);
					finish();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(GUAntanamoPostMessage.this);
					builder
						.setMessage("Unable to post message, " + error)
						.setCancelable(false)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			}
		}.execute();
	}

}
