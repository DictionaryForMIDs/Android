/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import de.kugihan.dictionaryformids.general.Util;
import de.kugihan.dictionaryformids.hmi_android.R;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * AboutScreen represents an Activity that displays the application's about
 * screen.
 * 
 */
public class HelpScreen extends Activity {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);

		TextView helpDescription = (TextView) findViewById(R.id.HelpDescription);
		helpDescription.setText(getString(R.string.desc_help,
				Util.wildcardAnySeriesOfCharacter,
				Util.wildcardAnySingleCharacter,
				Util.noSearchSubExpressionCharacter,
				Util.noSearchSubExpressionCharacter));

		TextView helpWebsite = (TextView) findViewById(R.id.HelpWebsite);
		helpWebsite.setText(getString(R.string.desc_see_website,
				getString(R.string.attribute_url)));
	}

}
