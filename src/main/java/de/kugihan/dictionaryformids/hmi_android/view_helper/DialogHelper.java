/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 *
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.view_helper;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import de.kugihan.dictionaryformids.hmi_android.DictionaryForMIDs;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.R;

/**
 * A helper class encapsulating all functionality for handling dialogs for
 * {@link DictionaryForMIDs}.
 *
 */
public final class DialogHelper {

	/**
	 * The activity this helper has been attached to.
	 */
	private final DictionaryForMIDs activity;

	/**
	 * An additional message used by ID_DICTIONARY_NOT_FOUND.
	 */
	private static String translationErrorMessage = "";

	/**
	 * The occurred exception during dictionary installation.
	 */
	private static Exception dictionaryInstallationException;

	/**
	 * The message to show to the user.
	 */
	private static String message;

	/**
	 * The intent specifiying the dictionary to load.
	 */
	private static Intent loadDictionary;

	/**
	 * The ID of the indefinite search progress bar.
	 */
	public static final int ID_SEARCHING = 0;

	/**
	 * The ID of the translation error dialog.
	 */
	public static final int ID_TRANSLATE_ERROR = 1;

	/**
	 * The ID of the dictionary error dialog.
	 */
	public static final int ID_DICTIONARY_NOT_FOUND = 2;

	/**
	 * The ID of the dialog shown on the application's first run.
	 */
	public static final int ID_FIRST_RUN = 3;

	/**
	 * The ID of the dialog suggesting to extract the dictionary from the
	 * archive for improved speed.
	 */
	public static final int ID_SUGGEST_DIRECTORY = 4;

	/**
	 * The ID of the dialog warning to extract the jar-dictionary form the
	 * zip-archive.
	 */
	public static final int ID_WARN_EXTRACT_DICTIONARY = 5;

	/**
	 * The ID of the dialog showing exceptions from dictionary installation.
	 */
	public static final int ID_INSTALLATION_EXCEPTION = 6;

	/**
	 * The ID of the dialog asking if the dictionary should be loaded.
	 */
	public static final int ID_CONFIRM_LOAD_DICTIONARY = 7;

	/**
	 * The ID of the dialog asking if the default dictionary should be downloaded and installed.
	 */
	public static final int ID_CONFIRM_INSTALL_DICTIONARY = 8;

	/**
	 * The ID of the dialog showing messages, e.g. from dictionary installation.
	 */
	public static final int ID_MESSAGE = 9;

	/**
	 * Saves the single instance of this class.
	 */
	private static DialogHelper instance = null;

	/**
	 * Returns a DialogHelper that has been attached to the given activity. All
	 * previous activities' dialogs are closed.
	 *
	 * @param mainActivity the activity this helper is attached to
	 * @return the new instance
	 */
	public static DialogHelper getInstance(final DictionaryForMIDs mainActivity) {
		if (instance != null) {
			instance.dismissAllDialogs();
		}
		instance = new DialogHelper(mainActivity);
		return instance;
	}

	/**
	 * The constructor attaches the class to an activity.
	 *
	 * @param mainActivity the activity this helper is attached to
	 */
	private DialogHelper(final DictionaryForMIDs mainActivity) {
		this.activity = mainActivity;
	}

	/**
	 * Dismisses all dialogs.
	 */
	public void dismissAllDialogs() {
		try {
			activity.dismissDialog(ID_SEARCHING);
			activity.dismissDialog(ID_TRANSLATE_ERROR);
			activity.dismissDialog(ID_DICTIONARY_NOT_FOUND);
			activity.dismissDialog(ID_FIRST_RUN);
			activity.dismissDialog(ID_SUGGEST_DIRECTORY);
			activity.dismissDialog(ID_WARN_EXTRACT_DICTIONARY);
			activity.dismissDialog(ID_INSTALLATION_EXCEPTION);
			activity.dismissDialog(ID_CONFIRM_LOAD_DICTIONARY);
			activity.dismissDialog(ID_CONFIRM_INSTALL_DICTIONARY);
			activity.dismissDialog(ID_MESSAGE);
		} catch (IllegalArgumentException e) {
			Log.v(DictionaryForMIDs.LOG_TAG, "IllegelArgumentException: " + e);
		}
	}

