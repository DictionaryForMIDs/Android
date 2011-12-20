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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * Preferences is an Activity that handles interaction with the
 * preferencesInstance and provides an API for all preferencesInstance.
 *
 * Before using this static class, it has to be attached to a Context using
 * attachToContext().
 */
public class Preferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	/**
	 * The version of the current preferencesInstance implementation.
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
	public static final String PREF_LANGUAGE_CODE = "languageCode";
	private static final String PREF_AUTO_INSTALL_DICTIONARY = "autoInstallDictionary";
	public static final String PREF_STARRED_WORDS = "starredWords";

	/**
	 * Saves an instance of the application's shared preferences.
	 */
	private static SharedPreferences preferencesInstance = null;

	/**
	 * Saves an instance of the application's resources.
	 */
	private static Resources resources = null;

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
	 * @param context
	 *            the context for which the settings are handled
	 */
	public static void attachToContext(final Context context) {
		resources = context.getResources();
		preferencesInstance = PreferenceManager
				.getDefaultSharedPreferences(context);
		firstRun = !preferencesInstance.contains(PREF_VERSION);
		if (firstRun) {
			saveCurrentVersion();
			saveAutoInstallDictionaryId();
		} else {
			final int preferencesVersion = preferencesInstance.getInt(PREF_VERSION,
					CURRENT_PREF_VERSION);
			if (preferencesVersion < CURRENT_PREF_VERSION) {
				// if needed later, migrate old settings into new format here
				return;
			}
		}
	}

	// TODO: extract all default values to res/values/preferences.xml

	public static int getMaxResults() {
		final String string = preferencesInstance.getString(PREF_MAX_RESULTS, "100");
		return Integer.parseInt(string);
	}

	public static int getResultFontSize() {
		final String string = preferencesInstance.getString(PREF_RESULT_FONT_SIZE,
				"18");
		return Integer.parseInt(string);
	}

	public static int getSearchTimeout() {
		final String string = preferencesInstance
				.getString(PREF_SEARCH_TIMEOUT, "30");
		return Integer.parseInt(string);
	}

	public static boolean getIgnoreDictionaryTextStyles() {
		return preferencesInstance.getBoolean(
				PREF_IGNORE_DICTIONARY_TEXT_STYLES, false);
	}

	public static void setSelectedLanguageIndex(final int selectedLanguageIndex) {
		final Editor editor = preferencesInstance.edit();
		editor.putInt(PREF_SELECTED_LANGUAGE_INDEX, selectedLanguageIndex);
		editor.commit();
	}

	private static void saveCurrentVersion() {
		final Editor editor = preferencesInstance.edit();
		editor.putInt(PREF_VERSION, CURRENT_PREF_VERSION);
		editor.commit();
	}

	private static void saveAutoInstallDictionaryId() {
		final Editor editor = preferencesInstance.edit();
		editor.putInt(PREF_AUTO_INSTALL_DICTIONARY, getOriginalAutoInstallId());
		editor.commit();
	}

	public static int getOriginalAutoInstallId() {
		return resources
				.getInteger(R.integer.preferences_default_auto_install_id);
	}

	public static boolean isFirstRun() {
		return firstRun;
	}

	public static int getSelectedLanguageIndex() {
		return preferencesInstance.getInt(PREF_SELECTED_LANGUAGE_INDEX, 0);
	}

	/**
	 * Returns the ID of the dictionary to pre-install.
	 *
	 * @return the dictionary ID or 0
	 */
	public static int getAutoInstallDictionaryId() {
		return preferencesInstance.getInt(PREF_AUTO_INSTALL_DICTIONARY, 0);
	}

	/**
	 * Returns if a dictionary can be auto-installed.
	 *
	 * @return true if a dictionary can be auto-installed
	 */
	public static boolean hasAutoInstallDictionary() {
		return getAutoInstallDictionaryId() > 0;
	}

	/**
	 * Checks if originally a dictionary could be auto-installed. It may already
	 * be installed by now.
	 *
	 * @return true if originally a dictionary was available for
	 *         auto-installation
	 */
	public static boolean hasOriginalAutoInstallDictionary() {
		return getOriginalAutoInstallId() > 0;
	}

	public static void removeAutoInstallDictionaryId() {
		final Editor editor = preferencesInstance.edit();
		editor.putInt(PREF_AUTO_INSTALL_DICTIONARY, 0);
		editor.commit();
	}

	public static void setLoadDictionary(final DictionaryType type,
			final String path, final String[] languages) {
		setLoadDictionaryType(type);
		setDictionaryPath(path);
		addRecentDictionaryUrl(type, path, languages);
	}

	private static void setLoadDictionaryType(final DictionaryType type) {
		final Editor editor = preferencesInstance.edit();
		editor.putInt(PREF_DICTIONARY_TYPE, type.ordinal());
		editor.commit();
	}

	private static int getLoadDictionaryType() {
		return preferencesInstance.getInt(PREF_DICTIONARY_TYPE, -1);
	}

	public static boolean getLoadIncludedDictionary() {
		return getLoadDictionaryType() == DictionaryType.INCLUDED.ordinal();
	}

	public static boolean getLoadArchiveDictionary() {
		return getLoadDictionaryType() == DictionaryType.ARCHIVE.ordinal();
	}

	public static boolean getLoadDirectoryDictionary() {
		return getLoadDictionaryType() == DictionaryType.DIRECTORY.ordinal();
	}

	public static void setWarnOnTimeout(final boolean warnOnTimeout) {
		final Editor editor = preferencesInstance.edit();
		editor.putBoolean(PREF_WARN_ON_TIMEOUT, warnOnTimeout);
		editor.commit();
	}

	public static boolean getWarnOnTimeout() {
		return preferencesInstance.getBoolean(PREF_WARN_ON_TIMEOUT, true);
	}

	private static void setDictionaryPath(final String path) {
		final Editor editor = preferencesInstance.edit();
		editor.putString(PREF_DICTIONARY_PATH, path);
		editor.commit();
	}

	public static String getDictionaryPath() {
		return preferencesInstance.getString(PREF_DICTIONARY_PATH,
				"/sdcard/dict");
	}

	public static String getLanguageCode() {
		return preferencesInstance.getString(PREF_LANGUAGE_CODE, "");
	}

	private static int getSearchMode() {
		final String string = preferencesInstance.getString(PREF_SEARCH_MODE, ""
				+ SearchMode.DEFAULT.ordinal());
		return Integer.parseInt(string);
	}

	public static boolean getIsSearchModeDefault() {
		return getSearchMode() == SearchMode.DEFAULT.ordinal();
	}

	public static boolean getIsStarredWordsEnabled() {
		return preferencesInstance.getBoolean(PREF_STARRED_WORDS, false);
	}

	public static void setIsStarredWordsEnabled(final boolean isEnabled) {
		final Editor editor = preferencesInstance.edit();
		editor.putBoolean(PREF_STARRED_WORDS, isEnabled);
		editor.commit();
	}

	public static boolean getFindExactMatch() {
		return getSearchMode() == SearchMode.FIND_EXACT_MATCH.ordinal();
	}

	public static boolean getFindEntryBeginningWithSearchTerm() {
		return getSearchMode() == SearchMode.FIND_ENTRIES_BEGINNING_WITH_SEARCH_TERM
				.ordinal();
	}

	public static boolean getFindEntryIncludingSearchTerm() {
		return getSearchMode() == SearchMode.FIND_ENTRIES_INCLUDING_SEARCH_TERM
				.ordinal();
	}

	public static String[] getRecentDictionaries() {
		final String stringData = preferencesInstance.getString(
				PREF_RECENT_DICTIONARIES, "");
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

	private static void setRecentDictionaries(final String[] dictionaries) {
		final JSONArray data = new JSONArray();
		for (int i = 0; i < dictionaries.length; i++) {
			if (dictionaries[i] == null || dictionaries[i].length() == 0) {
				continue;
			}
			data.put(dictionaries[i]);
		}

		final Editor editor = preferencesInstance.edit();
		editor.putString(PREF_RECENT_DICTIONARIES, data.toString());
		editor.commit();
	}

	public static void removeRecentDictionary(final String path,
			final DictionaryType type) {
		final String[] dictionaryUrls = getRecentDictionaries();
		// find out if it exists in the list
		final int position = findDictionary(path, type, dictionaryUrls);
		if (position < 0) {
			return;
		}
		// move every dictionary after the searched one one position earlier
		for (int j = position; j < dictionaryUrls.length - 1; j++) {
			dictionaryUrls[j] = dictionaryUrls[j + 1];
		}
		final String[] newDictionaryUrls = new String[dictionaryUrls.length - 1];
		for (int i = 0; i < newDictionaryUrls.length; i++) {
			newDictionaryUrls[i] = dictionaryUrls[i];
		}
		setRecentDictionaries(newDictionaryUrls);
	}

	public static String typeToProtocolString(final DictionaryType type) {
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

	public static void addRecentDictionaryUrl(final DictionaryType type,
			final String path, final String[] languages) {
		addRecentDictionaryUrl(type, path, languages, false);
	}

	public static void addRecentDictionaryUrl(final DictionaryType type,
			final String path, final String[] languages,
			final boolean ignoreExisting) {
		final String dictionary = dictionaryToString(type, path, languages);
		if (dictionary == null) {
			return;
		}
		final String[] dictionaries = getRecentDictionaries();

		// find out if it already exists in list
		final int position = findDictionary(path, type, dictionaries);
		if (ignoreExisting && position >= 0) {
			return;
		}

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
		final String[] biggerDictionaryUrls = new String[dictionaries.length + 1];
		biggerDictionaryUrls[0] = dictionary;
		for (int i = 0; i < dictionaries.length; i++) {
			biggerDictionaryUrls[i + 1] = dictionaries[i];
		}
		setRecentDictionaries(biggerDictionaryUrls);
	}

	/**
	 * Returns the index of the specified dictionary in the given array.
	 *
	 * @param searchDictionaryPath
	 *            the path of the dictionary to search for
	 * @param searchType
	 *            the type of the dictionary to search for
	 * @param dictionaries
	 *            the array to search in
	 * @return the index of the found string or -1
	 */
	private static int findDictionary(final String searchDictionaryPath,
			final DictionaryType searchType, final String[] dictionaries) {
		int position;
		for (position = 0; position < dictionaries.length; position++) {
			try {
				final JSONObject entry = new JSONObject(dictionaries[position]);
				final String entryPath = entry.getString("path");
				final int entryType = entry.getInt("type");
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
	 * @param type
	 *            the type of the dictionary
	 * @param path
	 *            the path of the dictionary
	 * @param languages
	 *            the languages included in the dictionary
	 * @return the URL of the dictionary
	 */
	private static String dictionaryToString(final DictionaryType type,
			final String path, final String[] languages) {
		final JSONObject dictionary = new JSONObject();
		final JSONArray abbreviationEntries = new JSONArray();
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
		setListSummary(PREF_LANGUAGE_CODE, R.array.language_codes,
				R.array.language_code_values, getLanguageCode());
	}

	/**
	 * Extracts the current value for a PreferenceList and updates the summary
	 * to show the value's title.
	 *
	 * @param preferencesId
	 *            the ID of the PreferenceList to update
	 * @param entriesResourceId
	 *            the ID of the string-array entries
	 * @param valuesResourceId
	 *            the ID of the string-array values
	 * @param currentValue
	 *            the current value of the list
	 */
	private void setListSummary(final String preferencesId,
			final int entriesResourceId, final int valuesResourceId,
			final String currentValue) {
		final String[] arrayTitles = getResources().getStringArray(entriesResourceId);
		final String[] arrayValues = getResources().getStringArray(valuesResourceId);
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
			final SharedPreferences sharedPreferences, final String key) {
		if (key.equals(PREF_LANGUAGE_CODE)) {
			// set new locale
			final String languageCode = getLanguageCode();
			DictionaryForMIDs.setCustomLocale(languageCode, getBaseContext()
					.getResources());
			// tell user to restart application
			Toast.makeText(getBaseContext(),
					R.string.msg_restart_app_after_settings_changed,
					Toast.LENGTH_LONG).show();
		}
		updateSummaries();
	}
}
