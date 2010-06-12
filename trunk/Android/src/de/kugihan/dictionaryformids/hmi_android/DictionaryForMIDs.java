/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android;

import java.util.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView.OnEditorActionListener;
import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.AssetDfMInputStreamAccess;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.DfMInputStreamAccess;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.FileDfMInputStreamAccess;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.NativeZipInputStreamAccess;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.general.Util;
import de.kugihan.dictionaryformids.hmi_android.Preferences.DictionaryType;
import de.kugihan.dictionaryformids.hmi_android.data.AndroidUtil;
import de.kugihan.dictionaryformids.hmi_android.data.LanguageSpinnerAdapter;
import de.kugihan.dictionaryformids.hmi_android.data.SingleTranslationHelper;
import de.kugihan.dictionaryformids.hmi_android.data.Translations;
import de.kugihan.dictionaryformids.hmi_android.service.DictionaryInstallationService;
import de.kugihan.dictionaryformids.hmi_android.thread.LoadDictionaryThread;
import de.kugihan.dictionaryformids.hmi_android.thread.LoadDictionaryThread.OnThreadResultListener;
import de.kugihan.dictionaryformids.hmi_android.view_helper.DialogHelper;
import de.kugihan.dictionaryformids.translation.SingleTranslation;
import de.kugihan.dictionaryformids.translation.TranslationExecution;
import de.kugihan.dictionaryformids.translation.TranslationExecutionCallback;
import de.kugihan.dictionaryformids.translation.TranslationParameters;
import de.kugihan.dictionaryformids.translation.TranslationResult;

/**
 * DictionaryForMIDs is the main Activity of the application. Most of the user
 * interaction will be handled by this class.
 * 
 */
public final class DictionaryForMIDs extends Activity {

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
		 * The current list of translations.
		 */
		private final Translations translations;

		/**
		 * Constructs a new instance and initalizes all members.
		 * 
		 * @param thread
		 *            the current load dictionary thread
		 * @param translations
		 *            the current translations
		 */
		public NonConfigurationInstance(final LoadDictionaryThread thread,
				final Translations translations) {
			this.thread = thread;
			this.translations = translations;
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
		 * Returns the translations.
		 * 
		 * @return the translations
		 */
		public Translations getTranslations() {
			return translations;
		}
	}

	/**
	 * Specifier of the dictionary property that holds the number of available
	 * languages.
	 */
	private static final String DICTIONARY_PROPERTY_NUMBER_OF_AVAILABLE_LANGUAGES = "numberOfAvailableLanguages";

	/**
	 * The key of an integer specifying the heading's visibility in a bundle.
	 */
	private static final String BUNDLE_HEADING_VISIBILITY = "headingVisibility";

	/**
	 * The key of a string specifying the status message in a bundle.
	 */
	private static final String BUNDLE_STATUS_MESSAGE = "statusMessage";

	/**
	 * The key of an integer specifying the selected language in a bundle.
	 */
	private static final String BUNDLE_SELECTED_LANGUAGE = "selectedLanguage";

	/**
	 * The key of an integer specifying the visibility of the search options in
	 * a bundle.
	 */
	private static final String BUNDLE_SEARCH_OPTIONS_VISIBILITY = "searchOptionsVisibility";
	
	/**
	 * The key of an integer specifying the number of currently displayed translations.
	 */
	private static final String BUNDLE_NUMBER_OF_TRANSLATIONS = "numberOfTranslations";

	/**
	 * The tag used for log messages.
	 */
	public static final String LOG_TAG = "MY";

	/**
	 * The number of milliseconds in one second.
	 */
	private static final int MILLISECONDS_IN_A_SECOND = 1000;

	/**
	 * The message id for new translation results.
	 */
	private static final int THREAD_NEW_TRANSLATION_RESULT = 1;

	/**
	 * The message id for deleting previous translation results.
	 */
	private static final int THREAD_DELETE_PREVIOUS_TRANSLATION_RESULT = 2;

	/**
	 * The message id for translation errors.
	 */
	public static final int THREAD_ERROR_MESSAGE = 3;

