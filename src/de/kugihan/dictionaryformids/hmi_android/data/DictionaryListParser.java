/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.data;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * DictionaryListParser is a helper class that parses the response of the
 * server.
 * 
 */
public final class DictionaryListParser {

	/**
	 * Specifies if client software has to be updated.
	 */
	private boolean forceUpdate;
	
	/**
	 * Specifies if the client software can be updated.
	 */
	private boolean mayUpdate;
	
	/**
	 * The message sent by the server.
	 */
	private String serverMessage;

	/**
	 * List of all available dictionaries.
	 */
	private final ArrayList<DownloadDictionaryItem> dictionaries
		= new ArrayList<DownloadDictionaryItem>();

	/**
	 * Loads all dictionaries from the specified JSON array.
	 * 
	 * @param array
	 *            the array of dictionaries to load
	 * @throws JSONException
	 *             if an exception occurs while parsing JSON data
	 */
	private void parseDictionaryList(final JSONArray array)
			throws JSONException {
		for (int i = 0; i < array.length(); i++) {
			final JSONObject dictionary = array.getJSONObject(i);
			parseDictionary(dictionary);
		}
	}

	/**
	 * Tries to load a dictionary from the given JSON object.
	 * 
	 * @param dictionary
	 *            the object from which the dictionary is loaded
	 * @throws JSONException
	 *             if an exception occurs while parsing JSON data
	 * @return true if the dictionary has been added to the list of dictionaries
	 */
	private boolean parseDictionary(final JSONObject dictionary)
			throws JSONException {
		final int id = dictionary.getInt("id");
		final String url = dictionary.getString("zipUrl");
		final String name = dictionary.getString("name");
		final String fileName = dictionary.getString("fileName");
		long size;
		try {
			size = dictionary.getLong("zipSize");
		} catch (JSONException e) {
			size = 0;
		}
		if (!isDictionaryDataValid(url, name, fileName, size)) {
			return false;
		}
		final DownloadDictionaryItem item = new DownloadDictionaryItem(id,
				name, url, fileName, size);
		return dictionaries.add(item);
	}

	/**
	 * Make sure the data does not include malicious or obviously wrong values.
	 * 
	 * @param url
	 *            the url of the dictionary
	 * @param name
	 *            the name of the dictionary
	 * @param fileName
	 *            the file name of the dictionary
	 * @param size
	 *            the size of the dictionary
	 * @return true if the data is valid
	 */
	private boolean isDictionaryDataValid(final String url, final String name,
			final String fileName, final long size) {
		final boolean isUrlProtocolSupported = !url.startsWith("http://")
				&& !url.startsWith("https://");
		if (isUrlProtocolSupported) {
			return false;
		}
		final boolean isNameInvisible = name.trim().length() == 0;
		if (isNameInvisible) {
			return false;
		}
		final boolean isFileNameReferencingChildDirectory = fileName
				.contains("/")
				|| fileName.contains("..");
		if (isFileNameReferencingChildDirectory) {
			return false;
		}
		if (fileName.length() == 0) {
			return false;
		}
		if (size < 0) {
			return false;
		}
		return true;
	}

	/**
	 * Parses the list of dictionaries.
	 * 
	 * @param stringResult
	 *            the server response
	 * @throws JSONException
	 *             if an exception while parsing JSON data occured
	 */
	public DictionaryListParser(final StringBuilder stringResult)
			throws JSONException {
		final JSONObject data = new JSONObject(stringResult.toString());
		parseAttributes(data);
		if (forceUpdate) {
			return;
		}
		final JSONArray array = data.getJSONArray("list");
		parseDictionaryList(array);
	}

	/**
	 * Parses the attributes received from the download server.
	 * 
	 * @param data
	 *            the object that includes the attributes
	 * @throws JSONException
	 *             if an exception occurs while parsing JSON data
	 */
	private void parseAttributes(final JSONObject data) throws JSONException {
		forceUpdate = data.getBoolean("forceUpdate");
		mayUpdate = data.getBoolean("mayUpdate");
		serverMessage = data.getString("message");
	}
	
	/**
	 * Returns the server message.
	 * 
	 * @return the server message
	 */
	public String getServerMessage() {
		return serverMessage;
	}
	
	/**
	 * Specifies if a software update is available.
	 * 
	 * @return true if an update is available
	 */
	public boolean mayUpdate() {
		return mayUpdate;
	}
	
	/**
	 * Specifies if the client software needs to be updated.
	 * 
	 * @return true if the client software has to be updated
	 */
	public boolean forceUpdate() {
		return forceUpdate;
	}
	
	/**
	 * Returns the list of dictionaries available for download.
	 * 
	 * @return the list of dictionaries available for download
	 */
	public ArrayList<DownloadDictionaryItem> getDictionaries() {
		return dictionaries;
	}
	
	/**
	 * Returns the dictionary which has the given id.
	 * 
	 * @param id
	 *            of the dictionary to return
	 * @return the dictionary or null
	 */
	public DownloadDictionaryItem getDictionary(int id) {
		for (DownloadDictionaryItem item : dictionaries) {
			if (item.getId() == id) {
				return item;
			}
		}
		return null;
	}

}
