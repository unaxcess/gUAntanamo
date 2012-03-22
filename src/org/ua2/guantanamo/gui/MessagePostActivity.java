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

	private static class State {
		boolean quickMode;
		int replyId;
		String folder;
		String to;
		String subject;
		String body;
		
		BackgroundCaller caller;
	}
	
	private State state;
	
	private Spinner folderList;
	private EditText toText;
	private EditText subjectText;
	private EditText bodyText;

	private static final String TAG = MessagePostActivity.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.post);

		state = (State)getLastNonConfigurationInstance();
		if(state == null) {
			state = new State();
			
			state.quickMode = getIntent().getBooleanExtra("quickMode", false);
			state.replyId = getIntent().getIntExtra("reply", 0);
			state.folder = getIntent().getStringExtra("folder");
			state.to = getIntent().getStringExtra("to");
			state.subject = getIntent().getStringExtra("subject");
			state.body = getIntent().getStringExtra("body");
		}
		
		folderList = (Spinner)findViewById(R.id.postFolderList);
		
		List<String> names = new ArrayList<String>();

		if(state.quickMode) {
			names.add(state.folder);
		} else {
			for(JSONFolder folder : GUAntanamoMessaging.getFolderList(false)) {
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
		
		if(state.folder != null) {
			int pos = 0;
			for(String name : names) {
				Log.i(TAG, "Checking " + name + " -vs- " + state.folder);
				if(name.equals(state.folder)) {
					folderList.setSelection(pos);
					break;
				}
				pos++;
			}
		}
		
		toText = (EditText)findViewById(R.id.postToText);
		if(state.to != null) {
			toText.setText(state.to);
		}

		subjectText = (EditText)findViewById(R.id.postSubjectText);
		if(state.subject != null) {
			subjectText.setText(state.subject);
		}
		
		bodyText = (EditText)findViewById(R.id.postBodyText);
		bodyText.setText(state.body);
		bodyText.setMovementMethod(new ScrollingMovementMethod());

		if(state.quickMode) {
			findViewById(R.id.postFolder).setVisibility(View.GONE);
			findViewById(R.id.postSubject).setVisibility(View.GONE);
		}
	}

	public Object onRetainNonConfigurationInstance() {
		state.caller.pause();
		
		return state;
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
		final ProgressDialog progress = ProgressDialog.show(this, "", "Posting message", true);

		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
				try {
					String folder = (String)folderList.getSelectedItem();
					String to = toText.getText().toString();
					String subject = subjectText.getText().toString();
					String body = bodyText.getText().toString();

					Log.i(TAG, "Posting message to " + state.replyId + " / " + folder + ", " + to + " " + subject + " " + body);
					GUAntanamo.getClient().postMessage(state.replyId, folder, to, subject, body);

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
