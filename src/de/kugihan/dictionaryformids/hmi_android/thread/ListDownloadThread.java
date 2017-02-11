package de.kugihan.dictionaryformids.hmi_android.thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;

import de.kugihan.dictionaryformids.hmi_android.data.DictionaryListParser;

/**
 * Thread to download and parse a list of dictionaries independently from
 * the view.
 */
public class ListDownloadThread extends Thread {
	
	/**
	 * The encoding expected for server responses.
	 */
	private static final String RESPONSE_ENCODING = "utf-8";

	/**
	 * The content type expected for server responses.
	 */
	private static final String RESPONSE_CONTENT_TYPE = "text/html";

	/**
	 * Interface specifying communication between the thread and the UI.
	 * 
	 */
	public interface OnPostExecutionListener {
		/**
		 * Gets called when the thread has finished.
		 * 
		 * @param parser
		 *            the parser created from the response
		 */
		void onPostExecution(DictionaryListParser parser);

		/**
		 * Gets called in case an exceptions occurred.
		 * 
		 * @param exception
		 *            the occurred exception, either an IOException or a
		 *            JSONException
		 */
		void onException(Exception exception);
	}

	/**
	 * URL of the web-service providing the list of dictionaries.
	 */
	private final String dictionaryListUrl;

	/**
	 * The exception that occurred while processing or null.
	 */
	private Exception exception = null;

	/**
	 * The parser that was created from the server response or null.
	 */
	private DictionaryListParser parser = null;

	/**
	 * The listener that is informed about the results or null.
	 */
	private volatile OnPostExecutionListener listener = null;

	/**
	 * Object used for synchronizing access to the listener.
	 */
	private final Object syncObject = new Object();

	/**
	 * Creates a new thread that can download the given url and parse the
	 * list dictionaries.
	 * 
	 * @param url
	 *            an url pointing to the list of dictionaries
	 */
	public ListDownloadThread(final String url) {
		this.dictionaryListUrl = url;
	}

	@Override
	public final void run() {
		try {
			final DictionaryListParser parser = downloadList(dictionaryListUrl);
			returnParser(parser);
		} catch (IOException e) {
			returnException(e);
		} catch (JSONException e) {
			returnException(e);
		}
	}

	/**
	 * Returns the exception to the attached listener or saves it for later
	 * retrieval if no listener is attached.
	 * 
	 * @param exception
	 *            the that occurred
	 */
	private void returnException(final Exception exception) {
		synchronized (syncObject) {
			if (listener != null) {
				listener.onException(exception);
				this.exception = null;
			} else {
				this.exception = exception;
			}
		}
	}

	/**
	 * Returns the parser to the attached listener or saves it for later
	 * retrieval if no listener is attached.
	 * 
	 * @param parser
	 *            the parser representing the list of dictionaries
	 */
	private void returnParser(final DictionaryListParser parser) {
		synchronized (syncObject) {
			if (listener != null) {
				listener.onPostExecution(parser);
				this.parser = null;
			} else {
				this.parser = parser;
			}
		}
	}

	/**
	 * Downloads a the list of dictionaries.
	 * 
	 * @param url
	 *            the url of the list
	 * @return a parser for the list of dictionaries
	 * @throws IOException
	 *             if an input or output exception occurs
	 * @throws JSONException
	 *             if an exception occurs while parsing JSON data
	 */
	private DictionaryListParser downloadList(final String url)
			throws IOException, JSONException {
		final URL urlObj = new URL(url);
		final HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();
        urlConnection.setInstanceFollowRedirects(true);
		final InputStream inputStream = urlConnection.getInputStream();

        final boolean isValidType = urlConnection.getContentType().startsWith(RESPONSE_CONTENT_TYPE);
		if (!isValidType) {
			final String message = "CONTENT_TYPE IS '" + urlConnection.getContentType() + "'";
			throw new IOException(message);
		}

		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				inputStream, RESPONSE_ENCODING));

		final StringBuilder stringResult = new StringBuilder();

		try {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				stringResult.append(line);
			}
		} finally {
			reader.close();
		}

		return new DictionaryListParser(stringResult);
	}

	/**
	 * Attaches the listener to the thread or removes the current if
	 * listener is null.
	 * 
	 * @param listener
	 *            the listener to attach or null
	 */
	public final void setOnPostExecutionListener(
			final OnPostExecutionListener listener) {
		synchronized (syncObject) {
			this.listener = listener;
			if (exception != null) {
				returnException(exception);
			}
			if (parser != null) {
				returnParser(parser);
			}
		}
	}

}