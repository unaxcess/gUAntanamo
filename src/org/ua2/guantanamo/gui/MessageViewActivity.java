package org.ua2.guantanamo.gui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.json.JSONException;
import org.ua2.guantanamo.GUAntanamo;
import org.ua2.guantanamo.GUAntanamoMessaging;
import org.ua2.guantanamo.NavType;
import org.ua2.guantanamo.ViewMode;
import org.ua2.guantanamo.data.CacheItem;
import org.ua2.json.JSONMessage;
import org.ua2.json.JSONWrapper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class MessageViewActivity extends Activity {

	private class CacheMessage extends CacheItem<JSONMessage> {

		public CacheMessage(Context context) throws JSONException {
			super(context, "message", Integer.toString(id));
		}

		@Override
		protected long getStaleMinutes() {
			return 0;
		}

		@Override
		protected JSONMessage refreshItem() throws JSONException {
			return new JSONMessage(GUAntanamo.getClient().getMessage(id));
		}

		protected boolean shouldRefresh() {
			return false;
		}

		@Override
		protected JSONMessage toItem(String data) throws JSONException {
			return new JSONMessage(JSONWrapper.parse(data).getObject());
		}
	}

	private class MyGestureDetector extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			NavType direction = null;
			
			if(getDirection(e1.getX(), e2.getX(), thresholdX, velocityX) == 1 && getDirection(e1.getY(), e2.getY(), thresholdY / 2, velocityY) == 0) {
				// Right
				direction = NavType.PrevMessage;
				
			} else if(getDirection(e1.getX(), e2.getX(), thresholdX, velocityX) == -1 && getDirection(e1.getY(), e2.getY(), thresholdY / 2, velocityY) == 0) {
				// Left
				direction = NavType.NextMessage;
				
			} else if(getDirection(e1.getX(), e2.getX(), thresholdX, velocityX / 2) == 0 && getDirection(e1.getY(), e2.getY(), thresholdY, velocityY) == 1) {
				// Down
				direction = NavType.PrevThread;
				
			} else if(getDirection(e1.getX(), e2.getX(), thresholdX, velocityX / 2) == 0 && getDirection(e1.getY(), e2.getY(), thresholdY, velocityY) == -1) {
				// Up
				direction = NavType.NextThread;
				
			}
			
			Log.i(TAG, "Fling " + e1.getX() + " -> " + e2.getX() + " @ " + velocityX + " , " + e1.getY() + " -> " + e2.getY() + " @ " + velocityY + " -> " + direction);
			
			if(direction != null) {
				showMessage(direction, false);
				
				return true;
			}
		
			return super.onFling(e1, e2, velocityX, velocityY);
		}
	}

	private static final DateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat("EE, dd MMM yyyy - HH:mm:ss");
	private static final int ACTIVITY_POST = 1;

	private int id;
	private JSONMessage message;
	
	// Stuff for swipe support
	private int thresholdX;
	private int thresholdY;

	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	private GestureDetector detector;;

	private static final String TAG = MessageViewActivity.class.getSimpleName();

	private int getDirection(float p1, float p2, int threshold, float velocity) {
		if(Math.abs(velocity) > SWIPE_THRESHOLD_VELOCITY) {
			if(p2 - p1 > threshold) {
				Log.d(TAG, "getDirection " + p2 + " - " + p1 + " > " + threshold + " exit 1");
				return 1;
			} else if(p1 - p2 > threshold) {
				Log.d(TAG, "getDirection " + p1 + " - " + p2 + " > " + threshold + " exit -1");
				return -1;
			}
		}
		
		Log.d(TAG, "getDirection " + p1 + " -vs- " + p2 + " @ " + velocity + " exit 0");
		return 0;
	}

	private void setMessageView(ViewMode viewMode) {
		GUAntanamo.setViewMode(viewMode, false);
		if(GUAntanamo.getViewMode(false) != viewMode) {
			setTitle(message);
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

	private void showMessage(final NavType type, final boolean refresh) {
		id = GUAntanamoMessaging.getCurrentMessageId(type);
		if(id > 0) {
			BackgroundCaller.run(this, "Getting message...", new BackgroundWorker() {
				@Override
				public void during() throws Exception {				
					CacheMessage cache = new CacheMessage(MessageViewActivity.this);
					
					if(refresh) {
						cache.clear();
					}
					message = cache.getItem();
					
					cache.close();
					
					GUAntanamoMessaging.setCurrentMessageId(message.getId());
				}

				@Override
				public void after() {
					setTitle(message);

					TextView dateText = (TextView)findViewById(R.id.viewDateText);
					dateText.setText(TIMESTAMP_FORMATTER.format(message.getDate()));

					TextView fromText = (TextView)findViewById(R.id.viewFromText);
					fromText.setText(message.getFrom());

					TextView toText = (TextView)findViewById(R.id.viewToText);
					if(message.getTo() != null) {
						toText.setText(message.getTo());
						findViewById(R.id.viewTo).setVisibility(View.VISIBLE);
					} else {
						// TODO: Why doesn't this hide properly?
						findViewById(R.id.viewTo).setVisibility(View.GONE);
					}

					TextView subjectText = (TextView)findViewById(R.id.viewSubjectText);
					if(message.getSubject() != null) {
						subjectText.setText(message.getSubject());
						findViewById(R.id.viewSubject).setVisibility(View.VISIBLE);
					} else {
						// TODO: Why doesn't this hide properly?
						findViewById(R.id.viewSubject).setVisibility(View.GONE);
					}

					TextView inReplyToText = (TextView)findViewById(R.id.viewInReplyToText);
					List<Integer> parents = message.getInReplyToHierarchy();
					List<Integer> children = message.getReplyToBy();
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
					if(message.getBody() != null) {
						bodyText.setText(message.getBody());
					} else {
						bodyText.setText("");
					}
					bodyText.scrollTo(0, 0);

					if(!message.isRead()) {
						try {
							GUAntanamoMessaging.markCurrentMessageRead();
						} catch(JSONException e) {
							GUAntanamo.handleException(MessageViewActivity.this, "Cannot mark message", e);
						}
					}
				}

				@Override
				public String getError() {
					return "Cannot get message";
				}
				
			});
		} else {
			setResult(RESULT_OK);
			finish();
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.view);

		DisplayMetrics metrics = new DisplayMetrics(); getWindowManager().getDefaultDisplay().getMetrics(metrics);
		thresholdX = metrics.widthPixels / 2;
		thresholdY = metrics.heightPixels / 2;
		
		detector = new GestureDetector(new MyGestureDetector());

		TextView bodyText = (TextView)findViewById(R.id.viewBodyText);
		bodyText.setMovementMethod(new ScrollingMovementMethod());

		MessageViewActivity retainedData = (MessageViewActivity)getLastNonConfigurationInstance();

		if(retainedData == null) {
			// No retained data from the running config change

			id = getIntent().getIntExtra("message", 0);
		} else {
			// Retrieve stored data from before the running config changed

			id = retainedData.id;
		}
		
		GUAntanamoMessaging.setCurrentMessageId(id);

		showMessage(null, false);
	}

	public void onStop() {
		super.onStop();
		
		setResult(RESULT_OK);
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
			Intent intent = new Intent(MessageViewActivity.this, MessagePostActivity.class);

			intent.putExtra("folder", message.getFolder());

			if(item.getItemId() == R.id.viewReply) {
				intent.putExtra("reply", message.getId());
				intent.putExtra("to", message.getFrom());
				intent.putExtra("subject", message.getSubject());
				intent.putExtra("body", message.getBody());
			}

			startActivityForResult(intent, ACTIVITY_POST);
			
		} else if(item.getItemId() == R.id.viewPage) {
			Intent intent = new Intent(this, MessagePostActivity.class);
			
			intent.putExtra("pageMode", true);
			
			startActivityForResult(intent, ACTIVITY_POST);

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
}
