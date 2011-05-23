package org.ua2.guantanamo;

import java.io.IOException;
import java.security.Principal;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.ua2.json.ClientResponse;
import org.ua2.json.JSONClient;

import android.app.Activity;
import android.util.Log;

class GUAntanamoClient extends JSONClient {
	private final String url;
	private final DefaultHttpClient client;
	
	private static String userAgent = "gUAntanmo";
	
	private static final String TAG = GUAntanamoClient.class.getSimpleName();
	
	public GUAntanamoClient(Activity activity, String url, final String username, final String password) {
		super();
		
		this.url = url;

		Log.i(TAG, "Creating HTTP client");
		client = new DefaultHttpClient();

		final Principal principal = new Principal() {
			@Override
			public String getName() {
				return username;
			}};

		client.getCredentialsProvider().setCredentials(AuthScope.ANY, new Credentials() {
			@Override
			public String getPassword() {
				return password;
			}

			@Override
			public Principal getUserPrincipal() {
				return principal;
			}
		});
	}
	
	public void start() {
	}

	@Override
	public ClientResponse getResponse(String uri, String data) {
		ClientResponse clientResponse = new ClientResponse();

		HttpRequestBase request = null;

		try {
			HttpGet get = null;
			HttpPost post = null;
			if(data == null) {
				get = new HttpGet(url + uri);
				request = get;
			} else {
				post = new HttpPost(url + uri);
				post.setEntity(new StringEntity(data));
				post.setHeader("Content-Type", "application/json");
				request = post;
			}
			request.setHeader("User-Agent", userAgent);

			Log.d(TAG, "Requesting " + request.getMethod() + " " + request.getURI());
			HttpResponse response = client.execute(request);
			int status = response.getStatusLine().getStatusCode();
			Log.d(TAG, "HTTP response status " + status);
			if(status != HttpStatus.SC_OK) {
				Log.w(TAG, "Error " + status + " for URL " + url + " - " + response.getStatusLine().getReasonPhrase());
				
				clientResponse.ok = false;
				clientResponse.exception = new HttpException(url + " error " + status + " - " + response.getStatusLine().getReasonPhrase());
				
				return clientResponse;
			}

			HttpEntity entity = response.getEntity();
			Log.d(TAG, "HTTP response entity " + entity);

			if(entity == null) {
				clientResponse.ok = false;
				clientResponse.exception = new Exception("No response entity");
				
				return clientResponse;
			}
			
			clientResponse.ok = true;
			clientResponse.data = EntityUtils.toString(entity);

		} catch(IOException e) {
			if(request != null) {
				request.abort();
			}
			
			Log.w(TAG, "Error for URL " + url, e);
			
			
		}
		
		return clientResponse;
	}
	
	public void stop() {
		
	}

	public static void setVersion(String version) {
		GUAntanamoClient.userAgent = "gUAntanamo v" + version;
	}
}
