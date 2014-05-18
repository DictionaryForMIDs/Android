package de.kugihan.dictionaryformids.hmi_android.thread;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import de.kugihan.dictionaryformids.hmi_android.FileList;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.Preferences.DictionaryType;
import de.kugihan.dictionaryformids.hmi_android.R;

/**
 * HiddenDictionaryFinderTask is an asynchronous task to search installed
 * dictionaries that are not on the list of recent dictionaries and add them
 * there. A progress dialog can be shown during the search.
 *
 */
public class HiddenDictionaryFinderTask extends AsyncTask<String, Integer, ArrayList<File>> {

	/**
	 * The id of the dialog to display while searching.
	 */
	private final int progressDialogId;

	/**
	 * Handle to the activity.
	 */
	private final Activity activity;

	/**
	 * Creates a new instance and attaches it to the given activity.
	 *
	 * @param activity
	 *            the activity to attach to
	 * @param dialogId
	 *            the id of the dialog to display while searching
	 */
	public HiddenDictionaryFinderTask(final Activity activity, final int dialogId) {
		this.activity = activity;
		this.progressDialogId = dialogId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPreExecute() {
		activity.showDialog(progressDialogId);
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ArrayList<File> doInBackground(String... params) {
		ArrayList<File> dictionaries = new ArrayList<File>();

		// check if sd card is available/accessible
		final String storageState = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(storageState)) {
			Toast.makeText(activity, R.string.msg_error_accessing_storage, Toast.LENGTH_LONG).show();
			return dictionaries;
		}

		final String dictionaryDirectoryPath = activity.getString(
				R.string.attribute_installation_directory,
				Environment.getExternalStorageDirectory());
		final File dictionaryDirectory = new File(dictionaryDirectoryPath);

		// make sure path exists
		if (!dictionaryDirectory.isDirectory()) {
			return dictionaries;
		}

		// enumerate dictionary folders
		for (File file : dictionaryDirectory.listFiles()) {
			if (!file.isDirectory() || !file.canRead()) {
				continue;
			}
			// enumerate files and search for properties file
			for (File dictionaryFile : file.listFiles()) {
				if (isCancelled()) {
					return dictionaries;
				}
				if (!FileList.isDictionaryPropertiesFile(dictionaryFile)) {
					continue;
				}
				dictionaries.add(dictionaryFile.getParentFile());
			}
		}

		return dictionaries;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecute(final ArrayList<File> result) {
		for (File file : result) {
			final String[] languages = { file.getName() };
			Preferences.addRecentDictionaryUrl(DictionaryType.DIRECTORY, file.getPath(), languages,
					true);
		}
		try {
			activity.dismissDialog(progressDialogId);
		} catch (IllegalArgumentException e) {
			// if activity was recreated in between, we ignore the exception as
			// the dialog is already closed
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCancelled() {
		activity.dismissDialog(progressDialogId);
	}
}
