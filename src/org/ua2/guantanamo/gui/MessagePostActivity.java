package org.ua2.guantanamo.gui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.GUAntanamoMessaging;
import org.ua2.json.JSONFolder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class MessagePostActivity extends Activity {

	private boolean quickMode;
	private int replyId;
	private String folder;
	private String to;
	private String subject;
	private String body;

	private Spinner folderList;
	private EditText toText;
	private EditText subjectText;
	private EditText bodyText;

	private static final String TAG = MessagePostActivity.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.post);

		MessagePostActivity retainedData = (MessagePostActivity)getLastNonConfigurationInstance();

		if(retainedData == null) {
			// No retained data from the running config change

			quickMode = getIntent().getBooleanExtra("quickMode", false);
			replyId = getIntent().getIntExtra("reply", 0);
			folder = getIntent().getStringExtra("folder");
			to = getIntent().getStringExtra("to");
			subject = getIntent().getStringExtra("subject");
			body = getIntent().getStringExtra("body");
			
		} else {
			// Retrieve stored data from before the running config changed

			quickMode = retainedData.quickMode;
			replyId = retainedData.replyId;
			folder = retainedData.folder;
			to = retainedData.to;
			subject = retainedData.subject;
			body = retainedData.body;
					
		}
		
		folderList = (Spinner)findViewById(R.id.postFolderList);
		
		List<String> names = new ArrayList<String>();

		if(quickMode) {
			names.add(folder);
		} else {
			for(JSONFolder folder : GUAntanamoMessaging.getFolders()) {
				names.add(folder.getName());
			}
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names);
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
		
		if(folder != null) {
			int pos = 0;
			for(String name : names) {
				Log.i(TAG, "Checking " + name + " -vs- " + folder);
				if(name.equals(folder)) {
					folderList.setSelection(pos);
					break;
				}
				pos++;
			}
		}
		
		toText = (EditText)findViewById(R.id.postToText);
		if(to != null) {
			toText.setText(to);
		}

		subjectText = (EditText)findViewById(R.id.postSubjectText);
		if(subject != null) {
			subjectText.setText(subject);
		}
		
		bodyText = (EditText)findViewById(R.id.postBodyText);
		bodyText.setText("");
		bodyText.setMovementMethod(new ScrollingMovementMethod());

		if(quickMode) {
			findViewById(R.id.postFolder).setVisibility(View.GONE);
			findViewById(R.id.postSubject).setVisibility(View.GONE);
		}
	}

	public Object onRetainNonConfigurationInstance() {
		// If the screen orientation, availability of keyboard, etc
		// changes, Android will kill and restart the Activity. This
		// stores its data so we can reuse it when the Activity
		// restarts

		return this;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.post, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.postSend) {
			postMessage();
			
		} else if(item.getItemId() == R.id.postQuote) {
			// TODO: Format original body with chevrons

		} else {
			return super.onContextItemSelected(item);
		}

		return true;
	}

	private void postMessage() {
		final ProgressDialog progress = ProgressDialog.show(this, "", "Posting message...", true);

		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
				try {
					String folder = (String)folderList.getSelectedItem();
					String to = toText.getText().toString();
					String subject = subjectText.getText().toString();
					String body = bodyText.getText().toString();

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
					AlertDialog.Builder builder = new AlertDialog.Builder(MessagePostActivity.this);
					builder.setMessage("Unable to post message, " + error).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
