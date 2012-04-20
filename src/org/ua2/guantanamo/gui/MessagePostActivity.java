package org.ua2.guantanamo.gui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.GUAntanamoMessaging;
import org.ua2.guantanamo.data.BackgroundTask;
import org.ua2.json.JSONMessage;

import android.app.Activity;
import android.content.Context;
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
		
		String replyBody;
		
		MessagePoster post;
	}
	
	private static class MessagePoster extends BackgroundTask<JSONMessage> {
		private int replyId;
		private String folder;
		private String to;
		private String subject;
		private String body;
		
		@Override
		protected String getRunningMessage() {
			return "Posting message";
		}

		@Override
		protected String getErrorMessage() {
			return "Cannot post message";
		}

		@Override
		protected JSONMessage runItem() throws JSONException {
			Log.i(TAG, "Posting message to " + replyId + " / " + folder + ", " + to + " " + subject + " " + body);
			return GUAntanamo.getClient().postMessage(replyId, folder, to, subject, body);
		}

		public void load(Context context, int replyId, String folder, String to, String subject, String body) {
			this.replyId = replyId;
			this.folder = folder;
			this.to = to;
			this.subject = subject;
			this.body = body;

			super._run(context);
		}

		@Override
		protected void doReady() {
		}
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
			state.body = "";
			
			state.replyBody = getIntent().getStringExtra("body");
			
			state.post = new MessagePoster();
		}
		
		folderList = (Spinner)findViewById(R.id.postFolderList);
		
		List<String> names = new ArrayList<String>();

		if(state.quickMode) {
			names.add(state.folder);
		} else {
			names.addAll(GUAntanamoMessaging.getFolderNames());
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
	
	public void onResume() {
		super.onResume();

		state.post.attach(this);
	}
	
	public void onStop() {
		super.onStop();

		state.post.detatch();
		
		setResult(RESULT_OK);
	}

	public Object onRetainNonConfigurationInstance() {
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
			bodyText.setText(bodyText.getText() + "\n\n>" + state.replyBody);

		} else {
			return super.onContextItemSelected(item);
		}

		return true;
	}

	private void postMessage() {
		state.post.load(this,
			state.replyId,
			(String)folderList.getSelectedItem(),
			toText.getText().toString(),
			subjectText.getText().toString(),
			bodyText.getText().toString());
	}

}
