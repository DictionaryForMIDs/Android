/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import java.util.ArrayList;

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
import android.view.Window;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import de.kugihan.dictionaryformids.hmi_android.data.ResultProvider;

/**
 * ChooseDictionary represents an Activity where the user can specify a
 * dictionary for the application. Different sources can be displayed.
 * 
 */
public final class ChooseDictionary extends TabActivity {

	/**
	 * The key of a boolean specifying if the default tab is the installation
	 * tab.
	 */
	public static final String BUNDLE_SHOW_DICTIONARY_INSTALLATION = "showDictionaryInstallation";

	/**
	 * The interface used for distributing dialog events.
	 * 
	 */
	public interface DialogCallback {
		/**
		 * This function is called when the parent onCreateDialog cannot handle
		 * the specified dialogId.
		 * 
		 * @param dialogId
		 *            the id of the dialog to create
		 * @return the newly created dialog or null
		 */
		Dialog onCreateDialogListener(final int dialogId);

		/**
		 * This function is called when the parent onPrepareDialog cannot handle
		 * the specified dialogId.
		 * 
		 * @param dialogId
		 *            the id of the dialog to prepare
		 * @param dialog
		 *            the created dialog
		 */
		void onPrepareDialogListener(final int dialogId, final Dialog dialog);
	}

	/**
	 * The list of dialog event listeners.
	 */
	private static ArrayList<DialogCallback> dialogListeners = new ArrayList<DialogCallback>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

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

		final Intent downloadDictionary = new Intent(this,
				InstallDictionary.class);
		final String downloadTag = getString(R.string.tag_tab_download);
		final CharSequence downloadTabTitle = getText(R.string.tab_load_download);
		final TabSpec downloadDictionariesTab = tabHost.newTabSpec(downloadTag)
				.setIndicator(downloadTabTitle).setContent(downloadDictionary);
		tabHost.addTab(downloadDictionariesTab);

		if (getIntent().getBooleanExtra(BUNDLE_SHOW_DICTIONARY_INSTALLATION,
				false)) {
			tabHost.setCurrentTabByTag(getString(R.string.tag_tab_download));
		} else {
			tabHost.setCurrentTab(0);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void finishFromChild(final Activity child) {
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
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.choose_dictionary_options, menu);
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

		default:
			break;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(final int id) {
		if (id == R.id.dialog_manual_download_instructions) {
			Builder alertBuilder = new AlertDialog.Builder(this);
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
			Builder alertBuilder = new AlertDialog.Builder(this);
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
		} else {
			// pass the call on to childs
			Dialog dialog = null;
			for (DialogCallback callback : dialogListeners) {
				dialog = callback.onCreateDialogListener(id);
				if (dialog != null) {
					break;
				}
			}
			if (dialog != null) {
				return dialog;
			}
		}
		return super.onCreateDialog(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPrepareDialog(final int id, final Dialog dialog) {
		for (DialogCallback callback : dialogListeners) {
			callback.onPrepareDialogListener(id, dialog);
		}
		super.onPrepareDialog(id, dialog);
	}

	/**
	 * Register a new dialog event listener.
	 * 
	 * @param listener
	 *            the listener to register
	 */
	public void registerDialogListener(final DialogCallback listener) {
		// make sure the client is registered only once
		removeDialogListener(listener);
		dialogListeners.add(listener);
	}

	/**
	 * Remove a registered dialog event listener.
	 * 
	 * @param listener
	 *            the listener to remove
	 */
	public void removeDialogListener(final DialogCallback listener) {
		dialogListeners.remove(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		try {
			dismissDialog(R.id.dialog_manual_download_instructions);
			dismissDialog(R.id.dialog_confirm_clear_recent_dictionaries);
		} catch (IllegalArgumentException e) {
			// ignore exceptions here
		}
		return super.onRetainNonConfigurationInstance();
	}

}
