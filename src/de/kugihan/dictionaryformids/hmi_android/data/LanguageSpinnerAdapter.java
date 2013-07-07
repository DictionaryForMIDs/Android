/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.data;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.kugihan.dictionaryformids.dataaccess.LanguageDefinition;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_android.view_helper.LocalizationHelper;

/**
 * LanguageSpinnerAdapter is the class that handles the data for the language
 * spinner.
 * 
 */
public class LanguageSpinnerAdapter extends BaseAdapter {

	/**
	 * The available languages of the current dictionary.
	 */
	private LanguageDefinition[] data;

	/**
	 * A look-up table where indices[i][0] and indices[i][1] save the indices of
	 * opposite language pairs of data.
	 */
	private int[][] indices;

	/**
	 * Creates an empty adapter.
	 */
	public LanguageSpinnerAdapter() {
		this(new LanguageDefinition[0]);
	}
	
	/**
	 * Creates a new instance representing the given data.
	 * 
	 * @param languages the languages to display
	 */
	public LanguageSpinnerAdapter(final LanguageDefinition[] languages) {
		data = languages;
		int searchableLanguages = 0;
		for (LanguageDefinition language : data) {
			if (language.isSearchable) {
				searchableLanguages++;
			}
		}
		
		indices = new int[searchableLanguages * (data.length - 1)][2];
		int counter = 0;
		for (int i = 0; i < data.length; i++) {
			if (!data[i].isSearchable) {
				continue;
			}
			for (int j = 0; j < data.length; j++) {
				if (i == j) {
					continue;
				}
				indices[counter][0] = i;
				indices[counter][1] = j;
				counter++;
			}
		}
	}
	
	/**
	 * Returns the position of the language pair which represents the opposite
	 * of the current language pair.
	 * 
	 * @param position the position of the current selection
	 * @return the position of the opposite language pair
	 */
	public final int getSwappedPosition(final int position) {
		for (int i = 0; i < indices.length; i++) {
			if (indices[position][0] == indices[i][1]
					&& indices[position][1] == indices[i][0]) {
				return i;
			}
		}
		return position;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final int getCount() {
		// +1 to include Load-Dictionary-Command in list
		return indices.length + 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Object getItem(final int position) {
		Object item;
		if (position == indices.length) {
			item = null;
		} else {
			item = indices[position]; 
		}
		return item;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final long getItemId(final int position) {
		return position;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final View getView(final int position, final View convertView,
			final ViewGroup parent) {
		View view;
		if (convertView == null) {
			final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			view = inflater.inflate(R.layout.languages, null);
		} else {
			view = convertView;
		}
		if (position == indices.length) {
			final TextView textView = (TextView) view
					.findViewById(R.id.LoadDictionary);
			textView.setText(R.string.title_load_dictionary);
			textView.setVisibility(View.VISIBLE);
			((LinearLayout) view.findViewById(R.id.LanguageDirectionLayout))
					.setVisibility(View.GONE);
		} else {
			final Resources resources = view.getResources();
			final String languageFromString = getLocalizedLanguage(resources, position, true);
			final String languageToString = getLocalizedLanguage(resources, position, false);
			final String formatString = view.getContext().getString(R.string.title_format_translation_direction, languageFromString, languageToString);
			final String separator = view.getContext().getString(
					R.string.title_format_translation_direction_separator);

			final String parts[] = formatString.split("\\Q" + separator + "\\E");
			if (parts.length != 3) {
				// parts should be LANGUAGE\tARROW\tLANGUAGE
				throw new IllegalArgumentException(
						"R.string.title_format_translation_direction is of wrong format");
			}

			final TextView languageFrom = (TextView) view.findViewById(R.id.languageFrom);
			languageFrom.setText(parts[0]);
			final TextView directionIndicator = (TextView) view.findViewById(R.id.directionIndicator);
			directionIndicator.setText(parts[1]);
			final TextView languageTo = (TextView) view.findViewById(R.id.languageTo);
			languageTo.setText(parts[2]);
			((TextView) view.findViewById(R.id.LoadDictionary))
					.setVisibility(View.GONE);
			((LinearLayout) view.findViewById(R.id.LanguageDirectionLayout))
					.setVisibility(View.VISIBLE);
		}
		return view;
	}

	/**
	 * Returns the localized name of the first or second language at a specific
	 * position.
	 * 
	 * @param position the position of the language in the language array
	 * @param 
	 * @return
	 */
	private String getLocalizedLanguage(final Resources resources,
			final int position, final boolean firstLanguage) {
		final int subPosition = firstLanguage ? 0 : 1;
		final int[] languagePairIndices = indices[position];
		final int languageDefinitionIndex = languagePairIndices[subPosition];
		final String languageDisplayText = data[languageDefinitionIndex].languageDisplayText;
		return LocalizationHelper.getLanguageName(resources, languageDisplayText);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final View getDropDownView(final int position,
			final View convertView,
			final ViewGroup parent) {
		View view;
		if (convertView == null) {
			final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			view = inflater.inflate(R.layout.languages_dropdown, null);
		} else {
			view = convertView;
		}
		if (position == indices.length) {
			final TextView textView = (TextView) view
					.findViewById(R.id.LoadDictionary);
			int loadDictionaryCommand;
			if (indices.length == 0) {
				loadDictionaryCommand = R.string.title_load_dictionary_first;
			} else {
				loadDictionaryCommand = R.string.title_load_dictionary;
			}
			textView.setText(loadDictionaryCommand);
			textView.setVisibility(View.VISIBLE);
			((LinearLayout) view.findViewById(R.id.LanguageDirectionLayout))
					.setVisibility(View.GONE);
		} else {
			final Resources resources = view.getResources();
			final String languageFromString = getLocalizedLanguage(resources, position, true);
			final String languageToString = getLocalizedLanguage(resources, position, false);
			final String formatString = view.getContext().getString(R.string.title_format_translation_direction, languageFromString, languageToString);
			
			final String parts[] = formatString.split("\t");
			if (parts.length != 3) {
				// parts should be LANGUAGE\tARROW\tLANGUAGE
				throw new IllegalArgumentException("R.string.title_format_translation_direction is of wrong format");
			}
			
			final TextView languageFrom = (TextView) view.findViewById(R.id.languageFrom);
			languageFrom.setText(parts[0]);
			final TextView directionIndicator = (TextView) view.findViewById(R.id.directionIndicator);
			directionIndicator.setText(parts[1]);
			final TextView languageTo = (TextView) view.findViewById(R.id.languageTo);
			languageTo.setText(parts[2]);
			((TextView) view.findViewById(R.id.LoadDictionary))
					.setVisibility(View.GONE);
			((LinearLayout) view.findViewById(R.id.LanguageDirectionLayout))
					.setVisibility(View.VISIBLE);
		}
		return view;
	}
}
