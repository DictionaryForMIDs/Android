/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 *
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import android.app.Activity;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.AssetDfMInputStreamAccess;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.DfMInputStreamAccess;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.FileDfMInputStreamAccess;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.NativeZipInputStreamAccess;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.general.Util;
import de.kugihan.dictionaryformids.hmi_android.data.DictionaryType;
import de.kugihan.dictionaryformids.hmi_android.data.AndroidUtil;
import de.kugihan.dictionaryformids.hmi_android.data.DfMTranslationExecutor;
import de.kugihan.dictionaryformids.hmi_android.data.DictionariesAdapter;
import de.kugihan.dictionaryformids.hmi_android.data.Dictionary;
import de.kugihan.dictionaryformids.hmi_android.data.DictionaryVector;
import de.kugihan.dictionaryformids.hmi_android.data.TranslationsAdapter;
import de.kugihan.dictionaryformids.hmi_android.service.DictionaryInstallationService;
import de.kugihan.dictionaryformids.hmi_android.thread.LoadDictionaryThread;
import de.kugihan.dictionaryformids.hmi_android.thread.LoadDictionaryThread.OnThreadResultListener;
import de.kugihan.dictionaryformids.hmi_android.thread.Translations;
import de.kugihan.dictionaryformids.hmi_android.view_helper.DialogHelper;
import de.kugihan.dictionaryformids.hmi_android.view_helper.TranslationScrollListener;
import de.kugihan.dictionaryformids.translation.SingleTranslationExtension;
import de.kugihan.dictionaryformids.translation.TranslationParameters;
import de.kugihan.dictionaryformids.translation.TranslationParametersBatch;
import de.kugihan.dictionaryformids.translation.TranslationResult;

/**
 * DictionaryForMIDs is the main Activity of the application. Most of the user
 * interaction will be handled by this class.
 *
 */
public final class DictionaryForMIDs extends Activity {

	public static final String BUNDLE_DICTIONARY_ABOUT_TEXT = "dictionaryAboutText";

	/**
	 * Summarizes all objects that should be saved in
	 * OnRetainNonConfigurationInstance.
	 *
	 */
	private static final class NonConfigurationInstance {

		/**
		 * The currently active load dictionary thread.
		 */
		private final LoadDictionaryThread thread;

		/**
		 * The current list of translationsAdapter.
		 */
		private final Vector<TranslationResult> translations;

		/**
		 * The current list of dictionaries
		 */
		private final DictionaryVector dictionaryVector;

		/**
		 * Constructs a new instance and initializes all members.
		 *
		 * @param thread
		 *            the current load dictionary thread
		 * @param translations
		 *            the current translationsAdapter
		 */
		public NonConfigurationInstance(final LoadDictionaryThread thread,
				final Vector<TranslationResult> translations, final DictionaryVector dictionaryVector) {
			this.thread = thread;
			this.translations = translations;
			this.dictionaryVector = dictionaryVector;
		}

		/**
		 * Returns the load dictionary thread.
		 *
		 * @return the load dictionary thread
		 */
		public LoadDictionaryThread getThread() {
			return thread;
		}

		/**
		 * Returns the current translations.
		 *
		 * @return the translations
		 */
		public Vector<TranslationResult> getTranslations() {
			return translations;
		}
	}

	private class TranslationsObserver extends DataSetObserver {
		@Override
		public void onChanged() {
			super.onChanged();

			int resultCount = 0;
			for (TranslationResult translationResult : translationsAdapter.getTranslationResults()) {
				resultCount += translationResult.numberOfFoundTranslations();
			}

			final TextView output = (TextView) findViewById(R.id.output);
			if (translationsAdapter.getGroupCount() == 0) {
				output.setText("");
			} else if (resultCount == 0) {
				output.setText(R.string.no_results_found);
			} else if (resultCount == 1) {
				output.setText(R.string.results_found_one);
			} else {
				output.setText(getString(R.string.results_found,
						resultCount));
			}
			// hide heading
			((LinearLayout) findViewById(R.id.HeadingLayout))
					.setVisibility(View.GONE);

			final ExpandableListView listView = (ExpandableListView) findViewById(R.id.translationsListView);

			// scroll to top
			listView.setSelectionFromTop(0, 0);
			// show list
			listView.setVisibility(View.VISIBLE);
			// expand first result when all results are available
			final boolean isTranslationFinished = translationsAdapter.getGroupCount() == dictionaries.getLoadedLanguagePairs();
			if (translationsAdapter.getGroupCount() > 0 && isTranslationFinished && translationsAdapter.getChildrenCount(0) > 0) {
				listView.expandGroup(0);
			}
			// try closing search progress dialog
			if (!Preferences.getSearchAsYouType()) {
				try {
					dismissDialog(DialogHelper.ID_SEARCHING);
				} catch (IllegalArgumentException e) {
					// ignore, dialog was already closed
				}
			}
		}

		@Override
		public void onInvalidated() {
			super.onInvalidated();
			final TextView output = (TextView) findViewById(R.id.output);
			output.setText("");
		}
	}

	/**
	 * The key of an integer specifying the heading's visibility in a bundle.
	 */
	private static final String BUNDLE_HEADING_VISIBILITY = "headingVisibility";

	/**
	 * The key of a string specifying the status message in a bundle.
	 */
	private static final String BUNDLE_STATUS_MESSAGE = "statusMessage";

	/**
	 * The key of an integer specifying the visibility of the search options in
	 * a bundle.
	 */
	private static final String BUNDLE_SEARCH_OPTIONS_VISIBILITY = "searchOptionsVisibility";

	/**
	 * The key of an integer specifying the number of currently displayed translationsAdapter.
	 */
	private static final String BUNDLE_NUMBER_OF_TRANSLATIONS = "numberOfTranslations";

	/**
	 * The key of a String specifying the message to display to the user.
	 */
	public static final String BUNDLE_DISPLAY_MESSAGE = "displayMessage";

	/**
	 * The tag used for log messages.
	 */
	public static final String LOG_TAG = "MY";

	/**
	 * The number of milliseconds in one second.
	 */
	private static final int MILLISECONDS_IN_A_SECOND = 1000;

	/**
	 * The message id for translation errors.
	 */
	public static final int THREAD_ERROR_MESSAGE = 1;

	/**
	 * The request code identifying a {@link ChooseDictionary} activity.
	 */
	private static final int REQUEST_DICTIONARY_PATH = 0;

	/**
	 * The request code identifying a {@link StarredWordsList} activity.
	 */
	private static final int REQUEST_STARRED_WORDS = 1;

	/**
	 * The result that instructs the activity to exit.
	 */
	public static final int RESULT_EXIT = RESULT_FIRST_USER;

	/**
	 * The handle of the thread that loads the new dictionary or null.
	 */
	private LoadDictionaryThread loadDictionaryThread = null;

	/**
	 * The object used to synchronize access on the load dictionary thread.
	 */
	private final Object loadDictionaryThreadSync = new Object();

	private final DictionaryVector dictionaries = new DictionaryVector();

	/**
	 * The data of the translation results list.
	 */
	private TranslationsAdapter translationsAdapter = null;

	private Translations translations = null;

	/**
	 * The helper for all dialogs.
	 */
	private DialogHelper dialogHelper;

	private final TranslationScrollListener onScrollListener = new TranslationScrollListener();

