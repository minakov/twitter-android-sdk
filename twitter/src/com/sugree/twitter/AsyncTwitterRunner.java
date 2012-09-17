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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import twitter4j.TwitterException;
import android.content.Context;

/**
 * A sample implementation of asynchronous API requests. This class provides the ability to execute API methods and have
 * the call return immediately, without blocking the calling thread. This is necessary when accessing the API in the UI
 * thread, for instance. The request response is returned to the caller via a callback interface, which the developer
 * must implement.
 * 
 * This sample implementation simply spawns a new thread for each request, and makes the API call immediately. This may
 * work in many applications, but more sophisticated users may re-implement this behavior using a thread pool, a network
 * thread, a request queue, or other mechanism. Advanced functionality could be built, such as rate-limiting of
 * requests, as per a specific application's needs.
 * 
 * @see RequestListener The callback interface.
 * 
 * @author ssoneff@facebook.com
 * 
 */
public class AsyncTwitterRunner {
	Twitter twitter;

	public AsyncTwitterRunner(Twitter twitter) {
		this.twitter = twitter;
	}

	public void retrieveRequestToken(final RequestListener listener) {
		new Thread() {
			@Override
			public void run() {
				try {
					final String response = twitter.retrieveRequestToken();
					listener.onComplete(response);
				} catch (TwitterException e) {
					TwitterError error = new TwitterError(e.getMessage());
					error.initCause(e);
					listener.onTwitterError(error);
				}
			}
		}.start();
	}

	public void retrieveAccessToken(final RequestListener listener) {
		new Thread() {
			@Override
			public void run() {
				try {
					twitter.retrieveAccessToken();
					listener.onComplete(null);
				} catch (TwitterException e) {
					TwitterError error = new TwitterError(e.getMessage());
					error.initCause(e);
					listener.onTwitterError(error);
				}
			}
		}.start();
	}

	public void retrieveProfile(final RequestListener listener) {
		new Thread() {
			@Override
			public void run() {
				try {
					twitter.retrieveAccessToken();
					twitter.requestProfileImage();
					listener.onComplete(null);
				} catch (TwitterException e) {
					TwitterError error = new TwitterError(e.getMessage());
					error.initCause(e);
					listener.onTwitterError(error);
				}
			}
		}.start();
	}

	/**
	 * Invalidate the current user session by removing the access token in memory, clearing the browser cookies. The
	 * application will be notified when logout is complete via the callback interface.
	 * 
	 * Note that this method is asynchronous and the callback will be invoked in a background thread; operations that
	 * affect the UI will need to be posted to the UI thread or an appropriate handler.
	 * 
	 * @param context
	 *            The Android context in which the logout should be called: it should be the same context in which the
	 *            login occurred in order to clear any stored cookies
	 * @param listener
	 *            Callback interface to notify the application when the request has completed.
	 */
	public void logout(final Context context, final RequestListener listener) {
		new Thread() {
			@Override
			public void run() {
				twitter.logout(context);
				listener.onComplete(null);
			}
		}.start();
	}

	public static interface RequestListener {
		public void onComplete(String response);

		public void onIOException(IOException e);

		public void onFileNotFoundException(FileNotFoundException e);

		public void onMalformedURLException(MalformedURLException e);

		public void onTwitterError(TwitterError e);

	}

}
