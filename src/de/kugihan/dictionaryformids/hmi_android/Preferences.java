/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 *
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

import de.kugihan.dictionaryformids.hmi_android.data.Dictionary;
import de.kugihan.dictionaryformids.hmi_android.view_helper.LocalizationHelper;

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
	public static final int CURRENT_PREF_VERSION = 2;

	public static final String PREF_LOAD_DICTIONARIES = "loadDictionaries";
	public static final String PREF_VERSION = "preferencesVersion";
	public static final String PREF_DICTIONARY_PATH = "dictionaryPath";
	public static final String PREF_SELECTED_LANGUAGE_INDEX = "selectedLanguageIndex";
	public static final String PREF_RESULT_FONT_SIZE = "resultFontSize";
	public static final String PREF_MAX_RESULTS = "maxResults";
	public static final String PREF_SEARCH_TIMEOUT = "searchTimeout";
	public static final String PREF_WARN_ON_TIMEOUT = "warnOnTimeout";
	public static final String PREF_SEARCH_MODE = "searchMode";
	public static final String PREF_IGNORE_DICTIONARY_TEXT_STYLES = "ignoreDictionaryStyles";
	public static final String PREF_RECENT_DICTIONARIES = "recentDictionaries";
	public static final String PREF_LANGUAGE_CODE = "languageCode";
	public static final String PREF_AUTO_INSTALL_DICTIONARY = "autoInstallDictionary";
	public static final String PREF_STARRED_WORDS = "starredWords";
	public static final String PREF_SEARCH_AS_YOU_TYPE = "searchAsYouType";
	public static final String PREF_THEME = "theme";

	/**
	 * Saves an instance of the application's context.
	 */
	private static Context contextInstance = null;

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
		DictionaryForMIDs.setApplicationTheme(this);
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		final String values[] = getResources().getStringArray(R.array.theme_values);
		final String entryValues[] = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			entryValues[i] = values[i];
			if (values[i].indexOf('/') < 0)
				continue;
			if (values[i].indexOf(':') < 0) {
				values[i] = getPackageName() + ":" + values[i];
			}
			int resourceId = getResources().getIdentifier(values[i], null, null);
			if (resourceId == 0)
				continue;
			entryValues[i] = Integer.toString(resourceId);
		}
		((ListPreference) findPreference(PREF_THEME)).setEntryValues(entryValues);

		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		updateSummaries();
	}

	private static boolean isInteger(final String string) {
		try {
			Integer.parseInt(string);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Connect the settings to the given context.
	 *
	 * @param context
	 *            the context for which the settings are handled
	 */
	public static void attachToContext(final Context context) {
		contextInstance = context;
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
				// save settings version
				saveCurrentVersion();
				// migrate old settings into new format here
				migrateSettings(preferencesVersion);
			}
		}
		// make sure the light theme is selected if dictionary styles are used
		final boolean shouldUseDictionaryStyles = !getIgnoreDictionaryTextStyles();
		final boolean lightThemeSelected = getApplicationTheme() == R.style.DarkThemeSelector;
		if (shouldUseDictionaryStyles && !lightThemeSelected) {
			setApplicationTheme(R.style.LightThemeSelector);
		}
	}

	/**
	 * Migrates old preferences to current version.
	 *
	 * @param previousVersion
	 *            the previously preferences version
	 */
	private static void migrateSettings(final int previousVersion) {
		if (previousVersion < 2) {
			// dictionary styles never worked before, reset setting
			setIgnoreDictionaryTextStyles(false);
		}
	}

	public static int getMaxResults() {
		return getStringPreferenceAsInteger(PREF_MAX_RESULTS,
				R.integer.preferences_default_max_results);
	}

	public static void setMaxResults(final int maxResults) {
		setStringPreferenceFromInteger(PREF_MAX_RESULTS, maxResults);
	}

	public static int getResultFontSize() {
		return getStringPreferenceAsInteger(PREF_RESULT_FONT_SIZE,
				R.integer.preferences_default_font_size);
	}

	public static void setResultFontSize(final int fontSize) {
		setStringPreferenceFromInteger(PREF_RESULT_FONT_SIZE, fontSize);
	}

	public static int getSearchTimeout() {
		return getStringPreferenceAsInteger(PREF_SEARCH_TIMEOUT,
				R.integer.preferences_default_search_timeout);
	}

	/**
	 * Returns the value of an string preference as integer or resets it to the
	 * given default value if it cannot be parsed as integer. This is used for
	 * numeric preferences that are edited by the user using an
	 * EditTextPreference.
	 *
	 * @param preferenceKey
	 *            the string key identifying the preference to return
	 * @param defaultIntegerResourceId
	 *            the id of the integer resource specifying the default value
	 * @return the value of the preference
	 * @throws NotFoundException
	 *             in case the resource of the default value was not found
	 */
	private static int getStringPreferenceAsInteger(final String preferenceKey,
			final int defaultIntegerResourceId) throws NotFoundException {
		final int defaultValue = resources.getInteger(defaultIntegerResourceId);
		final String defaultString = Integer.toString(defaultValue);
		final String value = preferencesInstance.getString(preferenceKey, defaultString);
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			setStringPreferenceFromInteger(preferenceKey, defaultValue);
			Toast.makeText(contextInstance, R.string.msg_error_parsing_integer_preference,
					Toast.LENGTH_LONG).show();
			return defaultValue;
		}
	}

	/**
	 * Sets the value of a string preference from an integer. This is used for
	 * numeric preferences that are edited by the user using an
	 * EditTextPreference.
	 *
	 * @param preferenceKey
	 *            the key identifying the preference to set
	 * @param value
	 *            the value to set the preference to
	 */
	private static void setStringPreferenceFromInteger(final String preferenceKey,
			final int value) {
		final Editor editor = preferencesInstance.edit();
		editor.putString(preferenceKey, Integer.toString(value));
		editor.commit();
	}

	public static void setSearchTimeout(final int timeout) {
		setStringPreferenceFromInteger(PREF_SEARCH_TIMEOUT, timeout);
	}

	public static boolean getIgnoreDictionaryTextStyles() {
		final boolean defaultValue = resources
				.getBoolean(R.bool.preferences_default_ignore_font_styles);
		return preferencesInstance.getBoolean(PREF_IGNORE_DICTIONARY_TEXT_STYLES, defaultValue);
	}

	public static void setIgnoreDictionaryTextStyles(final boolean ignoreStyles) {
		final Editor editor = preferencesInstance.edit();
		editor.putBoolean(PREF_IGNORE_DICTIONARY_TEXT_STYLES, ignoreStyles);
		editor.commit();
	}

	public static boolean getSearchAsYouType() {
		final boolean defaultValue = resources
				.getBoolean(R.bool.preferences_default_search_as_you_type);
		return preferencesInstance.getBoolean(PREF_SEARCH_AS_YOU_TYPE, defaultValue);
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

	public static void setLoadDictionary(final Iterable<Dictionary> dictionaries) {

		final JSONArray data = new JSONArray();

		for (Dictionary dictionary : dictionaries) {
			if (dictionary.getFile() == null) {
				continue;
			}
			final JSONObject jsonObject = new JSONObject();
			final JSONArray jsonArraySelectionPairs = new JSONArray();
			// Create object including info to load dictionary
			try {
				// Create array of selected language pairs
				for (Dictionary.LanguagePair languagePair : dictionary.getSelectedPairs()) {
					JSONObject languagePairJsonObject = new JSONObject();
					languagePairJsonObject.put("from", languagePair.getFromLanguage());
					languagePairJsonObject.put("to", languagePair.getToLanguage());
					jsonArraySelectionPairs.put(languagePairJsonObject);
				}
				jsonObject.put("path", dictionary.getPath());
				jsonObject.put("type", dictionary.getType().ordinal());
				jsonObject.put("abbreviation", dictionary.getAbbreviation());
				jsonObject.put("selectedLanguagePairs", jsonArraySelectionPairs);
			} catch (JSONException e) {
				Log.e(DictionaryForMIDs.LOG_TAG, "Failed to save loaded dictionaries", e);
			}

			data.put(jsonObject.toString());
		}

		final Editor editor = preferencesInstance.edit();
		editor.putString(PREF_LOAD_DICTIONARIES, data.toString());
		editor.commit();
	}

	public static final Vector<Dictionary> getLoadedDictionaries() {
		Vector<Dictionary> dictionaries = new Vector<Dictionary>();

		String stringData = preferencesInstance.getString(PREF_LOAD_DICTIONARIES, "");

		JSONArray jsonArray;
		try {
			jsonArray = new JSONArray(stringData);
		} catch (JSONException e) {
			jsonArray = new JSONArray();
		}

		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				String dictionaryString = jsonArray.getString(i);
				JSONObject dictionaryJsonObject = new JSONObject(dictionaryString);
				String path = dictionaryJsonObject.getString("path");
				int typeId = dictionaryJsonObject.getInt("type");
				String abbreviation = dictionaryJsonObject.getString("abbreviation");
				DictionaryType type = DictionaryType.values()[typeId];

				Dictionary dictionary = new Dictionary(abbreviation, type, path);
				JSONArray languagePairsJsonArray = dictionaryJsonObject.getJSONArray("selectedLanguagePairs");
				for (int j = 0; j < languagePairsJsonArray.length(); j++) {
					JSONObject languagePairJsonObject = languagePairsJsonArray.getJSONObject(j);
					int from = languagePairJsonObject.getInt("from");
					int to = languagePairJsonObject.getInt("to");
					dictionary.setPairSelection(from, to, true);
				}

				dictionaries.add(dictionary);
			} catch (JSONException e) {
				Log.e(DictionaryForMIDs.LOG_TAG, "Failed to retrieve loaded dictionaries", e);
			}
		}

		return dictionaries;
	}

