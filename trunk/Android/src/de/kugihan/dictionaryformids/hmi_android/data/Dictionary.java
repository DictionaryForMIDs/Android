package de.kugihan.dictionaryformids.hmi_android.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Observable;

import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.hmi_android.Preferences;

public class Dictionary extends Observable {

	private DictionaryDataFile file;
	private final HashSet<LanguagePair> selectedPairs = new HashSet<LanguagePair>();
	private final Preferences.DictionaryType type;
	private final String path;
	private String abbreviation;

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

	public Dictionary(String abbreviation, Preferences.DictionaryType type, String path) {
		this.file = null;
		this.type = type;
		this.path = path;
		this.abbreviation = abbreviation;
	}

	public Dictionary(DictionaryDataFile file, String abbreviation, Preferences.DictionaryType type, String path) {
		this.file = file;
		this.type = type;
		this.path = path;
		this.abbreviation = abbreviation;
	}

	public Dictionary(DictionaryDataFile file, Preferences.DictionaryType type, String path, LanguagePair[] selectedPairs) {
		this(file, file.dictionaryAbbreviation, type, path);
		if (selectedPairs != null) {
			this.selectedPairs.addAll(Arrays.asList(selectedPairs));
		}
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
		setChanged();
		notifyObservers();
	}

	public void unloadFile() {
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
		LanguagePair pair = getSelectedPair(fromLanguage, toLanguage);
		if (selected && pair == null) {
			selectedPairs.add(new LanguagePair(fromLanguage, toLanguage));
		} else if (!selected && pair != null) {
			selectedPairs.remove(pair);
		}
		setChanged();
		notifyObservers();
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

	@Override
	public boolean equals(Object o) {
		if (o instanceof Dictionary) {
			Dictionary dictionary = (Dictionary) o;
			return path.equals(dictionary.path) && type == dictionary.type;
		}
		return super.equals(o);
	}
}
