/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.data;

import android.os.Handler;
import android.os.Message;
import de.kugihan.dictionaryformids.general.Util;
import de.kugihan.dictionaryformids.hmi_android.DictionaryForMIDs;

/**
 * AndroidUtil extends the class {@link Util} to provide Android-specific
 * behavior.
 * 
 */
public class AndroidUtil extends Util {

	/**
	 * The handler used to communicate with the view.
	 */
	private Handler handler;

	/**
	 * Constructor that connects the utility to the view.
	 * 
	 * @param newHandler the handler that receives dictionary messages
	 */
	public AndroidUtil(final Handler newHandler) {
		setHandler(newHandler);
	}
	
	/**
	 * Updates the current handler.
	 * 
	 * @param newHandler the handler that receives dictionary messages
	 */
	public final void setHandler(final Handler newHandler) {
		handler = newHandler;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void outputMessage(final String message) {
        Message m = new Message();
        m.what = DictionaryForMIDs.THREAD_ERROR_MESSAGE;
        m.obj = message;
		handler.sendMessage(m);
	}

}
