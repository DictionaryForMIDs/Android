package de.kugihan.dictionaryformids.hmi_android.data;

import android.view.View;
import android.widget.Filter;
import android.widget.Filterable;
import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.general.Util;
import de.kugihan.dictionaryformids.hmi_android.DictionaryForMIDs;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.translation.TranslationExecution;
import de.kugihan.dictionaryformids.translation.TranslationExecutionCallback;
import de.kugihan.dictionaryformids.translation.TranslationParameters;
import de.kugihan.dictionaryformids.translation.TranslationResult;

public class AutoSearchTranslations extends Translations implements Filterable {
	
	private DictionaryForMIDs activity;
	
	private Filter filter = null;
	
	public AutoSearchTranslations(DictionaryForMIDs activity) {
		setActivity(activity);
	}
	
	public void setActivity(DictionaryForMIDs activity) {
		this.activity = activity;
	}

	@Override
	public Filter getFilter() {
		if (filter == null) {
			filter = new AutoCompleteFilter();
		}
		return filter;
	}
	
	public TranslationResult getData() {
		return data;
	}
	
	private class AutoCompleteFilter extends Filter implements TranslationExecutionCallback {
		
		private TranslationResult translationResult = new TranslationResult();

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			
			if (Preferences.getSearchAsYouType()) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						activity.findViewById(R.id.ProgressBarSearchAsYouType).setVisibility(
								View.VISIBLE);
					}
				});
			}
			
			FilterResults result = new FilterResults();
			
			result.values = new TranslationResult();
			result.count = 0;
			
			final String searchString = constraint.toString().trim();
			StringBuffer searchWord = new StringBuffer(searchString);
			if (searchWord.length() == 0) {
				return result;
			}
			if (DictionaryDataFile.numberOfAvailableLanguages == 0) {
				return result;
			}

			if (!DictionaryForMIDs.hasSearchModifiers(searchWord)) {
				DictionaryForMIDs.makeWordMatchBeginning(searchWord);
			}

			Util util = Util.getUtil();
			int numberOfAvailableLanguages;
			try {
				numberOfAvailableLanguages = util
						.getDictionaryPropertyInt(DictionaryForMIDs.DICTIONARY_PROPERTY_NUMBER_OF_AVAILABLE_LANGUAGES);
			} catch (DictionaryException e) {
				return result;
			}
			TranslationParameters translationParametersObj = activity.getTranslationParameters(
					searchWord.toString(), numberOfAvailableLanguages, false);
			TranslationExecution.setTranslationExecutionCallback(this);

			try {
				TranslationExecution.executeTranslation(translationParametersObj);
			} catch (DictionaryException e) {
				return result;
			}
			
			// callback changes translationResult
			
			result.values = translationResult;
			result.count = translationResult.numberOfFoundTranslations();
			
			return result;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			if (results.values == null) {
				data = new TranslationResult();
			} else {
				data = (TranslationResult) results.values;
			}
			notifyDataSetChanged();
			activity.findViewById(R.id.ProgressBarSearchAsYouType).setVisibility(View.GONE);
		}

		@Override
		public void deletePreviousTranslationResult() {
		}

		@Override
		public void newTranslationResult(TranslationResult resultOfTranslation) {
			translationResult = resultOfTranslation;
		}
	}

}
