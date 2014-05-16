package de.kugihan.dictionaryformids.hmi_android.data;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_android.view_helper.SingleTranslationViewHelper;

/**
 * Adapter to display starred words from the database in a list view.
 * 
 */
public class StarredWordsAdapter extends CursorAdapter {

	/**
	 * {@inheritDoc}
	 */
	public StarredWordsAdapter(Context context, Cursor cursor) {
		super(context, cursor);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		final TranslationsAdapter.ViewHolder holder = (TranslationsAdapter.ViewHolder) view.getTag();
		SingleTranslationViewHelper.display(holder, StarredWordsProvider.getTranslation(cursor));
		handleCheckedState(context, cursor, holder);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		final View view = inflater.inflate(R.layout.translation_row, null);
		final TranslationsAdapter.ViewHolder holder = new TranslationsAdapter.ViewHolder();
		holder.fromLanguageText = (TextView) view.findViewById(R.id.FromLanguageText);
		holder.toLanguagesRows = (LinearLayout) view.findViewById(R.id.ToLanguageRows);
		holder.checkBoxStar = (CheckBox) view.findViewById(R.id.checkBoxStar);
		view.setTag(holder);

		SingleTranslationViewHelper.display(holder, StarredWordsProvider.getTranslation(cursor));
		handleCheckedState(context, cursor, holder);
		return view;
	}

	/**
	 * Attach a listener to handle the changes to the checked state for new and
	 * binded views.
	 *
	 * @param cursor
	 *            the cursor pointing to the current item
	 * @param holder
	 *            the holder that includes the checkbox
	 */
	private void handleCheckedState(final Context context, final Cursor cursor, final TranslationsAdapter.ViewHolder holder) {
		final long id = StarredWordsProvider.getItemId(cursor);
		CheckBox star = holder.checkBoxStar;
		star.setOnCheckedChangeListener(null);
		star.setChecked(true);
		star.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						context.getContentResolver().delete(
								ContentUris.withAppendedId(StarredWordsProvider.CONTENT_URI, id), null,
								null);
						return null;
					}
				};

				task.execute();
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CharSequence convertToString(Cursor cursor) {
		return StarredWordsProvider.getTranslation(cursor).getFromText().getText();
	}
}
