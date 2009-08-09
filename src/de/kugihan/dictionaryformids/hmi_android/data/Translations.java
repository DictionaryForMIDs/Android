/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.data;

import android.graphics.Color;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.kugihan.dictionaryformids.dataaccess.content.FontStyle;
import de.kugihan.dictionaryformids.dataaccess.content.RGBColour;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_common.content.ContentParser;
import de.kugihan.dictionaryformids.hmi_common.content.StringColourItemText;
import de.kugihan.dictionaryformids.hmi_common.content.StringColourItemTextPart;
import de.kugihan.dictionaryformids.translation.SingleTranslation;
import de.kugihan.dictionaryformids.translation.TextOfLanguage;
import de.kugihan.dictionaryformids.translation.TranslationExecutionCallback;
import de.kugihan.dictionaryformids.translation.TranslationResult;

/**
 * Translations is the class that handles the data for the translation
 * list.
 * 
 */
public class Translations extends BaseAdapter implements
		TranslationExecutionCallback {

	/**
	 * Automatically generated serialization version ID. 
	 */
	private static final long serialVersionUID = 8675472684874783859L;

	/**
	 * The ContentParser to parse used to parse TranslationResutls.
	 */
	private static ContentParser parser = new ContentParser();
	
	/**
	 * The current TranslationResult represented by this adapter.
	 */
	private TranslationResult data = new TranslationResult();

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
		return data.getTranslationAt(position);
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
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			view = inflater.inflate(R.layout.translation_row, null);
		} else {
			view = convertView;
		}
		display(view, data.getTranslationAt(position));
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

	/**
	 * Adds the given translation to the given view.
	 * 
	 * @param view
	 *            the view that receives the translation
	 * @param result
	 *            the translation to be added to the view
	 */
	public final void display(final View view, final SingleTranslation result) {
		addFromLanguageRow(view, result);
		addToLanguageRows(view, result);
	}

	/**
	 * Adds the translated items to the view.
	 * 
	 * @param view
	 *            the view that receives the entries
	 * @param result
	 *            the entries to be added to the view
	 */
	private void addToLanguageRows(final View view,
			final SingleTranslation result) {
		LinearLayout toLanguagesRows = (LinearLayout) view
				.findViewById(R.id.ToLanguageRows);
		toLanguagesRows.removeAllViews();
		for (int i = 0; i < result.getToTexts().size(); i++) {
			LayoutInflater inflater = LayoutInflater.from(toLanguagesRows
					.getContext());
			TextView toLanguageText = (TextView) inflater.inflate(
					R.layout.translation_part, null);
			TextOfLanguage element = (TextOfLanguage) result.getToTexts()
					.elementAt(i);
			addTextToRow(element, toLanguageText);
			toLanguagesRows.addView(toLanguageText);
		}
	}

	/**
	 * Adds the entries from the original language to the view.
	 * 
	 * @param view
	 *            the view that receives the entries
	 * @param result
	 *            the entries to be added to the view
	 */
	private void addFromLanguageRow(final View view,
			final SingleTranslation result) {
		TextView fromLanguageText = (TextView) view
				.findViewById(R.id.FromLanguageText);
		addTextToRow(result.getFromText(), fromLanguageText);
	}

	/**
	 * Extracts all parts of the given item and adds them correctly styled to
	 * the given textView.
	 * 
	 * @param result the parts of this TextOfLanguage are used
	 * @param textView the TextView that receives the parts
	 */
	private void addTextToRow(final TextOfLanguage result,
			final TextView textView) {
		textView.setText("");
		try {
			StringColourItemText text = parser.determineItemsFromContent(
					result, true, true);
			for (int i = 0; i < text.size(); i++) {
				StringColourItemTextPart itemTextPart = text.getItemTextPart(i);
				appendItemToTextView(textView, itemTextPart);
				textView.setTextSize(Preferences.getResultFontSize());
			}
		} catch (DictionaryException e) {
			textView.setText(textView.getContext().getString(
					R.string.msg_parsing_error, e.toString()));
		}
	}

	/**
	 * Appends the given item to the given TextView and applies style
	 * information.
	 * 
	 * @param textView
	 *            the TextView where text and style should be added
	 * @param itemTextPart
	 *            the item that includes text and style information
	 */
	private void appendItemToTextView(final TextView textView,
			final StringColourItemTextPart itemTextPart) {
		appendText(textView, itemTextPart);
		if (!Preferences.getIgnoreDictionaryTextStyles()) {
			setStyle(textView, itemTextPart);
		}
	}

	/**
	 * Appends the given text part to the given view.
	 * 
	 * @param textView
	 *            the view that receives the part
	 * @param itemTextPart
	 *            the text item that is added to the view
	 */
	private void appendText(final TextView textView,
			final StringColourItemTextPart itemTextPart) {
		String textPart = itemTextPart.getText();
		textView.append(textPart);
	}

	/**
	 * Updates the style of the newly added itemTextPart in textView to the
	 * saved style values.
	 * 
	 * @param textView
	 *            the TextView that includes the newly added text and to which
	 *            the style information should be applied
	 * @param itemTextPart
	 *            the newly added text and its style information
	 */
	private void setStyle(final TextView textView,
			final StringColourItemTextPart itemTextPart) {
		final int newStringLength = textView.getText().length();
		final int oldStringLength = newStringLength
				- itemTextPart.getText().length();
		final int textStyle = itemTextPart.getStyle().style;
		final RGBColour textColor = itemTextPart.getColour();
		StyleSpan styleSpan = getStyleSpan(textStyle, textColor);
		Spannable str = (Spannable) textView.getText();
		str.setSpan(styleSpan, oldStringLength, newStringLength,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	/**
	 * Parses the DictionaryMID style and color information and returns a
	 * corresponding Android StyleSpan object.
	 * 
	 * @param textStyle
	 *            the DictionaryMID style information
	 * @param textColor
	 *            the DictionaryMID color information
	 * @return the corresponding Android StyleSpan object
	 */
	private StyleSpan getStyleSpan(final int textStyle,
			final RGBColour textColor) {
		int style = android.graphics.Typeface.NORMAL;
		TextPaint textPaint = new TextPaint();
		switch (textStyle) {
		case FontStyle.bold:
			style = android.graphics.Typeface.BOLD;
			break;

		case FontStyle.italic:
			style = android.graphics.Typeface.ITALIC;
			break;

		case FontStyle.underlined:
			textPaint.setUnderlineText(true);
			break;
			
		default:
			style = android.graphics.Typeface.NORMAL;
			break;
		}
		textPaint.setColor(Color.rgb(textColor.red, textColor.green,
				textColor.blue));
		StyleSpan styleSpan = new StyleSpan(style);
		styleSpan.updateDrawState(textPaint);
		return styleSpan;
	}
}
