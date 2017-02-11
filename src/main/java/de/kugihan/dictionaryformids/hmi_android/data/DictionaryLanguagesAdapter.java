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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.kugihan.dictionaryformids.dataaccess.LanguageDefinition;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_android.view_helper.LocalizationHelper;

/**
 * DictionaryLanguagesAdapter is the class that handles the data for the language
 * spinner.
 * 
 */
public class DictionaryLanguagesAdapter extends BaseAdapter {

	private static class ViewHolder {
		TextView loadDictionaryTextView;
		LinearLayout languageDirectionLayout;
		CheckBox languagePairCheckBox;
		TextView languageFromTextView;
		TextView languageToTextView;
		TextView directionIndicator;
	}

	/**
	 * The dictionary represented by this adapter.
	 */
	private final Dictionary dictionary;

	/**
	 * The available languages of the current dictionary.
	 */
	private final LanguageDefinition[] data;

	/**
	 * A look-up table where indices[i][0] and indices[i][1] save the indices of
	 * opposite language pairs of data.
	 */
	private final int[][] indices;

	/**
	 * Creates a new instance representing the given data.
	 * 
	 * @param dictionary the dictionary to display
	 */
	public DictionaryLanguagesAdapter(final Dictionary dictionary) {
		this.dictionary = dictionary;
		this.data = dictionary.getFile().supportedLanguages;
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
		final View view;
		final ViewHolder viewHolder;
		if (convertView == null) {
			final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			view = inflater.inflate(R.layout.languages, null);
			viewHolder = new ViewHolder();
			viewHolder.languageDirectionLayout = (LinearLayout) view.findViewById(R.id.LanguageDirectionLayout);
			viewHolder.loadDictionaryTextView = (TextView) view.findViewById(R.id.LoadDictionary);
			viewHolder.languagePairCheckBox = (CheckBox) view.findViewById(R.id.LanguagePairCheckBox);
			viewHolder.languageFromTextView = (TextView) view.findViewById(R.id.languageFrom);
			viewHolder.directionIndicator = (TextView) view.findViewById(R.id.directionIndicator);
			viewHolder.languageToTextView = (TextView) view.findViewById(R.id.languageTo);
			view.setTag(viewHolder);

		} else {
			view = convertView;
			viewHolder = (ViewHolder) view.getTag();
		}
		if (position == indices.length) {
			viewHolder.loadDictionaryTextView.setText(R.string.title_load_dictionary);
			viewHolder.loadDictionaryTextView.setVisibility(View.VISIBLE);
			viewHolder.languageDirectionLayout.setVisibility(View.GONE);
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

			final CheckBox checkBox = viewHolder.languagePairCheckBox;
			checkBox.setOnCheckedChangeListener(null);
			checkBox.setChecked(dictionary.isPairSelected(indices[position][0], indices[position][1]));
			checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
					int fromLanguage = indices[position][0];
					int toLanguage = indices[position][1];
					dictionary.setPairSelection(fromLanguage, toLanguage, checked);
				}
			});
			viewHolder.languageFromTextView.setText(parts[0]);
			viewHolder.directionIndicator.setText(parts[1]);
			viewHolder.languageToTextView.setText(parts[2]);
			viewHolder.loadDictionaryTextView.setVisibility(View.GONE);
			viewHolder.languageDirectionLayout.setVisibility(View.VISIBLE);
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
			view.findViewById(R.id.LanguageDirectionLayout)
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
			view.findViewById(R.id.LoadDictionary)
					.setVisibility(View.GONE);
			view.findViewById(R.id.LanguageDirectionLayout)
					.setVisibility(View.VISIBLE);
		}
		return view;
	}
}
