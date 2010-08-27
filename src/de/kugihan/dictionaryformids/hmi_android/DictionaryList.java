/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.kugihan.dictionaryformids.hmi_android.data.ResultProvider;

/**
 * DictionaryList represents an Activity that shows internal dictionaries and
 * allows the user to choose a dictionary to load.
 * 
 */
public class DictionaryList extends ListActivity implements ResultProvider {
	
	/**
	 * The string identifying dictionaries in the assets folder.
	 */
	public static final String ASSET_PATH = "assetPath";

	/**
	 * Specifies the subdirectories in the application's assets where
	 * dictionaries can be found.
	 */
	private static final String INTERNAL_DICT_PATH = "dict";
	
	/**
	 * Caches the path of the entries in the current directory.
	 */
	private List<String> items = null;
	
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
        setContentView(R.layout.included_dictionary_list);
        fillWithDictionaries();
        
		final TextView empty = (TextView) findViewById(android.R.id.empty);
		empty.setOnClickListener(clickListener);
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		final String dictionary = items.get(position);
		exitWithDictionary(dictionary);
	}

	/**
	 * Exits the view and returns the specified dictionary.
	 * @param dictionary the dictionary to return
	 */
	private void exitWithDictionary(final String dictionary) {
		resultCode = RESULT_OK;
		returnData = new Intent();
		final String path = INTERNAL_DICT_PATH + File.separator + dictionary;
		returnData.putExtra(ASSET_PATH, path);
		setResult(resultCode, returnData);
		finish();
	}

	/**
	 * Searches for dictionaries in the assets folder and adds them to the view.
	 */
    private void fillWithDictionaries() {
    	String[] dictionaries;
		try {
			dictionaries = getAssets().list(INTERNAL_DICT_PATH);
		} catch (IOException e) {
	        Toast.makeText(getBaseContext(), 
	                R.string.msg_internal_dictionary_load_error, 
	                Toast.LENGTH_LONG).show();
			return;
		}
		items = new ArrayList<String>();
		for (String dictionary : dictionaries) {
			items.add(dictionary);
		}
		Collections.sort(items, String.CASE_INSENSITIVE_ORDER);
		final ArrayAdapter<String> dictionaryList = new ArrayAdapter<String>(this,
				R.layout.file_row, items);
		setListAdapter(dictionaryList);
	}
    
	/**
	 * Checks if there is any dictionary in the assets folder.
	 * 
	 * @param resources
	 *            the resources to use
	 * @return true if at least one dictionary is available
	 */
    public static boolean hasDictionaries(Resources resources) {
    	String[] dictionaries;
		try {
			dictionaries = resources.getAssets().list(INTERNAL_DICT_PATH);
		} catch (IOException e) {
	        return false;
		}
		if (dictionaries.length > 0) {
			return true;
		} else {
			return false;
		}
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
}
