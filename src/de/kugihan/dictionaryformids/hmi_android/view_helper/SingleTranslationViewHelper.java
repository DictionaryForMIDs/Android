package de.kugihan.dictionaryformids.hmi_android.view_helper;

import android.graphics.Color;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.kugihan.dictionaryformids.dataaccess.content.FontStyle;
import de.kugihan.dictionaryformids.dataaccess.content.RGBColour;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.hmi_android.DictionaryForMIDs;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_common.content.StringColourItemText;
import de.kugihan.dictionaryformids.hmi_common.content.StringColourItemTextPart;
import de.kugihan.dictionaryformids.translation.SingleTranslationExtension;

/**
 * Helper for handling SingleTranslation objects.
 */
public class SingleTranslationViewHelper {

	private SingleTranslationViewHelper() {
		// prevent instances of this helper to be created
	}

	/**
	 * Adds the given translation to the given view.
	 *
	 * @param view
	 *            the view that receives the translation
	 * @param result
	 *            the translation to be added to the view
	 */
	public static final void display(final View view, final SingleTranslationExtension result) {
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
	private static void addToLanguageRows(final View view, final SingleTranslationExtension result) {
		final LinearLayout toLanguagesRows = (LinearLayout) view.findViewById(R.id.ToLanguageRows);
		toLanguagesRows.removeAllViews();
		try {
			for (StringColourItemText text : result.getToTextsAsColourItemTexts()) {
				final LayoutInflater inflater = LayoutInflater.from(toLanguagesRows.getContext());
				final TextView toLanguageText = (TextView) inflater.inflate(
						R.layout.translation_part, null);
				addTextToRow(text, toLanguageText);
				toLanguagesRows.addView(toLanguageText);
			}
		} catch (DictionaryException e) {
			Log.d(DictionaryForMIDs.LOG_TAG, "addToLanguageRows", e);
			final LayoutInflater inflater = LayoutInflater.from(toLanguagesRows.getContext());
			final TextView toLanguageText = (TextView) inflater.inflate(R.layout.translation_part,
					null);
			toLanguageText.setText(toLanguageText.getContext().getString(
					R.string.msg_parsing_error, e.toString()));
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
	private static void addFromLanguageRow(final View view, final SingleTranslationExtension result) {
		final TextView fromLanguageText = (TextView) view.findViewById(R.id.FromLanguageText);
		try {
			addTextToRow(result.getFromTextAsColourItemText(), fromLanguageText);
		} catch (DictionaryException e) {
			Log.d(DictionaryForMIDs.LOG_TAG, "addFromLanguageRow", e);
			fromLanguageText.setText(fromLanguageText.getContext().getString(
					R.string.msg_parsing_error, e.toString()));
		}
	}

	/**
	 * Extracts all parts of the given item and adds them correctly styled to
	 * the given textView.
	 *
	 * @param result
	 *            the parts of this TextOfLanguage are used
	 * @param textView
	 *            the TextView that receives the parts
	 */
	private static void addTextToRow(final StringColourItemText text, final TextView textView) {
		textView.setText("");
		for (int i = 0; i < text.size(); i++) {
			StringColourItemTextPart itemTextPart = text.getItemTextPart(i);
			appendItemToTextView(textView, itemTextPart);
			textView.setTextSize(Preferences.getResultFontSize());
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
	private static void appendItemToTextView(final TextView textView,
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
	private static void appendText(final TextView textView,
			final StringColourItemTextPart itemTextPart) {
		final String textPart = itemTextPart.getText();
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
	private static void setStyle(final TextView textView,
			final StringColourItemTextPart itemTextPart) {
		final int textStyle = itemTextPart.getStyle().style;
		final RGBColour textColor = itemTextPart.getColour();
		final Spannable str = (Spannable) textView.getText();
		str.setSpan(getStyleSpan(textStyle), 0, str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		str.setSpan(getStyleSpan(textColor), 0, str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		textView.setText(str);
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
	private static StyleSpan getStyleSpan(final int textStyle) {
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
		final StyleSpan styleSpan = new StyleSpan(style);
		styleSpan.updateDrawState(textPaint);
		return styleSpan;
	}

	private static CharacterStyle getStyleSpan(final RGBColour textColor) {
		return new ForegroundColorSpan(Color.rgb(textColor.red, textColor.green, textColor.blue));
	}

}
