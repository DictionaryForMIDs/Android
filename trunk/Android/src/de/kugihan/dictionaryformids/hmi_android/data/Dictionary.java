package de.kugihan.dictionaryformids.hmi_android.data;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Observable;

import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.dataaccess.LanguageDefinition;
import de.kugihan.dictionaryformids.hmi_android.DictionaryForMIDs;
import de.kugihan.dictionaryformids.hmi_android.Preferences;

public class Dictionary extends Observable {

	private DictionaryDataFile file;
	private final HashSet<LanguagePair> selectedPairs = new HashSet<LanguagePair>();
	private final Preferences.DictionaryType type;
	private final String path;
	private String abbreviation;

	/**
	 * Holds a language selection set when the dictionary has not been loaded yet.
	 */
	private LanguageSelectionSet languageSelectionSet = null;

	public static class LanguagePair {
		private final int fromLanguage;
		private final int toLanguage;

		public LanguagePair(int fromLanguage, int toLanguage) {
			this.fromLanguage = fromLanguage;
			this.toLanguage = toLanguage;
		}

		public boolean equals(int fromLanguage, int toLanguage) {
			return this.toLanguage == toLanguage
					&& this.fromLanguage == fromLanguage;
		}

		public int getFromLanguage() {
			return fromLanguage;
		}

		public int getToLanguage() {
			return toLanguage;
		}
	}

	public abstract static class LanguageSelectionSet {
		protected static final String JSON_TYPE_PROPERTY = "type";
		public abstract void applyToDictionary(Dictionary dictionary);
		public abstract JSONObject serializeAsJson() throws JSONException;
		public static LanguageSelectionSet unserialize(JSONObject jsonObject) throws JSONException {
			// Check if a selection was saved
			if (jsonObject.get(JSON_TYPE_PROPERTY) == null
					|| jsonObject.getString(JSON_TYPE_PROPERTY).length() == 0) {
				return new LanguagePairSelectionSet(new LanguagePair[0]);
			}

			// Try to create a selection object
			LanguageIndexSelectionSet indexSelection = LanguageIndexSelectionSet.createFromJson(jsonObject);
			if (indexSelection != null) {
				return indexSelection;
			}
			LanguageSelectionSet pairSelection = LanguagePairSelectionSet.createFromJson(jsonObject);
			if (pairSelection != null) {
				return pairSelection;
			}

			throw new JSONException("Unsupported selection type: " + jsonObject.get(JSON_TYPE_PROPERTY));
		}
	}

	public static class LanguagePairSelectionSet extends LanguageSelectionSet {
		public static final String JSON_TYPE_VALUE = "pairs";
		public final LanguagePair[] languagePairs;

		public LanguagePairSelectionSet(LanguagePair[] languagePairs) {
			this.languagePairs = languagePairs;
		}

		@Override
		public void applyToDictionary(Dictionary dictionary) {
			if (dictionary.getFile() == null) {
				throw new IllegalStateException("Dictionary must be loaded first");
			}
			for (Dictionary.LanguagePair selectedPair : languagePairs) {
				dictionary.setPairSelection(selectedPair.getFromLanguage(), selectedPair.getToLanguage(), true);
			}
		}

		@Override
		public JSONObject serializeAsJson() throws JSONException {
			// Create array of selected language pairs
			final JSONArray jsonArraySelectionPairs = new JSONArray();
			for (Dictionary.LanguagePair languagePair : languagePairs) {
				JSONObject languagePairJsonObject = new JSONObject();
				languagePairJsonObject.put("from", languagePair.getFromLanguage());
				languagePairJsonObject.put("to", languagePair.getToLanguage());
				jsonArraySelectionPairs.put(languagePairJsonObject);
			}
			final JSONObject jsonResult = new JSONObject();
			jsonResult.put(JSON_TYPE_PROPERTY, JSON_TYPE_VALUE);
			jsonResult.put("array", jsonArraySelectionPairs);
			return jsonResult;
		}

		public static LanguagePairSelectionSet createFromJson(JSONObject jsonObject) throws JSONException {
			boolean isPairSelection = LanguagePairSelectionSet.JSON_TYPE_VALUE.equals((jsonObject.getString(JSON_TYPE_PROPERTY)));
			if (!isPairSelection) {
				return null;
			}
			JSONArray languagePairsJsonArray = jsonObject.getJSONArray("array");
			LanguagePair[] languagePairs = new LanguagePair[languagePairsJsonArray.length()];
			for (int j = 0; j < languagePairsJsonArray.length(); j++) {
				JSONObject languagePairJsonObject = languagePairsJsonArray.getJSONObject(j);
				int from = languagePairJsonObject.getInt("from");
				int to = languagePairJsonObject.getInt("to");
				languagePairs[j] = new LanguagePair(from, to);
			}
			return new LanguagePairSelectionSet(languagePairs);
		}
	}

	public static class LanguageIndexSelectionSet extends LanguageSelectionSet {
		public static final String JSON_TYPE_VALUE = "index";
		private final int selectedIndex;

		public LanguageIndexSelectionSet(int selectedIndex) {
			this.selectedIndex = selectedIndex;
		}