//	private static int getLoadDictionaryType() {
//		return preferencesInstance.getInt(PREF_DICTIONARY_TYPE, -1);
//	}
//
//	public static boolean getLoadIncludedDictionary() {
//		return getLoadDictionaryType() == DictionaryType.INCLUDED.ordinal();
//	}
//
//	public static boolean getLoadArchiveDictionary() {
//		return getLoadDictionaryType() == DictionaryType.ARCHIVE.ordinal();
//	}
//
//	public static boolean getLoadDirectoryDictionary() {
//		return getLoadDictionaryType() == DictionaryType.DIRECTORY.ordinal();
//	}

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

	public static String getLanguageCode() {
		final String defaultValue = resources.getString(R.string.preferences_default_language_code);
		return preferencesInstance.getString(PREF_LANGUAGE_CODE, defaultValue);
	}

	private static int getSearchMode() {
		final int defaultValue = resources.getInteger(R.integer.preferences_default_search_mode);
		final String string = preferencesInstance.getString(PREF_SEARCH_MODE, Integer.toString(defaultValue));
		return Integer.parseInt(string);
	}

	public static boolean getIsSearchModeDefault() {
		return getSearchMode() == SearchMode.DEFAULT.ordinal();
	}

	public static boolean getIsStarredWordsEnabled() {
		final boolean defaultValue = resources.getBoolean(R.bool.preferences_default_enable_starred_words);
		return preferencesInstance.getBoolean(PREF_STARRED_WORDS, defaultValue);
	}

	public static void setStarredWordsEnabled(final boolean isEnabled) {
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

	public static int getApplicationTheme() {
		final int defaultValue = resources.getInteger(R.integer.preferences_default_theme);
		final String string = preferencesInstance.getString(PREF_THEME,
				Integer.toString(defaultValue));
		if (isInteger(string)) {
			return Integer.parseInt(string);
		} else {
			return Resources.getSystem().getIdentifier(string, null, null);
		}
	}

	public static void setApplicationTheme(int resourceId) {
		final Editor editor = preferencesInstance.edit();
		editor.putString(PREF_THEME, Integer.toString(resourceId));
		editor.commit();
	}

	public static String[] getRecentDictionaryStrings() {
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

	public static Vector<Dictionary> getRecentDictionaries() {
		final String[] dictionaries = Preferences.getRecentDictionaryStrings();
		final Vector<Dictionary> result = new Vector<Dictionary>();
		for (String dictionaryString : dictionaries) {
			JSONObject parts;
			int type;
			String path;
			String languages = "";
			try {
				parts = new JSONObject(dictionaryString);
				type = parts.getInt("type");
				path = parts.getString("path");
				JSONArray languagesArray = new JSONArray(parts
						.getString("languages"));
				for (int i = 0; i < languagesArray.length(); i++) {
					final String language = languagesArray.getString(i);
					final String localizedLanguage = LocalizationHelper
							.getLanguageName(contextInstance.getResources(), language);
					languages += localizedLanguage + " ";
				}
				languages = languages.trim();
			} catch (JSONException e) {
				continue;
			}
			DictionaryType dictionaryType = DictionaryType.values()[type];
			Dictionary dictionary = new Dictionary(languages, dictionaryType, path);
			result.add(dictionary);
		}
		return result;
	}

	private static void setRecentDictionaries(final String[] dictionaries) {
		final JSONArray data = new JSONArray();
		for (String dictionary : dictionaries) {
			if (dictionary == null || dictionary.length() == 0) {
				continue;
			}
			data.put(dictionary);
		}

		final Editor editor = preferencesInstance.edit();
		editor.putString(PREF_RECENT_DICTIONARIES, data.toString());
		editor.commit();
	}

	public static void removeRecentDictionary(final String path,
			final DictionaryType type) {
		final String[] dictionaryUrls = getRecentDictionaryStrings();
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
		final String[] dictionaries = getRecentDictionaryStrings();

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
		for (String language : languages) {
			abbreviationEntries.put(language);
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
		if (getIgnoreDictionaryTextStyles()) {
			findPreference(PREF_IGNORE_DICTIONARY_TEXT_STYLES).setSummary(
					R.string.summary_pref_ignore_font_style_unchecked);
		} else {
			findPreference(PREF_IGNORE_DICTIONARY_TEXT_STYLES).setSummary(
					R.string.summary_pref_ignore_font_style_checked);
		}
		setListSummary(PREF_SEARCH_MODE);
		setListSummary(PREF_RESULT_FONT_SIZE);
		setListSummary(PREF_LANGUAGE_CODE);
		setListSummary(PREF_THEME);
	}

	/**
	 * Extracts the current value for a PreferenceList and updates the summary
	 * to show the value's title.
	 *
	 * @param preferencesId
	 *            the ID of the PreferenceList to update
	 * @param currentValue
	 *            the current value of the list
	 */
	private void setListSummary(final String preferencesId, final String currentValue) {
		final ListPreference listPreference = (ListPreference) getPreferenceScreen()
				.findPreference(preferencesId);
		final CharSequence[] arrayTitles = listPreference.getEntries();
		final CharSequence[] arrayValues = listPreference.getEntryValues();
		for (int i = 0; i < arrayValues.length; i++) {
			if (arrayValues[i].equals(currentValue)) {
				listPreference.setSummary(arrayTitles[i]);
				break;
			}
		}
	}

	/**
	 * Extracts the current value for a PreferenceList and updates the summary
	 * to show the value's title.
	 *
	 * @param preferencesId
	 *            the ID of the PreferenceList to update
	 */
	private void setListSummary(final String preferencesId) {
		final ListPreference listPreference = (ListPreference) findPreference(preferencesId);
		listPreference.setSummary(listPreference.getEntry());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
			final String key) {

		if (key.equals(PREF_LANGUAGE_CODE)) {
			// set new locale
			final String languageCode = getLanguageCode();
			DictionaryForMIDs.setCustomLocale(languageCode, getBaseContext().getResources());
			// reset the title of preference activity as this does not happen automatically
			// after language change
			setTitle(R.string.title_activity_preferences);
		}

		if (key.equals(PREF_IGNORE_DICTIONARY_TEXT_STYLES) && !getIgnoreDictionaryTextStyles()) {
			// force light theme if styles from dictionary are used
			setApplicationTheme(R.style.LightThemeSelector);
		}

		// tell user to restart application
		if (key.equals(PREF_LANGUAGE_CODE) || key.equals(PREF_THEME)) {
			Toast.makeText(getBaseContext(), R.string.msg_restart_app_after_settings_changed,
					Toast.LENGTH_LONG).show();
		}

		updateSummaries();
	}
}
