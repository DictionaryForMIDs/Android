/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 *
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Observable;
import java.util.Observer;

import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.AssetDfMInputStreamAccess;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.DfMInputStreamAccess;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.FileDfMInputStreamAccess;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.NativeZipInputStreamAccess;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_android.thread.LoadDictionaryThread;

public class DictionariesAdapter extends BaseAdapter {

	private static class ViewHolder {
		TextView title;
		CheckBox selectionCheckbox;
		LinearLayout languagePairs;
	}

	private final DictionaryVector dictionaries;

	public DictionariesAdapter(DictionaryVector dictionaries) {
		this.dictionaries = dictionaries;
		dictionaries.addObserver(new Observer() {
			@Override
			public void update(Observable observable, Object o) {
				DictionariesAdapter.this.notifyDataSetChanged();
			}
		});
	}

	@Override
	public Object getItem(int i) {
		return dictionaries.elementAt(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public int getCount() {
		return dictionaries.size();
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public View getView(final int i, View view, final ViewGroup viewGroup) {
		ViewHolder holder;
		if (view == null) {
			final LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
			view = inflater.inflate(R.layout.dictionary, null);
			holder = new ViewHolder();
			holder.title = (TextView)view.findViewById(R.id.title);
			holder.selectionCheckbox = (CheckBox)view.findViewById(R.id.EnableDictionaryCheckbox);
			holder.languagePairs = (LinearLayout)view.findViewById(R.id.LanguagePairs);
			view.setTag(holder);
		} else {
			holder = (ViewHolder)view.getTag();
		}

		TextView title = holder.title;
		LinearLayout listView = holder.languagePairs;

		// Remove existing listener so that changes are ignored
		holder.selectionCheckbox.setOnCheckedChangeListener(null);

		final Dictionary dictionary = (Dictionary) getItem(i);
		if (dictionary.getFile() != null) {
			title.setText(dictionary.getFile().dictionaryAbbreviation);
			holder.selectionCheckbox.setChecked(true);

			DictionaryLanguagesAdapter dictionaryLanguagesAdapter = new DictionaryLanguagesAdapter(dictionary);
			// Re-use existing views
			if (listView.getChildCount() > dictionaryLanguagesAdapter.getCount()-1) {
				listView.removeViews(dictionaryLanguagesAdapter.getCount() - 1, listView.getChildCount() - dictionaryLanguagesAdapter.getCount() - 1);
			}
			for (int j = 0; j < dictionaryLanguagesAdapter.getCount() - 1; j++) {
				View existingView = null;
				if (j < listView.getChildCount()) {
					existingView = listView.getChildAt(j);
				}
				View newView = dictionaryLanguagesAdapter.getView(j, existingView, listView);
				if (j < listView.getChildCount()) {
					if (newView != existingView) {
						listView.removeViewAt(j);
						listView.addView(newView, j);
					}
				} else {
					listView.addView(newView);
				}
			}
		} else {
			// TODO: set title from Preferences with languages
			title.setText(dictionary.getAbbreviation());
			holder.selectionCheckbox.setChecked(false);
			listView.removeAllViews();
		}

		holder.selectionCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton compoundButton, boolean checked) {
				if (!checked) {
					dictionary.unloadFile();
				} else {

					final DfMInputStreamAccess inputStreamAccess;

					if (dictionary.getType() == Preferences.DictionaryType.INCLUDED) {
						inputStreamAccess = new AssetDfMInputStreamAccess(viewGroup.getContext().getAssets(), dictionary.getPath());
					} else if (dictionary.getType() == Preferences.DictionaryType.DIRECTORY) {
						inputStreamAccess = new FileDfMInputStreamAccess(dictionary.getPath());
					} else if (dictionary.getType() == Preferences.DictionaryType.ARCHIVE) {
						inputStreamAccess = new NativeZipInputStreamAccess(dictionary.getPath());
					} else {
						throw new IllegalArgumentException("Invalid Type: " + dictionary.getType().ordinal());
					}

					LoadDictionaryThread loadDictionaryThread = new LoadDictionaryThread();
					final LoadDictionaryThread.OnThreadResultListener threadListener = new LoadDictionaryThread.OnThreadResultListener() {
						@Override
						public void onSuccess(DictionaryDataFile dataFile) {
							dictionary.setFile(dataFile);
							Preferences.addRecentDictionaryUrl(dictionary.getType(), dictionary.getPath(), dictionary.getLanguages());
						}

						@Override
						public void onException(DictionaryException exception, boolean mayIncludeCompressedDictionary) {
							compoundButton.setChecked(false);
						}

						@Override
						public void onInterrupted() {
							compoundButton.setChecked(false);
						}
					};
					loadDictionaryThread.setOnThreadResultListener(threadListener);
					loadDictionaryThread.execute(inputStreamAccess);

				}
			}
		});

		return view;
	}
}