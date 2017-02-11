/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.service;

/**
 * Interface defining the communication between the
 * download-installation-service and the GUI.
 * 
 */
public interface ServiceUpdateListener {

	/**
	 * Handles progress updates.
	 * 
	 * @param task
	 *            the current task
	 * @param percentage
	 *            the completion percentage of the current task
	 */
	void onProgressUpdate(final int task, final int percentage);

	/**
	 * Handles a successful installation of a dictionary.
	 * 
	 * @param path
	 *            the path of the installed dictionary
	 */
	void onFinished(final String path);

	/**
	 * Handles exceptions while installing a dictionary.
	 * 
	 * @param exception the exception that occurred
	 */
	void onExitWithException(final Exception exception);

}
