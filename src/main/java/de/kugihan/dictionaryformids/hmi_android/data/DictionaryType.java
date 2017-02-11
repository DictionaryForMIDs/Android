package de.kugihan.dictionaryformids.hmi_android.data;

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
}
