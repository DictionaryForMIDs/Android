/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.kugihan.dictionaryformids.hmi_android.data.DictionaryListParser;
import de.kugihan.dictionaryformids.hmi_android.data.DownloadDictionaryItem;
import de.kugihan.dictionaryformids.hmi_android.data.ResultProvider;
import de.kugihan.dictionaryformids.hmi_android.service.DictionaryInstallationService;
import de.kugihan.dictionaryformids.hmi_android.service.ServiceUpdateListener;

/**
 * InstallDictionary represents an Activity that allows a user to automatically
 * download, extract and install dictionaries from the server.
 * 
 */
public final class InstallDictionary extends ListActivity implements
		ResultProvider {

	// TODO stop thread if activity will not be restored shortly
	/**
	 * Thread to download and parse a list of dictionaries independently from
	 * the view.
	 */
	private static final class ListDownloadThread extends Thread {

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
		public void run() {
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
		 * @throws ClientProtocolException
		 * @throws IOException
		 *             if an input or output exception occurs
		 * @throws JSONException
		 *             if an exception occurs while parsing JSON data
		 */
		private DictionaryListParser downloadList(final String url)
				throws IOException, JSONException {
			final HttpClient client = new DefaultHttpClient();
			final HttpGet httpGet = new HttpGet(url);

			final HttpResponse response = client.execute(httpGet);
			final HttpEntity entity = response.getEntity();
			if (entity == null) {
				throw new IOException("HttpResponse.getEntity() IS NULL");
			}
			final boolean isValidType = entity.getContentType().getValue().startsWith(
					RESPONSE_CONTENT_TYPE);
			if (!isValidType) {
				final String message = "CONTENT_TYPE IS '"
								+ entity.getContentType().getValue() + "'";
				throw new IOException(message);
			}

			final BufferedReader reader = new BufferedReader(new InputStreamReader(
					entity.getContent(), RESPONSE_ENCODING));

			StringBuilder stringResult = new StringBuilder();

			try {
				for (String line = reader.readLine(); line != null; line = reader
						.readLine()) {
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
		public void setOnPostExecutionListener(
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

	/**
	 * The encoding expected for server responses.
	 */
	private static final String RESPONSE_ENCODING = "utf-8";

	/**
	 * The content type expected for server responses.
	 */
	private static final String RESPONSE_CONTENT_TYPE = "text/html";

	/**
	 * The key of a integer specifying the id of the selected dictionary in a
	 * bundle.
	 */
	private static final String BUNDLE_SELECTED_ITEM = "selectedItem";

	/**
	 * The key of a serializable exception specifying an exception that occurred
	 * during download of the list of installable dictionaries in a bundle.
	 */
	private static final String BUNDLE_EXCEPTION = "exception";

	/**
	 * The key of a string specifying the message sent from the server in a
	 * bundle.
	 */
	private static final String BUNDLE_SERVER_MESSAGE = "serverMessage";

	/**
	 * The key of a ArrayList of parcable DownloadDictionaryItems in a bundle.
	 */
	private static final String BUNDLE_DICTIONARIES = "dictionaries";

	/**
	 * The key of an integer specifying the visible dialog when the activity was
	 * destroyed.
	 */
	private static final String BUNDLE_VISIBLE_DIALOG_ID = "visibleDialogId";

	/**
	 * ID specifying the Android platform to the dictionary list service.
	 */
	private static final int ANDROID_PLATFORM = 1;

	/**
	 * Bytes in a kilobyte.
	 */
	private static final int BYTES_PER_KILOBYTE = 1024;

	/**
	 * The id of the currently visible dialog or -1 if no dialog is visible.
	 * This is used to implement managed dialogs in TabHost.
	 */
	private int visibleDialogId = -1;

	/**
	 * List of all available dictionaries.
	 */
	private ArrayList<DownloadDictionaryItem> dictionaries = new ArrayList<DownloadDictionaryItem>();

	/**
	 * The result returned to TabHost.
	 */
	private Intent returnData = null;

	/**
	 * The result code returned to TabHost.
	 */
	private int resultCode = RESULT_CANCELED;

	/**
	 * Specifies if progress-bar updates should be handled or not.
	 */
	private boolean reactOnServiceUpdates = false;

	/**
	 * Saves an exception that occurred during installation of a dictionary for
	 * onPrepareDialog.
	 */
	private Exception exception = null;

	/**
	 * Saves a serverMessage from the server for onPrepareDialog.
	 */
	private String serverMessage = null;

	/**
	 * Saves the currently selected item to initialize a dialog for confirming
	 * installation in onPrepareDialog.
	 */
	private int selectedItem = -1;

	/**
	 * Receives updates to the user interface from background tasks.
	 */
	private final Handler userInterfaceCallback = new Handler();

	/**
	 * Handle of the thread that currently downloads the list of dictionaries or
	 * null.
	 */
	private volatile ListDownloadThread listDownloadThread = null;
	
	/**
	 * Object used to synchronize access to listDownloadThread.
	 */
	private final Object listDownloadThreadSync = new Object();

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.download_dictionary_list);

		reactOnServiceUpdates = true;
		DictionaryInstallationService.setUpdateListener(serviceListener);

		if (savedInstanceState == null) {
			startListDownload();
		} else {
			dictionaries = savedInstanceState
					.getParcelableArrayList(BUNDLE_DICTIONARIES);
			updateList();
			serverMessage = savedInstanceState.getString(BUNDLE_SERVER_MESSAGE);
			exception = (Exception) savedInstanceState.get(BUNDLE_EXCEPTION);
			selectedItem = savedInstanceState.getInt(BUNDLE_SELECTED_ITEM);
			if (exception != null) {
				TextView textViewError = (TextView) findViewById(R.id.TextViewError);
				textViewError.setText(getString(
						R.string.msg_error_downloading_available_dictionaries,
						exception.toString()));
			}
			// restore the dialog that was visible before activity got
			// re-created
			visibleDialogId = savedInstanceState
					.getInt(BUNDLE_VISIBLE_DIALOG_ID);
			if (visibleDialogId >= 0) {
				showDialog(visibleDialogId);
			}
		}

		TextView textViewError = (TextView) findViewById(R.id.TextViewError);
		textViewError.setOnClickListener(retryDownload);

		// get handle to thread
		Object object = getLastNonConfigurationInstance();
		if (object instanceof ListDownloadThread) {
			listDownloadThread = (ListDownloadThread) object;
			showActiveListDownload();
		}
	}

	/**
	 * Listener to react on clicks to retry the download of the list.
	 */
	private final OnClickListener retryDownload = new OnClickListener() {

		@Override
		public void onClick(final View v) {
			startListDownload();
		}

	};

	private final de.kugihan.dictionaryformids.hmi_android.InstallDictionary.ListDownloadThread.OnPostExecutionListener threadListener = new de.kugihan.dictionaryformids.hmi_android.InstallDictionary.ListDownloadThread.OnPostExecutionListener() {

		/**
		 * Parse the server message.
		 * 
		 * @param parser
		 *            the object that includes the server message
		 */
		private void loadServerMessage(final DictionaryListParser parser) {
			final String message = parser.getServerMessage();
			if (message != null && message.length() > 0) {
				serverMessage = message;
			} else {
				serverMessage = null;
			}
		}

		@Override
		public void onPostExecution(final DictionaryListParser parser) {
			synchronized (listDownloadThreadSync) {
				listDownloadThread = null;
			}
			loadServerMessage(parser);
			if (parser.forceUpdate()) {
				showDialogFromThread(R.id.dialog_suggest_update);
				return;
			} else if (parser.mayUpdate()) {
				showDialogFromThread(R.id.dialog_suggest_update);
			} else if (serverMessage != null) {
				showDialogFromThread(R.id.dialog_server_message);
			}
			updateListFromThread(parser.getDictionaries());
		}

		@Override
		public void onException(final Exception exception) {
			synchronized (listDownloadThreadSync) {
				listDownloadThread = null;
			}
			handler.post(new Runnable() {

				@Override
				public void run() {
					InstallDictionary.this.exception = exception;
					TextView textViewError = (TextView) findViewById(R.id.TextViewError);
					TextView textViewMessage = (TextView) findViewById(R.id.TextViewMessage);
					ProgressBar progressBar = (ProgressBar) findViewById(R.id.ProgressBar);
					textViewError
							.setText(getString(
									R.string.msg_error_downloading_available_dictionaries,
									exception.toString()));
					textViewError.setVisibility(View.VISIBLE);
					textViewMessage.setVisibility(View.GONE);
					progressBar.setVisibility(View.GONE);
				}

			});
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		reactOnServiceUpdates = false;
		DictionaryInstallationService.setUpdateListener(null);
		getMainActivity().setProgressBarVisibility(false);
		getMainActivity().setProgressBarIndeterminateVisibility(false);
		synchronized (listDownloadThreadSync) {
			if (listDownloadThread != null) {
				listDownloadThread.setOnPostExecutionListener(null);
			}
		}
		super.onPause();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		synchronized (listDownloadThreadSync) {
			if (listDownloadThread != null) {
				listDownloadThread.interrupt();
			}
		}
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		int task = DictionaryInstallationService.pollLastType();
		if (DictionaryInstallationService.isRunning() && task >= 0) {
			getMainActivity().setProgressBarVisibility(true);
			getMainActivity().setProgressBarIndeterminateVisibility(true);
			reactOnServiceUpdates = true;
			int percentage = DictionaryInstallationService.pollLastPercentage();
			serviceListener.onProgressUpdate(task, percentage);
			DictionaryInstallationService.setUpdateListener(serviceListener);
		}
		synchronized (listDownloadThreadSync) {
			if (listDownloadThread != null) {
				listDownloadThread.setOnPostExecutionListener(threadListener);
			}
		}
		super.onResume();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		outState.putParcelableArrayList(BUNDLE_DICTIONARIES, dictionaries);
		outState.putString(BUNDLE_SERVER_MESSAGE, serverMessage);
		outState.putSerializable(BUNDLE_EXCEPTION, exception);
		outState.putInt(BUNDLE_SELECTED_ITEM, selectedItem);
		outState.putInt(BUNDLE_VISIBLE_DIALOG_ID, visibleDialogId);
		super.onSaveInstanceState(outState);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Object result = super.onRetainNonConfigurationInstance();
		synchronized (listDownloadThreadSync) {
			if (listDownloadThread != null) {
				listDownloadThread.setOnPostExecutionListener(null);
				result = listDownloadThread;
				listDownloadThread = null;
			}
		}
		return result;
	}

	/**
	 * Returns the current main activity. If the parent activity is
	 * ChooseDictionary, this activity is displayed in a tab and therefore the
	 * parent activity is returned.
	 * 
	 * @return the current main activity
	 */
	private Activity getMainActivity() {
		if (getParent() instanceof ChooseDictionary) {
			return getParent();
		} else {
			return this;
		}
	}

	/**
	 * Starts the thread to download the list of dictionaries and prepares the
	 * view.
	 */
	private void startListDownload() {
		synchronized (listDownloadThreadSync) {
			if (listDownloadThread != null) {
				Toast.makeText(this, R.string.msg_list_already_downloaded,
						Toast.LENGTH_LONG).show();
				return;
			}
		}

		// remove old data form view
		dictionaries = new ArrayList<DownloadDictionaryItem>();
		exception = null;
		updateList();

		// show progress indicator
		showActiveListDownload();

		listDownloadThread = new ListDownloadThread(getDictionaryListUrl());
		listDownloadThread.setOnPostExecutionListener(threadListener);
		listDownloadThread.start();
	}

	private void showActiveListDownload() {
		TextView textViewEmpty = (TextView) findViewById(R.id.TextViewEmpty);
		TextView textViewError = (TextView) findViewById(R.id.TextViewError);
		TextView textViewMessage = (TextView) findViewById(R.id.TextViewMessage);
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.ProgressBar);
		textViewEmpty.setVisibility(View.GONE);
		textViewError.setVisibility(View.GONE);
		textViewError.setText("");
		textViewMessage.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.VISIBLE);
	}

	/**
	 * Gets the url used for downloading the list of dictionires. It includes
	 * platform and version information.
	 * 
	 * @return the url of the list of dictionaries
	 */
	private String getDictionaryListUrl() {
		PackageInfo packageInfo;
		int versionCode;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(),
					0);
			versionCode = packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			versionCode = -1;
		}
		String url = getString(R.string.attribute_dictionary_list_url,
				ANDROID_PLATFORM, versionCode);
		return url;
	}

	/**
	 * Stops the currently running DictionaryInstallation service.
	 */
	private void stopDictionaryInstallation() {
		Intent intent = new Intent(this, DictionaryInstallationService.class);
		stopService(intent);
	}

	/**
	 * Starts the installation of a new dictionary.
	 */
	private void startDictionaryInstallation() {
		if (DictionaryInstallationService.isRunning()) {
			Toast.makeText(this, R.string.msg_installation_already_started,
					Toast.LENGTH_LONG).show();
			return;
		}

		getMainActivity().setProgressBarVisibility(true);
		getMainActivity().setProgress(0);

		String url = dictionaries.get(selectedItem).getLink();
		String dictionaryName = dictionaries.get(selectedItem).getName();
		String dictionaryFile = dictionaries.get(selectedItem).getFileName();

		Intent intent = new Intent(this, DictionaryInstallationService.class);
		intent.putExtra(DictionaryInstallationService.BUNDLE_URL, url);
		intent.putExtra(DictionaryInstallationService.BUNDLE_DICTIONARY_NAME,
				dictionaryName);
		intent.putExtra(DictionaryInstallationService.BUNDLE_DICTIONARY_FILE,
				dictionaryFile);
		startService(intent);

		Toast.makeText(this, R.string.msg_installation_started,
				Toast.LENGTH_LONG).show();
	}

	/**
	 * Listener to handle communication with the background service.
	 */
	private final ServiceUpdateListener serviceListener = new ServiceUpdateListener() {

		@Override
		public void onExitWithException(final Exception exception) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					InstallDictionary.this.exception = exception;
					showDialog(R.id.dialog_installation_exception);
					updateProgres(10000);
					getMainActivity().setProgressBarVisibility(false);
					getMainActivity().setProgressBarIndeterminateVisibility(
							false);
				}
			});
		}

		@Override
		public void onFinished(final String path) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					updateProgres(10000);
					Toast.makeText(InstallDictionary.this,
							R.string.msg_installation_finished,
							Toast.LENGTH_LONG).show();
					exitWithData(path);
				}
			});
		}

		@Override
		public void onProgressUpdate(final int task, final int percentage) {
			final int progress = getProgressBarLength(task, percentage);
			updateProgres(progress);
		}
	};

	/**
	 * Calculates the length of the progress bar depending on the current active
	 * task and its percentage of completion.
	 * 
	 * @param task
	 *            the current task
	 * @param percentage
	 *            the percentage of completion of the current task
	 * @return the corresponding length of the progress bar
	 */
	public static int getProgressBarLength(final int task, final int percentage) {
		int progressStart;
		int progressLength;
		switch (task) {
		case DictionaryInstallationService.STATUS_CREATING_FOLDERS:
			progressStart = 0;
			progressLength = 1000;
			break;

		case DictionaryInstallationService.STATUS_DOWNLOADING:
			progressStart = 1000;
			progressLength = 3000;
			break;

		case DictionaryInstallationService.STATUS_EXTRACTING_JAR:
			progressStart = 4000;
			progressLength = 1000;
			break;

		case DictionaryInstallationService.STATUS_EXTRACTING_DICTIONARY:
			progressStart = 5000;
			progressLength = 5000;
			break;

		default:
			throw new IllegalArgumentException();
		}
		final int progress = progressStart + progressLength * percentage
				/ DictionaryInstallationService.PERCENTAGE_BASE;
		return progress;
	}

	/**
	 * Updates the current progress bar. Can be called from a non-GUI thread.
	 * 
	 * @param progress
	 *            the current progress
	 */
	private void updateProgres(final int progress) {
		userInterfaceCallback.post(new Runnable() {
			@Override
			public void run() {
				if (!reactOnServiceUpdates) {
					return;
				}
				getMainActivity().setProgress(progress);
			}
		});
	}

	private void updateListFromThread(
			final ArrayList<DownloadDictionaryItem> dictionaries) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				InstallDictionary.this.dictionaries = dictionaries;
				updateList();
			}

		});
	}

	/**
	 * Updates the list of current dictionaries from the downloaded data.
	 */
	private void updateList() {
		ArrayList<String> stringList = new ArrayList<String>();
		for (DownloadDictionaryItem dictionary : dictionaries) {
			stringList.add(dictionary.toString());
		}
		ArrayAdapter<String> fileList;
		fileList = new ArrayAdapter<String>(this, R.layout.file_row, stringList);
		setListAdapter(fileList);

		TextView textViewEmpty = (TextView) findViewById(R.id.TextViewEmpty);
		TextView textViewError = (TextView) findViewById(R.id.TextViewError);
		TextView textViewMessage = (TextView) findViewById(R.id.TextViewMessage);
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.ProgressBar);
		textViewEmpty.setVisibility(View.VISIBLE);
		if (textViewError.getText().length() > 0) {
			textViewError.setVisibility(View.VISIBLE);
		} else {
			textViewError.setVisibility(View.GONE);
		}
		textViewMessage.setVisibility(View.GONE);
		progressBar.setVisibility(View.GONE);
	}

	/**
	 * Shows a managed dialog. This method can be called from non-GUI threads.
	 * 
	 * @param dialogId
	 *            the id of the dialog to show
	 */
	private void showDialogFromThread(final int dialogId) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				showDialog(dialogId);
			}
		});
	}

	/**
	 * Mark the given dialog id to be no longer visible.
	 * 
	 * @param dialogId
	 *            id of the dialog that is no longer visible
	 */
	private void markDialogAsInvisible(final int dialogId) {
		if (visibleDialogId == dialogId) {
			visibleDialogId = -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(final int id) {
		Dialog dialog;
		switch (id) {
		case R.id.dialog_suggest_update:
			Builder alertBuilder = new AlertDialog.Builder(this);
			alertBuilder.setTitle(R.string.title_information);
			alertBuilder.setMessage("");
			alertBuilder.setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
							Intent startMarketUpdate = new Intent(
									Intent.ACTION_VIEW);
							startMarketUpdate.setData(Uri.parse(getString(
									R.string.attribute_market_search,
									getPackageName())));
							startActivity(startMarketUpdate);
						}
					});
			alertBuilder.setNegativeButton(R.string.button_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
						}
					});
			dialog = alertBuilder.create();
			break;

		case R.id.dialog_installation_exception:
			Builder exceptionAlert = new AlertDialog.Builder(this);
			exceptionAlert.setTitle(R.string.title_information);
			exceptionAlert.setMessage("");
			exceptionAlert.setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
						}
					});
			dialog = exceptionAlert.create();
			break;

		case R.id.dialog_confirm_installation:
			Builder confirmationAlert = new AlertDialog.Builder(this);
			confirmationAlert.setTitle(R.string.title_information);
			confirmationAlert.setMessage("");
			confirmationAlert.setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
							startDictionaryInstallation();
						}
					});
			confirmationAlert.setNegativeButton(R.string.button_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
						}
					});
			dialog = confirmationAlert.create();
			break;

		case R.id.dialog_server_message:
			Builder messageAlert = new AlertDialog.Builder(this);
			messageAlert.setTitle(R.string.title_information);
			messageAlert.setMessage("");
			messageAlert.setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
						}
					});
			dialog = messageAlert.create();
			break;

		case R.id.dialog_confirm_abort_installation:
			Builder cancelInstallation = new AlertDialog.Builder(this);
			cancelInstallation.setTitle(R.string.title_information);
			cancelInstallation.setMessage(R.string.msg_cancel_installation);
			cancelInstallation.setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
							stopDictionaryInstallation();
						}
					});
			cancelInstallation.setNegativeButton(R.string.button_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
						}
					});
			dialog = cancelInstallation.create();
			break;

		default:
			dialog = super.onCreateDialog(id);
		}
		if (dialog == null) {
			return dialog;
		}
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(final DialogInterface dialog) {
				markDialogAsInvisible(id);
			}
		});
		onPrepareDialog(id, dialog);
		return dialog;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPrepareDialog(final int id, final Dialog dialog) {
		switch (id) {
		case R.id.dialog_installation_exception:
			AlertDialog exceptionAlert = (AlertDialog) dialog;
			String exceptionMessage;
			if (exception.getCause() == null) {
				exceptionMessage = getString(
						R.string.msg_installation_exception, exception
								.getMessage());
			} else {
				exceptionMessage = getString(
						R.string.msg_installation_exception_cause, exception
								.getMessage(), exception.getCause());
			}
			exceptionAlert.setMessage(exceptionMessage);
			break;

		case R.id.dialog_confirm_installation:
			AlertDialog confirmAlert = (AlertDialog) dialog;
			final String name = dictionaries.get(selectedItem).getName();
			final long size = dictionaries.get(selectedItem).getSize();
			String message;
			if (size > 0) {
				String sizeString = formatBytes(size);
				message = getString(R.string.msg_confirm_installation_size,
						name, sizeString);
			} else {
				message = getString(R.string.msg_confirm_installation, name);
			}
			confirmAlert.setMessage(message);
			break;

		case R.id.dialog_server_message:
			AlertDialog messageAlert = (AlertDialog) dialog;
			messageAlert.setMessage(serverMessage);
			break;

		case R.id.dialog_suggest_update:
			final AlertDialog suggestUpdateAlert = (AlertDialog) dialog;
			final String serverMessageCopy = serverMessage;
			String details = getString(R.string.msg_suggest_update);
			if (serverMessageCopy != null && serverMessageCopy.length() > 0) {
				details = getString(R.string.msg_suggest_update_with_message,
						serverMessageCopy);
			}
			suggestUpdateAlert.setMessage(details);
			break;

		default:
		}
		visibleDialogId = id;
		super.onPrepareDialog(id, dialog);
	}

	/**
	 * Converts an size in bytes into a human readable representation.
	 * 
	 * @param size
	 *            the size
	 * @return the human readable representation
	 */
	private String formatBytes(final long size) {
		String[] sizes = { getString(R.string.unit_byte),
				getString(R.string.unit_kilo_byte),
				getString(R.string.unit_mega_byte),
				getString(R.string.unit_giga_byte) };
		double convertedSize = size;
		int i;
		for (i = 0; i < sizes.length; i++) {
			if (convertedSize < BYTES_PER_KILOBYTE) {
				break;
			}
			convertedSize /= BYTES_PER_KILOBYTE;
		}
		if (i >= sizes.length) {
			i--;
			convertedSize *= BYTES_PER_KILOBYTE;
		}
		return getString(R.string.file_size, convertedSize, sizes[i]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		if (DictionaryInstallationService.isRunning()) {
			Toast.makeText(this, R.string.msg_installation_already_started,
					Toast.LENGTH_LONG).show();
			return;
		}

		selectedItem = position;
		showDialog(R.id.dialog_confirm_installation);
	}

	/**
	 * Closes the activity and returns the given dictionary path to the calling
	 * activity.
	 * 
	 * @param path
	 *            the path of the dictionary
	 */
	private void exitWithData(final String path) {
		resultCode = RESULT_OK;
		returnData = new Intent();
		returnData.putExtra(FileList.FILE_PATH, path);
		setResult(resultCode, returnData);
		finish();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getResultCode() {
		return resultCode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Intent getReturnData() {
		return returnData;
	}

	/**
	 * Handler to receive view updates from non-GUI threads.
	 */
	private final Handler handler = new Handler();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.install_dictionary_options, menu);
		if (!DictionaryInstallationService.isRunning()) {
			menu.removeItem(R.id.itemCancelInstallation);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemReloadList:
			startListDownload();
			return true;

		case R.id.itemCancelInstallation:
			showDialog(R.id.dialog_confirm_abort_installation);
			return true;

		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}

}
