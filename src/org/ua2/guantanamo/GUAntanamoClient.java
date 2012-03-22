package org.ua2.guantanamo;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.ua2.json.ClientResponse;
import org.ua2.json.JSONClient;

import android.util.Log;

public class GUAntanamoClient extends JSONClient {
	private final String url;
	private final DefaultHttpClient client;

	private static String userAgent = "gUAntanmo";

	private static final String TAG = GUAntanamoClient.class.getName();

    private static class GzipDecompressingEntity extends HttpEntityWrapper {

        public GzipDecompressingEntity(final HttpEntity entity) {
            super(entity);
        }

        @Override
        public InputStream getContent()
            throws IOException, IllegalStateException {

            // the wrapped entity's getContent() decides about repeatability
            InputStream wrappedin = wrappedEntity.getContent();

            return new GZIPInputStream(wrappedin);
        }

        @Override
        public long getContentLength() {
            // length of ungzipped content is not known
            return -1;
        }

    }

	public GUAntanamoClient(String url, final String username, final String password) {
		super();

		this.url = url;

		Log.i(TAG, "Creating credentials");

		final Principal principal = new Principal() {
			@Override
			public String getName() {
				return username;
			}
		};

		Log.i(TAG, "Creating HTTP client");
		client = new DefaultHttpClient();

		client.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
				AuthState authState = (AuthState)context.getAttribute(ClientContext.TARGET_AUTH_STATE);
				CredentialsProvider provider = (CredentialsProvider)context.getAttribute(ClientContext.CREDS_PROVIDER);

				provider.setCredentials(AuthScope.ANY, new Credentials() {
					@Override
					public String getPassword() {
						return password;
					}

					@Override
					public Principal getUserPrincipal() {
						return principal;
					}
				});
				
				if (authState.getAuthScheme() == null) {
					Credentials creds = provider.getCredentials(AuthScope.ANY);
					if(creds != null) {
						authState.setAuthScheme(new BasicScheme());
						authState.setCredentials(creds);
					}
				}
			}
		}, 0);

        client.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(
                    final HttpRequest request,
                    final HttpContext context) throws HttpException, IOException {
                if (!request.containsHeader("Accept-Encoding")) {
                	Log.d(TAG, "Adding gzip acceptance header");
                    request.addHeader("Accept-Encoding", "gzip");
                }
            }

        });

        client.addResponseInterceptor(new HttpResponseInterceptor() {
            public void process(
                    final HttpResponse response,
                    final HttpContext context) throws HttpException, IOException {
                HttpEntity entity = response.getEntity();
                Header ceheader = entity.getContentEncoding();
                if (ceheader != null) {
                    HeaderElement[] codecs = ceheader.getElements();
                    for (int i = 0; i < codecs.length; i++) {
                        if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                        	Log.d(TAG, "Recieved gzip'd response of " + response.getEntity().getContentLength());
                            response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                            return;
                        }
                    }
                }
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

			if(entity == null) {
				clientResponse.ok = false;
				clientResponse.exception = new Exception("No response entity");

				return clientResponse;
			}

			clientResponse.ok = true;
			clientResponse.data = EntityUtils.toString(entity);

			Log.d(TAG, "HTTP response " + entity.getContentLength() + " of " + entity.getContentType().getValue() + " -> " + (clientResponse.data != null ? clientResponse.data.length() : -1));
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
