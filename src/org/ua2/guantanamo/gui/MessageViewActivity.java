package org.ua2.guantanamo.gui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.GUAntanamoMessaging;
import org.ua2.guantanamo.ViewMode;
import org.ua2.json.JSONMessage;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class MessageViewActivity extends Activity {

	private static final DateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat("EE, dd MMM yyyy - HH:mm:ss");
	private static final int ACTIVITY_POST = 1;

	private GestureDetector detector;

	private static class State {
		int id;
		JSONMessage message;
		NavType direction;
		boolean refresh;
		
		BackgroundCaller caller;
	}
	
	private State state;

	private static final String TAG = MessageViewActivity.class.getName();

	private void setMessageView(ViewMode viewMode) {
		GUAntanamo.setViewMode(viewMode, false);
		if(GUAntanamo.getViewMode(false) != viewMode) {
			setTitle(state.message);
		}
	}
	
	private void setTitle(JSONMessage message) {
		StringBuilder sb = new StringBuilder();
		sb.append(message.getId()).append(" in ").append(message.getFolder());
		sb.append(" [");
		sb.append(message.getPosition()).append(" of ").append(GUAntanamoMessaging.getCurrentFolder().getCount());
		sb.append(" ").append(GUAntanamo.getViewMode(false).name());
		sb.append("]");
	
		setTitle(sb.toString());
	}
	
	private void showMessage(NavType direction, boolean refresh) {
		showMessage(direction, refresh, true);
	}

	private void showMessage(NavType direction, boolean refresh, boolean newCaller) {
		state.direction = direction;
		state.refresh = refresh;
		if(newCaller) {
			state.caller = null;
		}
		
		if(state.direction != null || state.id == 0) {
			state.id = GUAntanamoMessaging.getMessageId(state.direction);
		}
		
		if(state.id > 0) {
			state.caller = BackgroundCaller.run(state.caller, "Getting message", new BackgroundWorker() {
				@Override
				public void during() throws Exception {
						state.message = GUAntanamoMessaging.setCurrentMessage(state.id, state.refresh);
					}
					
					public void after() {
						showMessage();
					}

					@Override
					public Context getContext() {
						return MessageViewActivity.this;
					}
				});
		} else {
			setResult(RESULT_OK);
			finish();
		}	
	}
	
	private void showMessage() {
		setTitle(state.message);

		TextView dateText = (TextView)findViewById(R.id.viewDateText);
		dateText.setText(TIMESTAMP_FORMATTER.format(state.message.getDate()));

		TextView fromText = (TextView)findViewById(R.id.viewFromText);
		fromText.setText(state.message.getFrom());

		TextView toText = (TextView)findViewById(R.id.viewToText);
		if(state.message.getTo() != null) {
			toText.setText(state.message.getTo());
			findViewById(R.id.viewTo).setVisibility(View.VISIBLE);
		} else {
			// TODO: Why doesn't this hide properly?
			findViewById(R.id.viewTo).setVisibility(View.GONE);
		}

		TextView subjectText = (TextView)findViewById(R.id.viewSubjectText);
		if(state.message.getSubject() != null) {
			subjectText.setText(state.message.getSubject());
			findViewById(R.id.viewSubject).setVisibility(View.VISIBLE);
		} else {
			// TODO: Why doesn't this hide properly?
			findViewById(R.id.viewSubject).setVisibility(View.GONE);
		}

		TextView inReplyToText = (TextView)findViewById(R.id.viewInReplyToText);
		List<Integer> parents = state.message.getInReplyToHierarchy();
		List<Integer> children = state.message.getReplyToBy();
		StringBuilder replyStr = new StringBuilder();
		if(parents.size() > 0) {
			replyStr.append(parents.get(0));
			if(parents.size() > 1) {
				replyStr.append(" [").append(parents.size() - 1).append(" more]");
			}
		}
		if(children.size() > 0) {
			if(replyStr.length() > 0) {
				replyStr.append(" / ");
			}
			if(children.size() != 1) {
				replyStr.append(children.size()).append(" replies");
			} else {
				replyStr.append("1 reply");
			}
		}
		if(replyStr.length() > 0) {
			inReplyToText.setText(replyStr);
			findViewById(R.id.viewInReplyToText).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.viewInReplyToText).setVisibility(View.GONE);
		}

		TextView bodyText = (TextView)findViewById(R.id.viewBodyText);
		if(state.message.getBody() != null) {
			bodyText.setText(state.message.getBody());
		} else {
			bodyText.setText("");
		}
		bodyText.scrollTo(0, 0);

		if(!state.message.isRead()) {
			try {
				GUAntanamoMessaging.markCurrentMessageRead();
			} catch(JSONException e) {
				GUAntanamo.handleException(this, "Cannot mark message", e);
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.view);

		DisplayMetrics metrics = new DisplayMetrics(); getWindowManager().getDefaultDisplay().getMetrics(metrics);
		detector = new GestureDetector(new NavGestureDetector(metrics.widthPixels / 2, metrics.heightPixels / 2) {
			@Override
			protected void performAction(NavType direction) {
				showMessage(direction, false);
			}
		});

		TextView bodyText = (TextView)findViewById(R.id.viewBodyText);
		bodyText.setMovementMethod(new ScrollingMovementMethod());

		state = (State)getLastNonConfigurationInstance();
		if(state == null) {
			state = new State();
			
			state.id = getIntent().getIntExtra("message", 0);
		}
		
		showMessage(null, false);
	}

	public void onStop() {
		super.onStop();
		
		setResult(RESULT_OK);
	}

	public Object onRetainNonConfigurationInstance() {
		state.caller.pause();
		
		return state;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.viewUnread) {
			setMessageView(ViewMode.Unread);

		} else if(item.getItemId() == R.id.viewAll) {
			setMessageView(ViewMode.All);

		} else if(item.getItemId() == R.id.viewRefresh) {
			showMessage(null, true);

		} else if(item.getItemId() == R.id.viewPost || item.getItemId() == R.id.viewReply) {
			Intent intent = new Intent(this, MessagePostActivity.class);

			intent.putExtra("folder", state.message.getFolder());

			if(item.getItemId() == R.id.viewReply) {
				intent.putExtra("reply", state.message.getId());
				intent.putExtra("to", state.message.getFrom());
				intent.putExtra("subject", state.message.getSubject());
				intent.putExtra("body", state.message.getBody());
			}

			startActivityForResult(intent, ACTIVITY_POST);
			
		} else if(item.getItemId() == R.id.viewPage) {
			Intent intent = new Intent(this, MessagePostActivity.class);
			
			intent.putExtra("pageMode", true);
			
			startActivityForResult(intent, ACTIVITY_POST);

		} else if(item.getItemId() == R.id.viewHold) {
			holdMessage();

		} else if(item.getItemId() == R.id.viewCatchup) {
			catchupThread();

		} else {
			return super.onContextItemSelected(item);
		}

		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(detector.onTouchEvent(event)) {
			return true;
		} else {
			return false;
		}
	}

	private void holdMessage() {
		final ProgressDialog progress = ProgressDialog.show(this, "", "Holding message...", true);

		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
				try {
					int id = GUAntanamoMessaging.getCurrentMessageId();

					Log.i(TAG, "Saving message " + id);
					GUAntanamo.getClient().saveMessage(id);

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
					AlertDialog.Builder builder = new AlertDialog.Builder(MessageViewActivity.this);
					builder.setMessage("Unable to save message, " + error).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
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

	private void catchupThread() {
		final ProgressDialog progress = ProgressDialog.show(this, "", "Catching up thread...", true);

		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
				try {
					int id = GUAntanamoMessaging.getCurrentThreadId();

					Log.i(TAG, "Marking thread " + id);
					GUAntanamo.getClient().markThread(id, true);

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
					AlertDialog.Builder builder = new AlertDialog.Builder(MessageViewActivity.this);
					builder.setMessage("Unable to catch up thread, " + error).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
