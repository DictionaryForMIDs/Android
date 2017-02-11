package de.kugihan.dictionaryformids.hmi_android.thread;

import android.os.AsyncTask;

import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.DfMInputStreamAccess;
import de.kugihan.dictionaryformids.dataaccess.fileaccess.NativeZipInputStreamAccess;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.translation.TranslationExecution;

/**
 * Thread to load a dictionary without interrupting the UI.
 * 
 */
public class LoadDictionaryThread extends AsyncTask<DfMInputStreamAccess, Void, DictionaryDataFile> {

	@Override
	protected DictionaryDataFile doInBackground(DfMInputStreamAccess... dfMInputStreamAccesses) {
		if (dfMInputStreamAccesses.length != 1) {
			throw new IllegalArgumentException();
		}

		// Configure DictionaryDataFile
		DictionaryDataFile.useStandardPath = false;

		final DfMInputStreamAccess inputStreamAccess = dfMInputStreamAccesses[0];
		DictionaryDataFile dataFile = null;

		try {
			dataFile = TranslationExecution.loadDictionary(inputStreamAccess);
		} catch (DictionaryException e) {
			this.exception = e;
			if (!isCancelled()) {
				this.mayIncludeCompressedDictionary = hasJarFile(inputStreamAccess);
			}
		}

		return dataFile;
	}

	@Override
	protected void onPostExecute(DictionaryDataFile dataFile) {
		synchronized (listenerSync) {
			if (listener == null) {
				return;
			}
			if (exception != null) {
				listener.onException(exception, mayIncludeCompressedDictionary);
			} else {
				listener.onSuccess(dataFile);
			}
		}
	}

	@Override
	protected void onCancelled(DictionaryDataFile dataFile) {
		synchronized (listenerSync) {
			if (listener == null) {
				return;
			}
			listener.onInterrupted();
		}
	}

	/**
	 * Interface specifying communication between the thread and the UI.
	 * 
	 */
	public interface OnThreadResultListener {

		/**
		 * This function gets called when the dictionary could successfully be
		 * loaded.
		 * 
		 * @param dataFile
		 *            the instance of the loaded dictionary
		 */
		void onSuccess(final DictionaryDataFile dataFile);

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
	 * Object to synchronize access to the result.
	 */
	private final Object resultSync = new Object();

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
	}

	/**
	 * Checks if the current inputStreamAccess includes a jar-File, which hints
	 * on a zipped dictionary.
	 * 
	 * @return true if current inputStreamAccess includes a jar-File
	 */
	private static boolean hasJarFile(DfMInputStreamAccess inputStreamAccess) {
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
}
