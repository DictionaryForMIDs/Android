/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.data;

import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import de.kugihan.dictionaryformids.hmi_android.DictionaryForMIDs;
import de.kugihan.dictionaryformids.hmi_android.R;

/**
 * InstanceState is a class that represents and handles temporary
 * configurations, e.g. during orientation changes. changes.
 * 
 */
public class InstanceState {

	/**
	 * The applications current Translations.
	 */
	private Translations translationListAdapter;
	
	/**
	 * The index of the currently selected language.
	 */
	private int selectedLanguage;
	
	/**
	 * The flag stating if the search options are currently visible.
	 */
	private int searchOptionsVisibility;
	
	/**
	 * The flag stating if the heading is currently visible.
	 */
	private int headingVisibility;
	
	/**
	 * The current status message.
	 */
	private CharSequence statusMessage;

	/**
	 * Creates a new configuration instance from the given class, saving all
	 * temporary configurations.
	 * 
	 * @param instance the class to represent
	 */
	public InstanceState(final DictionaryForMIDs instance) {
		translationListAdapter = instance.getTranslationListAdapter();
		searchOptionsVisibility = ((LinearLayout) instance
				.findViewById(R.id.selectLanguagesLayout)).getVisibility();
		selectedLanguage = ((Spinner) instance
				.findViewById(R.id.selectLanguages)).getSelectedItemPosition();
		statusMessage = ((TextView) instance.findViewById(R.id.output))
				.getText();
		headingVisibility = ((LinearLayout) instance
				.findViewById(R.id.HeadingLayout)).getVisibility();
	}
	
	/**
	 * Loads the saved configuration back into the given class.
	 * 
	 * @param instance the class to receive the configuration
	 */
	public final void loadConfiguration(final DictionaryForMIDs instance) {
		instance.setTranslationListAdapter(translationListAdapter);
		((Spinner) instance.findViewById(R.id.selectLanguages))
				.setSelection(selectedLanguage);
		((TextView) instance.findViewById(R.id.output)).setText(statusMessage);
		((LinearLayout) instance.findViewById(R.id.HeadingLayout))
				.setVisibility(headingVisibility);
		((LinearLayout) instance.findViewById(R.id.selectLanguagesLayout))
				.setVisibility(searchOptionsVisibility);
	}
}
