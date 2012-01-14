/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 *
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.data;

import java.util.Observable;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Filter;
import android.widget.Filterable;
import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.hmi_android.DictionaryForMIDs;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_android.view_helper.SingleTranslationViewHelper;
import de.kugihan.dictionaryformids.translation.SingleTranslationExtension;
import de.kugihan.dictionaryformids.translation.TranslationExecution;
import de.kugihan.dictionaryformids.translation.TranslationExecutionCallback;
import de.kugihan.dictionaryformids.translation.TranslationParameters;
import de.kugihan.dictionaryformids.translation.TranslationResult;

/**
 * TranslationsAdapter handles the data for the translation list.
 *
 */
public class TranslationsAdapter extends BaseAdapter implements Filterable {

	/**
	 * The current TranslationResult represented by this adapter.
	 */
	private TranslationResult data = new TranslationResult();

	/**
	 * Filter used to search the dictionary.
	 */
	private AutoCompleteFilter filter = null;

	/**
	 * True if the filter should keep waiting for the dictionary search result.
	 */
	private boolean shouldFilterWait = true;

	/**
	 * Observable used to publish changes to the state of the filter.
	 */
	private final FilterObservable filterStateObservable = new FilterObservable();

	/**
	 * Parameters that are used by filter.
	 */
	private TranslationParameters parameters = null;

	public TranslationsAdapter(final TranslationParameters parameters) {
		setTranslationParameters(parameters);
	}

	/**
	 * Sets the parameter used for the next translation.
	 *
	 * @param parameters
	 *            the parameters used for the next translation
	 */
	public void setTranslationParameters(final TranslationParameters parameters) {
		this.parameters = parameters;
	}

	/**
	 * Cancels any active filtering operation by interrupting the active
	 * translation thread.
	 */
	public void cancelActiveFilter() {
		TranslationExecution.cancelLastTranslation();
		synchronized (getFilter()) {
			shouldFilterWait = false;
			// manually notify the thread as a cancelled translation thread does
			// not receive a callback
			getFilter().notifyAll();
		}
	}

	/**
	 * Gets the last translation result or an empty translation result.
	 *
	 * @return the last translation result or an empty translation result
	 */
	public TranslationResult getData() {
		return data;
	}

	/**
	 * Clears the translation results.
	 */
	public void clearData() {
		data = new TranslationResult();
		notifyDataSetInvalidated();
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
	public Filter getFilter() {
		if (filter == null) {
			filter = new AutoCompleteFilter(filterStateObservable);
		}
		return filter;
	}

	/**
	 * Returns if there currently is an active filtering thread.
	 *
	 * @return true if there is an active filtering thread
	 */
	public boolean isFilterActive() {
		return filter != null && filter.isActive;
	}

	/**
	 * Returns the handle to the filter state observable.
	 *
	 * @return the handle to the filter state observable
	 */
	public Observable getFilterStateObservable() {
		return filterStateObservable;
	}

	private static class FilterObservable extends Observable {
		@Override
		protected void setChanged() {
			super.setChanged();
		}
	}

	private class AutoCompleteFilter extends Filter implements TranslationExecutionCallback {

		/**
		 * Saves if there currently is an active filter thread.
		 */
		private boolean isActive = false;

		/**
		 * Observable that receives state changes.
		 */
		private final FilterObservable stateChangeObservable;

		/**
		 * Class variable to share result of translation between the synchronous
		 * callback to newTranslationResult() and performFiltering().
		 */
		private TranslationResult translationResult = new TranslationResult();

		/**
		 * Creates a new instance of the filter providing an observable.
		 *
		 * @param stateObservable
		 *            the observable used for publishing state changes to
		 *            isActive
		 */
		public AutoCompleteFilter(final FilterObservable stateObservable) {
			if (stateObservable == null) {
				throw new NullPointerException();
			}
			this.stateChangeObservable = stateObservable;
		}

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {

			synchronized (this) {
				shouldFilterWait = true;
			}

			setState(true);

			final FilterResults result = new FilterResults();
			result.values = new TranslationResult();
			result.count = 0;

			if (parameters == null) {
				return result;
			}
			if (DictionaryDataFile.numberOfAvailableLanguages == 0) {
				return result;
			}

			final String searchString = constraint.toString().trim();
			final StringBuffer searchWord = new StringBuffer(searchString);
			if (searchWord.length() == 0) {
				return result;
			}

			if (!DictionaryForMIDs.hasSearchModifiers(searchWord)) {
				DictionaryForMIDs.makeWordMatchBeginning(searchWord);
			}

			parameters.executeInBackground = true;
			parameters.toBeTranslatedWordText = searchWord.toString();
			TranslationExecution.setTranslationExecutionCallback(this);

			try {
				TranslationExecution.executeTranslation(parameters);
			} catch (DictionaryException e) {
				return result;
			}

			// wait for background thread to finish translation
			synchronized (this) {
				try {
					while (shouldFilterWait) {
						wait();
					}
				} catch (InterruptedException e) {
					// return null to indicate an interruption
					return null;
				}
			}

			// callback to newTranslationResults changed translationResult

			result.values = translationResult;
			result.count = translationResult.numberOfFoundTranslations();

			return result;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			setState(false);

			// null indicates translation was interrupted
			if (results == null) {
				// if interrupted do nothing
				return;
			}

			if (results.values == null) {
				data = new TranslationResult();
				notifyDataSetInvalidated();
			} else {
				data = (TranslationResult) results.values;
				notifyDataSetChanged();
			}
		}

		@Override
		public void deletePreviousTranslationResult() {
			// ignore this event
			// as we only update the view when the new result is available
		}

		@Override
		public void newTranslationResult(TranslationResult resultOfTranslation) {
			translationResult = resultOfTranslation;
			synchronized (this) {
				shouldFilterWait = false;
				notifyAll();
			}
		}

		private void setState(boolean state) {
			isActive = state;
			stateChangeObservable.setChanged();
			stateChangeObservable.notifyObservers(isActive);
		}
	}
}
