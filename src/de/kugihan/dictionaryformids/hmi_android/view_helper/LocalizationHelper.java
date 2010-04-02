package de.kugihan.dictionaryformids.hmi_android.view_helper;

import android.content.res.Resources;
import de.kugihan.dictionaryformids.hmi_android.R;

public class LocalizationHelper {

	/**
	 * Hide constructor as this class only provides static helper functions.
	 */
	private LocalizationHelper() {
		super();
	}

	/**
	 * Returns the localized name for the given language from the given resources.
	 * 
	 * @param resources the resources to load the localization from
	 * @param languageDisplayText the language name that will be localized
	 * @return the localized name of the language
	 */
	public static String getLanguageName(final Resources resources,
			final String languageDisplayText) {
		final String[] languages = resources.getStringArray(R.array.language_localization);
		for (int i = 0; i < languages.length - 1; i += 2) {
			if (languages[i].equals(languageDisplayText)) {
				return languages[i+1];
			}
		}
		return languageDisplayText;
	}
	
	/**
	 * Searches the given name for language names and replaces them with a localized name.
	 * 
	 * @param resources the resources to load the localization from
	 * @param dictionaryName the name of the dictionary to localize
	 * @return the localized name of the dictionary
	 */
	public static String getLocalizedDictionaryName(final Resources resources,
			final String dictionaryName) {
		final String[] languages = resources.getStringArray(R.array.language_localization);
		String localizedDictionaryName = dictionaryName;
		for (int i = 0; i < languages.length - 1; i += 2) {
			localizedDictionaryName = localizedDictionaryName.replaceAll(
					languages[i], languages[i + 1]);
		}
		return localizedDictionaryName;
	}

}
