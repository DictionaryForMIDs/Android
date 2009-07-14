/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.view_helper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.util.Log;
import android.widget.Toast;
import de.kugihan.dictionaryformids.hmi_android.DictionaryForMIDs;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.translation.TranslationExecution;

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
		if (id == ID_SEARCHING) {
			return createSearchingDialog();
		} else if (id == ID_TRANSLATE_ERROR) {
			return createTranslateErrorDialog();
		} else if (id == ID_DICTIONARY_NOT_FOUND) {
			return createDictionaryNotFoundDialog();
		} else if (id == ID_FIRST_RUN) {
			return createFirstRunDialog();
		} else if (id == ID_SUGGEST_DIRECTORY) {
			return createSuggestDirectoryDialog();
		} else if (id == ID_WARN_EXTRACT_DICTIONARY) {
			return createWarnExtractDictionary();
		} else {
			return null;
		}
	}

	/**
	 * Creates a dialog suggesting the user to extract the currently used zipped
	 * archive to a directory on the sd card.
	 * 
	 * @return the created dialog
	 */
	private Dialog createSuggestDirectoryDialog() {
		Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.title_information);
		alertBuilder.setMessage(R.string.msg_slow_archive_loading);
		alertBuilder.setPositiveButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
					}
				});
		alertBuilder.setNegativeButton(R.string.button_do_not_show_again,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
						Preferences.setWarnOnTimeout(false);
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
		Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.title_welcome);
		alertBuilder.setMessage(R.string.msg_first_run);
		alertBuilder.setPositiveButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
						activity.showChooseDictionaryActivity();
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
		Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.msg_dictionary_error);
		alertBuilder.setMessage(R.string.msg_extract_dictionary);
		alertBuilder.setPositiveButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
						activity.showChooseDictionaryActivity();
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
		Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.msg_dictionary_error);
		alertBuilder.setMessage(R.string.msg_dictionary_not_found);
		alertBuilder.setNeutralButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						dialog.cancel();
						activity.showChooseDictionaryActivity();
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
		Builder alertBuilder = new AlertDialog.Builder(
				activity);
		alertBuilder.setTitle(R.string.title_translation_error);
		alertBuilder.setMessage(translationErrorMessage);
		alertBuilder.setNeutralButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
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
		ProgressDialog loadingDialog = new ProgressDialog(activity);
		loadingDialog.setTitle(activity.getString(R.string.title_please_wait));
		loadingDialog.setMessage(activity.getString(R.string.msg_searching));
		loadingDialog.setIndeterminate(true);
		loadingDialog.setCancelable(true);
		loadingDialog.setOnCancelListener(cancelTranslationListener);
		return loadingDialog;
	}
	
	/**
	 * Handles onPrepareDialog events.
	 * 
	 * @param id the id of the dialog
	 * @param dialog the dialog that shall be prepared
	 */
	public void onPrepareDialog(final int id, final Dialog dialog) {
		if (id == ID_TRANSLATE_ERROR) {
			AlertDialog alert = (AlertDialog) dialog;
			alert.setMessage(translationErrorMessage);
		}
	}

	/**
	 * The listener handles cancel events of the translation progress dialog.
	 */
	private OnCancelListener cancelTranslationListener = new OnCancelListener() {
		@Override
		public void onCancel(final DialogInterface dialog) {
			TranslationExecution.cancelLastTranslation();
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

}
