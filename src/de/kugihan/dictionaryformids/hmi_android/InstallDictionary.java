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
import org.apache.http.client.ClientProtocolException;
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
import de.kugihan.dictionaryformids.hmi_android.ChooseDictionary.DialogCallback;
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
		ResultProvider, DialogCallback {

	/**
	 * The encoding expected for server responses.
	 */
	private static final String RESPONSE_ENCODING = "utf-8";

	/**
	 * The content type expected for server responses.
	 */
	private static final String RESPONSE_CONTENT_TYPE = "text/html";

	/**
	 * The key of a boolean specifying an active download in a bundle.
	 */
	private static final String BUNDLE_ACTIVE_DOWNLOAD = "activeDownload";

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
	 * ID specifying the Android platform to the dictionary list service.
	 */
	private static final int ANDROID_PLATFORM = 1;

	/**
	 * Bytes in a kilobyte.
	 */
	private static final int BYTES_PER_KILOBYTE = 1024;

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
	private Thread listDownloadThread = null;

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
			serverMessage = savedInstanceState.getString(BUNDLE_SERVER_MESSAGE);
			exception = (Exception) savedInstanceState.get(BUNDLE_EXCEPTION);
			selectedItem = savedInstanceState.getInt(BUNDLE_SELECTED_ITEM);
			if (exception != null) {
				TextView textViewError = (TextView) findViewById(R.id.TextViewError);
				textViewError.setText(getString(
						R.string.msg_error_downloading_available_dictionaries,
						exception.toString()));
			}
			if (savedInstanceState.getBoolean(BUNDLE_ACTIVE_DOWNLOAD)) {
				startListDownload();
			} else {
				updateList();
			}
		}

		TextView textViewError = (TextView) findViewById(R.id.TextViewError);
		textViewError.setOnClickListener(retryDownload);

		registerDialogListener();
	}

	/**
	 * Registers the activity to listen for dialog events if the parent activity
	 * is a ChooseDictionary class.
	 */
	private void registerDialogListener() {
		if (getMainActivity() instanceof ChooseDictionary) {
			ChooseDictionary chooseDictionary = (ChooseDictionary) getMainActivity();
			chooseDictionary.registerDialogListener(this);
		}
	}

	/**
	 * Removes the listener for dialog events if the parent activity is a
	 * ChooseDictionary class.
	 */
	private void removeDialogListener() {
		if (getMainActivity() instanceof ChooseDictionary) {
			ChooseDictionary chooseDictionary = (ChooseDictionary) getMainActivity();
			chooseDictionary.removeDialogListener(this);
		}
	}

	/**
	 * Listener to react on clicks to retry the download of the list.
	 */
	private OnClickListener retryDownload = new OnClickListener() {

		@Override
		public void onClick(final View v) {
			startListDownload();
		}

	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		removeDialogListener();
		reactOnServiceUpdates = false;
		DictionaryInstallationService.setUpdateListener(null);
		getMainActivity().setProgressBarVisibility(false);
		getMainActivity().setProgressBarIndeterminateVisibility(false);
		super.onPause();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		if (listDownloadThread != null) {
			listDownloadThread.interrupt();
		}
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		registerDialogListener();
		int task = DictionaryInstallationService.pollLastType();
		int percentage = DictionaryInstallationService.pollLastPercentage();
		if (DictionaryInstallationService.isRunning() && task >= 0) {
			getMainActivity().setProgressBarVisibility(true);
			getMainActivity().setProgressBarIndeterminateVisibility(true);
			reactOnServiceUpdates = true;
			serviceListener.onProgressUpdate(task, percentage);
			DictionaryInstallationService.setUpdateListener(serviceListener);
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
		outState.putBoolean(BUNDLE_ACTIVE_DOWNLOAD, listDownloadThread != null);
		super.onSaveInstanceState(outState);
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
		if (listDownloadThread != null) {
			Toast.makeText(this, R.string.msg_list_already_downloaded,
					Toast.LENGTH_LONG).show();
			return;
		}

		// remove old data form view
		dictionaries = new ArrayList<DownloadDictionaryItem>();
		exception = null;
		updateList();

		// show progress indicator
		TextView textViewEmpty = (TextView) findViewById(R.id.TextViewEmpty);
		TextView textViewError = (TextView) findViewById(R.id.TextViewError);
		TextView textViewMessage = (TextView) findViewById(R.id.TextViewMessage);
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.ProgressBar);
		textViewEmpty.setVisibility(View.GONE);
		textViewError.setVisibility(View.GONE);
		textViewError.setText("");
		textViewMessage.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.VISIBLE);

		listDownloadThread = new Thread() {
			@Override
			public void run() {
				try {
					downloadList();
				} catch (IOException e) {
					postException(e);
					listDownloadThread = null;
					return;
				} catch (JSONException e) {
					postException(e);
					listDownloadThread = null;
					return;
				}
				if (interrupted()) {
					listDownloadThread = null;
					return;
				}
				userInterfaceCallback.post(new Runnable() {
					@Override
					public void run() {
						updateList();
					}
				});
				listDownloadThread = null;
			}

			private void postException(final Exception e) {
				if (interrupted()) {
					return;
				}
				userInterfaceCallback.post(new Runnable() {
					@Override
					public void run() {
						TextView textViewError = (TextView) findViewById(R.id.TextViewError);
						TextView textViewMessage = (TextView) findViewById(R.id.TextViewMessage);
						ProgressBar progressBar = (ProgressBar) findViewById(R.id.ProgressBar);
						textViewError
								.setText(getString(
										R.string.msg_error_downloading_available_dictionaries,
										e.toString()));
						textViewError.setVisibility(View.VISIBLE);
						textViewMessage.setVisibility(View.GONE);
						progressBar.setVisibility(View.GONE);
						exception = e;
					}
				});
			}
		};
		listDownloadThread.start();
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
	}

	/**
	 * Listener to handle communication with the background service.
	 */
	private ServiceUpdateListener serviceListener = new ServiceUpdateListener() {

		@Override
		public void onExitWithException(final Exception e) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					exception = e;
					showDialogInMainActivity(R.id.dialog_installation_exception);
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
	 * Downloads a the list of dictionaries.
	 * 
	 * @throws ClientProtocolException
	 * @throws IOException
	 *             if an input or output exception occurs
	 * @throws JSONException
	 *             if an exception occurs while parsing JSON data
	 */
	public void downloadList() throws IOException, JSONException {
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(getDictionaryListUrl());

		HttpResponse response = client.execute(httpGet);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			if (entity.getContentType().getValue().startsWith(
					RESPONSE_CONTENT_TYPE)) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(entity.getContent(),
								RESPONSE_ENCODING));

				StringBuilder stringResult = new StringBuilder();

				try {
					for (String line = reader.readLine(); line != null; line = reader
							.readLine()) {
						stringResult.append(line);
					}
				} finally {
					reader.close();
				}

				parseDictionaryList(stringResult);
			}
		}
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
	 * Parses the list of dictionaries.
	 * 
	 * @param stringResult
	 *            the server response
	 * @throws JSONException
	 *             if an exception while parsing JSON data occurred
	 */
	private void parseDictionaryList(final StringBuilder stringResult)
			throws JSONException {
		DictionaryListParser parser = new DictionaryListParser(stringResult);
		if (parser.forceUpdate()) {
			showDialogInMainActivityFromThread(R.id.dialog_suggest_update);
			return;
		} else if (parser.mayUpdate()) {
			showDialogInMainActivityFromThread(R.id.dialog_suggest_update);
		}
		handleServerMessage(parser);
		dictionaries = parser.getDictionaries();
	}

	/**
	 * Parse the server message.
	 * 
	 * @param parser
	 *            the object that includes the server message
	 */
	private void handleServerMessage(final DictionaryListParser parser) {
		String message = parser.getServerMessage();
		if (message != null && message.length() > 0) {
			serverMessage = message;
			showDialogInMainActivityFromThread(R.id.dialog_server_message);
		}
	}

	/**
	 * Shows a managed dialog on the main activity. This method can be called
	 * from non-GUI threads.
	 * 
	 * @param dialogId
	 *            the id of the dialog to show
	 */
	private void showDialogInMainActivityFromThread(final int dialogId) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				showDialogInMainActivity(dialogId);
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(final int id) {
		AlertDialog dialog;
		switch (id) {
		case R.id.dialog_suggest_update:
			Builder alertBuilder = new AlertDialog.Builder(this);
			alertBuilder.setTitle(R.string.title_information);
			alertBuilder.setMessage(R.string.msg_suggest_update);
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
			return alertBuilder.create();

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
			onPrepareDialog(id, dialog);
			return dialog;

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
			onPrepareDialog(id, dialog);
			return dialog;

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
			onPrepareDialog(id, dialog);
			return dialog;

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
			return cancelInstallation.create();

		default:
			return super.onCreateDialog(id);
		}
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
			if (exception.getCause() != null) {
				exceptionMessage = getString(
						R.string.msg_installation_exception_cause, exception
								.getMessage(), exception.getCause());
			} else {
				exceptionMessage = getString(
						R.string.msg_installation_exception, exception
								.getMessage());
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

		default:
		}
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
		showDialogInMainActivity(R.id.dialog_confirm_installation);
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
	private Handler handler = new Handler();

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
			showDialogInMainActivity(R.id.dialog_confirm_abort_installation);
			return true;

		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}

	/**
	 * Shows a managed dialog in the activity's main activity.
	 * 
	 * @param dialogId
	 *            the id of the dialog to show
	 */
	private void showDialogInMainActivity(final int dialogId) {
		getMainActivity().showDialog(dialogId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Dialog onCreateDialogListener(final int dialogId) {
		return onCreateDialog(dialogId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPrepareDialogListener(final int dialogId, final Dialog dialog) {
		onPrepareDialog(dialogId, dialog);
	}

}
