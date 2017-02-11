/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

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
		DictionaryForMIDs.setApplicationTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);

		final TextView dictionaryTitle = (TextView) findViewById(R.id.DictionaryTitle);
		final TextView dictionaryView = (TextView) findViewById(R.id.Dictonary);

		if (getIntent() != null && getIntent().hasExtra(DictionaryForMIDs.BUNDLE_DICTIONARY_ABOUT_TEXT)) {
			dictionaryView.setText(getIntent().getStringExtra(DictionaryForMIDs.BUNDLE_DICTIONARY_ABOUT_TEXT));
			dictionaryTitle.setVisibility(View.VISIBLE);
			dictionaryView.setVisibility(View.VISIBLE);
		} else {
			dictionaryTitle.setVisibility(View.GONE);
			dictionaryView.setVisibility(View.GONE);
		}

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

		final String translator = getString(R.string.title_user_interface_translator);
		final boolean isTranslatorGiven = translator.length() > 0 && !"USER_INTERFACE_TRANSLATOR_NAME".equals(translator);
		if (!isTranslatorGiven) {
			final TextView translatorView = (TextView) findViewById(R.id.Translator);
			translatorView.setVisibility(View.GONE);
		}

	}

}
