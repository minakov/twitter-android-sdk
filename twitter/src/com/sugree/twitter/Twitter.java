package com.sugree.twitter;

import twitter4j.TwitterFactory;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

public class Twitter {
	private static final String TAG = "twitter";

	protected static final String CALLBACK_URI = "x-oauthflow-twitter://callback";
	protected static final String CANCEL_URI = "twitter://cancel";

	protected static final String ACCESS_TOKEN = "access_token";
	protected static final String SECRET_TOKEN = "secret_token";
	protected static final String USER_ID = "user_id";
	protected static final String SCREEN_NAME = "screen_name";

	protected static String REQUEST_ENDPOINT = "https://api.twitter.com/1";

	protected static String OAUTH_REQUEST_TOKEN = "https://api.twitter.com/oauth/request_token";
	protected static String OAUTH_ACCESS_TOKEN = "https://api.twitter.com/oauth/access_token";
	protected static String OAUTH_AUTHORIZE = "https://api.twitter.com/oauth/authorize";

	private String accessToken;
	private String secretToken;
	private long userId;
	private String screenName;

	private twitter4j.Twitter mTwitter;

	public void authorize(Context ctx, Handler handler, String consumerKey, String consumerSecret,
			final DialogListener listener) {
		mTwitter = new TwitterFactory().getInstance();
		mTwitter.setOAuthConsumer(consumerKey, consumerSecret);
		CookieSyncManager.createInstance(ctx);
		dialog(ctx, handler, new DialogListener() {

			@Override
			public void onComplete(Bundle values) {
				CookieSyncManager.getInstance().sync();
				setAccessToken(values.getString(ACCESS_TOKEN));
				setSecretToken(values.getString(SECRET_TOKEN));
				setUserId(values.getLong(USER_ID));
				setScreenName(values.getString(SCREEN_NAME));
				if (isSessionValid()) {
					listener.onComplete(values);
				} else {
					onTwitterError(new TwitterError("failed to receive oauth token"));
				}
			}

			@Override
			public void onTwitterError(TwitterError e) {
				Log.e(TAG, "Login failed: " + e, e);
				listener.onTwitterError(e);
			}

			@Override
			public void onError(DialogError e) {
				Log.e(TAG, "Login failed: " + e, e);
				listener.onError(e);
			}

			@Override
			public void onCancel() {
				Log.d(TAG, "Login cancelled");
				listener.onCancel();
			}
		});
	}

	public void logout(Context context) {
		// Edge case: an illegal state exception is thrown if an instance of CookieSyncManager has not be created.
		// CookieSyncManager is normally created by a WebKit view, but this might happen if you start the app, restore
		// saved state, and click logout before running a UI dialog in a WebView -- in which case the app crashes
		@SuppressWarnings("unused")
		CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(context);
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();
		setAccessToken(null);
		setSecretToken(null);
	}

	private void dialog(final Context ctx, Handler handler, final DialogListener listener) {
		if (ctx.checkCallingOrSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(ctx);
			alertBuilder.setTitle("Error");
			alertBuilder.setMessage("Application requires permission to access the Internet");
			alertBuilder.show();
			return;
		}
		new TwDialog(ctx, mTwitter, listener).show();
	}

	public boolean isSessionValid() {
		return getAccessToken() != null && getSecretToken() != null;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getSecretToken() {
		return secretToken;
	}

	public void setSecretToken(String secretToken) {
		this.secretToken = secretToken;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public String getScreenName() {
		return screenName;
	}

	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}

	public static interface DialogListener {
		public void onComplete(Bundle values);

		public void onTwitterError(TwitterError e);

		public void onError(DialogError e);

		public void onCancel();
	}
}
