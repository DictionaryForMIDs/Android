/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.data;

import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.hmi_common.content.ContentParser;
import de.kugihan.dictionaryformids.hmi_common.content.StringColourItemText;
import de.kugihan.dictionaryformids.translation.SingleTranslation;
import de.kugihan.dictionaryformids.translation.TextOfLanguage;

/**
 * SingleTranslationHelper is a static class that provides convenient methods
 * for accessing data of a {@link SingleTranslation} as Strings.
 * 
 */
public final class SingleTranslationHelper {
	
	/**
	 * The items of the from-language.
	 */
	private static String[] fromItems;
	
	/**
	 * The items of the each row of toItems.
	 */
	private static String[][] toItems;
	
	/**
	 * A String-representation of the translation item.
	 */
	private static String all;
	
	/**
	 * A String-representation of all fromItems.
	 */
	private static String fromRow;
	
	/**
	 * A String-representation of all rows of toItems.
	 */
	private static String toRows;
	
	/**
	 * The sum of the items in each toRow. 
	 */
	private static int countToItems;
	
	/**
	 * Loads default values.
	 */
	private static void reset() {
		fromItems = null;
		toItems = null;
		all = "";
		fromRow = "";
		toRows = "";
		countToItems = 0;
	}
	
	/**
	 * Private constructor to prevent instantiating of this utility class.
	 */
	private SingleTranslationHelper() {
		// making constructor private as class is static
	}
	
	/**
	 * Initializes the class to the given translation.
	 * 
	 * @param translation the translation that will be used by all getters
	 * @throws DictionaryException if a parsing error occurred
	 */
	public static void setTranslation(final SingleTranslation translation)
			throws DictionaryException {
		reset();
		ContentParser parser = new ContentParser();
		StringColourItemText colourItemText;
		colourItemText = parser.determineItemsFromContent(translation
				.getFromText(), true, true);

		fromItems = new String[colourItemText.size()];
		for (int i = 0; i < colourItemText.size(); i++) {
			fromItems[i] = colourItemText.getItemTextPart(i).getText();
			fromRow += fromItems[i] + " ";
		}
		fromRow = fromRow.trim();
		all += fromRow + "\n";
		toItems = new String[translation.getToTexts().size()][];
		for (int j = 0; j < translation.getToTexts().size(); j++) {
			TextOfLanguage element = (TextOfLanguage) translation.getToTexts()
					.elementAt(j);
			colourItemText = parser.determineItemsFromContent(element, true, true);
			toItems[j] = new String[colourItemText.size()];
			for (int i = 0; i < colourItemText.size(); i++) {
				toItems[j][i] = colourItemText.getItemTextPart(i).getText();
				toRows += toItems[j][i] + " ";
				countToItems++;
			}
			toRows = toRows.trim();
			toRows += "\n";
		}
		toRows = toRows.trim();
		all += toRows;
		all = all.trim();
	}
	
	/**
	 * Returns a representation of the whole translation.
	 * 
	 * @return a String containing one line for the fromText and n lines for
	 *         each toText item
	 */
	public static String getAll() {
		return all;
	}
	
	/**
	 * Returns a single line representation of the fromText.
	 * 
	 * @return a String containing a single fromText line
	 */
	public static String getFromRow() {
		return fromRow;
	}
	
	/**
	 * Returns a multi-line representation of toTexts.
	 * 
	 * @return a String containing one line for each toText item
	 */
	public static String getToRows() {
		return toRows;
	}
	
	/**
	 * Returns a single fromText item.
	 * 
	 * @param part
	 *            the part of fromText to return
	 * @return a String containing the fromText item
	 */
	public static String getFromItem(final int part) {
		return fromItems[part];
	}

	/**
	 * Returns a single toText item.
	 * 
	 * @param row the row of toText to consider
	 * @param part the part of the row to return
	 * @return a String containing the toText item
	 */
	public static String getToItem(final int row, final int part) {
		return toItems[row][part];
	}
	
	/**
	 * Returns the number of fromText items.
	 * 
	 * @return the number of fromText items
	 */
	public static int getCountFromItems() {
		return fromItems.length;
	}
	
	/**
	 * Returns the number of toItem rows.
	 * 
	 * @return the number of toItem rows
	 */
	public static int getCountToItemRows() {
		return toItems.length;
	}
	
	/**
	 * Returns the number of toItems in the specified row.
	 * 
	 * @param row the row to consider
	 * @return the number of toItems in the specified row
	 */
	public static int getCountToItems(final int row) {
		return toItems[row].length;
	}
	
	/**
	 * Returns the total number of items.
	 * 
	 * @return the total number of items
	 */
	public static int getCountAllToItems() {
		return countToItems;
	}

}
