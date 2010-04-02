/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import java.util.Locale;

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
		final String localization = Locale.getDefault().toString();
		final String versionStringCode = getString(R.string.format_version, version,
				versionCode, localization);
		final String versionText = getString(R.string.title_version, versionStringCode);
		final TextView versionView = (TextView) findViewById(R.id.Version);
		versionView.setText(versionText);

		final TextView dictionary = (TextView) findViewById(R.id.Dictonary);
		dictionary.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View view) {
				setResult(RESULT_OK);
				finish();
			}
		});
		
		final String translator = getString(R.string.title_user_interface_translator);
		final boolean isTranslatorGiven = translator.length() > 0;
		if (!isTranslatorGiven) {
			final TextView translatorTitle = (TextView) findViewById(R.id.TranslatorTitle);
			final TextView translatorView = (TextView) findViewById(R.id.Translator);
			translatorView.setVisibility(View.GONE);
			translatorTitle.setVisibility(View.GONE);
		}

	}

}