	/**
	 * Handles onCreateDialog events.
	 *
	 * @param id the id of the dialog
	 * @return the created dialog
	 */
	public Dialog onCreateDialog(final int id) {
		Dialog result = null;
		if (id == ID_SEARCHING) {
			result = createSearchingDialog();
		} else if (id == ID_TRANSLATE_ERROR) {
			result = createTranslateErrorDialog();
		} else if (id == ID_DICTIONARY_NOT_FOUND) {
			result = createDictionaryNotFoundDialog();
		} else if (id == ID_FIRST_RUN) {
			result = createFirstRunDialog();
		} else if (id == ID_SUGGEST_DIRECTORY) {
			result = createSuggestDirectoryDialog();
		} else if (id == ID_WARN_EXTRACT_DICTIONARY) {
			result = createWarnExtractDictionary();
		} else if (id == ID_INSTALLATION_EXCEPTION) {
			result = createInstallationExceptionDialog();
		} else if (id == ID_CONFIRM_LOAD_DICTIONARY) {
			result = createConfirmLoadDictionary();
		} else if (id == ID_CONFIRM_INSTALL_DICTIONARY) {
			result = createConfirmInstallDictionaryDialog();
		} else if (id == ID_MESSAGE) {
			result = createMessageDialog();
		}
		if (result != null) {
			onPrepareDialog(id, result);
		}
		return result;
	}

