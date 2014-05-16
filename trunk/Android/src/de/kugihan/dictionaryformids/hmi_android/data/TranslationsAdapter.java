/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 *
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.data;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.dataaccess.LanguageDefinition;
import de.kugihan.dictionaryformids.dataaccess.content.RGBColour;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_android.view_helper.LocalizationHelper;
import de.kugihan.dictionaryformids.hmi_android.view_helper.SingleTranslationViewHelper;
import de.kugihan.dictionaryformids.translation.SingleTranslation;
import de.kugihan.dictionaryformids.translation.SingleTranslationExtension;
import de.kugihan.dictionaryformids.translation.TranslationParameters;
import de.kugihan.dictionaryformids.translation.TranslationResult;

/**
 * TranslationsAdapter handles the data for the translation list.
 *
 */
public class TranslationsAdapter extends BaseExpandableListAdapter implements Observer {

	static class GroupViewHolder {
		TextView line1;
		TextView line2;
	}

	public static class ViewHolder {
		public TextView fromLanguageText;
		public LinearLayout toLanguagesRows;
		public CheckBox checkBoxStar;
	}

	private final Vector<TranslationResult> translationResults = new Vector<TranslationResult>();

	private final Activity activity;

	public TranslationsAdapter(Activity activity) {
		this.activity = activity;
	}

	private String getString(int resId) {
		return activity.getString(resId);
	}

