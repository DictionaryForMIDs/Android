/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import de.kugihan.dictionaryformids.hmi_android.Preferences.DictionaryType;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_android.data.ResultProvider;

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
	 * The result returned to TabHost.
	 */
	private Intent returnData = null;

	/**
	 * The result code returned to TabHost.
	 */
	private int resultCode = RESULT_CANCELED;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.recent_dictionary_list);
        Preferences.attachToContext(getApplicationContext());
        fillWithDictionaries();
        
        SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		preferences
				.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
		
		registerForContextMenu(getListView());
    }
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.recent_dictionary_context, menu);
	}
	
	private OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(
				final SharedPreferences sharedPreferences, final String key) {
			if (key.equals(Preferences.PREF_RECENT_DICTIONARIES)) {
				// push font recent dictionaries change into list items
				fillWithDictionaries();
			}
		}
	};
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		exitWithDictionary(position);
	}

	private void exitWithDictionary(final int position) {
		resultCode = RESULT_OK;
		returnData = new Intent();
		DictionaryType type = itemsType.get(position);
		String typeProtocol = Preferences.typeToProtocolString(type);
		String path = itemsPath.get(position);
		returnData.putExtra(typeProtocol, path);
		setResult(resultCode, returnData);
		finish();
	}
	
    private void fillWithDictionaries() {
    	String[] dictionaries = Preferences.getRecentDictionaries();
    	itemsType = new ArrayList<DictionaryType>();
    	itemsPath = new ArrayList<String>();
    	ArrayList<String> view = new ArrayList<String>();
		for (String dictionary : dictionaries) {
			JSONObject parts;
			int type;
			String path;
			String languages = "";
			try {
				parts = new JSONObject(dictionary);
				type = parts.getInt("type");
				path = parts.getString("path");
				JSONArray languagesArray = new JSONArray(parts.getString("languages"));
				for (int i = 0; i < languagesArray.length(); i++) {
					languages += languagesArray.getString(i) + " ";
				}
				languages = languages.trim();
			} catch (JSONException e) {
				continue;
			}
			String typeName;
			if (type == DictionaryType.DIRECTORY.ordinal()) {
				typeName = "DIR";
			} else if (type == DictionaryType.ARCHIVE.ordinal()) {
				typeName = "ZIP";
			} else if (type == DictionaryType.INCLUDED.ordinal()) {
				typeName = "INC";
			} else {
				continue;
			}
			itemsType.add(DictionaryType.values()[type]);
			itemsPath.add(path);
			view.add(getString(R.string.title_recent_dictionary, typeName,
					path, languages));
		}
		ArrayAdapter<String> dictionaryList = new ArrayAdapter<String>(this,
				R.layout.file_row, view);
		setListAdapter(dictionaryList);
	}
    
    /**
     * {@inheritDoc}
     */
    public final Intent getReturnData() {
    	return returnData;
    }

    /**
     * {@inheritDoc}
     */
    public final int getResultCode() {
    	return resultCode;
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
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
}