	private ActionBarDrawerToggle mDrawerToggle = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		outState.putInt(BUNDLE_SEARCH_OPTIONS_VISIBILITY,
				((LinearLayout) findViewById(R.id.selectLanguagesLayout))
						.getVisibility());
		outState.putCharSequence(BUNDLE_STATUS_MESSAGE,
				((TextView) findViewById(R.id.output)).getText());
		outState.putInt(BUNDLE_HEADING_VISIBILITY,
				((LinearLayout) findViewById(R.id.HeadingLayout))
						.getVisibility()
		);
		outState.putInt(BUNDLE_NUMBER_OF_TRANSLATIONS, translationsAdapter.getAllChildrenCount());
		super.onSaveInstanceState(outState);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {

		loadLastNonConfigurationInstance();

		if (!dictionaries.isEmpty()) {
			DictionariesAdapter adapter = new DictionariesAdapter(dictionaries);
			ListView listView = (ListView) findViewById(R.id.loaded_dictionary_list);
			listView.setAdapter(adapter);
		} else {
			// When the previously loaded dictionary is not available any more
			// (because Android used the memory while we were in background)
			// we just reload the last dictionary
			loadLastUsedDictionary(false);
		}

		final int previousNumberOfTranslations = savedInstanceState
				.getInt(BUNDLE_NUMBER_OF_TRANSLATIONS);

		CharSequence statusMessage = savedInstanceState
				.getCharSequence(BUNDLE_STATUS_MESSAGE);
		if (translationsAdapter.isEmpty() && previousNumberOfTranslations > 0) {
			// status could be something like 5 results found
			// so clear it if there are no translationsAdapter available
			// and at least 1 translation was available before
			statusMessage = "";
		}
		((TextView) findViewById(R.id.output)).setText(statusMessage);

		final int headingVisiblity = savedInstanceState
				.getInt(BUNDLE_HEADING_VISIBILITY);
		((LinearLayout) findViewById(R.id.HeadingLayout))
				.setVisibility(headingVisiblity);

		final int searchOptionsVisibility = savedInstanceState
				.getInt(BUNDLE_SEARCH_OPTIONS_VISIBILITY);
		((LinearLayout) findViewById(R.id.selectLanguagesLayout))
				.setVisibility(searchOptionsVisibility);

		// temporarily remove listeners to make sure setText is ignored in input field
		final EditText translationInput = (EditText) findViewById(R.id.TranslationInput);
		translationInput.removeTextChangedListener(textWatcher);
		final OnFocusChangeListener focusListener = translationInput.getOnFocusChangeListener();
		translationInput.setOnFocusChangeListener(null);

		super.onRestoreInstanceState(savedInstanceState);

		translationInput.addTextChangedListener(textWatcher);
		translationInput.setOnFocusChangeListener(focusListener);

		final boolean isTranslationResultLost = getLastNonConfigurationInstance() == null;
		if (isTranslationResultLost && Preferences.getSearchAsYouType()
				&& translationInput.getText().length() > 0 && isDictionaryAvailable()) {
			// initiate search as the translation is not available and in
			// search-as-you-type there is no button to initiate a new search
			startTranslation();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		LoadDictionaryThread tempThread = null;
		synchronized (loadDictionaryThreadSync) {
			if (isLoadDictionaryThreadActive()) {
				loadDictionaryThread.setOnThreadResultListener(null);
				tempThread = loadDictionaryThread;
				loadDictionaryThread = null;
			}
		}
		return new NonConfigurationInstance(tempThread, translationsAdapter.getTranslationResults(), dictionaries);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// set up preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		Preferences.attachToContext(getApplicationContext());
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		preferences
				.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

		// set preferred locale for application
		setCustomLocale(Preferences.getLanguageCode());

		// load theme before call to setContentView()
		setApplicationTheme();

		// call super.onCreate() AFTER setTheme() to prevent bug with wrong
		// background on Android-1.6
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_container);
		final ViewStub stub = (ViewStub) findViewById(R.id.InputLayoutStub);
		if (!Preferences.getSearchAsYouType()) {
			stub.setLayoutResource(R.layout.search_bar);
		}
		stub.inflate();

		// make sure progress bars are hidden until needed
		setProgressBarIndeterminateVisibility(false);
		setProgressBarVisibility(false);


		translations = new Translations();
		translations.setExecutor(new DfMTranslationExecutor());

		// create the adapter to display translations
		final TranslationsAdapter translationsAdapter = new TranslationsAdapter(this);
		setTranslationAdapter(translationsAdapter);

		dialogHelper = DialogHelper.getInstance(this);

		setupSearchBar();
		updateActiveLanguagesCount();
		dictionaries.addObserver(new Observer() {
			@Override
			public void update(Observable observable, Object o) {
				updateActiveLanguagesCount();
			}
		});

		final ExpandableListView translationListView = (ExpandableListView) findViewById(R.id.translationsListView);
		translationListView.setAdapter(this.translationsAdapter);
		translationListView.setOnFocusChangeListener(focusChangeListener);
		translationListView.setOnScrollListener(onScrollListener);
		translationListView.setOnTouchListener(touchListener);
		translationListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
			private int currentlyExpandedGroup = -1;
			@Override
			public void onGroupExpand(int groupPosition) {
				if (currentlyExpandedGroup < 0) {
					// Collapse all other groups
					for (int i = 0; i < translationsAdapter.getGroupCount(); i++) {
						if (i != groupPosition) {
							translationListView.collapseGroup(i);
						}
					}
				} else if (currentlyExpandedGroup >= 0 && currentlyExpandedGroup != groupPosition) {
					// Collapse last expanded group
					translationListView.collapseGroup(currentlyExpandedGroup);
				}
				currentlyExpandedGroup = groupPosition;
			}
		});
		registerForContextMenu(translationListView);

		final View openDictionaryMenu = findViewById(R.id.openDictionaryMenu);
		openDictionaryMenu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isDictionaryAvailable()) {
					openDictionaryDrawer();
				} else {
					startChooseDictionaryActivity();
				}
			}
		});

		final Button loadDictionaryButton = (Button) findViewById(R.id.load_dictionary_button);
		loadDictionaryButton.setOnClickListener(loadDictionaryClickListener);

		createDictionariesAdapterAndConnectToList();

		Util util = Util.getUtil();
		if (util instanceof AndroidUtil) {
			final AndroidUtil androidUtil = (AndroidUtil) util;
			androidUtil.setHandler(updateHandler);
		} else {
			util = new AndroidUtil(updateHandler);
			Util.setUtil(util);
		}

		if (savedInstanceState == null) {
			if (Preferences.hasAutoInstallDictionary()
					&& !DictionaryInstallationService.isRunning()) {
				showDialog(DialogHelper.ID_CONFIRM_INSTALL_DICTIONARY);
			} else if (Preferences.isFirstRun()) {
				processIntent(getIntent());
				showDialog(DialogHelper.ID_FIRST_RUN);
			} else {
				final boolean silent = processIntent(getIntent());
				loadLastUsedDictionary(silent);
			}
			boolean hasNoActiveDictionaries = Preferences.getLoadedDictionaries().isEmpty();
			boolean hasRecentDictionaries = !dictionaries.isEmpty();
			if (hasNoActiveDictionaries && hasRecentDictionaries) {
				openDictionaryDrawer();
			}
		}

		final CharSequence mTitle = getTitle(), mDrawerTitle = getTitle();
		DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerToggle = new ActionBarDrawerToggle(
				this,                  /* host Activity */
				mDrawerLayout,         /* DrawerLayout object */
				R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
				R.string.app_name_build,  /* "open drawer" description */
				R.string.app_name_build  /* "close drawer" description */
		) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					getActionBar().setTitle(mTitle);
				}
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					getActionBar().setTitle(mDrawerTitle);
				}
				hideSoftKeyboard();
			}
		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setHomeButtonEnabled(true);
		}
	}

	private void openDictionaryDrawer() {
		DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerLayout.openDrawer(findViewById(R.id.left_drawer));
	}

	private static int getExpandedGroup(TranslationsAdapter translationsAdapter, ExpandableListView translationListView) {
		int expandedGroup = -1;
		for (int i = 0; i < translationsAdapter.getGroupCount(); i++) {
			if (translationListView.isGroupExpanded(i)) {
				expandedGroup = i;
				break;
			}
		}
		return expandedGroup;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		// Handle your other action bar items...

		return super.onOptionsItemSelected(item);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		super.onPause();
		Preferences.setLoadDictionary(dictionaries);
	}

	/**
	 * Sets and registers a new TranslationAdapter to the activity to receive
	 * updates.
	 *
	 * @param translationsAdapter
	 *            the TranslationAdapter to use
	 */
	public void setTranslationAdapter(final TranslationsAdapter translationsAdapter) {
		this.translationsAdapter = translationsAdapter;
		this.translationsAdapter.registerDataSetObserver(translationsObserver);
		this.translations.getTranslationState().addObserver(onFilterStateChangedObserver);
		this.translations.addObserver(translationsAdapter);
	}

	/**
	 * Sets the theme of the application according to the current settings.
	 *
	 * @param context
	 *            the context the theme shall be applied to
	 */
	static void setApplicationTheme(final ContextThemeWrapper context) {
		final int theme = Preferences.getApplicationTheme();
		if (theme > 0) {
			context.setTheme(theme);
		}
	}

	/**
	 * Sets the theme of the application according to the current settings.
	 */
	private void setApplicationTheme() {
		setApplicationTheme(this);
	}

	/**
	 *
	 */
	private void setupSearchBar() {
		final EditText translationInput = (EditText) findViewById(R.id.TranslationInput);
		translationInput.setOnFocusChangeListener(focusChangeListener);
		translationInput.setOnClickListener(clickListener);
		translationInput.setOnTouchListener(touchListener);
		translationInput.setOnEditorActionListener(editorActionListener);
		translationInput.addTextChangedListener(textWatcher);

		final ImageButton startTranslation = (ImageButton) findViewById(R.id.StartTranslation);
		if (startTranslation != null) {
			startTranslation.setOnClickListener(clickListener);
		}
		final Button clearInput = (Button) findViewById(R.id.ClearInput);
		if (clearInput != null) {
			clearInput.setOnClickListener(clickListener);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// unregister observer for adapter as adapter is used in re-created
		// activity
		translationsAdapter.unregisterDataSetObserver(translationsObserver);
		translations.getTranslationState().deleteObserver(onFilterStateChangedObserver);
	}

	/**
	 * Sets the locale of the current base context.
	 *
	 * @param languageCode
	 *            the language code of the new locale
	 */
	private void setCustomLocale(final String languageCode) {
		if (languageCode.length() == 0) {
			// use system default
			return;
		}
		setCustomLocale(languageCode, getBaseContext().getResources());
	}

	/**
	 * Sets the locale of the given base context's resources.
	 *
	 * @param languageCode
	 *            the language code of the new locale
	 * @param resources
	 *            the base context's resources
	 */
	public static void setCustomLocale(final String languageCode,
			final Resources resources) {
		final Locale locale = getLocaleFromLanguageCode(languageCode);
		Locale.setDefault(locale);
		final Configuration config = new Configuration();
		config.locale = locale;
		resources.updateConfiguration(config, resources.getDisplayMetrics());
	}

	/**
	 * Parses the given language code for language, country and variant and
	 * creates a corresponding locale.
	 *
	 * @param languageCode
	 *            the language code containing language-country-variant
	 * @return the corresponding locale
	 * @throws IllegalArgumentException
	 *             if languageCode cannot be parsed
	 */
	private static Locale getLocaleFromLanguageCode(final String languageCode)
			throws IllegalArgumentException {
		final String parts[] = languageCode.split("-");
		Locale locale;
		if (parts.length == 1) {
			locale = new Locale(languageCode);
		} else if (parts.length == 2) {
			locale = new Locale(parts[0], parts[1]);
		} else if (parts.length == 3) {
			locale = new Locale(parts[0], parts[1], parts[2]);
		} else {
			throw new IllegalArgumentException("languageCode contains "
					+ parts.length + ". Expected 1, 2 or 3");
		}
		return locale;
	}

	/**
	 * Loads the last used dictionary.
	 *
	 * @param silent
	 *            true if the user should not be informed about loading results
	 */
	private void loadLastUsedDictionary(final boolean silent) {
		Vector<Dictionary> recentDictionaries = Preferences.getRecentDictionaries();

		// Remove dictionaries that have been removed from the list of recent dictionaries
		Vector<Dictionary> dictionariesToRemove = new Vector<Dictionary>(recentDictionaries);
		for (Dictionary dictionary : recentDictionaries) {
			if (dictionaries.contains(dictionary)) {
				dictionariesToRemove.remove(dictionary);
			}
			if (isDictionaryLoaded(dictionary.getType(), dictionary.getPath())) {
				dictionariesToRemove.remove(dictionary);
			}
		}
		dictionaries.removeAll(dictionariesToRemove);

		// Load newly added dictionaries
		for (Dictionary recentDictionary : recentDictionaries) {
			if (dictionaries.contains(recentDictionary)) {
				continue;
			} else {
				dictionaries.addEnd(recentDictionary);
			}
		}

		// Load all dictionaries that should be loaded but are not loaded yet
		Vector<Dictionary> loadedDictionaries = Preferences.getLoadedDictionaries();
		for (Dictionary dictionary : loadedDictionaries) {
			if (isDictionaryLoaded(dictionary.getType(), dictionary.getPath())) {
				continue;
			}

			// Move initially loaded dictionaries to the front
			dictionaries.remove(dictionary);
			dictionaries.addAfterLoadedDictionaries(dictionary);

			DfMInputStreamAccess inputStreamAccess = null;

			if (dictionary.getType() == DictionaryType.INCLUDED) {
				inputStreamAccess = new AssetDfMInputStreamAccess(this.getAssets(), dictionary.getPath());
			} else if (dictionary.getType() == DictionaryType.DIRECTORY) {
				inputStreamAccess = new FileDfMInputStreamAccess(dictionary.getPath());
			} else if (dictionary.getType() == DictionaryType.ARCHIVE) {
				inputStreamAccess = new NativeZipInputStreamAccess(dictionary.getPath());
			} else {
				return;
			}
			startLoadDictionary(inputStreamAccess, dictionary.getType(), dictionary.getPath(), dictionary.getSelectedLanguages(),
					silent);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onNewIntent(final Intent intent) {
		processIntent(intent);
		super.onNewIntent(intent);
	}

	/**
	 * Processes the given intent and reacts on included information.
	 *
	 * @param intent
	 *            the intent to process
	 * @return true if the intent included meaningful information that has been
	 *         reacted on
	 */
	private boolean processIntent(final Intent intent) {
		final Bundle bundle = intent.getExtras();
		if (bundle == null) {
			return false;
		}
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			// load string
			final String query = intent.getStringExtra(SearchManager.QUERY);
			// copy string to input field
			final TextView translationInput = (TextView) findViewById(R.id.TranslationInput);
			translationInput.removeTextChangedListener(textWatcher);
			translationInput.setText(query);
			translationInput.addTextChangedListener(textWatcher);
			// start search if dictionary finished loading
			final boolean isDictionaryLoaded = !isLoadDictionaryThreadActive()
					&& isDictionaryAvailable();
			if (isDictionaryLoaded) {
				getIntent().removeExtra(SearchManager.QUERY);
				startTranslation();
			}
		} else if (hasNewDictionary(intent)) {
			intent
					.removeExtra(DictionaryInstallationService.BUNDLE_LOAD_DICTIONARY);
			DialogHelper.setLoadDictionary(intent);
			showDialog(DialogHelper.ID_CONFIRM_LOAD_DICTIONARY);
			return true;
		} else if (bundle
				.getBoolean(DictionaryInstallationService.BUNDLE_SHOW_DICTIONARY_INSTALLATION)) {
			intent
					.removeExtra(DictionaryInstallationService.BUNDLE_SHOW_DICTIONARY_INSTALLATION);
			startChooseDictionaryActivity(true);
			return true;
		} else if (bundle.containsKey(BUNDLE_DISPLAY_MESSAGE)) {
			DialogHelper.setMessage(bundle.getString(BUNDLE_DISPLAY_MESSAGE));
			showDialog(DialogHelper.ID_MESSAGE);
			intent.removeExtra(BUNDLE_DISPLAY_MESSAGE);
			return true;
		} else {
			final Object exceptionObject = bundle
					.get(DictionaryInstallationService.BUNDLE_EXCEPTION);
			if (exceptionObject instanceof Exception) {
				DialogHelper
						.setInstallationException((Exception) exceptionObject);
				showDialog(DialogHelper.ID_INSTALLATION_EXCEPTION);
				intent
						.removeExtra(DictionaryInstallationService.BUNDLE_EXCEPTION);
				return true;
			} else {
				DialogHelper.setInstallationException(null);
			}
		}
		return false;
	}

	/**
	 * Checks if the intent includes information about a new dictionary.
	 *
	 * @param intent
	 *            the intent to analyze
	 * @return true if the intent includes information about a new dictionary
	 */
	private boolean hasNewDictionary(final Intent intent) {
		final Bundle bundle = intent.getExtras();
		if (bundle == null) {
			return false;
		}
		return bundle
				.getBoolean(DictionaryInstallationService.BUNDLE_LOAD_DICTIONARY);
	}

	/**
	 * Loads a configuration that has been saved before the activity got
	 * temporarily destroyed, e.g. after orientation changes.
	 */
	private void loadLastNonConfigurationInstance() {
		final Object lastConfiguration = getLastNonConfigurationInstance();
		if (lastConfiguration == null) {
			return;
		}
		final NonConfigurationInstance data = (NonConfigurationInstance) lastConfiguration;
		// Load dictionaries
		dictionaries.addAllFromIterable(data.dictionaryVector);
		// Load translations
		if (data.getTranslations() != null) {
			translationsAdapter = new TranslationsAdapter(this, data.getTranslations());
			translationsAdapter.registerDataSetObserver(translationsObserver);
			final ExpandableListView listView = (ExpandableListView) findViewById(R.id.translationsListView);
			listView.setAdapter(translationsAdapter);
			listView.setVisibility(View.VISIBLE);
			onFilterStateChangedObserver.update(translations.getTranslationState(), translations.getTranslationState().isActive());
			translations.getTranslationState().addObserver(onFilterStateChangedObserver);
		}
		if (data.getThread() != null) {
			synchronized (loadDictionaryThreadSync) {
				// TODO: handle multiple dictionaries
//				setProgressBarIndeterminateVisibility(true);
//				loadDictionaryThread = data.getThread();
//				loadDictionaryThread
//						.setOnThreadResultListener(createThreadListener(false));
			}
		}
	}

	/**
	 * Listener to react on preferences changes, for example to reload the view
	 * with smaller/bigger font size.
	 */
	private final OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {

		@Override
		public void onSharedPreferenceChanged(
				final SharedPreferences sharedPreferences, final String key) {
			if (key.equals(Preferences.PREF_RESULT_FONT_SIZE)) {
				// push font size change into list items
				translationsAdapter.notifyDataSetChanged();
			} else if (key.equals(Preferences.PREF_IGNORE_DICTIONARY_TEXT_STYLES)) {
				// push style information change into list items
				translationsAdapter.notifyDataSetChanged();
			} else if (key.equals(Preferences.PREF_STARRED_WORDS)) {
				// push starred words option to list items
				translationsAdapter.notifyDataSetChanged();
			} else if (key.equals(Preferences.PREF_SEARCH_AS_YOU_TYPE)) {
				final EditText inputEditText = (EditText) findViewById(R.id.TranslationInput);
				// cache current text
				final CharSequence text = inputEditText.getText();
				// get the id of the new layout
				int id = R.layout.search_bar_auto;
				if (!Preferences.getSearchAsYouType()) {
					id = R.layout.search_bar;
				}
				// replace view
				final View view = findViewById(R.id.InputLayout);
				final ViewGroup parent = (ViewGroup) view.getParent();
				final int index = parent.indexOfChild(view);
				parent.removeViewAt(index);
				parent.addView(getLayoutInflater().inflate(id, parent, false), index);
				setupSearchBar();
				// restore text (may trigger search)
				((EditText) findViewById(R.id.TranslationInput)).setText(text);
			} else if (key.equals(Preferences.PREF_MAX_RESULTS)
					|| key.equals(Preferences.PREF_SEARCH_TIMEOUT)
					|| key.equals(Preferences.PREF_SEARCH_MODE)) {
				final boolean hasSearchTerm = ((EditText) findViewById(R.id.TranslationInput))
						.getText().length() > 0;
				if (Preferences.getSearchAsYouType() && hasSearchTerm && isDictionaryAvailable()) {
					// trigger new search
					startTranslation();
				}
			} else if (key.equals(Preferences.PREF_RECENT_DICTIONARIES)) {
				loadLastUsedDictionary(true);
			}
		}

	};

	/**
	 * Start the thread to load a new dictionary and update the view.
	 *
	 * @param inputStreamAccess
	 *            the input stream to load the dictionary
	 * @param dictionaryType
	 *            the type of the dictionary
	 * @param dictionaryPath
	 *            the path of the dictionary
	 * @param languageSelectionSet
	 *            the selected language pairs
	 * @param exitSilently
	 *            true if the thread should not display dialogs
	 */
	private void startLoadDictionary(
			final DfMInputStreamAccess inputStreamAccess,
			final DictionaryType dictionaryType, final String dictionaryPath,
			final Dictionary.LanguageSelectionSet languageSelectionSet, final boolean exitSilently) {

		if (isDictionaryLoaded(dictionaryType, dictionaryPath)) {
			// dictionary is already loaded
			if (!exitSilently) {
				// TODO: show toast
			}
			return;
		}

		// cancel running thread
		if (isLoadDictionaryThreadActive()) {
			synchronized (loadDictionaryThreadSync) {
				// TODO: handle multiple threads
//				loadDictionaryThread.cancel(true);
			}
		}

		// remove previously shown, loadDictionary-related dialogs
		removeDialog(DialogHelper.ID_DICTIONARY_NOT_FOUND);
		removeDialog(DialogHelper.ID_FIRST_RUN);

		// check if results are shown or a dictionary is available
		if (translationsAdapter.hasData() || isDictionaryAvailable()) {
			// remove results from view
			translationsAdapter.clearData();
		}

		setProgressBarIndeterminateVisibility(true);
		loadDictionaryThread = new LoadDictionaryThread();
		final OnThreadResultListener threadListener = createThreadListener(dictionaryType, dictionaryPath, languageSelectionSet, exitSilently);
		loadDictionaryThread.setOnThreadResultListener(threadListener);
		loadDictionaryThread.execute(inputStreamAccess);
	}

	/**
	 * Checks if a dictionary is currently loaded
	 *
	 * @param dictionaryType the type of the dictionary
	 * @param dictionaryPath the path of the dictionary
	 * @return true if the dictionary is currently loaded
	 */
	private boolean isDictionaryLoaded(DictionaryType dictionaryType, String dictionaryPath) {
		Dictionary dictionary = getLoadedDictionary(dictionaryType, dictionaryPath);
		return dictionary != null;
	}

	/**
	 * Gets the instance of a dictionary if it has been loaded.
	 *
	 * @param dictionaryType the type of the dictionary
	 * @param dictionaryPath the path of the dictionary
	 * @return the instance of the dictionary or null if it has not yet been loaded
	 */
	private Dictionary getLoadedDictionary(DictionaryType dictionaryType, String dictionaryPath) {
		if (dictionaries == null) {
			return null;
		}
		for (Dictionary dictionary : dictionaries) {
			if (dictionary.getFile() != null && dictionary.getType().ordinal() == dictionaryType.ordinal() && dictionary.getPath().equals(dictionaryPath)) {
				return dictionary;
			}
		}
		return null;
	}

	private boolean isLoadDictionaryThreadActive() {
		return loadDictionaryThread != null;
	}

	/**
	 * Creates a listener for thread results.
	 *
	 * @param exitSilently
	 *            true if the thread should exit silently
	 * @return the thread result listener
	 */
	private OnThreadResultListener createThreadListener(final DictionaryType type, final String path,
														final Dictionary.LanguageSelectionSet languageSelectionSet, final boolean exitSilently) {
		return new OnThreadResultListener() {

			@Override
			public void onSuccess(DictionaryDataFile dataFile) {
				forgetThread();

				Dictionary activeDictionary = dictionaries.findMatchOrNull(type, path);
				if (activeDictionary != null) {
					if (activeDictionary.getFile() != dataFile) {
						activeDictionary.setFile(dataFile);
					}
					languageSelectionSet.applyToDictionary(activeDictionary);
				} else {
					Dictionary dictionary = new Dictionary(dataFile, type, path);
					if (languageSelectionSet != null) {
						languageSelectionSet.applyToDictionary(dictionary);
					}
					dictionaries.add(0, dictionary);
					Preferences.addRecentDictionaryUrl(dictionary.getType(), dictionary.getPath(), dictionary.getLanguages());
				}
				hideProgressBar();
			}

			@Override
			public void onInterrupted() {
				forgetThread();
			}


			@Override
			public void onException(final DictionaryException exception,
					final boolean mayIncludeCompressedDictionary) {
				forgetThread();
				hideProgressBar();
				if (exitSilently) {
					return;
				}
				if (mayIncludeCompressedDictionary) {
					showDialogAndFail(DialogHelper.ID_WARN_EXTRACT_DICTIONARY);
				} else if (Preferences.isFirstRun()) {
					showDialogAndFail(DialogHelper.ID_FIRST_RUN);
				} else {
					showDialogAndFail(DialogHelper.ID_DICTIONARY_NOT_FOUND);
				}
			}

			/**
			 * Hides the progress bar. Can be called from non-UI threads.
			 */
			private void hideProgressBar() {
				updateHandler.post(new Runnable() {
					@Override
					public void run() {
						setProgressBarIndeterminateVisibility(false);
					}
				});
			}

			/**
			 * Shows the specified dialog in the UI and posts the onFailure
			 * runnable.
			 *
			 * @param dialog
			 *            the id of the dialog to show
			 */
			private void showDialogAndFail(final int dialog) {
				updateHandler.post(new Runnable() {
					@Override
					public void run() {
						showDialog(dialog);
						hideProgressBar();
					}
				});
			}

			private void forgetThread() {
				synchronized (loadDictionaryThreadSync) {
					// TODO: handle multiple threads
					if (isLoadDictionaryThreadActive()) {
//						loadDictionaryThread.setOnThreadResultListener(null);
						loadDictionaryThread = null;
					}
				}
			}
		};
	}

	private void createDictionariesAdapterAndConnectToList() {
		DictionariesAdapter adapter = new DictionariesAdapter(dictionaries);

		ListView listView = (ListView) findViewById(R.id.loaded_dictionary_list);
		listView.setAdapter(adapter);

		DataSetObserver dataSetObserver = new DataSetObserver() {
			@Override
			public void onChanged() {
				if (!isDictionaryAvailable()) {
					translationsAdapter.clearData();
					return;
				}

				// start search according to intent
				final String translationInput = ((TextView) findViewById(R.id.TranslationInput))
						.getText().toString();
				final String query = getIntent().getStringExtra(SearchManager.QUERY);
				final boolean hasSearchIntent = Intent.ACTION_SEARCH.equals(getIntent()
						.getAction());
				if (hasSearchIntent && translationInput.equals(query)) {
					getIntent().removeExtra(SearchManager.QUERY);
					startTranslation();
				} else if (Preferences.getSearchAsYouType()
						&& translationInput.length() > 0) {
					startTranslation();
				}
			}
		};
		adapter.registerDataSetObserver(dataSetObserver);
		dataSetObserver.onChanged();
	}

	private void updateActiveLanguagesCount() {
		int size = dictionaries.getLoadedLanguagePairs();
		TextView openDictionaryView = (TextView) findViewById(R.id.openDictionaryMenu);
		String htmlString = getResources().getQuantityString(R.plurals.open_dictionary_drawer, size, size);
		openDictionaryView.setText(Html.fromHtml(htmlString));
	}

	/**
	 * Start the thread to load a new dictionary and update the view.
	 *
	 * @param inputStreamAccess
	 *            the input stream to load the dictionary
	 * @param dictionaryType
	 *            the type of the dictionary
	 * @param dictionaryPath
	 *            the path of the dictionary
	 */
	private void startLoadDictionary(
			final DfMInputStreamAccess inputStreamAccess,
			final DictionaryType dictionaryType, final String dictionaryPath) {
		startLoadDictionary(inputStreamAccess, dictionaryType, dictionaryPath,
				null, false);
	}

	/**
	 * The watcher of the search input field to show the search options when the
	 * search input changes.
	 */
	private final TextWatcher textWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(final Editable s) {
			updateInputTextAlignment();
			showSearchOptions();
			if (Preferences.getSearchAsYouType()) {
				final EditText text = (EditText) findViewById(R.id.TranslationInput);
				final String searchString = text.getText().toString();
				final boolean isInputEmpty = searchString.trim().length() == 0;
				if (isInputEmpty) {
					return;
				}
				startTranslation();
			}
		}

		@Override
		public void beforeTextChanged(final CharSequence s, final int start,
				final int count, final int after) {
		}

		@Override
		public void onTextChanged(final CharSequence s, final int start,
				final int before, final int count) {
		}

	};

	/**
	 * Updates the alignment of the translation input text field.
	 */
	private void updateInputTextAlignment() {

		final EditText text = (EditText) findViewById(R.id.TranslationInput);

		// calculate and set the max height of the input box
		final double factor;
		if (findViewById(R.id.HeadingLayout).getVisibility() == View.VISIBLE) {
			// smaller max height if heading is visible
			factor = 0.3;
		} else {
			factor = 0.5;
		}
		final DisplayMetrics display = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(display);
		text.setMaxHeight((int) (display.heightPixels * factor));
	}

	/**
	 * Hides the search options if appropriate.
	 */
	private void hideSearchOptions() {
		hideSearchOptions(false);
	}

	/**
	 * Hides the search options.
	 *
	 * @param force
	 *            specifies if the search options should always be hidden
	 */
	private void hideSearchOptions(final boolean force) {
		if (force) {
			((LinearLayout) findViewById(R.id.selectLanguagesLayout))
					.setVisibility(View.GONE);
			return;
		}
		final EditText inputEditText = (EditText) findViewById(R.id.TranslationInput);
		if (inputEditText != null && !inputEditText.hasFocus()) {
			((LinearLayout) findViewById(R.id.selectLanguagesLayout))
					.setVisibility(View.GONE);
		}
	}

	/**
	 * Shows the search options.
	 */
	private void showSearchOptions() {
		((LinearLayout) findViewById(R.id.selectLanguagesLayout))
				.setVisibility(View.VISIBLE);
	}

	/**
	 * Listener to react on button clicks.
	 */
	private final OnClickListener clickListener = new OnClickListener() {
		@Override
		public void onClick(final View button) {
			final EditText inputText = (EditText) findViewById(R.id.TranslationInput);
			switch (button.getId()) {
			case R.id.StartTranslation:
				showDialog(DialogHelper.ID_SEARCHING);
				final boolean hasStarted = startTranslation();
				if (hasStarted) {
					hideSearchOptions(true);
				} else {
					dismissDialog(DialogHelper.ID_SEARCHING);
				}
				break;

			case R.id.TranslationInput:
				showSearchOptions();
				break;

			case R.id.ClearInput:
				inputText.setText("");
				showSoftKeyboard();
				break;

			default:
				break;
			}
		}
	};

	/**
	 * Listener to react on actions in the search input field.
	 */
	private final OnEditorActionListener editorActionListener = new OnEditorActionListener() {

		@Override
		public boolean onEditorAction(final TextView view, final int actionId,
				final KeyEvent event) {
			if (view == findViewById(R.id.TranslationInput)) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					return false;
				}
				if (Preferences.getSearchAsYouType()) {
					return false;
				}
				startTranslation();
				return true;
			}
			return false;
		}

	};

	/**
	 * Listener to react on changed focus to show or hide the search options.
	 */
	private final OnFocusChangeListener focusChangeListener = new OnFocusChangeListener() {
		@Override
		public void onFocusChange(final View view, final boolean hasFocus) {
			if (view == findViewById(R.id.TranslationInput)) {
				updateInputTextAlignment();
				if (hasFocus) {
					showSearchOptions();
				}
			} else if (view == findViewById(R.id.translationsListView)) {
				if (hasFocus) {
					hideSearchOptions();
				}
			}
		}
	};

	/**
	 * Listener to react on touch events to show or hide the search options.
	 */
	private final OnTouchListener touchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(final View view, final MotionEvent event) {
			if (view == findViewById(R.id.translationsListView)) {
				ListView list = (ListView) findViewById(R.id.translationsListView);
				if (list.getCount() == 0) {
					return false;
				}
				hideSearchOptions(true);
				hideSoftKeyboard();
			} else if (view == findViewById(R.id.TranslationInput)) {
				showSearchOptions();
				updateInputTextAlignment();
			}
			return false;
		}

	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onKeyUp(final int keyCode, final KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			final EditText text = (EditText) findViewById(R.id.TranslationInput);
			if (text.isFocused()) {
				text.selectAll();
			}
			showSoftKeyboard();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	};

	/**
	 * Handler to process messages from non-GUI threads that need to interact
	 * with the view.
	 */
	private final Handler updateHandler = new Handler() {
		@Override
		public void handleMessage(final Message message) {
			switch (message.what) {
			case THREAD_ERROR_MESSAGE:
				handleTranslationThreadError(message);
				break;

			default:
				break;
			}
			super.handleMessage(message);
		}

		private void handleTranslationThreadError(final Message message) {
			final String translationErrorMessage = getString(
					R.string.msg_translation_error, (String) message.obj);
			DialogHelper.setTranslationErrorMessage(translationErrorMessage);
			showDialog(DialogHelper.ID_TRANSLATE_ERROR);
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options, menu);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		final MenuItem item = menu.findItem(R.id.itemStarred);
		item.setVisible(Preferences.getIsStarredWordsEnabled());
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemAbout:
			Intent aboutScreenIntent = new Intent(DictionaryForMIDs.this,
					AboutScreen.class);
			// Pass information of active dictionaries to about activity if available
			String infoText = getActiveDictionariesInfoText();
			if (infoText != null && infoText.length() > 0) {
				aboutScreenIntent.putExtra(BUNDLE_DICTIONARY_ABOUT_TEXT, infoText);
			}
			startActivity(aboutScreenIntent);
			return true;

		case R.id.itemDictionary:
			startChooseDictionaryActivity();
			return true;

		case R.id.itemPreferences:
			Intent settingsIntent = new Intent(DictionaryForMIDs.this,
					Preferences.class);
			startActivity(settingsIntent);
			return true;

		case R.id.itemHelp:
			Intent helpIntent = new Intent(DictionaryForMIDs.this,
					HelpScreen.class);
			startActivity(helpIntent);
			return true;

		case R.id.itemStarred:
			Intent starredIntent = new Intent(DictionaryForMIDs.this, StarredWordsList.class);
			startActivityForResult(starredIntent, REQUEST_STARRED_WORDS);
			return true;

		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}

	private String getActiveDictionariesInfoText() {
		StringBuilder dictionariesInfo = new StringBuilder();
		for (Dictionary dictionary : dictionaries) {
			if (dictionary.getFile() == null) {
				// Ignore unloaded dictionaries
				continue;
			}
			if (dictionary.getFile().infoText.length() == 0) {
				// Ignore dictionaries with empty info
				continue;
			}
			if (dictionariesInfo.length() > 0) {
				// Add line break between dictionaries
				dictionariesInfo.append("\n\n");
			}
			dictionariesInfo.append(dictionary.getFile().dictionaryAbbreviation);
			dictionariesInfo.append(":\n");
			dictionariesInfo.append(dictionary.getFile().infoText);
		}
		return dictionariesInfo.toString();
	}

	/**
	 * Starts a translation if possible and updates the view.
	 */
	private boolean startTranslation() {
		EditText text = (EditText) findViewById(R.id.TranslationInput);
		final String searchString = text.getText().toString().trim();
		final StringBuffer searchWord = new StringBuffer(searchString);

		if (searchWord.length() == 0) {
			Toast.makeText(getBaseContext(), R.string.msg_enter_word_first,
					Toast.LENGTH_LONG).show();
			return false;
		}
		if (!isDictionaryAvailable()) {
			Toast.makeText(getBaseContext(),
					R.string.msg_load_dictionary_first, Toast.LENGTH_LONG)
					.show();
			return false;
		}

		applySearchModeModifiers(searchWord);

		cancelActiveTranslation();

		// TODO: handle multiple dictionaries

		TranslationParametersBatch batchParameters = new TranslationParametersBatch();
		for (Dictionary dictionary : dictionaries) {
			final DictionaryDataFile file = dictionary.getFile();
			if (file == null) {
				continue;
			}
			for (int i = 0; i < file.supportedLanguages.length; i++) {
				for (int j = 0; j < file.supportedLanguages.length; j++) {
					if (i == j || !dictionary.isPairSelected(i, j)) {
						continue;
					}

					boolean[] inputLanguages = new boolean[file.supportedLanguages.length];
					boolean[] outputLanguages = new boolean[file.supportedLanguages.length];

					inputLanguages[i] = true;
					outputLanguages[j] = true;

					TranslationParameters translationParameters = new TranslationParameters(file,
							searchWord.toString().trim(), inputLanguages, outputLanguages, true,
							Preferences.getMaxResults(), Preferences.getSearchTimeout()
							* MILLISECONDS_IN_A_SECOND);

					batchParameters.addTranslationParameters(translationParameters);

				}
			}
		}
		translations.startTranslation(batchParameters);


		return true;
	}

	/**
	 * Checks if there currently is a dictionary loaded and available for
	 * searching.
	 *
	 * @return true if a dictionary is available
	 */
	private boolean isDictionaryAvailable() {
		if (dictionaries.isEmpty()) {
			return false;
		}

		for (Dictionary dictionary : dictionaries) {
			if (dictionary.getFile() == null) {
				continue;
			}
			final boolean isLanguageAvailable = dictionary.getFile().numberOfAvailableLanguages > 0;
			if (isLanguageAvailable) {
				return true;
			}
		}

		return false;
	}

	public void cancelActiveTranslation() {
		translations.cancelTranslation();
	}

	/**
	 * Hides the soft keyboard.
	 */
	private void hideSoftKeyboard() {
		InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		manager.hideSoftInputFromWindow(findViewById(R.id.TranslationInput)
				.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS, null);
		// move focus away from keyboard to list
		findViewById(R.id.translationsListView).requestFocus();
	}

	/**
	 * Shows the soft keyboard for inputing translation terms.
	 */
	private void showSoftKeyboard() {
		final View input = findViewById(R.id.TranslationInput);
		input.requestFocus();
		final InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		manager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
	}

	/**
	 * Apply search modifiers on the search word.
	 *
	 * @param searchWord
	 *            the search input to apply the modifiers on
	 */
	public static void applySearchModeModifiers(final StringBuffer searchWord) {
		if (hasSearchModifiers(searchWord)) {
			return;
		}

		if (Preferences.getFindEntryBeginningWithSearchTerm()) {
			makeWordMatchBeginning(searchWord);
		} else if (Preferences.getFindExactMatch()) {
			if (searchWord.charAt(0) != Util.noSearchSubExpressionCharacter) {
				searchWord.insert(0, "" + Util.noSearchSubExpressionCharacter);
			}
			if (searchWord.charAt(searchWord.length() - 1) != Util.noSearchSubExpressionCharacter) {
				searchWord.append(Util.noSearchSubExpressionCharacter);
			}
		} else if (Preferences.getFindEntryIncludingSearchTerm()) {
			if (searchWord.charAt(0) != Util.wildcardAnySeriesOfCharacter) {
				searchWord.insert(0, "" + Util.wildcardAnySeriesOfCharacter);
			}
			if (searchWord.charAt(searchWord.length() - 1) != Util.wildcardAnySeriesOfCharacter) {
				searchWord.append(Util.wildcardAnySeriesOfCharacter);
			}
		}
	}

	/**
	 * Modifies the given term to match words beginning with the term.
	 *
	 * @param searchWord
	 *            the search term to modify
	 */
	public static void makeWordMatchBeginning(final StringBuffer searchWord) {
		if (searchWord.charAt(searchWord.length() - 1) != Util.wildcardAnySeriesOfCharacter) {
			searchWord.append(Util.wildcardAnySeriesOfCharacter);
		}
	}

	/**
	 * Checks if the given word includes search modifiers.
	 *
	 * @param searchWord
	 *            the word to check
	 * @return true if the word includes search modifiers, false otherwise
	 */
	public static boolean hasSearchModifiers(final StringBuffer searchWord) {
		return searchWord.indexOf("" + Util.noSearchSubExpressionCharacter) >= 0
				|| searchWord.indexOf("" + Util.wildcardAnySeriesOfCharacter) >= 0
				|| searchWord.indexOf("" + Util.wildcardAnySingleCharacter) >= 0;
	}

	/**
	 * Creates the TranslationParamters from the current state.
	 *
	 * @param searchTerm
	 *            the term to search for
	 * @param numberOfAvailableLanguages
	 *            the number of available languages
	 * @return an object representing the current translation parameters
	 */
