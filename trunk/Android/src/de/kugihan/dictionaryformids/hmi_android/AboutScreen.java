/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * AboutScreen represents an Activity that displays the application's about
 * screen.
 * 
 */
public class AboutScreen extends Activity {
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);

		final TextView dictionaryView = (TextView) findViewById(R.id.Dictonary);
		dictionaryView.setText(DictionaryDataFile.infoText);

		String version;
		int versionCode;
		try {
			final PackageInfo packageInfo = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			version = packageInfo.versionName;
			versionCode = packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			version = getString(R.string.not_found);
			versionCode = -1;
		}
		final TextView versionView = (TextView) findViewById(R.id.Version);
		final String versionString = getString(R.string.title_version, version,
				versionCode);
		versionView.setText(versionString);

		final TextView dictionary = (TextView) findViewById(R.id.Dictonary);
		dictionary.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View view) {
				setResult(RESULT_OK);
				finish();
			}
		});

	}

}
