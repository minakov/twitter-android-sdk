/*
 * Copyright 2010 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sugree.twitter;

import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.sugree.twitter.Twitter.DialogListener;

@SuppressLint("SetJavaScriptEnabled")
public class TwDialog extends Dialog {
	private static final String TAG = "twitter";
	private static final float[] DIMENSIONS_LANDSCAPE = { 460, 260 };
	private static final float[] DIMENSIONS_PORTRAIT = { 280, 430 };
	private static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);

	private final String mUrl;
	private final DialogListener mListener;
	// private ProgressDialog mSpinner;
	private final Handler mHandler;

	private final twitter4j.Twitter mTwitter;

	private RequestToken mRequestToken;

	public TwDialog(Context context, String url, twitter4j.Twitter twitter, Handler handler, DialogListener listener) {
		super(context, 0);
		mUrl = url;
		mTwitter = twitter;
		mHandler = handler;
		mListener = listener;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// mSpinner = new ProgressDialog(getContext());
		// mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
		// mSpinner.setMessage("Loading...");

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		final WebView webView = new WebView(getContext());
		webView.setVerticalScrollBarEnabled(false);
		webView.setHorizontalScrollBarEnabled(false);
		webView.setWebViewClient(new TwDialog.TwWebViewClient());
		webView.getSettings().setJavaScriptEnabled(true);
		webView.loadUrl(mUrl);
		webView.setLayoutParams(FILL);
		webView.getSettings().setSavePassword(false);

		Display display = getWindow().getWindowManager().getDefaultDisplay();
		final float scale = getContext().getResources().getDisplayMetrics().density;
		float[] dimensions = display.getWidth() < display.getHeight() ? DIMENSIONS_PORTRAIT : DIMENSIONS_LANDSCAPE;
		final int w = (int) (dimensions[0] * scale + 0.5f);
		final int h = (int) (dimensions[1] * scale + 0.5f);
		addContentView(webView, new LayoutParams(w, h));
	}

	// private void retrieveRequestToken() {
	// if (!mSpinner.isShowing()) {
	// mSpinner.show();
	// }
	// new Thread() {
	// @Override
	// public void run() {
	// try {
	// mRequestToken = mTwitter.getOAuthRequestToken(Twitter.CALLBACK_URI);
	// mUrl = mRequestToken.getAuthorizationURL();
	// mWebView.loadUrl(mUrl);
	// } catch (TwitterException e) {
	// mListener.onError(new DialogError(e.getMessage(), -1, Twitter.OAUTH_REQUEST_TOKEN));
	// mHandler.post(new Runnable() {
	// @Override
	// public void run() {
	// mSpinner.dismiss();
	// TwDialog.this.dismiss();
	// }
	// });
	// }
	// }
	// }.start();
	// }

	private void retrieveAccessToken(final String url) {
		// if (!mSpinner.isShowing()) {
		// mSpinner.show();
		// }
		new Thread() {
			@Override
			public void run() {
				final Bundle values = new Bundle();
				try {
					AccessToken at = mTwitter.getOAuthAccessToken(mRequestToken);
					values.putString(Twitter.ACCESS_TOKEN, at.getToken());
					values.putString(Twitter.SECRET_TOKEN, at.getTokenSecret());
					values.putLong(Twitter.USER_ID, at.getUserId());
					values.putString(Twitter.SCREEN_NAME, at.getScreenName());
					mListener.onComplete(values);
				} catch (TwitterException e) {
					mListener.onTwitterError(new TwitterError(e.getMessage()));
				}
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						// mSpinner.dismiss();
						TwDialog.this.dismiss();
					}
				});
			}
		}.start();
	}

	private class TwWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.d(TAG, "Override URL: " + url);
			if (url.startsWith(Twitter.CALLBACK_URI)) {
				retrieveAccessToken(url);
				return true;
			} else if (url.startsWith(Twitter.CANCEL_URI)) {
				mListener.onCancel();
				TwDialog.this.dismiss();
				return true;
			}
			return false;
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);
			mListener.onError(new DialogError(description, errorCode, failingUrl));
			// mSpinner.dismiss();
			TwDialog.this.dismiss();
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Log.d(TAG, "WebView started URL: " + url);
			super.onPageStarted(view, url, favicon);
			// if (!mSpinner.isShowing()) {
			// mSpinner.show();
			// }
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			Log.d(TAG, "WebView finished URL: " + url);
			super.onPageFinished(view, url);
			// mSpinner.dismiss();
			// Once webview is fully loaded, set the mContent background to be transparent and make visible the 'x'
			// image.
			// mContent.setBackgroundColor(Color.TRANSPARENT);
			// mWebView.setVisibility(View.VISIBLE);
			// mCrossImage.setVisibility(View.VISIBLE);
		}
	}
}
