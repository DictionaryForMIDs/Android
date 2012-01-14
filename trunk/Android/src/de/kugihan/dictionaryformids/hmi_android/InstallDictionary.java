/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.kugihan.dictionaryformids.hmi_android.data.DictionaryListParser;
import de.kugihan.dictionaryformids.hmi_android.data.DownloadDictionaryItem;
import de.kugihan.dictionaryformids.hmi_android.data.ResultProvider;
import de.kugihan.dictionaryformids.hmi_android.service.DictionaryInstallationService;
import de.kugihan.dictionaryformids.hmi_android.service.ServiceUpdateListener;
import de.kugihan.dictionaryformids.hmi_android.thread.ListDownloadThread;
import de.kugihan.dictionaryformids.hmi_android.thread.ListDownloadThread.OnPostExecutionListener;
import de.kugihan.dictionaryformids.hmi_android.view_helper.LocalizationHelper;

/**
 * InstallDictionary represents an Activity that allows a user to automatically
 * download, extract and install dictionaries from the server.
 * 
 */
public final class InstallDictionary extends ListActivity implements
		ResultProvider {

	// TODO stop thread if activity will not be restored shortly

	/**
	 * Class to hold non-serializable objects to be passed from
	 * onRetainNonConfigurationState for new activity.
	 */
	private static class NonConfigurationInstance {
		/**
		 * Handle of the thread that currently downloads the list of
		 * dictionaries or null.
		 */
		ListDownloadThread listDownloadThread = null;

		/**
		 * Saves an exception that occurred during installation of a dictionary
		 * for onPrepareDialog.
		 */
		Exception dictionaryInstallationException = null;

		/**
		 * Checks if at least one object is non-null.
		 *
		 * @return true if at least one object is non-null
		 */
		boolean hasValues() {
			return listDownloadThread != null || dictionaryInstallationException != null;
		}
	}

	/**
	 * The key of a integer specifying the id of the selected dictionary in a
	 * bundle.
	 */
	private static final String BUNDLE_SELECTED_ITEM = "selectedItem";

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
	 * The key of a parcelable specifying the dictionary item which was selected for installation.
	 */
	private static final String BUNDLE_INSTALL_DICTIONARY_ITEM = "installDictionaryItem";
	
	/**
	 * The key of an integer > 0 specifying the dictionary that should be installed.
	 */
	public static final String INTENT_AUTO_INSTALL_ID = "autoInstallId";

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
	 * Filtered list of dictionaries.
	 */
	private ArrayList<DownloadDictionaryItem> filteredDictionaries = new ArrayList<DownloadDictionaryItem>();

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
	 * Saves the currently selected item of the list of filtered dictionaries to
	 * initialize a dialog for confirming installation in onPrepareDialog.
	 */
	private int selectedFilteredItem = -1;
	
	/**
	 * Saves the dictionary that is currently being installed for
	 * onPrepareDialog.
	 */
	private DownloadDictionaryItem installDictionaryItem = null;

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
		DictionaryInstallationService.removePendingStatusNotifications(this);

		// load non configuration instance
		final Object object = getLastNonConfigurationInstance();
		NonConfigurationInstance instance = null;
		if (object instanceof NonConfigurationInstance) {
			instance = (NonConfigurationInstance) object;
			exception = instance.dictionaryInstallationException;
		}

		if (savedInstanceState == null) {
			startListDownload();
		} else {
			// restore application state
			serverMessage = savedInstanceState.getString(BUNDLE_SERVER_MESSAGE);
			selectedFilteredItem = savedInstanceState.getInt(BUNDLE_SELECTED_ITEM);
			installDictionaryItem = savedInstanceState
					.getParcelable(BUNDLE_INSTALL_DICTIONARY_ITEM);
			if (exception != null) {
				final TextView textViewError = (TextView) findViewById(R.id.TextViewError);
				textViewError.setText(getString(
						R.string.msg_error_downloading_available_dictionaries,
						exception.toString()));
			}
			dictionaries = savedInstanceState
					.getParcelableArrayList(BUNDLE_DICTIONARIES);
			updateList();
			// the dialog that was visible before activity got re-created will
			// be restored in onResume
			visibleDialogId = savedInstanceState
					.getInt(BUNDLE_VISIBLE_DIALOG_ID);
		}

		final TextView textViewError = (TextView) findViewById(R.id.TextViewError);
		textViewError.setOnClickListener(retryDownload);
		
		final EditText editTextFilter = (EditText) findViewById(R.id.EditTextFilter);
		editTextFilter.addTextChangedListener(filterTextWatcher);
		final ImageButton clearFilterButton = (ImageButton) findViewById(R.id.ClearFilterButton);
		clearFilterButton.setOnClickListener(clearFilterClickListener);

		// get handle to thread
		if (instance != null && instance.listDownloadThread != null) {
			listDownloadThread = instance.listDownloadThread;
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

	/**
	 * Listener to react on list downloads.
	 */
	private final OnPostExecutionListener threadListener = new OnPostExecutionListener() {

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
			
			// handle auto install dictionary
			final int dictionaryId = getIntent().getIntExtra(
					INTENT_AUTO_INSTALL_ID, 0);
			final boolean isAutoInstallAvailable = dictionaryId > 0;
			if (!isAutoInstallAvailable) {
				return;
			}
			final DownloadDictionaryItem dictionary = parser
					.getDictionary(dictionaryId);
			if (dictionary == null) {
				showDialogFromThread(R.id.dialog_dictionary_not_found);
				return;
			}
			final boolean result = DictionaryInstallationService
					.startDictionaryInstallation(InstallDictionary.this,
							dictionary);
			if (result) {
				// reset auto install id
				getIntent().putExtra(INTENT_AUTO_INSTALL_ID, 0);
				// show dialog
				installDictionaryItem = dictionary;
				showDialogFromThread(R.id.dialog_auto_installing_dictionary);
			} else {
				handler.post(new Runnable() {
					@Override
					public void run() {
						// send toast to UI thread
						Toast.makeText(InstallDictionary.this,
								R.string.msg_installation_already_started,
								Toast.LENGTH_LONG).show();
						return;
					}
				});
			}
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
					final TextView textViewError = (TextView) findViewById(R.id.TextViewError);
					final TextView textViewMessage = (TextView) findViewById(R.id.TextViewMessage);
					final ProgressBar progressBar = (ProgressBar) findViewById(R.id.ProgressBar);
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
		if (visibleDialogId >= 0) {
			removeDialog(visibleDialogId);
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
		final int task = DictionaryInstallationService.pollLastType();
		if (DictionaryInstallationService.isRunning() && task >= 0) {
			getMainActivity().setProgressBarVisibility(true);
			getMainActivity().setProgressBarIndeterminateVisibility(true);
			reactOnServiceUpdates = true;
			final int percentage = DictionaryInstallationService.pollLastPercentage();
			serviceListener.onProgressUpdate(task, percentage);
			DictionaryInstallationService.setUpdateListener(serviceListener);
		}
		synchronized (listDownloadThreadSync) {
			if (listDownloadThread != null) {
				listDownloadThread.setOnPostExecutionListener(threadListener);
			}
		}
		if (visibleDialogId >= 0) {
			showDialog(visibleDialogId);
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
		outState.putInt(BUNDLE_SELECTED_ITEM, selectedFilteredItem);
		outState.putInt(BUNDLE_VISIBLE_DIALOG_ID, visibleDialogId);
		outState.putParcelable(BUNDLE_INSTALL_DICTIONARY_ITEM, installDictionaryItem);
		super.onSaveInstanceState(outState);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		final NonConfigurationInstance state = new NonConfigurationInstance();
		state.dictionaryInstallationException = exception;
		synchronized (listDownloadThreadSync) {
			if (listDownloadThread != null) {
				listDownloadThread.setOnPostExecutionListener(null);
				state.listDownloadThread = listDownloadThread;
				listDownloadThread = null;
			}
		}
		if (state.hasValues()) {
			return state;
		} else {
			return super.onRetainNonConfigurationInstance();
		}
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

	/**
	 * Modify user interface to show that there is an active download.
	 */
	private void showActiveListDownload() {
		final TextView textViewEmpty = (TextView) findViewById(R.id.TextViewEmpty);
		final TextView textViewError = (TextView) findViewById(R.id.TextViewError);
		final TextView textViewMessage = (TextView) findViewById(R.id.TextViewMessage);
		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.ProgressBar);
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
		final boolean isInstalling = DictionaryInstallationService
				.startDictionaryInstallation(this, installDictionaryItem);
		if (isInstalling) {
			getMainActivity().setProgressBarVisibility(true);
			getMainActivity().setProgress(0);
		}
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

	/**
	 * Updates the list of available dictionaries. This method can be called
	 * from non-GUI threads.
	 * 
	 * @param dictionaries
	 *            the list of dictionaries
	 */
	private void updateListFromThread(
			final ArrayList<DownloadDictionaryItem> dictionaries) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				// remove filter
				((EditText) findViewById(R.id.EditTextFilter)).setText("");
				// set data
				InstallDictionary.this.dictionaries = dictionaries;
				updateList();
			}

		});
	}

	/**
	 * Updates the list of current dictionaries from the downloaded data.
	 */
	private void updateList() {
		updateFilteredDictionaries();
		setListAdapter(getDictionaryAdapterFromFilteredList());

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
	 * Creates an adapter from the list of filtered dictionaries for use in a
	 * list.
	 * 
	 * @return the adapter representing the data of the list of filtered
	 *         dictionaries
	 */
	private ArrayAdapter<String> getDictionaryAdapterFromFilteredList() {
		ArrayList<String> dictionaryNames = new ArrayList<String>();
		for (DownloadDictionaryItem dictionary : filteredDictionaries) {
			final String localizedName = dictionary.getLocalizedName(getResources());
			dictionaryNames.add(localizedName);
		}
		return new ArrayAdapter<String>(this, R.layout.file_row, dictionaryNames);
	}

	/**
	 * Creates the list of filtered dictionaries using the current filter text
	 * and list of dictionaries.
	 */
	private void updateFilteredDictionaries() {
		final EditText editTextFilter = (EditText) findViewById(R.id.EditTextFilter);
		final CharSequence lowerCaseFilter = editTextFilter.getText().toString().toLowerCase();
		filteredDictionaries = new ArrayList<DownloadDictionaryItem>();
		for (DownloadDictionaryItem dictionary : dictionaries) {
			final String localizedName = dictionary.getLocalizedName(getResources());
			final boolean doesNotMatchFilter = !localizedName.toLowerCase()
					.contains(lowerCaseFilter);
			if (doesNotMatchFilter) {
				continue;
			}
			filteredDictionaries.add(dictionary);
		}
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
			
		case R.id.dialog_dictionary_not_found:
			Builder notFoundAlert = new AlertDialog.Builder(this);
			notFoundAlert.setTitle(R.string.title_information);
			notFoundAlert.setMessage("");
			notFoundAlert.setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
						}
					});
			dialog = notFoundAlert.create();
			break;
			
		case R.id.dialog_auto_installing_dictionary:
			Builder installingAlert = new AlertDialog.Builder(this);
			installingAlert.setTitle(R.string.title_information);
			installingAlert.setMessage("");
			installingAlert.setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
						}
					});
			installingAlert.setNeutralButton(R.string.button_exit,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							exitWithoutData(DictionaryForMIDs.RESULT_EXIT);
						}
					});
			dialog = installingAlert.create();
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
				// Remove all internal handles to the dialog to make sure
				// onPrepareDialog is not called after re-creating the activity.
				// If onPrepareDialog was called, the id of the active dialog
				// would be set and the closed dialog recreated.
				removeDialog(id);
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
			
		case R.id.dialog_dictionary_not_found:
			AlertDialog notFoundAlert = (AlertDialog) dialog;
			final int dictionaryId = getIntent().getIntExtra(
					INTENT_AUTO_INSTALL_ID, 0);
			final String notFoundMessage = getString(
					R.string.msg_error_dictionary_not_found, dictionaryId);
			notFoundAlert.setMessage(notFoundMessage);
			break;
			
		case R.id.dialog_auto_installing_dictionary:
			AlertDialog installAlert = (AlertDialog) dialog;
			final String dictionaryName = LocalizationHelper.getLocalizedDictionaryName(
					getResources(), installDictionaryItem.getName());
			final String installationStartedMessage = getString(
					R.string.msg_auto_installation_started, dictionaryName);
			installAlert.setMessage(installationStartedMessage);
			break;

		case R.id.dialog_confirm_installation:
			AlertDialog confirmAlert = (AlertDialog) dialog;
			final String name = installDictionaryItem.getName();
			final long size = installDictionaryItem.getSize();
			final String localizedName = LocalizationHelper
					.getLocalizedDictionaryName(getResources(), name);
			String message;
			if (size > 0) {
				String sizeString = formatBytes(size);
				message = getString(R.string.msg_confirm_installation_size,
						localizedName, sizeString);
			} else {
				message = getString(R.string.msg_confirm_installation, localizedName);
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
		
		selectedFilteredItem = position;
		installDictionaryItem = filteredDictionaries.get(position);

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
	 * Closes the activity and returns the given result code to the calling
	 * activity.
	 * 
	 * @param returnResultCode
	 *            the result code returned to the calling activity
	 */
	private void exitWithoutData(final int returnResultCode) {
		resultCode = returnResultCode;
		returnData = null;
		setResult(resultCode);
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
	 * Reacts on changes in the filter input. 
	 */
	private final TextWatcher filterTextWatcher = new TextWatcher() {
		
		@Override
		public void onTextChanged(final CharSequence sequence, final int start,
				final int before, final int count) {
		}

		@Override
		public void beforeTextChanged(final CharSequence sequence,
				final int start, final int count, final int after) {
		}
		
		@Override
		public void afterTextChanged(final Editable s) {
			if (dictionaries == null || dictionaries.isEmpty()) {
				return;
			}
			updateList();
		}
	};

	/**
	 * Reacts on clicks on the clear filter button.
	 */
	private final OnClickListener clearFilterClickListener = new OnClickListener() {

		@Override
		public void onClick(final View view) {
			final EditText editTextFilter = (EditText) findViewById(R.id.EditTextFilter);
			editTextFilter.setText("");
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.install_dictionary_options, menu);
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		if (DictionaryInstallationService.isRunning()) {
			menu.findItem(R.id.itemCancelInstallation).setVisible(true);
		} else {
			menu.findItem(R.id.itemCancelInstallation).setVisible(false);
		}
		if (Preferences.hasOriginalAutoInstallDictionary() && dictionaries.size() > 0) {
			menu.findItem(R.id.itemReinstallDefaultDictionary).setVisible(true);
		} else {
			menu.findItem(R.id.itemReinstallDefaultDictionary).setVisible(false);
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
			
		case R.id.itemReinstallDefaultDictionary:
			// make sure no other installation is running
			if (DictionaryInstallationService.isRunning()) {
				Toast.makeText(this, R.string.msg_installation_already_started,
						Toast.LENGTH_LONG).show();
				return true;
			}
			// find the dictionary data
			installDictionaryItem = null;
			for (int i = 0; i < dictionaries.size(); i++) {
				final DownloadDictionaryItem dictionary = dictionaries.get(i);
				if (dictionary.getId() == Preferences.getOriginalAutoInstallId()) {
					installDictionaryItem = dictionary;
					break;
				}
			}
			if (installDictionaryItem != null) {
				showDialog(R.id.dialog_confirm_installation);
			} else {
				showDialog(R.id.dialog_dictionary_not_found);
			}
			return true;

		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}

}
