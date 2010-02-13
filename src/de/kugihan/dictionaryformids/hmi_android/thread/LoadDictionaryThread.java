package de.kugihan.dictionaryformids.hmi_android.thread;

import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.DfMInputStreamAccess;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.FileAccessHandler;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.NativeZipInputStreamAccess;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.hmi_android.Preferences.DictionaryType;

/**
 * Thread to load a dictionary without interrupting the UI.
 * 
 */
public class LoadDictionaryThread extends Thread {

	/**
	 * Interface specifying communication between the thread and the UI.
	 * 
	 */
	public interface OnThreadResultListener {

		/**
		 * This function gets called when the dictionary could successfully be
		 * loaded.
		 * 
		 * @param type
		 *            the type of the loaded dictionary
		 * @param path
		 *            the path of the loaded dictionary
		 * @param selectedIndex
		 *            the id of the language that should be selected
		 */
		void onSuccess(final DictionaryType type, final String path,
				final int selectedIndex);

		/**
		 * This function gets if an exception occurred while loading the
		 * dictionary.
		 * 
		 * @param exception
		 *            the exception that occurred while loading the dictionary
		 * @param mayIncludeCompressedDictionary
		 *            true if no dictionary but a jar file is found in an
		 *            archive specified by the InputStream
		 */
		void onException(DictionaryException exception,
				boolean mayIncludeCompressedDictionary);

		/**
		 * This function gets called if the thread exists because it was
		 * interrupted.
		 */
		void onInterrupted();
	}

	/**
	 * The listener that is informed about thread completion.
	 */
	private OnThreadResultListener listener = null;

	/**
	 * Object to synchronize access to the listener.
	 */
	private final Object listenerSync = new Object();

	/**
	 * Saves occurred exceptions.
	 */
	private DictionaryException exception = null;

	/**
	 * True if the archive does not represent a dictionary, but the archive may
	 * include a compressed dictionary.
	 */
	private boolean mayIncludeCompressedDictionary = false;

	/**
	 * Current state of the thread.
	 */
	private ThreadState result = ThreadState.WORKING;

	/**
	 * Object to synchronize access to the result.
	 */
	private final Object resultSync = new Object();

	/**
	 * Path of the dictionary being loaded by the thread.
	 */
	private final String dictionaryPath;

	/**
	 * Type of the dictionary being loaded by the thread.
	 */
	private final DictionaryType dictionaryType;

	/**
	 * The language index to select after loading the dictionary.
	 */
	private final int selectedIndex;

	/**
	 * The current state of the thread.
	 */
	private enum ThreadState {
		/**
		 * Thread will inform listener of successfully finished loading.
		 */
		RETURNING_SUCCESS,
		/**
		 * Thread will inform listener of an interruption.
		 */
		RETURNING_INTERRUPTED,
		/**
		 * Thread will inform listener of an exception.
		 */
		RETURNING_EXCEPTION,
		/**
		 * Thread is currently loading a dictionary.
		 */
		WORKING,
		/**
		 * Thread finished and the result was delivered to a listener.
		 */
		DELIVERED
	}

	/**
	 * The stream to load the dictionary from.
	 */
	private final DfMInputStreamAccess inputStreamAccess;

	/**
	 * Creates a new thread.
	 * 
	 * @param inputStreamAccess
	 *            the stream to load the dictionary from
	 * @param dictionaryType
	 *            the type of the dictionary
	 * @param dictionaryPath
	 *            the path of the dictionary
	 * @param selectedIndex
	 *            the index of the language that is selected
	 */
	public LoadDictionaryThread(final DfMInputStreamAccess inputStreamAccess,
			final DictionaryType dictionaryType, final String dictionaryPath,
			final int selectedIndex) {
		this.inputStreamAccess = inputStreamAccess;
		this.dictionaryType = dictionaryType;
		this.dictionaryPath = dictionaryPath;
		this.selectedIndex = selectedIndex;
	}

	/**
	 * Attaches the listener to the thread or removes the current one if
	 * listener is null.
	 * 
	 * @param listener
	 *            the listener to attach or null
	 */
	public final void setOnThreadResultListener(
			final OnThreadResultListener listener) {
		synchronized (listenerSync) {
			this.listener = listener;
		}
		// try to return already available results
		pushResultToListener();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void run() {
		FileAccessHandler.setDictionaryDataFileISAccess(inputStreamAccess);
		DictionaryDataFile.useStandardPath = false;
		try {
			DictionaryDataFile.initValues(false);
			if (interrupted()) {
				exitAfterInterruption();
				return;
			}
		} catch (DictionaryException e) {
			if (interrupted()) {
				exitAfterInterruption();
				return;
			}
			exitWithException(e, hasJarFile());
			return;
		}
		exitSuccessfully();
	}

	/**
	 * Checks if the current inputStreamAccess includes a jar-File, which hints
	 * on a zipped dictionary.
	 * 
	 * @return true if current inputStreamAccess includes a jar-File
	 */
	private boolean hasJarFile() {
		final boolean isZipStream = inputStreamAccess instanceof NativeZipInputStreamAccess;
		if (!isZipStream) {
			return false;
		}
		final NativeZipInputStreamAccess stream = (NativeZipInputStreamAccess) inputStreamAccess;
		try {
			return stream.hasJarDictionary();
		} catch (DictionaryException e1) {
			return false;
		}
	}

	/**
	 * Handles the successfully loaded dictionaries.
	 */
	private void exitSuccessfully() {
		synchronized (resultSync) {
			result = ThreadState.RETURNING_SUCCESS;
		}
		pushResultToListener();
	}

	/**
	 * Handles exits caused by exceptions.
	 * 
	 * @param exception
	 *            the exception that occurred
	 * @param mayIncludeCompressedDictionary
	 *            true if the dictionary file may include a compressed
	 *            dictionary
	 */
	private void exitWithException(final DictionaryException exception,
			final boolean mayIncludeCompressedDictionary) {
		synchronized (resultSync) {
			this.exception = exception;
			this.mayIncludeCompressedDictionary = mayIncludeCompressedDictionary;
			result = ThreadState.RETURNING_EXCEPTION;
		}
		pushResultToListener();
	}

	/**
	 * Handles exits caused by user interruption.
	 */
	private void exitAfterInterruption() {
		synchronized (resultSync) {
			result = ThreadState.RETURNING_INTERRUPTED;
		}
		pushResultToListener();
	}

	/**
	 * Tries to push the result to the attached listener.
	 */
	private void pushResultToListener() {
		synchronized (listenerSync) {
			if (listener == null) {
				return;
			}
			synchronized (resultSync) {
				switch (result) {
				case RETURNING_SUCCESS:
					listener.onSuccess(dictionaryType, dictionaryPath,
							selectedIndex);
					result = ThreadState.DELIVERED;
					break;

				case RETURNING_EXCEPTION:
					listener.onException(exception,
							mayIncludeCompressedDictionary);
					result = ThreadState.DELIVERED;
					break;

				case RETURNING_INTERRUPTED:
					listener.onInterrupted();
					result = ThreadState.DELIVERED;
					break;

				case WORKING:
					// nothing to return yet
					break;

				case DELIVERED:
					// result already returned
					break;

				default:
					throw new IllegalArgumentException();
				}
			}
		}
	}
}
