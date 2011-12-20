/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 *
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.data;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_android.view_helper.SingleTranslationViewHelper;
import de.kugihan.dictionaryformids.translation.SingleTranslationExtension;
import de.kugihan.dictionaryformids.translation.TranslationExecutionCallback;
import de.kugihan.dictionaryformids.translation.TranslationResult;

/**
 * TranslationsAdapter handles the data for the translation list.
 *
 */
public class TranslationsAdapter extends BaseAdapter implements
		TranslationExecutionCallback {

	/**
	 * The current TranslationResult represented by this adapter.
	 */
	private TranslationResult data = new TranslationResult();

	public TranslationsAdapter() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final int getCount() {
		return data.numberOfFoundTranslations();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Object getItem(final int position) {
		return new SingleTranslationExtension(data.getTranslationAt(position));
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
		View view = null;
		if (convertView == null) {
			final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			view = inflater.inflate(R.layout.translation_row, null);
		} else {
			view = convertView;
		}

		view.setTag(null);

		// set text
		final SingleTranslationExtension translation = (SingleTranslationExtension) getItem(position);
		SingleTranslationViewHelper.display(view, translation);

		final CheckBox star = (CheckBox) view.findViewById(R.id.checkBoxStar);
		// remove starred words feature if disabled
		if (!Preferences.getIsStarredWordsEnabled()) {
			star.setVisibility(View.GONE);
			return view;
		}

		// enable starred words feature
		star.setVisibility(View.VISIBLE);
		// handle database insertions
		final OnCheckedChangeListener listener = new OnCheckedChangeListener() {
			private Uri item = null;

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					item = parent
							.getContext()
							.getContentResolver()
							.insert(StarredWordsProvider.CONTENT_URI,
									StarredWordsProvider.getContentValues(
											DictionaryDataFile.dictionaryAbbreviation, translation));
				} else {
					parent.getContext().getContentResolver().delete(item, null, null);
					item = null;
				}
			}
		};
		// remove potentially existing listener as views get recycled
		star.setOnCheckedChangeListener(null);
		star.setChecked(false);
		star.setOnCheckedChangeListener(listener);

		return view;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void deletePreviousTranslationResult() {
		data = new TranslationResult();
		notifyDataSetChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void newTranslationResult(
			final TranslationResult resultOfTranslation) {
		data = resultOfTranslation;
		notifyDataSetChanged();
	}
}