	/**
	 * The request code identifying a {@link ChooseDictionary} activity.
	 */
	private static final int REQUEST_DICTIONARY_PATH = 0;

	/**
	 * The handle of the thread that loads the new dictionary or null.
	 */
	private LoadDictionaryThread loadDictionaryThread = null;

	/**
	 * The object used to synchronize access on the load dictionary thread.
	 */
	private final Object loadDictionaryThreadSync = new Object();

	/**
	 * The data of the translation results list.
	 */
	private Translations translations = new Translations();

	/**
	 * The helper for all dialogs.
	 */
	private DialogHelper dialogHelper;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		outState.putInt(BUNDLE_SEARCH_OPTIONS_VISIBILITY,
				((LinearLayout) findViewById(R.id.selectLanguagesLayout))
						.getVisibility());
		outState.putInt(BUNDLE_SELECTED_LANGUAGE,
				((Spinner) findViewById(R.id.selectLanguages))
						.getSelectedItemPosition());
		outState.putCharSequence(BUNDLE_STATUS_MESSAGE,
				((TextView) findViewById(R.id.output)).getText());
		outState.putInt(BUNDLE_HEADING_VISIBILITY,
				((LinearLayout) findViewById(R.id.HeadingLayout))
						.getVisibility());
		outState.putInt(BUNDLE_NUMBER_OF_TRANSLATIONS, translations.getCount());
		super.onSaveInstanceState(outState);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		final Spinner spinner = (Spinner) findViewById(R.id.selectLanguages);
		if (DictionaryDataFile.supportedLanguages != null) {
			final LanguageSpinnerAdapter languageSpinnerAdapter = new LanguageSpinnerAdapter(
					DictionaryDataFile.supportedLanguages);
			spinner.setAdapter(languageSpinnerAdapter);
		} else {
			// When the previously loaded dictionary is not available any more
			// (because Android used the memory while we were in background)
			// we just reload the last dictionary
			loadLastUsedDictionary(false);
		}

		loadLastNonConfigurationInstance();

		final int selectedLanguage = savedInstanceState
				.getInt(BUNDLE_SELECTED_LANGUAGE);
		spinner.setSelection(selectedLanguage);
		
		final int previousNumberOfTranslations = savedInstanceState
				.getInt(BUNDLE_NUMBER_OF_TRANSLATIONS);

