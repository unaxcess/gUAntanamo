package org.ua2.guantanamo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GUAntanamoViewMessage extends Activity {
	
	private static final DateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat("EE, dd MMM yyyy - HH:mm:ss");

	private static final int ACTIVITY_POST = 1;

	private static final String TAG = GUAntanamoViewMessage.class.getSimpleName();
	
	private int id;
	private boolean read;
	private int pos;
	private JSONObject message;
	
	private String getSuffix(int pos) {
		String suffix = "" + pos;
		
		if(pos % 10 == 1 && pos % 100 != 11) {
			suffix += "st";
		} else if(pos % 10 == 2 && pos % 100 != 12) {
			suffix += "nd";
		} else if(pos % 10 == 3 && pos % 100 != 13) {
			suffix += "rd";
		} else {
			suffix += "th";
		}
		
		return suffix;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.viewmessage);
        
        TextView bodyText = (TextView)findViewById(R.id.messageBodyText);
        bodyText.setMovementMethod(new ScrollingMovementMethod());
        
        id = getIntent().getIntExtra("message", 0);

        read = getIntent().getBooleanExtra("read", false);
        
        Button prevButton = (Button)findViewById(R.id.messagePrevButton);
        prevButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMessage(KeyEvent.KEYCODE_DPAD_LEFT);
			}
        });

        Button nextButton = (Button)findViewById(R.id.messageNextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMessage(KeyEvent.KEYCODE_DPAD_RIGHT);
			}
        });

        Button replyButton = (Button)findViewById(R.id.messageReplyButton);
        replyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
	    		Intent intent = new Intent(GUAntanamoViewMessage.this, GUAntanamoPostMessage.class);

	    		try {
					intent.putExtra("reply", message.getInt("id"));
					intent.putExtra("folder", message.getString("folder"));
					intent.putExtra("to", message.getString("from"));
					intent.putExtra("subject", message.getString("subject"));
					intent.putExtra("body", message.getString("body"));

					startActivityForResult(intent, ACTIVITY_POST);
				} catch(JSONException e) {
					Log.e(TAG, "Unable to start post message", e);
					
					AlertDialog.Builder builder = new AlertDialog.Builder(GUAntanamoViewMessage.this);
					builder
						.setMessage("Unable to start post message, " + e.getMessage())
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
        });

        showMessage(0);
	}

	private void showMessage(final int direction) {
		final ProgressDialog progress = ProgressDialog.show(this, "", "Loading message...", true);
		
		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
		        try {
					for(int msgPos = 0; msgPos < GUAntanamo.getMessages().length(); msgPos++) {
						int msgPosId = GUAntanamo.getMessages().getJSONObject(msgPos).getInt("id");
						if(msgPosId == id) {
							pos = msgPos;
							break;
						}
					}
					
					boolean valid = true;
					
					if(direction == KeyEvent.KEYCODE_DPAD_LEFT) {
						if(pos > 0) {
							pos--;
							id = GUAntanamo.getMessages().getJSONObject(pos).getInt("id");
						} else {
							valid = false;
						}
					} else if(direction == KeyEvent.KEYCODE_DPAD_RIGHT) {
						if(pos < GUAntanamo.getMessages().length() - 1) {
							pos++;
							id = GUAntanamo.getMessages().getJSONObject(pos).getInt("id");
						} else {
							valid = false;
						}
					}

					if(valid) {
				        message = GUAntanamo.getClient().getMessage(id);
					}
					
					return null;
		        } catch(JSONException e) {
		        	return e.getMessage();
		        }
			}
			
			protected void onPostExecute(String error) {
				progress.dismiss();

				if(error == null) {
					try {
						setTitle(message.getString("folder") + (read ? " [Re-read]" : ""));
						
				        TextView idText = (TextView)findViewById(R.id.messageIdText);
				        idText.setText(message.getInt("id") + " (" + getSuffix(pos + 1) + " of " + GUAntanamo.getMessages().length() + ")");
				        
				        TextView dateText = (TextView)findViewById(R.id.messageDateText);
				        dateText.setText(TIMESTAMP_FORMATTER.format(new Date(1000 * message.getLong("epoch"))));
				        
				        TextView fromText = (TextView)findViewById(R.id.messageFromText);
				        fromText.setText(message.getString("from"));
				        
				        TextView toText = (TextView)findViewById(R.id.messageToText);
				        if(!message.isNull("to")) {
				        	toText.setText(message.getString("to"));
				        	findViewById(R.id.messageTo).setVisibility(View.VISIBLE);
				        } else {
				        	findViewById(R.id.messageTo).setVisibility(View.GONE);
				        	// toText.setVisibility(View.GONE);
				        }
				        
				        TextView subjectText = (TextView)findViewById(R.id.messageSubjectText);
				        if(!message.isNull("subject")) {
				        	subjectText.setText(message.getString("subject"));
				        	findViewById(R.id.messageSubject).setVisibility(View.VISIBLE);
				        } else {
				        	//subjectText.setVisibility(View.GONE);
				        	findViewById(R.id.messageSubject).setVisibility(View.GONE);
				        }
				        
				        TextView inReplyToText = (TextView)findViewById(R.id.messageInReplyToText);
				        if(!message.isNull("inReplyTo")) {
				        	StringBuilder inReplyToStringBuilder = new StringBuilder(message.getString("inReplyTo"));
				        	
				        	JSONArray inReplyToArray = message.optJSONArray("inReplyToHierarchy");
				        	if(inReplyToArray != null) {
				        		int replyArraySize = inReplyToArray.length();
				        		
				        		if(replyArraySize > 1)
				        		{
				        			inReplyToStringBuilder
				        				.append(", plus ")
				        				.append(replyArraySize - 1)
				        				.append(" more");
				        		}
				        	}
				        	
				        	inReplyToText.setText(inReplyToStringBuilder.toString());
				        	findViewById(R.id.messageInReplyTo).setVisibility(View.VISIBLE);
				        } else {
				        	findViewById(R.id.messageInReplyTo).setVisibility(View.GONE);			        	
				        }
				        
				        TextView repliedToInText = (TextView)findViewById(R.id.messageRepliedToInText);
				        findViewById(R.id.messageRepliedToIn).setVisibility(View.GONE);
				        
				        TextView bodyText = (TextView)findViewById(R.id.messageBodyText);
				        if(!message.isNull("body")) {
				        	bodyText.setText(message.getString("body"));
				        } else {
				        	bodyText.setText("");
				        }
				        
				        boolean read = GUAntanamo.getMessages().getJSONObject(pos).getBoolean("read");
				        if(!read) {
				        	GUAntanamo.getMessages().getJSONObject(pos).put("read", true);
				        	GUAntanamo.getFolder().put("unread", GUAntanamo.getFolder().getInt("unread"));
				        }
			
				        GUAntanamo.getClient().markMessage(id, true);
					} catch(JSONException e) {
						Log.e(TAG, "Cannot show message", e);
					}
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(GUAntanamoViewMessage.this);
					builder
						.setMessage("Unable to get message, " + error)
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