	private String getString(int resId, Object... formatArgs) {
		return activity.getString(resId, formatArgs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(final Observable observable, final Object data) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (data == null) {
					translationResults.clear();
				} else if (data instanceof TranslationResult) {
					TranslationResult result = (TranslationResult) data;
					addTranslationResult(result);
				} else {
					throw new IllegalArgumentException();
				}
				notifyDataSetChanged();
			}
		});
	}

	/**
	 * Add a new translation result to the sorted collection of results.
	 *
	 * @param result the result to add
	 */
	private void addTranslationResult(TranslationResult result) {
		final int newTranslations = result.numberOfFoundTranslations();
		int i = 0;
		for (; i < translationResults.size(); i++) {
			int dictionaryTranslations = translationResults.elementAt(i).numberOfFoundTranslations();
			if (newTranslations > dictionaryTranslations) {
				break;
			}
		}
		translationResults.add(i, result);
	}

	@Override
	public int getGroupCount() {
		return translationResults.size();
	}

	@Override
	public int getChildrenCount(int i) {
		return translationResults.elementAt(i).numberOfFoundTranslations();
	}

	@Override
	public Object getGroup(int i) {
		return translationResults.elementAt(i);
	}

	@Override
	public Object getChild(int i, int i2) {
		SingleTranslation translation = translationResults.elementAt(i).getTranslationAt(i2);
		DictionaryDataFile dataFile = translationResults.elementAt(i).dictionary;
		return new SingleTranslationExtension(translation, dataFile);
	}

	@Override
	public long getGroupId(int i) {
		return i;
	}

	@Override
	public long getChildId(int i, int i2) {
		return i2;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
		GroupViewHolder holder;
		View result = null;
		if (view == null) {
			final LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
			result = inflater.inflate(android.R.layout.simple_expandable_list_item_2, null);
			holder = new GroupViewHolder();
			holder.line1 = (TextView)result.findViewById(android.R.id.text1);
			holder.line2 = (TextView)result.findViewById(android.R.id.text2);
			result.setTag(holder);
		} else {
			result = view;
			holder = (GroupViewHolder)result.getTag();
		}

		TextView line1 = holder.line1;
		TextView line2 = holder.line2;

		TranslationResult translationResult = (TranslationResult)getGroup(i);
		TranslationParameters translationParameters = translationResult.translationParametersObj;

		LanguageDefinition[] languagesArray = translationResult.dictionary.supportedLanguages;
		String languagesFrom = "";
		String languagesTo = "";
		for (int j = 0; j < languagesArray.length; j++) {
			final LanguageDefinition definition = languagesArray[j];
			final String language = definition.languageDisplayText;
			final String localizedLanguage = LocalizationHelper
					.getLanguageName(activity.getResources(), language);
			if (translationParameters.getInputLanguages()[j]) {
				languagesFrom += localizedLanguage;
				languagesFrom += " ";
			}
			if (translationParameters.getOutputLanguages()[j]) {
				languagesTo += localizedLanguage;
				languagesTo += " ";
			}
		}
		languagesFrom = languagesFrom.trim();
		languagesTo = languagesTo.trim();

		final String formatString = activity.getString(R.string.title_format_translation_direction, languagesFrom, languagesTo);
		line1.setText(formatString);

		if (translationResult.translationBreakOccurred) {
			switch (translationResult.translationBreakReason) {
				case TranslationResult.BreakReasonCancelMaxNrOfHitsReached:
					line2.setText(getString(R.string.results_found_maximum,
							translationResult.numberOfFoundTranslations()));
					break;

				case TranslationResult.BreakReasonCancelReceived:
					line2.setText(getString(R.string.results_found_cancel,
							translationResult.numberOfFoundTranslations()));
					break;

				case TranslationResult.BreakReasonMaxExecutionTimeReached:
					line2.setText(getString(R.string.results_found_timeout,
							translationResult.numberOfFoundTranslations()));
					// TODO: warn about timeout
//					if (Preferences.getLoadArchiveDictionary()
//							&& Preferences.getWarnOnTimeout()) {
//						showDialog(DialogHelper.ID_SUGGEST_DIRECTORY);
//					}
					break;

				default:
					throw new IllegalStateException();
			}
		} else if (translationResult.numberOfFoundTranslations() == 0) {
			line2.setText(R.string.no_results_found);
		} else {
			if (translationResult.numberOfFoundTranslations() == 1) {
				line2.setText(R.string.results_found_one);
			} else {
				line2.setText(getString(R.string.results_found,
						translationResult.numberOfFoundTranslations()));
			}
		}

		// set text
//		final SingleTranslationExtension translation = (SingleTranslationExtension) getGroup(i);
//		SingleTranslationViewHelper.display(result, translation);

		return result;
	}

	@Override
	public View getChildView(int i, int i2, boolean b, View view, final ViewGroup viewGroup) {
		View result = null;
		ViewHolder holder;
		if (view == null) {
			final LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
			result = inflater.inflate(R.layout.translation_row, null);
			holder = new ViewHolder();
			holder.fromLanguageText = (TextView) result.findViewById(R.id.FromLanguageText);
			holder.toLanguagesRows = (LinearLayout) result.findViewById(R.id.ToLanguageRows);
			holder.checkBoxStar = (CheckBox) result.findViewById(R.id.checkBoxStar);
			result.setTag(holder);
		} else {
			result = view;
			holder = (ViewHolder) view.getTag();
		}

		// set text
		final SingleTranslationExtension translation = (SingleTranslationExtension) getChild(i, i2);
		SingleTranslationViewHelper.display(holder, translation);

		final CheckBox star = holder.checkBoxStar;
		// remove starred words feature if disabled
		if (!Preferences.getIsStarredWordsEnabled()) {
			star.setVisibility(View.GONE);
		} else {
			// enable starred words feature
			star.setVisibility(View.VISIBLE);
			// handle database insertions
			final CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
				private Uri item = null;

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						item = viewGroup.getContext().getContentResolver()
								.insert(StarredWordsProvider.CONTENT_URI,
										StarredWordsProvider.getContentValues(translation));
					} else {
						viewGroup.getContext().getContentResolver().delete(item, null, null);
						item = null;
					}
				}
			};
			// remove potentially existing listener as views get recycled
			star.setOnCheckedChangeListener(null);
			star.setChecked(translation.isStarred());
			star.setOnCheckedChangeListener(listener);
		}

		final int color = getDictionaryBackgroundColor(i, viewGroup.getResources());
		result.setBackgroundColor(color);

		return result;
	}

	/**
	 * Returns the background color of the associated dictionary or transparent if dictionary styles should be ignored
	 *
	 * @param i the id of the translation entry
	 * @param resources the resources to load the fallback background color from
	 * @return the associated background color or transparent
	 */
	private int getDictionaryBackgroundColor(int i, Resources resources) {
		if (Preferences.getIgnoreDictionaryTextStyles()) {
			return Color.TRANSPARENT;
		}
		TranslationResult translationResult = translationResults.elementAt(i);
		TranslationParameters translationParameters = translationResult.translationParametersObj;
		final RGBColour rgb = translationParameters.getDictionary().getBackgroundColour();
		if (rgb == null) {
			return resources.getColor(android.R.color.background_light);
		} else {
			return Color.rgb(rgb.red, rgb.green, rgb.blue);
		}
	}

	@Override
	public boolean isChildSelectable(int i, int i2) {
		return false;
	}

	public int getAllChildrenCount() {
		int count = 0;
		for (TranslationResult translationResult : translationResults) {
			count += translationResult.numberOfFoundTranslations();
		}
		return count;
	}

	public Vector<TranslationResult> getTranslationResults() {
		return translationResults;
	}

	public boolean hasData() {
		for (TranslationResult translationResult : translationResults) {
			if (translationResult.numberOfFoundTranslations() > 0) {
				return true;
			}
		}
		return false;
	}

	public void clearData() {
		translationResults.clear();
		notifyDataSetChanged();
	}
}