/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.data;

import android.content.Intent;

/**
 * This interface is used to propagate the result of an Activity back to the
 * originating Activity where Android does not provide such methods.
 */
public interface ResultProvider {

	/**
	 * Specifies the result code to propagate back to the originating activity.
	 *  
	 * @return the result code to propagate back to the originating activity
	 * @see android.app.Activity.setResult
	 */
	int getResultCode();
	
	/**
	 * Specifies the data to propagate back to the originating activity.
	 * 
	 * @return the data to propagate back to the originating activity
	 */
	Intent getReturnData();

}