		@Override
		public void applyToDictionary(Dictionary dictionary) {
			if (dictionary.getFile() == null) {
				throw new IllegalStateException("Dictionary must be loaded first");
			}
			final DictionaryDataFile file = dictionary.getFile();
			final int numberOfTargetLanguages = file.numberOfAvailableLanguages - 1;
			int numberOfSearchableLanguages = 0;
			int fromLanguage;
			for (fromLanguage = 0; fromLanguage < file.supportedLanguages.length; fromLanguage++) {
				LanguageDefinition supportedLanguage = file.supportedLanguages[fromLanguage];
				if (supportedLanguage.isSearchable) {
					if (selectedIndex < (numberOfSearchableLanguages + 1) * numberOfTargetLanguages) {
						break;
					}
					numberOfSearchableLanguages++;
				}
			}
			int toLanguage = selectedIndex % numberOfTargetLanguages;
			if (fromLanguage >= file.numberOfAvailableLanguages) {
				Log.i(DictionaryForMIDs.LOG_TAG, "Invalid language selection index (higher than available selections): " + selectedIndex);
				return;
			}

			// Add one to language as the diagonal is not selectable (Language0 -> Language0)
			if (toLanguage >= fromLanguage) {
				toLanguage++;
			}
			dictionary.setPairSelection(fromLanguage, toLanguage, true);
		}

		@Override
		public JSONObject serializeAsJson() throws JSONException {
			final JSONObject jsonObject = new JSONObject();
			jsonObject.put(JSON_TYPE_PROPERTY, JSON_TYPE_VALUE);
			jsonObject.put("index", selectedIndex);
			return jsonObject;
		}

		public static LanguageIndexSelectionSet createFromJson(JSONObject jsonObject) throws JSONException {
			boolean isIndexSelection = LanguageIndexSelectionSet.JSON_TYPE_VALUE.equals((jsonObject.getString(JSON_TYPE_PROPERTY)));
			if (!isIndexSelection) {
				return null;
			}
			int index = jsonObject.getInt("index");
			return new LanguageIndexSelectionSet(index);
		}
	}

	public Dictionary(String abbreviation, Preferences.DictionaryType type, String path) {
		this(abbreviation, type, path, null);
	}

	public Dictionary(String abbreviation, Preferences.DictionaryType type, String path, LanguageSelectionSet languageSelectionSet) {
		this.file = null;
		this.type = type;
		this.path = path;
		this.abbreviation = abbreviation;
		this.languageSelectionSet = languageSelectionSet;
	}

	public Dictionary(DictionaryDataFile file, String abbreviation, Preferences.DictionaryType type, String path) {
		this.file = file;
		this.type = type;
		this.path = path;
		this.abbreviation = abbreviation;
	}

	public Dictionary(DictionaryDataFile file, Preferences.DictionaryType type, String path) {
		this(file, file.dictionaryAbbreviation, type, path);
	}

	public DictionaryDataFile getFile() {
		return file;
	}

	public void setFile(DictionaryDataFile file) {
		if (file == null) {
			throw new IllegalArgumentException();
		}
		this.file = file;
		this.abbreviation = file.dictionaryAbbreviation;
		if (languageSelectionSet != null) {
			languageSelectionSet.applyToDictionary(this);
		}
		setChanged();
		notifyObservers();
	}

	public void setLanguageSelectionSet(LanguageSelectionSet selectionSet) {
		if (file != null) {
			throw new IllegalStateException("LanguageSet can only be set if file is not active.");
		}
		this.languageSelectionSet = selectionSet;
		setChanged();
		notifyObservers();
	}

	public void unloadFile() {
		String languages = TextUtils.join(" ", getLanguages());
		this.abbreviation = languages;
		this.file = null;
		this.selectedPairs.clear();
		setChanged();
		notifyObservers();
	}

	public Preferences.DictionaryType getType() {
		return type;
	}

	public String getPath() {
		return path;
	}

	public String getAbbreviation() {
		return abbreviation;
	}

	public String[] getLanguages() {
		final String[] results = new String[file.numberOfAvailableLanguages];
		for (int i = 0; i < file.supportedLanguages.length; i++) {
			results[i] = file.supportedLanguages[i].languageDisplayText;
		}
		return results;
	}

	public boolean isPairSelected(int fromLanguage, int toLanguage) {
		if (fromLanguage < 0 || toLanguage < 0) {
			throw new IllegalArgumentException("from: " + fromLanguage + " to: " + toLanguage);
		}
		LanguagePair pair = getSelectedPair(fromLanguage, toLanguage);
		return pair != null;
	}

	public void setPairSelection(int fromLanguage, int toLanguage, boolean selected) {
		// Reset saved selection
		languageSelectionSet = null;
		// Update selection state
		LanguagePair pair = getSelectedPair(fromLanguage, toLanguage);
		if (selected && pair == null) {
			selectedPairs.add(new LanguagePair(fromLanguage, toLanguage));
		} else if (!selected && pair != null) {
			selectedPairs.remove(pair);
		}
		setChanged();
		notifyObservers();
	}

	public LanguageSelectionSet getSelectedLanguages() {
		if (languageSelectionSet != null) {
			return languageSelectionSet;
		} else {
			LanguagePairSelectionSet selection = new LanguagePairSelectionSet(getSelectedPairs());
			return selection;
		}
	}

	public LanguagePair[] getSelectedPairs() {
		return selectedPairs.toArray(new LanguagePair[selectedPairs.size()]);
	}

	private LanguagePair getSelectedPair(int fromLanguage, int toLanguage) {
		for (LanguagePair selectedPair : selectedPairs) {
			if (selectedPair.equals(fromLanguage, toLanguage)) {
				return selectedPair;
			}
		}
		return null;
	}

	public boolean equalsDictionary(Dictionary dictionary) {
		return this.equalsDictionary(dictionary.type, dictionary.path);
	}

	public boolean equalsDictionary(Preferences.DictionaryType type, String path) {
		return this.path.equals(path) && this.type == type;
	}
}