	/**
	 * Creates a dialog suggesting the user to extract the currently used zipped
	 * archive to a directory on the sd card.
	 *
	 * @return the created dialog
	 */
	private Dialog createSuggestDirectoryDialog() {
		final Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.title_information);
		alertBuilder.setMessage(R.string.msg_slow_archive_loading);
		alertBuilder.setPositiveButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
					}
				});
		alertBuilder.setNegativeButton(R.string.button_do_not_show_again,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
						Preferences.setWarnOnTimeout(false);
					}
				});
		return alertBuilder.create();
	}

	/**
	 * Creates a dialog asking the user to confirm loading of a dictionary.
	 *
	 * @return the created dialog
	 */
	private Dialog createConfirmLoadDictionary() {
		final Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.title_information);
		alertBuilder.setMessage(R.string.msg_load_dictionary);
		alertBuilder.setPositiveButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
						activity.loadDictionaryFromRemoteIntent(loadDictionary);
						loadDictionary = null;
					}
				});
		alertBuilder.setNegativeButton(R.string.button_cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
					}
				});
		return alertBuilder.create();
	}
	/**
	 * Creates a dialog asking the user to pick a dictionary.
	 *
	 * @return the created dialog
	 */
	private Dialog createFirstRunDialog() {
		final Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.title_welcome);
		alertBuilder.setMessage(R.string.msg_first_run);
		alertBuilder.setPositiveButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
						activity.startChooseDictionaryActivity();
					}
				});
		alertBuilder.setCancelable(false);
		return alertBuilder.create();
	}

	/**
	 * Creates a dialog informing the user that the dictionary could not be
	 * loaded from the selected archive, but still the archive includes another
	 * archive that may be a dictionary.
	 *
	 * @return the created dialog
	 */
	private Dialog createWarnExtractDictionary() {
		final Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.msg_dictionary_error);
		alertBuilder.setMessage(R.string.msg_extract_dictionary);
		alertBuilder.setPositiveButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
						activity.startChooseDictionaryActivity();
					}
				});
		alertBuilder.setCancelable(false);
		return alertBuilder.create();
	}

	/**
	 * Creates a dialog informing the user that the selected dictionary could
	 * not be found.
	 *
	 * @return the created dialog
	 */
	private Dialog createDictionaryNotFoundDialog() {
		final Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.msg_dictionary_error);
		alertBuilder.setMessage(R.string.msg_dictionary_not_found);
		alertBuilder.setNeutralButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
						activity.startChooseDictionaryActivity();
					}
				});
		alertBuilder.setNegativeButton(R.string.button_cancel,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				dialog.cancel();
			}
		});
		return alertBuilder.create();
	}

	/**
	 * Creates a dialog informing the user of errors while translating.
	 *
	 * @return the created dialog
	 */
	private Dialog createTranslateErrorDialog() {
		final Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.title_translation_error);
		alertBuilder.setMessage(translationErrorMessage);
		alertBuilder.setNeutralButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
					}
				});
		return alertBuilder.create();
	}

	/**
	 * Creates a dialog informing the user of exceptions while installing new
	 * dictionaries.
	 *
	 * @return the created dialog
	 */
	private Dialog createInstallationExceptionDialog() {
		final Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.title_information);
		alertBuilder.setMessage("");
		alertBuilder.setNeutralButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
					}
				});
		return alertBuilder.create();
	}

	/**
	 * Creates a dialog showing the user a message, e.g. from the installation service.
	 *
	 * @return the created dialog
	 */
	private Dialog createMessageDialog() {
		final Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.title_information);
		alertBuilder.setMessage("");
		alertBuilder.setPositiveButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
					}
				});
		return alertBuilder.create();
	}

	/**
	 * Creates a indeterminate progress bar informing the user that the
	 * application is currently searching.
	 *
	 * @return the created dialog
	 */
	private Dialog createSearchingDialog() {
		final ProgressDialog loadingDialog = new ProgressDialog(activity);
		loadingDialog.setTitle(activity.getString(R.string.title_please_wait));
		loadingDialog.setMessage(activity.getString(R.string.msg_searching));
		loadingDialog.setIndeterminate(true);
		loadingDialog.setCancelable(true);
		loadingDialog.setOnCancelListener(cancelTranslationListener);
		return loadingDialog;
	}

	/**
	 * Creates a dialog asking the user to confirm the automatic installation of
	 * a preselected dictionary.
	 *
	 * @return the created dialog
	 */
	private Dialog createConfirmInstallDictionaryDialog() {
		final Builder alertBuilder = new AlertDialog.Builder(activity);
		alertBuilder.setMessage(R.string.msg_auto_install).setPositiveButton(
				R.string.button_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						DictionaryForMIDs.startChooseDictionaryActivity(
								activity, true, true);
					}
				});
		alertBuilder.setNeutralButton(R.string.button_cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// just close the dialog and do nothing
					}
				});
		alertBuilder.setNegativeButton(R.string.button_do_not_show_again,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// don't show the dialog again
						Preferences.removeAutoInstallDictionaryId();
					}
				});
		return alertBuilder.create();
	}

	/**
	 * Handles onPrepareDialog events.
	 *
	 * @param id
	 *            the id of the dialog
	 * @param dialog
	 *            the dialog that shall be prepared
	 */
	public void onPrepareDialog(final int id, final Dialog dialog) {
		if (id == ID_TRANSLATE_ERROR) {
			final AlertDialog alert = (AlertDialog) dialog;
			alert.setMessage(translationErrorMessage);
		} else if (id == ID_INSTALLATION_EXCEPTION) {
			final AlertDialog alert = (AlertDialog) dialog;
			String exceptionMessage = "Exception while installing:\n";
			if (dictionaryInstallationException != null) {
					exceptionMessage += dictionaryInstallationException.getMessage();
					if (dictionaryInstallationException.getCause() != null) {
						exceptionMessage += "\n\nCause:\n"
								+ dictionaryInstallationException.getCause();
					}
			} else {
				exceptionMessage += "<null>";
			}
			alert.setMessage(exceptionMessage);
		} else if (id == ID_MESSAGE) {
			final AlertDialog alert = (AlertDialog) dialog;
			alert.setMessage(message);
		}
	}

	/**
	 * The listener handles cancel events of the translation progress dialog.
	 */
	private final OnCancelListener cancelTranslationListener = new OnCancelListener() {
		@Override
		public void onCancel(final DialogInterface dialog) {
			activity.cancelActiveTranslation();
			Toast.makeText(activity,
					R.string.msg_translation_cancelled, Toast.LENGTH_SHORT)
					.show();
		}
	};

	/**
	 * Sets the message that is displayed by the TranslationErrorDialog.
	 *
	 * @param message the message to display
	 */
	public static void setTranslationErrorMessage(final String message) {
		translationErrorMessage = message;
	}

	/**
	 * Sets the exception that occurred while installing a dictionary.
	 *
	 * @param exception
	 *            the occurred exception
	 */
	public static void setInstallationException(final Exception exception) {
		dictionaryInstallationException = exception;
	}

	/**
	 * Sets the intent that specifies the dictionary to load.
	 *
	 * @param intent
	 *            the intent specifying the dictionary to load
	 */
	public static void setLoadDictionary(final Intent intent) {
		loadDictionary = intent;
	}

	/**
	 * Sets the message that will be displayed to the user.
	 *
	 * @param informationMessage the message to display
	 */
	public static void setMessage(final String informationMessage) {
		message = informationMessage;
	}
}