//	public TranslationParameters getTranslationParameters(final DictionaryDataFile file, final String searchTerm,
//			final int numberOfAvailableLanguages) {
//		return getTranslationParameters(file, searchTerm, numberOfAvailableLanguages, true);
//	}

//	public TranslationParameters getTranslationParameters(final DictionaryDataFile file, final String searchTerm,
//			final int numberOfAvailableLanguages, boolean executeInBackground) {
//		boolean[] inputLanguages = new boolean[numberOfAvailableLanguages];
//		boolean[] outputLanguages = new boolean[numberOfAvailableLanguages];
//		for (int i = 0; i < numberOfAvailableLanguages; i++) {
//			inputLanguages[i] = false;
//			outputLanguages[i] = false;
//		}
//		Spinner languages = (Spinner) findViewById(R.id.selectLanguages);
//		Preferences.setSelectedLanguageIndex(languages
//				.getSelectedItemPosition());
//		int[] indices = (int[]) languages.getSelectedItem();
//		inputLanguages[indices[0]] = true;
//		outputLanguages[indices[1]] = true;
//		TranslationParameters translationParametersObj = new TranslationParameters(file,
//				searchTerm.trim(), inputLanguages, outputLanguages, executeInBackground,
//				Preferences.getMaxResults(), Preferences.getSearchTimeout()
//						* MILLISECONDS_IN_A_SECOND);
//		return translationParametersObj;
//	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(final int id) {
		Dialog dialog = dialogHelper.onCreateDialog(id);
		if (dialog == null) {
			dialog = super.onCreateDialog(id);
		}
		return dialog;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPrepareDialog(final int id, final Dialog dialog) {
		dialogHelper.onPrepareDialog(id, dialog);
		super.onPrepareDialog(id, dialog);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			((LinearLayout) findViewById(R.id.HeadingLayout))
					.setOrientation(LinearLayout.HORIZONTAL);
			updateInputTextAlignment();
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			((LinearLayout) findViewById(R.id.HeadingLayout))
					.setOrientation(LinearLayout.VERTICAL);
			updateInputTextAlignment();
		} else {
			super.onConfigurationChanged(newConfig);
		}
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v,
			final ContextMenuInfo contextMenuInfo) {
		// get selected translation
		final ListView list = (ListView) findViewById(R.id.translationsListView);
		final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) contextMenuInfo;
		final SingleTranslationExtension translation = (SingleTranslationExtension) list
				.getItemAtPosition(menuInfo.position);

		// load texts from translation
		String fromText = null;
		String toTexts = null;
		try {
			fromText = translation.getFromTextAsString();
			toTexts = translation.getToTextsAsString("\n");
		} catch (DictionaryException e) {
			Log.d(LOG_TAG, "Parsing error", e);
			Toast.makeText(getBaseContext(), R.string.msg_parsing_error, Toast.LENGTH_SHORT).show();
		}

		initializeMenu(menu, fromText, toTexts);
		Log.d(LOG_TAG, "onCreateContextMenu");
	}

	/**
	 * Initializes the given context menu for copying translationsAdapter.
	 *
	 * @param menu
	 *            the menu to initialize
	 * @param fromText
	 *            the fromText to use
	 * @param toTexts
	 *            the toTexts to use
	 */
	private void initializeMenu(final ContextMenu menu, final String fromText, final String toTexts) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.translation_context, menu);

		final ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

		final MenuItem copyFromWord = menu.findItem(R.id.itemCopyFromWord);
		if (fromText == null || fromText.length() == 0) {
			copyFromWord.setVisible(false);
			copyFromWord.setEnabled(false);
		} else {
			final String copyFromWordTitle = getString(R.string.copy_word, fromText);
			copyFromWord.setTitle(copyFromWordTitle);
			copyFromWord.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					clipboardManager.setText(fromText);
					return true;
				}
			});
		}

		final MenuItem copyToWord = menu.findItem(R.id.itemCopyToWord);
		if (toTexts == null || toTexts.length() == 0) {
			copyToWord.setVisible(false);
			copyToWord.setEnabled(false);
		} else {
			final String copyToWordTitle = getString(R.string.copy_word, toTexts);
			copyToWord.setTitle(copyToWordTitle);
			copyToWord.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					clipboardManager.setText(toTexts);
					return true;
				}
			});
		}

		final MenuItem copyAll = menu.findItem(R.id.itemCopyAll);
		copyAll.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				clipboardManager.setText(fromText + "\n" + toTexts);
				return true;
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		if (requestCode == REQUEST_DICTIONARY_PATH) {
			switch (resultCode) {
			case RESULT_OK:
				loadDictionaryFromIntent(data);
				break;

			case RESULT_EXIT:
				finish();
				break;

			default:
				break;
			}
		} else if (requestCode == REQUEST_STARRED_WORDS) {
			// reload data set to push changes to stars to the view
			translationsAdapter.notifyDataSetChanged();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Loads a dictionary specified by an intent in the GUI thread.
	 *
	 * @param data
	 *            the intent specifying a dictionary
	 */
	public void loadDictionaryFromRemoteIntent(final Intent data) {
		updateHandler.post(new Runnable() {
			@Override
			public void run() {
				loadDictionaryFromIntent(data);
			}
		});
	}

	/**
	 * Loads a dictionary that is specified by an intent.
	 *
	 * @param data
	 *            the intent specifying a dictionary
	 */
	private void loadDictionaryFromIntent(final Intent data) {
		Bundle extras = data.getExtras();
		String filePath = extras.getString(FileList.FILE_PATH);
		String zipPath = extras.getString(FileList.ZIP_PATH);
		String assetPath = extras.getString(DictionaryList.ASSET_PATH);
		if (filePath != null) {
			startLoadDictionary(new FileDfMInputStreamAccess(filePath),
					DictionaryType.DIRECTORY, filePath);
		} else if (assetPath != null) {
			startLoadDictionary(new AssetDfMInputStreamAccess(this.getAssets(), assetPath),
					DictionaryType.INCLUDED, assetPath);
		} else if (zipPath != null) {
			NativeZipInputStreamAccess inputStreamAccess;
			inputStreamAccess = new NativeZipInputStreamAccess(zipPath);
			startLoadDictionary(inputStreamAccess, DictionaryType.ARCHIVE,
					zipPath);
		}
	}

	/**
	 * Starts the ChooseDictionary activity.
	 */
	public void startChooseDictionaryActivity() {
		startChooseDictionaryActivity(false);
	}

	/**
	 * Starts the ChooseDictionary activity and optionally set the installation
	 * tab as default tab.
	 *
	 * @param showDictionaryInstallation
	 *            true if the installation tab should be the default tab
	 */
	private void startChooseDictionaryActivity(
			final boolean showDictionaryInstallation) {
		startChooseDictionaryActivity(DictionaryForMIDs.this, showDictionaryInstallation);
	}

	/**
	 * Starts the ChooseDictionary activity and optionally set the installation
	 * tab as default tab.
	 *
	 * @param activity
	 *            the activity to use
	 * @param showDictionaryInstallation
	 *            true if the installation tab should be the default tab
	 */
	public static void startChooseDictionaryActivity(final Activity activity,
			final boolean showDictionaryInstallation) {
		startChooseDictionaryActivity(activity, showDictionaryInstallation, false);
	}

	/**
	 * Starts the ChooseDictionary activity and optionally set the installation
	 * tab as default tab.
	 *
	 * @param activity
	 *            the activity to use
	 * @param showDictionaryInstallation
	 *            true if the installation tab should be the default tab
	 * @param autoInstallDictionary
	 *            true if the auto-install-dictionary should be installed now
	 */
	public static void startChooseDictionaryActivity(final Activity activity,
			final boolean showDictionaryInstallation,
			final boolean autoInstallDictionary) {
		int autoInstallId = 0;
		if (autoInstallDictionary) {
			autoInstallId = Preferences.getAutoInstallDictionaryId();
		}
		final Intent i = new Intent(activity.getApplicationContext(),
				ChooseDictionary.class);
		i.putExtra(ChooseDictionary.BUNDLE_SHOW_DICTIONARY_INSTALLATION,
				showDictionaryInstallation);
		i.putExtra(InstallDictionary.INTENT_AUTO_INSTALL_ID, autoInstallId);
		activity.startActivityForResult(i, REQUEST_DICTIONARY_PATH);
	}

	private final TranslationsObserver translationsObserver = new TranslationsObserver();

	/**
	 * Observer to react on changes to the translation filter state.
	 */
	private final Observer onFilterStateChangedObserver = new Observer() {
		@Override
		public void update(final Observable observable, final Object state) {
			if (!Preferences.getSearchAsYouType()) {
				return;
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final ProgressBar bar = (ProgressBar) findViewById(R.id.ProgressBarSearchAsYouType);
					final boolean isFilterActive = (Boolean) state;
					if (isFilterActive) {
						if (bar != null) {
							bar.setVisibility(View.VISIBLE);
						}
					} else {
						// try hiding the progress bar after filter completed
						if (bar != null) {
							bar.setVisibility(View.GONE);
						}
						// try hiding the search dialog
						try {
							dismissDialog(DialogHelper.ID_SEARCHING);
						} catch (IllegalArgumentException e) {
							// ignore
						}
					}
				}
			});
		}
	};

	private final OnClickListener loadDictionaryClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			startChooseDictionaryActivity();
		}
	};
}
