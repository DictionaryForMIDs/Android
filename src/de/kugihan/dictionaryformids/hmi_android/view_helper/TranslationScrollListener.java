package de.kugihan.dictionaryformids.hmi_android.view_helper;

import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CheckBox;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_android.data.StarredWordsProvider;
import de.kugihan.dictionaryformids.translation.SingleTranslationExtension;

/**
 * ScollListener that lazy-loads starred state of words from the database.
 *
 */
public class TranslationScrollListener implements OnScrollListener {

	private String dictionaryIdentifier = null;

	/**
	 * Instantiates a new ScrollListener.
	 * 
	 * @param dictionaryIdentifier
	 *            the identifier of the dictionary who's items are currently
	 *            displayed.
	 */
	public TranslationScrollListener(final String dictionaryIdentifier) {
		setDictionaryIdentifier(dictionaryIdentifier);
	}

	public void setDictionaryIdentifier(String dictionaryIdentifier) {
		this.dictionaryIdentifier = dictionaryIdentifier;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScrollStateChanged(AbsListView listView, int scrollState) {
		if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
			return;
		}
		updateView(listView);
	}

	/**
	 * Updates all visible item's starred state.
	 *
	 * @param listView
	 *            the list view to update
	 */
	private void updateView(AbsListView listView) {
		if (!Preferences.getIsStarredWordsEnabled()) {
			// no need to update if starring is disabled
			return;
		}
		if (dictionaryIdentifier == null) {
			return;
		}
		final int first = listView.getFirstVisiblePosition();
		final int count = listView.getChildCount();
		for (int i = 0; i < count; i++) {
			final View item = listView.getChildAt(i);
			if (item.getTag() != null) {
				// item has already been updated
				continue;
			}
			final SingleTranslationExtension translation = (SingleTranslationExtension) listView
					.getAdapter().getItem(first + i);
			final Long itemId = StarredWordsProvider.getItemId(listView.getContext().getContentResolver(),
					translation, dictionaryIdentifier);
			final CheckBox checkBox = (CheckBox) item.findViewById(R.id.checkBoxStar);
			if (itemId == null) {
				checkBox.setChecked(false);
			} else {
				checkBox.setChecked(true);
			}
			// mark item as updated
			item.setTag(new Object());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
			int totalItemCount) {
		updateView(view);
	}
}