		CharSequence statusMessage = savedInstanceState
				.getCharSequence(BUNDLE_STATUS_MESSAGE);
		if (translations.isEmpty() && previousNumberOfTranslations > 0) {
			// status could be something like 5 results found
			// so clear it if there are no translations available
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

		super.onRestoreInstanceState(savedInstanceState);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		LoadDictionaryThread tempThread = null;
		synchronized (loadDictionaryThreadSync) {
			if (loadDictionaryThread != null) {
				loadDictionaryThread.setOnThreadResultListener(null);
				tempThread = loadDictionaryThread;
				loadDictionaryThread = null;
			}
		}
		return new NonConfigurationInstance(tempThread, translations);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// set up preferences
		Preferences.attachToContext(getApplicationContext());
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		preferences
				.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
		
		// set preferred locale for application
		setCustomLocale(Preferences.getLanguageCode());

		setContentView(R.layout.main);

		dialogHelper = DialogHelper.getInstance(this);

		final TextView translationInput = (TextView) findViewById(R.id.TranslationInput);
		translationInput.setOnFocusChangeListener(focusChangeListener);
		translationInput.setOnClickListener(clickListener);
		translationInput.setOnTouchListener(touchListener);
		translationInput.setOnEditorActionListener(editorActionListener);
		translationInput.addTextChangedListener(textWatcher);

		final ListView translationListView = (ListView) findViewById(R.id.translationsListView);
		translationListView.setAdapter(translations);
		translationListView.setOnFocusChangeListener(focusChangeListener);
		translationListView.setOnScrollListener(scrollListener);
		translationListView.setOnTouchListener(touchListener);
		translationListView
				.setOnCreateContextMenuListener(createContextMenuListener);

		((ImageButton) findViewById(R.id.StartTranslation))
				.setOnClickListener(clickListener);
		((ImageButton) findViewById(R.id.swapLanguages))
				.setOnClickListener(clickListener);

		final Spinner languageSpinner = (Spinner) findViewById(R.id.selectLanguages);
		languageSpinner.setAdapter(new LanguageSpinnerAdapter());
		languageSpinner.setOnItemSelectedListener(languageSelectedListener);
		languageSpinner.setOnTouchListener(languagesTouchListener);

		TranslationExecution
				.setTranslationExecutionCallback(translationCallback);

		Util util = Util.getUtil();
		if (util instanceof AndroidUtil) {
			final AndroidUtil androidUtil = (AndroidUtil) util;
			androidUtil.setHandler(updateHandler);
		} else {
			util = new AndroidUtil(updateHandler);
			Util.setUtil(util);
		}

		if (savedInstanceState == null) {
			if (Preferences.isFirstRun()) {
				showDialog(DialogHelper.ID_FIRST_RUN);
			} else {
				final boolean silent = processIntent(getIntent());
				loadLastUsedDictionary(silent);
			}
		}
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
	public void loadLastUsedDictionary(final boolean silent) {
		DfMInputStreamAccess inputStreamAccess = null;
		DictionaryType dictionaryType;

		if (Preferences.getLoadIncludedDictionary()) {
			inputStreamAccess = new AssetDfMInputStreamAccess(this, Preferences
					.getDictionaryPath());
			dictionaryType = DictionaryType.INCLUDED;
		} else if (Preferences.getLoadDirectoryDictionary()) {
			inputStreamAccess = new FileDfMInputStreamAccess(Preferences
					.getDictionaryPath());
			dictionaryType = DictionaryType.DIRECTORY;
		} else if (Preferences.getLoadArchiveDictionary()) {
			inputStreamAccess = new NativeZipInputStreamAccess(Preferences
					.getDictionaryPath());
			dictionaryType = DictionaryType.ARCHIVE;
		} else {
			return;
		}
		startLoadDictionary(inputStreamAccess, dictionaryType, Preferences
				.getDictionaryPath(), Preferences.getSelectedLanguageIndex(),
				silent);
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
		if (hasNewDictionary(intent)) {
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
		if (data.getTranslations() != null) {
			translations = data.getTranslations();
			((ListView) findViewById(R.id.translationsListView))
					.setAdapter(translations);
		}
		if (data.getThread() != null) {
			synchronized (loadDictionaryThreadSync) {
				setProgressBarIndeterminateVisibility(true);
				loadDictionaryThread = data.getThread();
				loadDictionaryThread
						.setOnThreadResultListener(createThreadListener(false));
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
				translations.notifyDataSetChanged();
			} else if (key
					.equals(Preferences.PREF_IGNORE_DICTIONARY_TEXT_STYLES)) {
				// push style information change into list items
				translations.notifyDataSetChanged();
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
	 * @param selectedIndex
	 *            the selected language index
	 * @param exitSilently
	 *            true if the thread should not display dialogs
	 */
	private void startLoadDictionary(
			final DfMInputStreamAccess inputStreamAccess,
			final DictionaryType dictionaryType, final String dictionaryPath,
			final int selectedIndex, final boolean exitSilently) {

		// cancel running thread
		if (loadDictionaryThread != null) {
			synchronized (loadDictionaryThread) {
				loadDictionaryThread.interrupt();
			}
		}

		// remove previously shown, loadDictionary-related dialogs
		removeDialog(DialogHelper.ID_DICTIONARY_NOT_FOUND);
		removeDialog(DialogHelper.ID_FIRST_RUN);

		deletePreviousTranslationResult();

		setProgressBarIndeterminateVisibility(true);
		loadDictionaryThread = new LoadDictionaryThread(inputStreamAccess,
				dictionaryType, dictionaryPath, selectedIndex);
		final OnThreadResultListener threadListener = createThreadListener(exitSilently);
		loadDictionaryThread.setOnThreadResultListener(threadListener);
		loadDictionaryThread.start();
	}

	/**
	 * Creates a listener for thread results.
	 * 
	 * @param exitSilently
	 *            true if the thread should exit silently
	 * @return the thread result listener
	 */
	private OnThreadResultListener createThreadListener(
			final boolean exitSilently) {
		return new OnThreadResultListener() {

			@Override
			public void onSuccess(final DictionaryType type, final String path,
					final int selectedIndex) {
				forgetThread();
				final LanguageSpinnerAdapter languageSpinnerAdapter = new LanguageSpinnerAdapter(
						DictionaryDataFile.supportedLanguages);
				updateHandler.post(new Runnable() {
					public void run() {
						((Spinner) findViewById(R.id.selectLanguages))
								.setAdapter(languageSpinnerAdapter);
						showSearchOptions();

						String[] languages = new String[DictionaryDataFile.supportedLanguages.length];
						for (int i = 0; i < DictionaryDataFile.supportedLanguages.length; i++) {
							languages[i] = DictionaryDataFile.supportedLanguages[i].languageDisplayText;
						}
						Preferences.setLoadDictionary(type, path, languages);

						((Spinner) findViewById(R.id.selectLanguages))
								.setSelection(selectedIndex);

					};
				});
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
					if (loadDictionaryThread != null) {
						loadDictionaryThread.setOnThreadResultListener(null);
						loadDictionaryThread = null;
					}
				}
			}
		};
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
				0, false);
	}

	/**
	 * The watcher of the search input field to show the search options when the
	 * search input changes.
	 */
	private final TextWatcher textWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(final Editable s) {
			showSearchOptions();
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
		if (!((EditText) findViewById(R.id.TranslationInput)).hasFocus()) {
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
		public void onClick(final View button) {
			switch (button.getId()) {
			case R.id.StartTranslation:
				startTranslation();
				break;

			case R.id.swapLanguages:
				Spinner languages = (Spinner) findViewById(R.id.selectLanguages);
				int position = languages.getSelectedItemPosition();
				if (position >= 0) {
					int newPosition = ((LanguageSpinnerAdapter) languages
							.getAdapter()).getSwappedPosition(position);
					languages.setSelection(newPosition, false);
				}
				break;

			case R.id.TranslationInput:
				showSearchOptions();
				EditText text = (EditText) findViewById(R.id.TranslationInput);
				text.selectAll();
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
				if (hasFocus) {
					showSearchOptions();
					EditText text = (EditText) findViewById(R.id.TranslationInput);
					text.selectAll();
				}
			} else if (view == findViewById(R.id.selectLanguages)) {
				if (!hasFocus) {
					hideSearchOptions();
				}
			} else if (view == findViewById(R.id.translationsListView)) {
				if (hasFocus) {
					hideSearchOptions();
				}
			}
		}
	};

	/**
	 * Listener to react on scroll events to hide search options.
	 */
	private final OnScrollListener scrollListener = new OnScrollListener() {

		@Override
		public void onScroll(final AbsListView view,
				final int firstVisibleItem, final int visibleItemCount,
				final int totalItemCount) {
			final ListView listView = (ListView) findViewById(R.id.translationsListView);
			if (view == listView && listView.hasFocusable()
					&& listView.getCount() > 0) {
				hideSearchOptions();
			}
		}

		@Override
		public void onScrollStateChanged(final AbsListView view,
				final int scrollState) {
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
			} else if (view == findViewById(R.id.TranslationInput)) {
				showSearchOptions();
				EditText text = (EditText) findViewById(R.id.TranslationInput);
				text.selectAll();
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
			} else {
				text.requestFocus();
			}
			return true;
		}
		return false;
	};

	/**
	 * Handler to process messages from non-GUI threads that need to interact
	 * with the view.
	 */
	private final Handler updateHandler = new Handler() {
		@Override
		public void handleMessage(final Message message) {
			switch (message.what) {
			case THREAD_NEW_TRANSLATION_RESULT:
				handleNewTranslationResult(message);
				break;

			case THREAD_DELETE_PREVIOUS_TRANSLATION_RESULT:
				handleDeletePreviousTranslationResult();
				break;

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

		private void handleDeletePreviousTranslationResult() {
			deletePreviousTranslationResult();
		}

		private void handleNewTranslationResult(final Message message) {
			boolean hideSearchOptions = true;
			final TranslationResult translationResult = (TranslationResult) message.obj;
			final TextView output = (TextView) findViewById(R.id.output);
			if (translationResult.translationBreakOccurred) {
				switch (translationResult.translationBreakReason) {
				case TranslationResult.BreakReasonCancelMaxNrOfHitsReached:
					output.setText(getString(R.string.results_found_maximum,
							translationResult.numberOfFoundTranslations()));
					break;

				case TranslationResult.BreakReasonCancelReceived:
					output.setText(getString(R.string.results_found_cancel,
							translationResult.numberOfFoundTranslations()));
					break;

				case TranslationResult.BreakReasonMaxExecutionTimeReached:
					output.setText(getString(R.string.results_found_timeout,
							translationResult.numberOfFoundTranslations()));
					if (Preferences.getLoadArchiveDictionary()
							&& Preferences.getWarnOnTimeout()) {
						showDialog(DialogHelper.ID_SUGGEST_DIRECTORY);
					}
					break;

				default:
					throw new IllegalStateException();
				}
			} else if (translationResult.numberOfFoundTranslations() == 0) {
				output.setText(R.string.no_results_found);
				hideSearchOptions = false;
			} else {
				if (translationResult.numberOfFoundTranslations() == 1) {
					output.setText(R.string.results_found_one);
				} else {
					output.setText(getString(R.string.results_found,
							translationResult.numberOfFoundTranslations()));
				}
			}
			// show results
			translations.newTranslationResult(translationResult);
			// hide search options if results have been found
			if (hideSearchOptions) {
				hideSearchOptions(true);
			} else {
				showSearchOptions();
			}
			// hide heading
			((LinearLayout) findViewById(R.id.HeadingLayout))
					.setVisibility(View.GONE);
			// scroll to top
			((ListView) findViewById(R.id.translationsListView))
					.setSelectionFromTop(0, 0);

			// close search dialog
			try {
				dismissDialog(DialogHelper.ID_SEARCHING);
			} catch (IllegalArgumentException e) {
				Log.v(LOG_TAG, "IllegelArgumentException: " + e);
			}
		}
	};

	/**
	 * Clears previous translation results form the view.
	 */
	private void deletePreviousTranslationResult() {
		TextView output;
		output = (TextView) findViewById(R.id.output);
		output.setText("");
		translations.deletePreviousTranslationResult();
	}

	/**
	 * OnPostExecutionListener-object that transforms calls from the translation
	 * thread into messages to the update handler of the user interface.
	 */
	private TranslationExecutionCallback translationCallback = new TranslationExecutionCallback() {

		@Override
		public void deletePreviousTranslationResult() {
			Message message = new Message();
			message.what = THREAD_DELETE_PREVIOUS_TRANSLATION_RESULT;
			DictionaryForMIDs.this.updateHandler.sendMessage(message);
		}

		@Override
		public void newTranslationResult(
				final TranslationResult resultOfTranslation) {
			Message message = new Message();
			message.what = THREAD_NEW_TRANSLATION_RESULT;
			message.obj = resultOfTranslation;
			DictionaryForMIDs.this.updateHandler.sendMessage(message);
		}

	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		switch (item.getItemId()) {
		case R.id.itemCopyAll:
			clipboardManager.setText(SingleTranslationHelper.getAll());
			break;

		case R.id.itemCopyFromWord:
			clipboardManager.setText(SingleTranslationHelper.getFromRow());
			break;

		case R.id.itemCopyToWord:
			clipboardManager.setText(SingleTranslationHelper.getToRows());
			break;

		default:
			return super.onContextItemSelected(item);
		}
		return true;
	}

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
	public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemAbout:
			Intent aboutScreenIntent = new Intent(DictionaryForMIDs.this,
					AboutScreen.class);
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

		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}

	/**
	 * Starts a translation if possible and updates the view.
	 */
	private void startTranslation() {
		EditText text = (EditText) findViewById(R.id.TranslationInput);
		final String searchString = text.getText().toString().trim();
		StringBuffer searchWord = new StringBuffer(searchString);
		if (searchWord.length() == 0) {
			Toast.makeText(getBaseContext(), R.string.msg_enter_word_first,
					Toast.LENGTH_LONG).show();
			return;
		}
		if (DictionaryDataFile.numberOfAvailableLanguages == 0) {
			Toast.makeText(getBaseContext(),
					R.string.msg_load_dictionary_first, Toast.LENGTH_LONG)
					.show();
			return;
		}

		applySearchModeModifiers(searchWord);

		Util util = Util.getUtil();
		int numberOfAvailableLanguages;
		try {
			numberOfAvailableLanguages = util
					.getDictionaryPropertyInt(DICTIONARY_PROPERTY_NUMBER_OF_AVAILABLE_LANGUAGES);
		} catch (DictionaryException e) {
			Toast.makeText(getBaseContext(), R.string.msg_reload_dictionary,
					Toast.LENGTH_LONG).show();
			return;
		}
		TranslationParameters translationParametersObj = getTranslationParameters(
				searchWord.toString(), numberOfAvailableLanguages);

		hideSoftKeyboard();

		hideSearchOptions(true);

		showDialog(DialogHelper.ID_SEARCHING);
		try {
			TranslationExecution.executeTranslation(translationParametersObj);
		} catch (DictionaryException e) {
			dismissDialog(DialogHelper.ID_SEARCHING);
			Toast.makeText(getBaseContext(),
					getString(R.string.msg_exception, e.getMessage()),
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Hides the soft keyboard.
	 */
	private void hideSoftKeyboard() {
		InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		manager.hideSoftInputFromWindow(findViewById(R.id.TranslationInput)
				.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS, null);
	}

	/**
	 * Apply search modifiers on the search word.
	 * 
	 * @param searchWord
	 *            the search input to apply the modifiers on
	 */
	private void applySearchModeModifiers(final StringBuffer searchWord) {
		if (searchWord.indexOf("" + Util.noSearchSubExpressionCharacter) >= 0
				|| searchWord.indexOf("" + Util.wildcardAnySeriesOfCharacter) >= 0
				|| searchWord.indexOf("" + Util.wildcardAnySingleCharacter) >= 0) {
			return;
		}

		if (Preferences.getFindEntryBeginningWithSearchTerm()) {
			if (searchWord.charAt(0) != Util.noSearchSubExpressionCharacter) {
				searchWord.insert(0, "" + Util.noSearchSubExpressionCharacter);
			}
			if (searchWord.charAt(searchWord.length() - 1) != Util.wildcardAnySeriesOfCharacter) {
				searchWord.append(Util.wildcardAnySeriesOfCharacter);
			}
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
	 * Creates the TranslationParamters from the current state.
	 * 
	 * @param searchTerm
	 *            the term to search for
	 * @param numberOfAvailableLanguages
	 *            the number of available languages
	 * @return an object representing the current translation parameters
	 */
	private TranslationParameters getTranslationParameters(
			final String searchTerm, final int numberOfAvailableLanguages) {
		boolean[] inputLanguages = new boolean[numberOfAvailableLanguages];
		boolean[] outputLanguages = new boolean[numberOfAvailableLanguages];
		for (int i = 0; i < numberOfAvailableLanguages; i++) {
			inputLanguages[i] = false;
			outputLanguages[i] = false;
		}
		Spinner languages = (Spinner) findViewById(R.id.selectLanguages);
		Preferences.setSelectedLanguageIndex(languages
				.getSelectedItemPosition());
		int[] indices = (int[]) languages.getSelectedItem();
		inputLanguages[indices[0]] = true;
		outputLanguages[indices[1]] = true;
		TranslationParameters translationParametersObj = new TranslationParameters(
				searchTerm.trim(), inputLanguages, outputLanguages, true,
				Preferences.getMaxResults(), Preferences.getSearchTimeout()
						* MILLISECONDS_IN_A_SECOND);
		return translationParametersObj;
	}

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
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			((LinearLayout) findViewById(R.id.HeadingLayout))
					.setOrientation(LinearLayout.VERTICAL);
		} else {
			super.onConfigurationChanged(newConfig);
		}
	}

	/**
	 * Listener to create a context menu.
	 */
	private OnCreateContextMenuListener createContextMenuListener = new OnCreateContextMenuListener() {
		@Override
		public void onCreateContextMenu(final ContextMenu menu, final View v,
				final ContextMenuInfo contextMenuInfo) {
			loadTranslation(contextMenuInfo);
			initializeMenu(menu);
		}

		private void loadTranslation(final ContextMenuInfo contextMenuInfo) {
			ListView list = (ListView) findViewById(R.id.translationsListView);
			AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) contextMenuInfo;
			SingleTranslation translation = (SingleTranslation) list
					.getItemAtPosition(menuInfo.position);

			try {
				SingleTranslationHelper.setTranslation(translation);
			} catch (DictionaryException e) {
				Toast.makeText(getBaseContext(), R.string.msg_parsing_error,
						Toast.LENGTH_SHORT).show();
			}
		}

		private void initializeMenu(final ContextMenu menu) {
			final MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.translation_context, menu);

			final MenuItem copyFromWord = menu.findItem(R.id.itemCopyFromWord);
			if (SingleTranslationHelper.getFromRow().length() == 0) {
				copyFromWord.setVisible(false);
				copyFromWord.setEnabled(false);
			}
			final String copyFromWordTitle = getString(R.string.copy_word,
					SingleTranslationHelper.getFromRow());
			copyFromWord.setTitle(copyFromWordTitle);

			final MenuItem copyToWord = menu.findItem(R.id.itemCopyToWord);
			if (SingleTranslationHelper.getToRows().length() == 0) {
				copyToWord.setVisible(false);
				copyToWord.setEnabled(false);
			}
			final String copyToWordTitle = getString(R.string.copy_word,
					SingleTranslationHelper.getToRows());
			copyToWord.setTitle(copyToWordTitle);
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		if (requestCode == REQUEST_DICTIONARY_PATH) {
			if (resultCode == RESULT_OK) {
				loadDictionaryFromIntent(data);
			}
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
			startLoadDictionary(new AssetDfMInputStreamAccess(this, assetPath),
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
		Intent i = new Intent(DictionaryForMIDs.this, ChooseDictionary.class);
		i.putExtra(ChooseDictionary.BUNDLE_SHOW_DICTIONARY_INSTALLATION,
				showDictionaryInstallation);
		startActivityForResult(i, REQUEST_DICTIONARY_PATH);
	}

	/**
	 * Reacts on changes in the selection of the translation languages to open
	 * the dialog for choosing a new dictionary if appropriate.
	 */
	private OnItemSelectedListener languageSelectedListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(final AdapterView<?> parent, final View v,
				final int position, final long id) {
			assert (parent.getId() == R.id.selectLanguages);
			final boolean isLoadDictionarySelected = position == parent
					.getCount() - 1;
			if (parent.getCount() > 1 && isLoadDictionarySelected) {
				startChooseDictionaryActivity();
				if (position != 0) {
					parent.setSelection(0);
				}
			}
		}

		@Override
		public void onNothingSelected(final AdapterView<?> arg0) {
		}

	};

	/**
	 * Reacts on touch events of the translation languages to open the dialog
	 * for choosing a new dictionary if appropriate.
	 */
	private final OnTouchListener languagesTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(final View view, final MotionEvent event) {
			assert (view.getId() == R.id.selectLanguages);
			final Spinner spinner = (Spinner) view;
			if (spinner.getCount() != 1
					|| event.getAction() != MotionEvent.ACTION_UP) {
				return false;
			}
			startChooseDictionaryActivity();
			return true;
		}

	};

}
