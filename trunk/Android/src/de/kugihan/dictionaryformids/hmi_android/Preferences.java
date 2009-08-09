/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.kugihan.dictionaryformids.hmi_android.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Preferences is an Activity that handles interaction with the preferences and
 * provides an API for all preferences.
 * 
 * Before using this static class, it has to be attached to a Context using
 * attachToContext().
 */
public class Preferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	
	/**
	 * The version of the current preferences implementation.
	 */
	private static final int CURRENT_PREF_VERSION = 1;
	
	private static final String PREF_DICTIONARY_TYPE = "dictionaryType";
	private static final String PREF_VERSION = "preferencesVersion";
	private static final String PREF_DICTIONARY_PATH = "dictionaryPath";
	private static final String PREF_SELECTED_LANGUAGE_INDEX = "selectedLanguageIndex";
	public static final String PREF_RESULT_FONT_SIZE = "resultFontSize";
	public static final String PREF_MAX_RESULTS = "maxResults";
	public static final String PREF_SEARCH_TIMEOUT = "searchTimeout";
	private static final String PREF_WARN_ON_TIMEOUT = "warnOnTimeout";
	private static final String PREF_SEARCH_MODE = "searchMode";
	public static final String PREF_IGNORE_DICTIONARY_TEXT_STYLES = "ignoreDictionaryStyles";
	public static final String PREF_RECENT_DICTIONARIES = "recentDictionaries";

	/**
	 * Saves an instance of the application's shared preferences.
	 */
	private static SharedPreferences preferences = null;
	
	/**
	 * Specifies if the application is run for the first time.
	 */
	private static boolean firstRun = false;
	
	/**
	 * This type includes all supported search modes.
	 */
	private enum SearchMode {
		// Make sure order corresponds to R.array.search_mode_values
		/**
		 * Default search mode.
		 */
		DEFAULT,
		
		/**
		 * Find entries where a word exactly matches the search term.
		 */
		FIND_EXACT_MATCH,
		
		/**
		 * Find entries where the beginning of a word exactly matches the search
		 * term.
		 */
		FIND_ENTRIES_BEGINNING_WITH_SEARCH_TERM,
		
		/**
		 * Find entries where a word includes the search term.
		 */
		FIND_ENTRIES_INCLUDING_SEARCH_TERM 
	}

	/**
	 * This type includes states for all supported dictionary types.
	 */
	public enum DictionaryType {
		/**
		 * Dictionary files are located in a directory.
		 */
		DIRECTORY,
		
		/**
		 * Dictionary files have been packed into an archive.
		 */
		ARCHIVE,
		
		/**
		 * Dictionary files are included into the application.
		 */
		INCLUDED;
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			if (ordinal() == DictionaryType.DIRECTORY.ordinal()) {
				return "DIR";
			} else if (ordinal() == DictionaryType.ARCHIVE.ordinal()) {
				return "ZIP";
			} else if (ordinal() == DictionaryType.INCLUDED.ordinal()) {
				return "INC";
			} else {
				return "";
			}
		}
	};
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		updateSummaries();
	}
	
	/**
	 * Connect the settings to the given context.
	 * 
	 * @param context the context for which the settings are handled
	 */
	public static void attachToContext(final Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		firstRun = !preferences.contains(PREF_VERSION);
		if (firstRun) {
			saveCurrentVersion();
		} else {
			int preferencesVersion = preferences.getInt(PREF_VERSION,
					CURRENT_PREF_VERSION);
			if (preferencesVersion < CURRENT_PREF_VERSION) {
				// if needed later, migrate old settings into new format here
				return;
			}
		}
	}
	
	// TODO: extract all default values somewhere
	
	public static int getMaxResults() {
		String string = preferences.getString(PREF_MAX_RESULTS, "100");
		return Integer.parseInt(string);
	}
	
	public static int getResultFontSize() {
		String string = preferences.getString(PREF_RESULT_FONT_SIZE, "18");
		return Integer.parseInt(string);
	}
	
	public static int getSearchTimeout() {
		String string = preferences.getString(PREF_SEARCH_TIMEOUT, "30");
		return Integer.parseInt(string);
	}
	
	public static boolean getIgnoreDictionaryTextStyles() {
		return preferences.getBoolean(PREF_IGNORE_DICTIONARY_TEXT_STYLES, false);
	}
	
	public static void setSelectedLanguageIndex(final int selectedLanguageIndex) {
		Editor editor = preferences.edit();
		editor.putInt(PREF_SELECTED_LANGUAGE_INDEX, selectedLanguageIndex);
		editor.commit();
	}
	
	private static void saveCurrentVersion() {
		Editor editor = preferences.edit();
		editor.putInt(PREF_VERSION, CURRENT_PREF_VERSION);
		editor.commit();
	}
	
	public static boolean isFirstRun() {
		return firstRun;
	}
	
	public static int getSelectedLanguageIndex() {
		return preferences.getInt(PREF_SELECTED_LANGUAGE_INDEX, 0);
	}
	
	public static void setLoadDictionary(final DictionaryType type,
			final String path, final String[] languages) {
		setLoadDictionaryType(type);
		setDictionaryPath(path);
		addRecentDictionaryUrl(type, path, languages);
	}
	
	private static void setLoadDictionaryType(final DictionaryType type) {
		Editor editor = preferences.edit();
		editor.putInt(PREF_DICTIONARY_TYPE, type.ordinal());
		editor.commit();
	}
	
	public static boolean getLoadIncludedDictionary() {
		return preferences.getInt(PREF_DICTIONARY_TYPE,
				DictionaryType.DIRECTORY.ordinal()) == DictionaryType.INCLUDED
				.ordinal();
	}
	
	public static boolean getLoadArchiveDictionary() {
		return preferences.getInt(PREF_DICTIONARY_TYPE,
				DictionaryType.DIRECTORY.ordinal()) == DictionaryType.ARCHIVE
				.ordinal();
	}

	public static boolean getLoadDirectoryDictionary() {
		return preferences.getInt(PREF_DICTIONARY_TYPE,
				DictionaryType.DIRECTORY.ordinal()) == DictionaryType.DIRECTORY
				.ordinal();
	}

	public static void setWarnOnTimeout(final boolean warnOnTimeout) {
		Editor editor = preferences.edit();
		editor.putBoolean(PREF_WARN_ON_TIMEOUT, warnOnTimeout);
		editor.commit();
	}
	
	public static boolean getWarnOnTimeout() {
		return preferences.getBoolean(PREF_WARN_ON_TIMEOUT, true);
	}
	
	private static void setDictionaryPath(final String path) {
		Editor editor = preferences.edit();
		editor.putString(PREF_DICTIONARY_PATH, path);
		editor.commit();
	}
	
	public static String getDictionaryPath() {
		return preferences.getString(PREF_DICTIONARY_PATH, "/sdcard/dict");
	}
	
	private static int getSearchMode() {
		String string = preferences.getString(PREF_SEARCH_MODE, ""
				+ SearchMode.DEFAULT.ordinal());
		int value = Integer.parseInt(string);
		return value;
	}
	
	public static boolean getIsSearchModeDefault() {
		return getSearchMode() == SearchMode.DEFAULT.ordinal();
	}
	
	public static boolean getFindExactMatch() {
		return getSearchMode() == SearchMode.FIND_EXACT_MATCH.ordinal();
	}
	
	public static boolean getFindEntryBeginningWithSearchTerm() {
		return getSearchMode() == SearchMode.FIND_ENTRIES_BEGINNING_WITH_SEARCH_TERM
				.ordinal();
	}
	
	public static boolean getFindEntryIncludingSearchTerm() {
		return getSearchMode() == SearchMode.FIND_ENTRIES_INCLUDING_SEARCH_TERM.ordinal();
	}
	
	public static String[] getRecentDictionaries() {
		String stringData = preferences.getString(PREF_RECENT_DICTIONARIES, "");
		JSONArray data;
		String[] dictionaryEntries;
		try {
			data = new JSONArray(stringData);
		} catch (JSONException e) {
			data = new JSONArray();
		}
		dictionaryEntries = new String[data.length()];
		for (int i = 0; i < data.length(); i++) {
			try {
				dictionaryEntries[i] = data.getString(i);
			} catch (JSONException e) {
				dictionaryEntries[i] = "";
			}
		}
		return dictionaryEntries;
	}
	
	private static void setRecentDictionaries(String[] dictionaries) {
		JSONArray data = new JSONArray();
		for (int i = 0; i < dictionaries.length; i++) {
			if (dictionaries[i] == null || dictionaries[i].length() == 0) {
				continue;
			}
			data.put(dictionaries[i]);
		}
		
		Editor editor = preferences.edit();
		editor.putString(PREF_RECENT_DICTIONARIES, data.toString());
		editor.commit();
	}
	
	public static void removeRecentDictionary(String path, DictionaryType type) {
		String[] dictionaryUrls = getRecentDictionaries();
		// find out if it exists in the list
		int position = findDictionary(path, type, dictionaryUrls);
		if (position < 0) {
			return;
		}
		// move every dictionary after the searched one one position earlier
		for (int j = position; j < dictionaryUrls.length - 1; j++) {
			dictionaryUrls[j] = dictionaryUrls[j + 1];
		}
		String[] newDictionaryUrls = new String[dictionaryUrls.length - 1];
		for (int i = 0; i < newDictionaryUrls.length; i++) {
			newDictionaryUrls[i] = dictionaryUrls[i];
		}
		setRecentDictionaries(newDictionaryUrls);
		return;
	}
	
	public static String typeToProtocolString(DictionaryType type) {
		String result;
		if (type == DictionaryType.DIRECTORY) {
			result = FileList.FILE_PATH;
		} else if (type == DictionaryType.ARCHIVE) {
			result = FileList.ZIP_PATH;
		} else if (type == DictionaryType.INCLUDED) {
			result = DictionaryList.ASSET_PATH;
		} else {
			throw new IllegalArgumentException();
		}
		return result;
	}
	
	public static void clearRecentDictionaryUrls() {
		setRecentDictionaries(new String[0]);
	}
	
	public static void addRecentDictionaryUrl(DictionaryType type,
			String path, String[] languages) {
		String dictionary = dictionaryToString(type, path, languages);
		if (dictionary == null) {
			return;
		}
		String[] dictionaries = getRecentDictionaries();
		// find out if it already exists in list
		int position = findDictionary(path, type, dictionaries);
		if (position >= 0) {
			// move every dictionary one position later
			for (int j = position; j >= 1; j--) {
				dictionaries[j] = dictionaries[j - 1];
			}
			dictionaries[0] = dictionary;
			setRecentDictionaries(dictionaries);
			return;
		}
		
		// add new entry to the beginning
		String[] biggerDictionaryUrls = new String[dictionaries.length + 1];
		biggerDictionaryUrls[0] = dictionary;
		for (int i = 0; i < dictionaries.length; i++) {
			biggerDictionaryUrls[i + 1] = dictionaries[i];
		}
		setRecentDictionaries(biggerDictionaryUrls);
	}

	/**
	 * Returns the index of the specified dictionary in the given array.
	 * 
	 * @param searchDictionaryPath the path of the dictionary to search for
	 * @param searchType the type of the dictionary to search for
	 * @param dictionaries the array to search in
	 * @return the index of the found string or -1
	 */
	private static int findDictionary(final String searchDictionaryPath,
			final DictionaryType searchType, final String[] dictionaries) {
		int position;
		for (position = 0; position < dictionaries.length; position++) {
			try {
				JSONObject entry = new JSONObject(dictionaries[position]);
				String entryPath = entry.getString("path");
				int entryType = entry.getInt("type");
				if (entryPath.equals(searchDictionaryPath)
						&& searchType.ordinal() == entryType) {
					break;
				}
			} catch (JSONException e) {
				continue;
			}
		}
		if (position >= dictionaries.length) {
			position = -1;
		}
		return position;
	}

	/**
	 * Returns the dictionary URL specified by the parameters.
	 * 
	 * @param type the type of the dictionary
	 * @param path the path of the dictionary
	 * @param languages the languages included in the dictionary
	 * @return the URL of the dictionary
	 */
	private static String dictionaryToString(final DictionaryType type,
			final String path, final String[] languages) {
		JSONObject dictionary = new JSONObject();
		JSONArray abbreviationEntries = new JSONArray();
		for (int i = 0; i < languages.length; i++) {
			abbreviationEntries.put(languages[i]);
		}
		try {
			dictionary.put("path", path);
			dictionary.put("type", type.ordinal());
			dictionary.put("languages", abbreviationEntries.toString());
		} catch (JSONException e) {
			return null;
		}
		return dictionary.toString();
	}
	
	/**
	 * Updates the summary descriptions in the view.
	 */
	private void updateSummaries() {
		getPreferenceScreen().findPreference(PREF_MAX_RESULTS).setSummary(
				"" + getMaxResults());
		getPreferenceScreen().findPreference(PREF_SEARCH_TIMEOUT).setSummary(
				getString(R.string.title_seconds, getSearchTimeout()));
		setListSummary(PREF_SEARCH_MODE, R.array.search_mode,
				R.array.search_mode_values, "" + getSearchMode());
		setListSummary(PREF_RESULT_FONT_SIZE, R.array.font_sizes,
				R.array.font_size_values, "" + getResultFontSize());
	}

	/**
	 * Extracts the current value for a PreferenceList and updates the summary
	 * to show the value's title.
	 * 
	 * @param preferencesId the ID of the PreferenceList to update
	 * @param entriesResourceId the ID of the string-array entries
	 * @param valuesResourceId the ID of the string-array values
	 * @param currentValue the current value of the list
	 */
	private void setListSummary(final String preferencesId,
			final int entriesResourceId, final int valuesResourceId,
			final String currentValue) {
		String[] arrayTitles = getResources().getStringArray(entriesResourceId);
		String[] arrayValues = getResources().getStringArray(valuesResourceId);
		for (int i = 0; i < arrayValues.length; i++) {
			if (arrayValues[i].equals(currentValue)) {
				getPreferenceScreen().findPreference(preferencesId).setSummary(
						arrayTitles[i]);
				break;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onSharedPreferenceChanged(
			final SharedPreferences sharedPreferences,
			final String key) {
		updateSummaries();
	}
}
