package de.kugihan.dictionaryformids.hmi_android;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.ListActivity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.hmi_android.data.StarredWordsAdapter;
import de.kugihan.dictionaryformids.hmi_android.data.StarredWordsProvider;
import de.kugihan.dictionaryformids.hmi_android.data.StarredWordsProvider.StarredWords;

/**
 * Displays the list of starred words.
 *
 */
public class StarredWordsList extends ListActivity {

	/**
	 * Observer to be notified about data changes to trigger view updates.
	 */
	private final DataSetObserver dataSetObserver = new DataSetObserver() {

		@Override
		public void onChanged() {
			new RefreshItemCountTask().execute();
		}
	};

	/**
	 * Listener to be notified of changes to the checked radio buttons.
	 */
	private final RadioGroup.OnCheckedChangeListener onCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			// Gets called twice, once for the previously checked button
			// and once for the newly checked button.
			// Ignore the call for the previous one, only handle the new.
			final RadioButton button = (RadioButton) group.findViewById(checkedId);
			if (button == null || !button.isChecked()) {
				return;
			}
			// refresh word list
			new RefreshAdapterTask().execute();
		}
	};

	/**
	 * Task to update the TextView displaying number of available starred words.
	 */
	private class RefreshItemCountTask extends AsyncTask<Void, Void, Long> {

		private boolean showAllDictionaries = true;

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onPreExecute() {
			final RadioGroup group = (RadioGroup) findViewById(R.id.listState);
			radioGroupSetEnabled(false);

			// Cancel the task if there is no active dictionary
			if (!isDictionaryLoaded()) {
				cancel(true);
                return;
            }

			if (group.getCheckedRadioButtonId() == R.id.showFromAllDictionaries) {
				showAllDictionaries = true;
			} else {
				showAllDictionaries = false;
			}
    		setProgressBarIndeterminateVisibility(true);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected Long doInBackground(Void... params) {
			// get the count for the invisible list
			String selection = null;
			String[] selectionArgs = null;
			if (showAllDictionaries) {
				selection = StarredWords.DICTIONARY_NAME + " = ?";
				selectionArgs = new String[] { DictionaryDataFile.dictionaryAbbreviation };
			}
			final Cursor cursor = getContentResolver().query(
					Uri.withAppendedPath(StarredWordsProvider.CONTENT_URI, "count"), null, selection,
					selectionArgs, null);
			cursor.moveToFirst();
			return cursor.getLong(0);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onPostExecute(Long totalItemsCount) {
			final RadioButton radioAll = (RadioButton) findViewById(R.id.showFromAllDictionaries);
			final RadioButton radioCurrent = (RadioButton) findViewById(R.id.showFromCurrentDictionary);
			if (!showAllDictionaries) {
				radioAll.setText(getResources().getString(R.string.starred_words_all_dictionaries,
						totalItemsCount));
				radioCurrent.setText(getResources().getString(
						R.string.starred_words_current_dictionary, getListAdapter().getCount()));
			} else {
				radioAll.setText(getResources().getString(R.string.starred_words_all_dictionaries,
						getListAdapter().getCount()));
				radioCurrent.setText(getResources().getString(
						R.string.starred_words_current_dictionary, totalItemsCount));
			}
			radioAll.setVisibility(View.VISIBLE);
			radioCurrent.setVisibility(View.VISIBLE);
			radioGroupSetEnabled(true);
			setProgressBarIndeterminateVisibility(false);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onCancelled() {
    		final RadioButton radioAll = (RadioButton) findViewById(R.id.showFromAllDictionaries);
			final RadioButton radioCurrent = (RadioButton) findViewById(R.id.showFromCurrentDictionary);
			if (!showAllDictionaries) {
				radioAll.setText(getResources().getString(R.string.starred_words_all_dictionaries,
						0));
				radioCurrent.setText(getResources().getString(
						R.string.starred_words_current_dictionary, getListAdapter().getCount()));
			} else {
				radioAll.setText(getResources().getString(R.string.starred_words_all_dictionaries,
						getListAdapter().getCount()));
				radioCurrent.setText(getResources().getString(
						R.string.starred_words_current_dictionary, 0));
			}
			radioAll.setVisibility(View.VISIBLE);
			radioCurrent.setVisibility(View.VISIBLE);

			if (isDictionaryLoaded()) {
				radioGroupSetEnabled(true);
            }

			setProgressBarIndeterminateVisibility(false);
		}
	}

	/**
	 * Task to update the displayed list of starred words.
	 */
	private class RefreshAdapterTask extends AsyncTask<Void, Void, Cursor> {

		private boolean showAllDictionaries = true;

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
			final TextView view = (TextView) findViewById(android.R.id.empty);
			view.setVisibility(View.INVISIBLE);
			radioGroupSetEnabled(false);
			final RadioGroup group = (RadioGroup) findViewById(R.id.listState);
			if (group.getCheckedRadioButtonId() == R.id.showFromAllDictionaries) {
				showAllDictionaries = true;
			} else {
				showAllDictionaries = false;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected Cursor doInBackground(Void... params) {
			String selection = null;
			String[] selectionArgs = null;
			if (!showAllDictionaries) {
				selection = StarredWords.DICTIONARY_NAME + " = ?";
				selectionArgs = new String[] { DictionaryDataFile.dictionaryAbbreviation };
			}
			final String[] projection = new String[] { StarredWords._ID, StarredWords.TRANSLATION };
			return getContentResolver().query(StarredWordsProvider.CONTENT_URI, projection, selection,
					selectionArgs, null);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onPostExecute(Cursor cursor) {
			final StarredWordsAdapter adapter = (StarredWordsAdapter) getListAdapter();
			adapter.changeCursor(cursor);
			// listen for database content
			cursor.registerContentObserver(new ContentObserver(new Handler()) {
				@Override
				public void onChange(boolean selfChange) {
					// refresh the adapter as database content changed
					new RefreshAdapterTask().execute();
				}
			});
			if (isDictionaryLoaded()) {
				radioGroupSetEnabled(true);
    		}
			setProgressBarIndeterminateVisibility(false);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onCancelled() {
			setProgressBarIndeterminateVisibility(false);
		}
	}

	/**
	 * Task to export all starred words to a file per dictionary to external
	 * storage.
	 */
	private class StarredWordsExporter extends AsyncTask<Void, Void, Exception> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected Exception doInBackground(Void... params) {
			File root = Environment.getExternalStorageDirectory();
			if (!root.canWrite()) {
				Log.e(DictionaryForMIDs.LOG_TAG, "Root not writeable: " + root.getAbsolutePath());
				return new IOException("Root not writeable: " + root.getAbsolutePath());
			}
			final Cursor cursor = getContentResolver().query(StarredWordsProvider.CONTENT_URI, null, null,
					null, null);
			try {
				while (cursor.moveToNext()) {
					String fileName = StarredWordsProvider.getDictionaryId(cursor)
							+ StarredWordsProvider.getFromLanguageId(cursor);
					File file = new File(root, "DfM-export-" + fileName + ".txt");
					FileWriter writer = new FileWriter(file, true);
					BufferedWriter out = new BufferedWriter(writer);
					// fromTextLanguage
					out.write(StarredWordsProvider.getTranslation(cursor).getFromTextAsString() + "; ");
					out.write(StarredWordsProvider.getTranslation(cursor).getToTextsAsString("; ") + ";");
					out.write("\n");
					out.close();
				}
			} catch (IOException e) {
				Log.e(DictionaryForMIDs.LOG_TAG, "Could not write file " + e.getMessage());
				return e;
			} catch (DictionaryException e) {
				Log.e(DictionaryForMIDs.LOG_TAG, "Could not parse entry", e);
				return e;
			} finally {
				cursor.close();
			}
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onPostExecute(Exception exception) {
			if (exception != null) {
				Toast.makeText(StarredWordsList.this, R.string.msg_external_storage_inaccessible,
						Toast.LENGTH_LONG).show();
				return;
			}
			Toast.makeText(StarredWordsList.this, R.string.msg_exported_starred_words,
					Toast.LENGTH_LONG).show();
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// load theme before call to setContentView()
		DictionaryForMIDs.setApplicationTheme(this);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.starred_words_list);

		final StarredWordsAdapter adapter = new StarredWordsAdapter(this, null);
		setListAdapter(adapter);
		adapter.registerDataSetObserver(dataSetObserver);

		final RadioGroup group = (RadioGroup) findViewById(R.id.listState);
		if (!isDictionaryLoaded()) {
            // make sure entries from all dictionaries are shown
            group.check(R.id.showFromAllDictionaries);
            // disable switching to current dictionary
			radioGroupSetEnabled(false);
		} else {
			radioGroupSetEnabled(true);
		}
		group.setOnCheckedChangeListener(onCheckedChangeListener);

		if (savedInstanceState == null) {
			// load words
			new RefreshAdapterTask().execute();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		// temporarily remove the checked change listener to prevent multiple
		// refreshes of the adapter
		final RadioGroup group = (RadioGroup) findViewById(R.id.listState);
		group.setOnCheckedChangeListener(null);
		super.onRestoreInstanceState(state);
		group.setOnCheckedChangeListener(onCheckedChangeListener);
		// manually trigger refresh
		new RefreshAdapterTask().execute();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.starred_words_options, menu);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.itemExport) {
			exportStarredWordsToFiles();
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Exports the starred words for each dictionary to a separate file.
	 */
	private void exportStarredWordsToFiles() {
		new StarredWordsExporter().execute();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// close cursor
		final StarredWordsAdapter adapter = (StarredWordsAdapter) getListAdapter();
		adapter.unregisterDataSetObserver(dataSetObserver);
		final Cursor cursor = adapter.getCursor();
		if (cursor != null) {
			cursor.close();
		}
	}

	/**
	 * Checks if a loaded dictionary is currently available.
	 *
	 * @return true if a loaded dictionary is currently available
	 */
	private boolean isDictionaryLoaded() {
		final boolean isDictionaryLoaded = DictionaryDataFile.dictionaryAbbreviation != null;
		return isDictionaryLoaded;
	}

	/**
	 * Set the enabled state of the radio group and all its children.
	 *
	 * @param enabled
	 *            true if this view is enabled, false otherwise
	 */
	private void radioGroupSetEnabled(final boolean enabled) {
		final RadioGroup group = (RadioGroup) findViewById(R.id.listState);
		group.setEnabled(enabled);
		for (int i = 0; i < group.getChildCount(); i++) {
			group.getChildAt(i).setEnabled(enabled);
		}
	}

}