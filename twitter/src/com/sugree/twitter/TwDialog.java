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
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.sugree.twitter.Twitter.DialogListener;

public class TwDialog extends Dialog {
	private static final String TAG = "twitter";

	private static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);

	private String mUrl;
	private final DialogListener mListener;
	private ProgressDialog mSpinner;
	private ImageView mCrossImage;
	private WebView mWebView;
	private LinearLayout mContent;
	private final Handler mHandler;

	private final twitter4j.Twitter mTwitter;

	private RequestToken mRequestToken;

	public TwDialog(Context context, twitter4j.Twitter twitter, DialogListener listener) {
		super(context, android.R.style.Theme_Translucent_NoTitleBar);
		mTwitter = twitter;
		mListener = listener;
		mHandler = new Handler();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSpinner = new ProgressDialog(getContext());
		mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mSpinner.setMessage("Loading...");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mContent = new LinearLayout(getContext());

		// Create the 'x' image, but don't add to the mContent layout yet at this point, we only need to know its
		// drawable width and height to place the webview
		createCrossImage();

		// Now we know 'x' drawable width and height, layout the webivew and add it the mContent layout
		int crossWidth = mCrossImage.getDrawable().getIntrinsicWidth();
		setUpWebView(crossWidth / 2);

		// Finally add the 'x' image to the mContent layout and add mContent to the Dialog view
		mContent.addView(mCrossImage, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		addContentView(mContent, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		retrieveRequestToken();
	}

	private void createCrossImage() {
		mCrossImage = new ImageView(getContext());
		// Dismiss the dialog when user click on the 'x'
		mCrossImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mListener.onCancel();
				TwDialog.this.dismiss();
			}
		});
		Drawable crossDrawable = getContext().getResources().getDrawable(R.drawable.close);
		mCrossImage.setImageDrawable(crossDrawable);
		// 'x' should not be visible while webview is loading make it visible only after webview has fully loaded
		mCrossImage.setVisibility(View.INVISIBLE);
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void setUpWebView(int margin) {
		LinearLayout webViewContainer = new LinearLayout(getContext());
		mWebView = new WebView(getContext());
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.setWebViewClient(new TwDialog.TwWebViewClient());
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setLayoutParams(FILL);
		mWebView.setVisibility(View.INVISIBLE);
		mWebView.getSettings().setSavePassword(false);

		webViewContainer.setPadding(margin, margin, margin, margin);
		webViewContainer.addView(mWebView);
		mContent.addView(webViewContainer);
	}

	private void retrieveRequestToken() {
		if (!mSpinner.isShowing()) {
			mSpinner.show();
		}
		new Thread() {
			@Override
			public void run() {
				try {
					mRequestToken = mTwitter.getOAuthRequestToken(Twitter.CALLBACK_URI);
					mUrl = mRequestToken.getAuthorizationURL();
					mWebView.loadUrl(mUrl);
				} catch (TwitterException e) {
					mListener.onError(new DialogError(e.getMessage(), -1, Twitter.OAUTH_REQUEST_TOKEN));
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							TwDialog.this.dismiss();
						}
					});
				}
			}
		}.start();
	}

	private void retrieveAccessToken(final String url) {
		if (!mSpinner.isShowing()) {
			mSpinner.show();
		}
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
			TwDialog.this.dismiss();
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Log.d(TAG, "WebView loading URL: " + url);
			super.onPageStarted(view, url, favicon);
			if (!mSpinner.isShowing()) {
				mSpinner.show();
			}
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			mSpinner.dismiss();
			// Once webview is fully loaded, set the mContent background to be transparent and make visible the 'x'
			// image.
			mContent.setBackgroundColor(Color.TRANSPARENT);
			mWebView.setVisibility(View.VISIBLE);
			mCrossImage.setVisibility(View.VISIBLE);
		}
	}
}
