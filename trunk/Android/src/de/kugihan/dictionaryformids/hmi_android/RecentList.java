/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.kugihan.dictionaryformids.hmi_android.Preferences.DictionaryType;
import de.kugihan.dictionaryformids.hmi_android.data.ResultProvider;
import de.kugihan.dictionaryformids.hmi_android.thread.HiddenDictionaryFinderTask;
import de.kugihan.dictionaryformids.hmi_android.view_helper.LocalizationHelper;

/**
 * DictionaryList represents an Activity that shows internal dictionaries and
 * allows the user to choose a dictionary to load.
 * 
 */
public class RecentList extends ListActivity implements ResultProvider {

	/**
	 * Caches the type of the entries.
	 */
	private List<DictionaryType> itemsType = null;

	/**
	 * Caches the path of the entries.
	 */
	private List<String> itemsPath = null;

	/**
	 * Caches the languages of the entries.
	 */
	private List<String> itemsLanguages = null;

	/**
	 * The result returned to TabHost.
	 */
	private Intent returnData = null;

	/**
	 * The result code returned to TabHost.
	 */
	private int resultCode = RESULT_CANCELED;

	/**
	 * Handler to receive tasks that change the user interface.
	 */
	private Handler handler = new Handler();
	
	/**
	 * Object to hold a findDictionaries searching for hidden dictionaries.
	 */
	private AsyncTask<String, Integer, ArrayList<File>> findDictionaries = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.recent_dictionary_list);
		Preferences.attachToContext(getApplicationContext());
		fillWithDictionaries();

		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		preferences
				.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

		registerForContextMenu(getListView());

		final TextView empty = (TextView) findViewById(android.R.id.empty);
		empty.setOnClickListener(clickListener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreateContextMenu(final ContextMenu menu, final View v,
			final ContextMenuInfo menuInfo) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.recent_dictionary_context, menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(final int id) {
		if (id == R.id.dialog_manual_download_instructions) {
			final Builder alertBuilder = new AlertDialog.Builder(this);
			alertBuilder.setTitle(R.string.title_information);
			alertBuilder.setMessage(R.string.msg_download_dictionaries);
			alertBuilder.setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
							Intent downloadDictionaries = new Intent(
									Intent.ACTION_VIEW);
							downloadDictionaries
									.setData(Uri
											.parse(getString(R.string.attribute_dictionaries_url)));
							startActivity(downloadDictionaries);
						}
					});
			alertBuilder.setNegativeButton(R.string.button_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
						}
					});
			return alertBuilder.create();
		} else if (id == R.id.dialog_confirm_clear_recent_dictionaries) {
			final Builder alertBuilder = new AlertDialog.Builder(this);
			alertBuilder.setTitle(R.string.title_information);
			alertBuilder
					.setMessage(R.string.msg_clear_recent_dictionaries_list);
			alertBuilder.setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							Preferences.clearRecentDictionaryUrls();
							dialog.cancel();
						}
					});
			alertBuilder.setNegativeButton(R.string.button_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
						}
					});
			return alertBuilder.create();
		} else if (id == R.id.dialog_finding_hidden_dictionaries) {
			final ProgressDialog loadingDialog = new ProgressDialog(this);
			loadingDialog.setTitle(getString(R.string.title_please_wait));
			loadingDialog.setMessage(getString(R.string.msg_searching));
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(true);
			loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (findDictionaries != null) {
						findDictionaries.cancel(true);
					}
				}
			});
			return loadingDialog;
		} else {
			return super.onCreateDialog(id);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.recent_dictionary_options, menu);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemDownloadDictionaries:
			showDialog(R.id.dialog_manual_download_instructions);
			break;

		case R.id.itemClearRecentDictionariesList:
			showDialog(R.id.dialog_confirm_clear_recent_dictionaries);
			break;
			
		case R.id.itemFindHiddenDictionaries:
			findDictionaries = new HiddenDictionaryFinderTask(this,
					R.id.dialog_finding_hidden_dictionaries).execute("");
			break;

		default:
			break;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		try {
			dismissDialog(R.id.dialog_manual_download_instructions);
			dismissDialog(R.id.dialog_confirm_clear_recent_dictionaries);
			dismissDialog(R.id.dialog_finding_hidden_dictionaries);
		} catch (IllegalArgumentException e) {
			// ignore exceptions here
		}
		return super.onRetainNonConfigurationInstance();
	}

	/**
	 * Listener to react on preferences changes to reload the list of recent
	 * dictionaries.
	 */
	private final OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(
				final SharedPreferences sharedPreferences, final String key) {
			if (key.equals(Preferences.PREF_RECENT_DICTIONARIES)) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						// push font recent dictionaries change into list items
						fillWithDictionaries();
					}
				});
			}
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onListItemClick(final ListView list, final View view,
			final int position, final long id) {
		super.onListItemClick(list, view, position, id);
		exitWithDictionary(position);
	}

	/**
	 * Closes the application and returns the dictionary at the given position
	 * to the calling activity.
	 * 
	 * @param position
	 *            the position of the selected dictionary
	 */
	private void exitWithDictionary(final int position) {
		resultCode = RESULT_OK;
		returnData = new Intent();
		final DictionaryType type = itemsType.get(position);
		final String typeProtocol = Preferences.typeToProtocolString(type);
		final String path = itemsPath.get(position);
		returnData.putExtra(typeProtocol, path);
		setResult(resultCode, returnData);
		finish();
	}

	/**
	 * Fill the view with recently loaded dictionaries.
	 */
	private void fillWithDictionaries() {
		final String[] dictionaries = Preferences.getRecentDictionaries();
		itemsType = new ArrayList<DictionaryType>();
		itemsPath = new ArrayList<String>();
		itemsLanguages = new ArrayList<String>();
		for (String dictionary : dictionaries) {
			JSONObject parts;
			int type;
			String path;
			String languages = "";
			try {
				parts = new JSONObject(dictionary);
				type = parts.getInt("type");
				path = parts.getString("path");
				JSONArray languagesArray = new JSONArray(parts
						.getString("languages"));
				for (int i = 0; i < languagesArray.length(); i++) {
					final String language = languagesArray.getString(i);
					final String localizedLanguage = LocalizationHelper
							.getLanguageName(getResources(), language);
					languages += localizedLanguage + " ";
				}
				languages = languages.trim();
			} catch (JSONException e) {
				continue;
			}
			itemsType.add(DictionaryType.values()[type]);
			itemsPath.add(path);
			itemsLanguages.add(languages);
		}
		ListAdapter dictionaryList = new ListAdapter();
		setListAdapter(dictionaryList);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Intent getReturnData() {
		return returnData;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final int getResultCode() {
		return resultCode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onContextItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemRemoveFromList:
			final AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
					.getMenuInfo();
			final int position = menuInfo.position;
			DictionaryType type = itemsType.get(position);
			Preferences.removeRecentDictionary(itemsPath.get(position), type);
			break;

		default:
			return super.onContextItemSelected(item);
		}
		return true;
	}

	/**
	 * A listener for clicks on the download dictionaries field.
	 */
	private final OnClickListener clickListener = new OnClickListener() {

		@Override
		public void onClick(final View v) {
			final ChooseDictionary parent = (ChooseDictionary) getParent();
			final String downloadTag = getString(R.string.tag_tab_download);
			parent.getTabHost().setCurrentTabByTag(downloadTag);
		}

	};

	/**
	 * Implementation of the BaseAdapter to create a custom view of the recent
	 * dictionary list.
	 * 
	 */
	private class ListAdapter extends BaseAdapter {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getCount() {
			return itemsPath.size();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object getItem(final int position) {
			return itemsPath.get(position);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public long getItemId(final int position) {
			return position;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public View getView(final int position, final View convertView,
				final ViewGroup parent) {
			View view;
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(parent
						.getContext());
				view = inflater.inflate(R.layout.recent_dictionary_row, null);
			} else {
				view = convertView;
			}
			final String languagesString = itemsLanguages.get(position);
			final String typeString = itemsType.get(position).toString();
			final String pathString = itemsPath.get(position);
			final String completePathString = getString(
					R.string.title_recent_dictionary, typeString, pathString);
			final TextView languages = (TextView) view.findViewById(R.id.languages);
			final TextView path = (TextView) view.findViewById(R.id.path);
			languages.setText(languagesString);
			path.setText(completePathString);
			return view;
		}

	}
}
