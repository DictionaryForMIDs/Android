/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.kugihan.dictionaryformids.hmi_android.data.ResultProvider;

/**
 * FileList represents an Activity that represents a file browser and allows the
 * user to choose a dictionary to load.
 * 
 */
public class FileList extends ListActivity implements ResultProvider {

	/**
	 * The string identifying dictionaries in the file system.
	 */
	public static final String FILE_PATH = "filePath";

	/**
	 * The string identifying dictionaries in the archives.
	 */
	public static final String ZIP_PATH = "zipPath";

	/**
	 * The key of a string specifying the current directory of the file browser.
	 */
	private static final String BUNDLE_CURRENT_DIRECTORY = "currentDirectory";

	/**
	 * The name of the dictionary's properties file.
	 */
	private static final String PROPERTIES_FILE = "DictionaryForMIDs.properties";

	/**
	 * The array of supported archive file extensions.
	 */
	private static final String[] ARCHIVE_EXTENSIONS = { ".jar", ".zip" };

	/**
	 * The list of items in the current directory.
	 */
	private List<String> items = null;

	/**
	 * The current directory.
	 */
	private File currentDirectory = null;

	/**
	 * The result returned to TabHost.
	 */
	private Intent returnData = null;

	/**
	 * The result code returned to TabHost.
	 */
	private int resultCode = RESULT_CANCELED;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.directory_list);

		// try to restore old data
		if (icicle != null) {
			String path = icicle.getString(BUNDLE_CURRENT_DIRECTORY);
			if (path != null) {
				fill(new File(path));
			}
		}

		// if current directory has not been restored load standard
		if (currentDirectory == null) {
			fillWithCard();
		}

		((Button) findViewById(R.id.ButtonCard))
				.setOnClickListener(clickListener);
		((Button) findViewById(R.id.ButtonParent))
				.setOnClickListener(clickListener);
		((Button) findViewById(R.id.ButtonRoot))
				.setOnClickListener(clickListener);
		((Button) findViewById(R.id.okButton))
				.setOnClickListener(clickListener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		outState
				.putString(BUNDLE_CURRENT_DIRECTORY, currentDirectory.getPath());
		super.onSaveInstanceState(outState);
	}

	/**
	 * Fills the file browser with the content of the specified directory.
	 * 
	 * @param parentDirectory
	 *            this directory's content will be displayed
	 */
	private void fill(final File parentDirectory) {
		currentDirectory = parentDirectory;
		updateNavigationButtonState();
		items = new ArrayList<String>();
		List<String> view = new ArrayList<String>();
		boolean directoryIncludesDictionary = false;
		for (File file : parentDirectory.listFiles()) {
			String path = file.getPath();
			String name = file.getName();
			boolean isDictionaryFile = isDictionaryPropertiesFile(file);
			boolean isDictionaryArchive = isArchiveFile(file);
			if (file.isDirectory()) {
				path += File.separator;
				name += File.separator;
			} else if (isDictionaryFile) {
				directoryIncludesDictionary = true;
				// if there is an extracted dictionary in this directory, only
				// display properties file to speed up loading of large
				// dictionaries
				items.clear();
				view.clear();
				items.add(path);
				view.add(name);
				break;
			} else if (!isDictionaryArchive) {
				continue;
			}
			items.add(path);
			view.add(name);
		}
		Collections.sort(items, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(view, String.CASE_INSENSITIVE_ORDER);
		ArrayAdapter<String> fileList;
		if (findViewById(R.id.PathView).getVisibility() == View.GONE) {
			fileList = new ArrayAdapter<String>(this, R.layout.file_row, items);
		} else {
			fileList = new ArrayAdapter<String>(this, R.layout.file_row, view);
		}
		setListAdapter(fileList);
		if (directoryIncludesDictionary) {
			((Button) findViewById(R.id.okButton)).setVisibility(View.VISIBLE);
		} else {
			((Button) findViewById(R.id.okButton)).setVisibility(View.GONE);
		}
		final TextView pathView = (TextView) findViewById(R.id.PathView);
		pathView.setText(getString(R.string.title_current_path, parentDirectory
				.getPath()));
	}

	/**
	 * Enables and disables navigation buttons according to the current
	 * directory.
	 */
	private void updateNavigationButtonState() {
		if (currentDirectory != null
				&& currentDirectory.getPath().equals(
						getString(R.string.attribute_root_path))) {
			((Button) findViewById(R.id.ButtonRoot)).setEnabled(false);
		} else {
			((Button) findViewById(R.id.ButtonRoot)).setEnabled(true);
		}
		if (currentDirectory != null
				&& currentDirectory.getPath().equals(
						Environment.getExternalStorageDirectory())) {
			((Button) findViewById(R.id.ButtonCard)).setEnabled(false);
		} else {
			((Button) findViewById(R.id.ButtonCard)).setEnabled(true);
		}
		if (currentDirectory != null && currentDirectory.getParent() == null) {
			((Button) findViewById(R.id.ButtonParent)).setEnabled(false);
		} else {
			((Button) findViewById(R.id.ButtonParent)).setEnabled(true);
		}
	}

	/**
	 * Checks if the specified file exists and has the name of a dictionary
	 * properties file.
	 * 
	 * @param file
	 *            the file to analyze
	 * @return true if the file exists and has the name of a dictionary
	 *         properties file
	 */
	private boolean isDictionaryPropertiesFile(final File file) {
		return file.isFile() && file.getName().equals(PROPERTIES_FILE);
	}

	/**
	 * Checks if the file exists and if it has the extension of a supported
	 * archive file.
	 * 
	 * @param file
	 *            the file to analyze
	 * @return true if the file exists and has a supported archive's extension
	 */
	private boolean isArchiveFile(final File file) {
		if (!file.isFile()) {
			return false;
		}
		for (String extension : ARCHIVE_EXTENSIONS) {
			if (file.getName().endsWith(extension)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		File file = new File(items.get(position));
		if (file.isDirectory()) {
			fill(file);
		} else if (isDictionaryPropertiesFile(file)) {
			exitWithCurrentDirectory();
		} else if (isArchiveFile(file)) {
			exitWithZipArchive(file.getPath());
		} else {
			Toast.makeText(getBaseContext(), R.string.msg_no_dictionary,
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Closes the activity and returns the specified zip file to the activity's
	 * caller.
	 * 
	 * @param path
	 *            the path of the zip file to return
	 */
	private void exitWithZipArchive(final String path) {
		exitWithData(ZIP_PATH, path);
	}

	/**
	 * Closes the activity and returns the current directory to the activity's
	 * caller.
	 */
	private void exitWithCurrentDirectory() {
		String data = currentDirectory.getPath();
		exitWithData(FILE_PATH, data);
	}

	/**
	 * Closes the activity and returns the specified dictionary to the
	 * activity's caller.
	 * 
	 * @param dataName
	 *            the type of the dictionary
	 * @param data
	 *            the path of the dictionary
	 */
	private void exitWithData(final String dataName, final String data) {
		resultCode = RESULT_OK;
		returnData = new Intent();
		returnData.putExtra(dataName, data);
		setResult(resultCode, returnData);
		finish();
	}

	/**
	 * Fills the view with the content of the current directory's parent
	 * directory.
	 */
	private void fillWithParent() {
		File file = currentDirectory.getParentFile();
		if (file != null && file.isDirectory()) {
			fill(file);
		} else {
			Toast.makeText(getBaseContext(), R.string.msg_no_dictionary,
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Fills the view with the content of the root directory.
	 */
	private void fillWithRoot() {
		fill(new File(getString(R.string.attribute_root_path)));
	}

	/**
	 * Fills the view with the content of sdcard's root directory.
	 */
	private void fillWithCard() {
		fill(Environment.getExternalStorageDirectory());
	}

	/**
	 * Listener to handle clicks on the buttons.
	 */
	private OnClickListener clickListener = new OnClickListener() {
		public void onClick(final View button) {
			switch (button.getId()) {
			case R.id.ButtonCard:
				fillWithCard();
				break;

			case R.id.ButtonParent:
				fillWithParent();
				break;

			case R.id.ButtonRoot:
				fillWithRoot();
				break;

			case R.id.okButton:
				exitWithCurrentDirectory();
				break;

			default:
				break;
			}
		}
	};

	/**
	 * {@inheritDoc}
	 */
	public final Intent getReturnData() {
		return returnData;
	}

	/**
	 * {@inheritDoc}
	 */
	public final int getResultCode() {
		return resultCode;
	}
}
