/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_android.data.ResultProvider;

/**
 * ChooseDictionary represents an Activity where the user can specify a
 * dictionary for the application. Different sources can be displayed.
 * 
 */
public class ChooseDictionary extends TabActivity {
	
	/**
	 * ID of the dialog with instructions to download a dictionary.
	 */
	public static final int ID_DOWNLOAD = 0;

	/**
	 * ID of the dialog that asks for confirmation to clear the list of recently
	 * loaded dictionaries.
	 */
	private static final int ID_CLEAR_RECENT = 1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.choose_dictionary);
	    Preferences.attachToContext(getApplicationContext());

	    final TabHost tabHost = getTabHost();
	    
	    final Intent recentList = new Intent(this, RecentList.class);
	    final String recentTag = getString(R.string.tag_tab_recent);
	    final CharSequence recentTabTitle = getText(R.string.tab_load_recent);
	    final TabSpec recentTab = tabHost.newTabSpec(recentTag).setIndicator(
	    		recentTabTitle).setContent(recentList);
		tabHost.addTab(recentTab);
	    
	    final Intent fileList = new Intent(this, FileList.class);
	    final String fileTag = getString(R.string.tag_tab_file);
	    final CharSequence fileTabTitle = getText(R.string.tab_load_from_file);
	    final TabSpec fileSystemTab = tabHost.newTabSpec(fileTag).setIndicator(
				fileTabTitle).setContent(fileList);
		tabHost.addTab(fileSystemTab);

		final Intent includedDictionary = new Intent(this, DictionaryList.class);
		final String includedTag = getString(R.string.tag_tab_included);
		final CharSequence includedTabTitle = getText(R.string.tab_load_included);
		final TabSpec includedDictionariesTab = tabHost.newTabSpec(includedTag)
				.setIndicator(includedTabTitle).setContent(includedDictionary);
		tabHost.addTab(includedDictionariesTab);
	    
		tabHost.setCurrentTab(0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void finishFromChild(final Activity child) {
		if (child instanceof ResultProvider) {
			ResultProvider list = (ResultProvider) child;
			setResult(list.getResultCode(), list.getReturnData());
		}
		super.finishFromChild(child);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.choose_dictionary_options, menu);
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemDownloadDictionaries:
			showDialog(ID_DOWNLOAD);
			break;
			
		case R.id.itemClearRecentDictionariesList:
			showDialog(ID_CLEAR_RECENT);
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
	protected final Dialog onCreateDialog(final int id) {
		if (id == ID_DOWNLOAD) {
			Builder alertBuilder = new AlertDialog.Builder(this);
			alertBuilder.setTitle(R.string.title_information);
			alertBuilder.setMessage(R.string.msg_download_dictionaries);
			alertBuilder.setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							dialog.cancel();
							Intent downloadDictionaries = new Intent(Intent.ACTION_VIEW);
							downloadDictionaries.setData(Uri
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
		} else if (id == ID_CLEAR_RECENT) {
			Builder alertBuilder = new AlertDialog.Builder(this);
			alertBuilder.setTitle(R.string.title_information);
			alertBuilder.setMessage(R.string.msg_clear_recent_dictionaries_list);
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
		}
		return super.onCreateDialog(id);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Object onRetainNonConfigurationInstance() {
		try {
			dismissDialog(ID_DOWNLOAD);
			dismissDialog(ID_CLEAR_RECENT);
		} catch (IllegalArgumentException e) {
			// ignore exceptions here
		}
		return super.onRetainNonConfigurationInstance();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onDestroy() {
		super.onDestroy();
	}
	

}
