/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 *
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
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
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.choose_dictionary);
		setProgressBarIndeterminateVisibility(false);
		setProgressBarVisibility(false);
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

		if (DictionaryList.hasDictionaries(getResources())) {
			final Intent includedDictionary = new Intent(this, DictionaryList.class);
			final String includedTag = getString(R.string.tag_tab_included);
			final CharSequence includedTabTitle = getText(R.string.tab_load_included);
			final TabSpec includedDictionariesTab = tabHost.newTabSpec(includedTag)
					.setIndicator(includedTabTitle).setContent(includedDictionary);
			tabHost.addTab(includedDictionariesTab);
		}

		final Intent downloadDictionary = new Intent(this,
				InstallDictionary.class);
		final int autoInstallId = getIntent().getIntExtra(
				InstallDictionary.INTENT_AUTO_INSTALL_ID, 0);
		downloadDictionary.putExtra(InstallDictionary.INTENT_AUTO_INSTALL_ID,
				autoInstallId);
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

		if (Build.VERSION.SDK.equals("3")) {
			// Build.Version.SDK_INT does not exist here
			// so just return as menu should already be working anyways
			return;
		}
		tabHost.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				// make sure menu button is recreated on newer APIs
				if (Build.VERSION.SDK_INT >= 11) {
					invalidateOptionsMenu();
				}
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// create and prepare menus in onPrepareOptionsMenu
		final Activity activity = getLocalActivityManager().getCurrentActivity();
		if (activity instanceof RecentList) {
			activity.onCreateOptionsMenu(menu);
		} else if (activity instanceof InstallDictionary) {
			activity.onCreateOptionsMenu(menu);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		// display menus for known activities
		final Activity activity = getLocalActivityManager().getCurrentActivity();
		menu.clear();
		if (activity instanceof RecentList) {
			activity.onCreateOptionsMenu(menu);
		} else if (activity instanceof InstallDictionary) {
			activity.onCreateOptionsMenu(menu);
			activity.onPrepareOptionsMenu(menu);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		final Activity activity = getLocalActivityManager().getCurrentActivity();
		if (activity instanceof RecentList) {
			return activity.onMenuItemSelected(featureId, item);
		} else if (activity instanceof InstallDictionary) {
			return activity.onMenuItemSelected(featureId, item);
		}
		return super.onMenuItemSelected(featureId, item);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void finishFromChild(final Activity child) {
		if (child instanceof ResultProvider) {
			final ResultProvider list = (ResultProvider) child;
			setResult(list.getResultCode(), list.getReturnData());
		}
		super.finishFromChild(child);
	}
}
