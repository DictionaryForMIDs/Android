/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 *
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.kugihan.dictionaryformids.hmi_android.DictionaryForMIDs;
import de.kugihan.dictionaryformids.hmi_android.FileList;
import de.kugihan.dictionaryformids.hmi_android.InstallDictionary;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.R;
import de.kugihan.dictionaryformids.hmi_android.data.DictionaryType;
import de.kugihan.dictionaryformids.hmi_android.data.DownloadDictionaryItem;

/**
 * DictionaryInstallationService is the class for downloading and installing
 * dictionaries in the background.
 *
 */
public final class DictionaryInstallationService extends Service {

	/**
	 * Number of milliseconds between pushing status updates to the listeners.
	 */
	private static final int LISTENER_NOTIFICATION_INTERVAL = 300;

	/**
	 * Size of the buffer used for copying stream data.
	 */
	private static final int COPY_BUFFER_SIZE = 1024;

	/**
	 * The pattern that is deleted from paths of extracted dictionaries.
	 */
	private static final String PATTERN_DELETE_FROM_PATH = "dictionary/";

	/**
	 * The pattern of the file that is returned for extracted dictionaries.
	 */
	private static final String PATTERN_RETURN_PATH = ".*\\.properties";

	/**
	 * The pattern of the files that should be extracted.
	 */
	private static final String PATTERN_EXTRACT_PATH = "dictionary/.*";

	/**
	 * The pattern of jar archives.
	 */
	private static final String PATTERN_JAR_ARCHIVE = ".*\\.jar";

	/**
	 * The extension of jar archives.
	 */
	private static final String EXTENSION_JAR_ARCHIVE = ".jar";

	/**
	 * The extension of zip archives.
	 */
	private static final String EXTENSION_ZIP_ARCHIVE = ".zip";

	/**
	 * The key specifying an exception in a bundle.
	 */
	public static final String BUNDLE_EXCEPTION = "exception";

	/**
	 * The key of a boolean specifying if a new dictionary can be loaded in a
	 * bundle.
	 */
	public static final String BUNDLE_LOAD_DICTIONARY = "loadDictionary";

	/**
	 * The key of a boolean specifying if the current installation status of a
	 * dictionary should be displayed.
	 */
	public static final String BUNDLE_SHOW_DICTIONARY_INSTALLATION = "showDictionaryInstallation";

	/**
	 * The key of a string specifying the download dictionary item in a bundle.
	 */
	public static final String BUNDLE_DOWNLOAD_DICTIONARY_ITEM = "downloadDictionaryItem";

	/**
	 * Status specifying the service is currently downloading files.
	 */
	public static final int STATUS_DOWNLOADING = 0;

	/**
	 * Status specifying the service is currently extracting the plain,
	 * uncompressed dictionary files.
	 */
	public static final int STATUS_EXTRACTING_DICTIONARY = 1;

	/**
	 * Status specifying the service is currently extracting a jar file from an
	 * archive.
	 */
	public static final int STATUS_EXTRACTING_JAR = 2;

	/**
	 * Status specifying the service is currently creating required folders.
	 */
	public static final int STATUS_CREATING_FOLDERS = 3;

	/**
	 * The base used for calculating "percentages".
	 */
	public static final int PERCENTAGE_BASE = 1000;

	/**
	 * The GUI that listens for updates.
	 */
	private static ServiceUpdateListener listener = null;

	/**
	 * Handle of the current worker thread.
	 */
	private static volatile Thread thread = null;

	/**
	 * Timer to execute scheduled tasks.
	 */
	private final Timer updateTimer = new Timer();

	/**
	 * Scheduled task that pushes status updates to registered listeners.
	 */
	private volatile StatusUpdateTask statusUpdateTask = null;

	/**
	 * Caches the status that have been sent by the last call to sendUpdate.
	 */
	private static int lastSendType = -1;

	/**
	 * Caches the percentage that have been sent by the last call to sendUpdate.
	 */
	private static int lastSendPercentage = -1;

	/**
	 * The ID of the update notification.
	 */
	private static final int NOTIFICATION_STATUS_UPDATE = 1;

	/**
	 * The ID of the installation finished notification.
	 */
	private static final int NOTIFICATION_RESULT = 2;

	/**
	 * The ID of the exception notification.
	 */
	private static final int NOTIFICATION_EXCEPTION = 3;

	/**
	 * Handle to the application's notification manager.
	 */
	private NotificationManager notificationManager = null;

	/**
	 * Signature of Android 2.0- function setForeground. Necessary for
	 * compatibility with previous versions of the Android API.
	 */
	private static final Class<?>[] setForegroundSignature = new Class[] { boolean.class };

	/**
	 * Signature of Android 2.0+ function startForeground. Necessary for
	 * compatibility with previous versions of the Android API.
	 */
	@SuppressWarnings("rawtypes")
	private static final Class[] startForegroundSignature = new Class[] { int.class,
			Notification.class };
	/**
	 * Signature of Android 2.0+ function stopForeground. Necessary for
	 * compatibility with previous versions of the Android API.
	 */
	@SuppressWarnings("rawtypes")
	private static final Class[] stopForegroundSignature = new Class[] { boolean.class };

	/**
	 * Object to hold Android 2.0- function setForeground. Necessary for
	 * compatibility with previous versions of the Android API.
	 */
	private Method setForeground;

	/**
	 * Object to hold Android 2.0+ function startForeground. Necessary for
	 * compatibility with previous versions of the Android API.
	 */
	private Method startForeground;

	/**
	 * Object to hold Android 2.0+ function stopForeground. Necessary for
	 * compatibility with previous versions of the Android API.
	 */
	private Method stopForeground;

	/**
	 * Object to hold parameters for Android 2.0- function setForeground.
	 * Necessary for compatibility with previous versions of the Android API.
	 */
	private final Object[] setForegroundArgs = new Object[1];

	/**
	 * Object to hold parameters for Android 2.0+ function startForeground.
	 * Necessary for compatibility with previous versions of the Android API.
	 */
	private final Object[] startForegroundArgs = new Object[2];

	/**
	 * Object to hold parameters for Android 2.0+ function stopForeground.
	 * Necessary for compatibility with previous versions of the Android API.
	 */
	private final Object[] stopForegroundArgs = new Object[1];

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}

	/**
	 * Creates a new installation service.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFICATION_STATUS_UPDATE);
		try {
			// try to get handles to functions of API version 5 or later
			startForeground = getClass().getMethod("startForeground", startForegroundSignature);
			stopForeground = getClass().getMethod("stopForeground", stopForegroundSignature);
			return;
		} catch (NoSuchMethodException e) {
			startForeground = null;
			stopForeground = null;
		}
		try {
			setForeground = getClass().getMethod("setForeground", setForegroundSignature);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(
					"OS doesn't have Service.startForeground OR Service.setForeground!");
		}
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		if (startForeground != null) {
			startForegroundArgs[0] = Integer.valueOf(id);
			startForegroundArgs[1] = notification;
			invokeMethod(startForeground, startForegroundArgs);
			return;
		}

		// Fall back on the old API.
		setForegroundArgs[0] = Boolean.TRUE;
		invokeMethod(setForeground, setForegroundArgs);
		notificationManager.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	void stopForegroundCompat(int id) {
		// If we have the new stopForeground API, then use it.
		if (stopForeground != null) {
			stopForegroundArgs[0] = Boolean.TRUE;
			invokeMethod(stopForeground, stopForegroundArgs);
			return;
		}

		// Fall back on the old API. Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
		notificationManager.cancel(id);
		setForegroundArgs[0] = Boolean.FALSE;
		invokeMethod(setForeground, setForegroundArgs);
	}

	void invokeMethod(Method method, Object[] args) {
		try {
			method.invoke(this, args);
		} catch (InvocationTargetException e) {
			// Should not happen.
			Log.w(DictionaryForMIDs.LOG_TAG, "Unable to invoke method", e);
		} catch (IllegalAccessException e) {
			// Should not happen.
			Log.w(DictionaryForMIDs.LOG_TAG, "Unable to invoke method", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStart(final Intent intent, final int startId) {
		final DownloadDictionaryItem dictionaryItem = (DownloadDictionaryItem) intent
				.getParcelableExtra(BUNDLE_DOWNLOAD_DICTIONARY_ITEM);
		if (dictionaryItem == null) {
			handleException(new IllegalArgumentException());
		} else {
			startService(dictionaryItem);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		onStart(intent, startId);
		// make sure intent is redelivered if service gets killed
		return START_REDELIVER_INTENT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		stopForegroundCompat(NOTIFICATION_STATUS_UPDATE);
		if (thread == null) {
			return;
		}
		final Thread current = thread;
		thread = null;
		current.interrupt();
	}

	/**
	 * Starts the installation service.
	 *
	 * @param item
	 *            information about the dictionary to install
	 */
	private void startService(final DownloadDictionaryItem item) {
		statusUpdateTask = new StatusUpdateTask();
		updateTimer.schedule(statusUpdateTask, LISTENER_NOTIFICATION_INTERVAL,
				LISTENER_NOTIFICATION_INTERVAL);

		thread = new InstallDictionaryThread(item);
		thread.start();
	}

	/**
	 * Overwrites the current update listener.
	 *
	 * @param updateListener
	 *            the new update listener
	 */
	public static void setUpdateListener(final ServiceUpdateListener updateListener) {
		DictionaryInstallationService.listener = updateListener;
	}

	/**
	 * Checks if there is an active installation thread.
	 *
	 * @return true if there is an active installation thread
	 */
	public static boolean isRunning() {
		if (thread == null) {
			return false;
		}
		if (thread.isAlive()) {
			return true;
		}
		return false;
	}

	/**
	 * Removes pending status notifications if the service is not running.
	 *
	 * @param context
	 *            the application context
	 */
	public static void removePendingStatusNotifications(Context context) {
		if (DictionaryInstallationService.isRunning()) {
			return;
		}
		((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
				.cancel(NOTIFICATION_STATUS_UPDATE);
	}

	/**
	 * Handles an update of the installation thread's status.
	 *
	 * @param type
	 *            the current type of action performed by the installation
	 *            thread
	 * @param percentage
	 *            the current completion percentage of the activity specified
	 *            with type
	 */
	private void handleUpdate(final int type, final int percentage) {
		lastSendType = type;
		lastSendPercentage = percentage;
	}

	/**
	 * Creates a notification and informs any listeners about the new status.
	 *
	 * @param type
	 * @param percentage
	 */
	public void pushUpdateNotification(final int type, final int percentage) {
		final double progressBarPercentage = InstallDictionary.getProgressBarLength(type,
				percentage) / 100.0;
		final int icon = R.drawable.ic_notification_logo;
		final CharSequence tickerText = getText(R.string.msg_installing_dictionary);
		final long when = System.currentTimeMillis();
		final Context context = getApplicationContext();
		final CharSequence contentTitle = getText(R.string.title_installation_status);
		final CharSequence contentText = getString(R.string.msg_installation_status,
				progressBarPercentage);

		final Intent notificationIntent = new Intent(this, DictionaryForMIDs.class);
		notificationIntent.putExtra(BUNDLE_SHOW_DICTIONARY_INSTALLATION, true);
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		// the next two lines initialize the Notification, using the
		// configurations above
		final Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		notificationManager.notify(NOTIFICATION_STATUS_UPDATE, notification);
		startForegroundCompat(NOTIFICATION_STATUS_UPDATE, notification);

		if (listener != null) {
			listener.onProgressUpdate(type, percentage);
		}
	}

	/**
	 * Returns the last sent update type.
	 *
	 * @return the last sent update type
	 */
	public static int pollLastType() {
		return lastSendType;
	}

	/**
	 * Returns the last sent percentage.
	 *
	 * @return the last sent percentage
	 */
	public static int pollLastPercentage() {
		return lastSendPercentage;
	}

	/**
	 * Handles the result of the installation thread.
	 *
	 * @param dictionaryItem
	 *            the information of the dictionary that has been installed
	 * @param path
	 *            the path to the installed dictionary
	 */
	private void handleResult(final DownloadDictionaryItem dictionaryItem, final String path) {
		// remove status notifications
		notificationManager.cancel(NOTIFICATION_STATUS_UPDATE);

		final boolean hasAutoInstallDictionary = Preferences.hasAutoInstallDictionary();
		final int id = dictionaryItem.getId();
		final int autoInstallDictionaryId = Preferences.getAutoInstallDictionaryId();
		if (hasAutoInstallDictionary && id == autoInstallDictionaryId) {
			// remove the auto install id from preferences
			// as we just installed that dictionary
			Preferences.removeAutoInstallDictionaryId();
		}

		if (statusUpdateTask != null) {
			synchronized (updateTimer) {
				if (statusUpdateTask != null) {
					statusUpdateTask.cancel();
					statusUpdateTask = null;
				}
			}
		}

		if (listener != null) {
			listener.onFinished(path);
			stopSelf();
			return;
		}

		lastSendType = -1;
		lastSendPercentage = 0;

		// start notification for finished loading
		final int icon = R.drawable.ic_notification_logo;
		final CharSequence tickerText = getText(R.string.msg_installation_complete);
		final long when = System.currentTimeMillis();
		final Context context = getApplicationContext();
		final CharSequence contentTitle = getText(R.string.title_installation_finished);
		final CharSequence contentText = getText(R.string.msg_successfully_installed_dictionary);

		final Intent notificationIntent = new Intent(this, DictionaryForMIDs.class);
		notificationIntent.putExtra(BUNDLE_LOAD_DICTIONARY, true);
		notificationIntent.putExtra(FileList.FILE_PATH, path);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		// the next two lines initialize the Notification, using the
		// configurations above
		final Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(NOTIFICATION_RESULT, notification);

		final String[] languages = { dictionaryItem.getLocalizedName(getResources()) };
		Preferences.attachToContext(getApplicationContext());
		Preferences.addRecentDictionaryUrl(DictionaryType.DIRECTORY, path, languages);

		stopSelf();
	}

	/**
	 * Handles exception thrown by the installation thread.
	 *
	 * @param exception
	 *            the exception thrown by the installation thread
	 */
	private void handleException(final Exception exception) {
		// remove old notifications
		notificationManager.cancel(NOTIFICATION_STATUS_UPDATE);

		if (statusUpdateTask != null) {
			synchronized (updateTimer) {
				if (statusUpdateTask != null) {
					statusUpdateTask.cancel();
					statusUpdateTask = null;
				}
			}
		}

		if (listener != null) {
			listener.onExitWithException(exception);
			stopSelf();
			return;
		}

		final int icon = R.drawable.ic_notification_logo;
		final CharSequence tickerText = getText(R.string.msg_installation_error);
		final long when = System.currentTimeMillis();
		final Context context = getApplicationContext();
		final CharSequence contentTitle = getText(R.string.title_exception);
		final CharSequence contentText = exception.getMessage();

		// show exception
		final Intent notificationIntent = new Intent(this, DictionaryForMIDs.class);
		notificationIntent.putExtra(BUNDLE_EXCEPTION, exception);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		// the next two lines initialize the Notification, using the
		// configurations above
		final Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(NOTIFICATION_EXCEPTION, notification);
		stopSelf();
	}

	/**
	 * Creates the parent directory of a file if it does not yet exist.
	 *
	 * @param file
	 *            the file's parent directory will be created
	 * @throws IOException
	 *             the exception thrown when the directory could not be created
	 */
	private void createParentDirectories(final File file) throws IOException {
		if (file.exists()) {
			return;
		}
		boolean directoryWasCreated = true;
		if (!file.getParentFile().exists()) {
			directoryWasCreated = file.getParentFile().mkdirs();
		}
		if (!directoryWasCreated) {
			throw new IOException(getString(R.string.exception_failed_create_directory, file
					.getParentFile().toString()));
		}
	}

	/**
	 * Creates a new file, removes previously existing files.
	 *
	 * @param file
	 *            the file to create
	 * @throws IOException
	 *             the exception that is thrown if the file could not be created
	 */
	private void createFile(final File file) throws IOException {
		if (file.exists()) {
			if (!file.delete()) {
				throw new IOException(getString(R.string.exception_failed_delete_file,
						file.toString()));
			}
		}
		if (!file.createNewFile()) {
			throw new IOException(getString(R.string.exception_failed_create_file, file.toString()));
		}
	}

	/**
	 * Decouples status updates and pushing them to the view, allowing for
	 * regularly GUI updates.
	 */
	private final class StatusUpdateTask extends TimerTask {

		@Override
		public void run() {
			final int type = DictionaryInstallationService.lastSendType;
			final int percentage = DictionaryInstallationService.lastSendPercentage;
			DictionaryInstallationService.this.pushUpdateNotification(type, percentage);
		}

	}

	/**
	 * InstallDictionaryThread implements the thread for downloading and
	 * installing a new dictionary.
	 *
	 */
	private final class InstallDictionaryThread extends Thread {
		/**
		 * The name of the dictionary.
		 */
		private final DownloadDictionaryItem dictionaryItem;

		/**
		 * Constructor that initializes the dictionary's values.
		 *
		 * @param dictionaryItem
		 *            the dictionary to install
		 */
		private InstallDictionaryThread(final DownloadDictionaryItem dictionaryItem) {
			this.dictionaryItem = dictionaryItem;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run() {
			if (interrupted()) {
				handleException(new InterruptedException(
						getString(R.string.msg_installation_aborted)));
				return;
			}
			handleUpdate(STATUS_CREATING_FOLDERS, 0);

			// check if sd card is available/accessible
			final String storageState = Environment.getExternalStorageState();
			if (!Environment.MEDIA_MOUNTED.equals(storageState)) {
				handleException(new InterruptedException(getString(
						R.string.msg_error_accessing_storage, storageState)));
				return;
			}

			File zipDownloadDirectory = new File(getString(R.string.attribute_zip_directory,
					Environment.getExternalStorageDirectory()));
			try {
				createParentDirectories(zipDownloadDirectory);
			} catch (IOException exception) {
				handleException(exception);
				return;
			}
			if (interrupted()) {
				handleException(new InterruptedException(
						getString(R.string.msg_installation_aborted)));
				return;
			}
			handleUpdate(STATUS_CREATING_FOLDERS, PERCENTAGE_BASE / 2);

			File jarDirectory = new File(getString(R.string.attribute_jar_directory,
					Environment.getExternalStorageDirectory()));
			try {
				createParentDirectories(jarDirectory);
			} catch (IOException exception) {
				handleException(exception);
				return;
			}
			if (interrupted()) {
				handleException(new InterruptedException(
						getString(R.string.msg_installation_aborted)));
				return;
			}
			handleUpdate(STATUS_CREATING_FOLDERS, PERCENTAGE_BASE);

			final String zipFile = getString(R.string.attribute_zip_directory,
					Environment.getExternalStorageDirectory())
					+ dictionaryItem.getFileName() + EXTENSION_ZIP_ARCHIVE;
			final String jarFile = getString(R.string.attribute_jar_directory,
					Environment.getExternalStorageDirectory())
					+ dictionaryItem.getFileName() + EXTENSION_JAR_ARCHIVE;
			final String dictionaryDirectory = getString(R.string.attribute_installation_directory,
					Environment.getExternalStorageDirectory())
					+ dictionaryItem.getFileName()
					+ File.separator;

			String resultPath;
			try {
				downloadFile(dictionaryItem.getLink(), zipFile);
				if (interrupted()) {
					handleException(new InterruptedException(
							getString(R.string.msg_installation_aborted)));
					return;
				}
				handleUpdate(STATUS_DOWNLOADING, PERCENTAGE_BASE);
				extractFirstMatchingEntry(zipFile, jarFile, PATTERN_JAR_ARCHIVE);
				if (interrupted()) {
					handleException(new InterruptedException(
							getString(R.string.msg_installation_aborted)));
					return;
				}
				handleUpdate(STATUS_EXTRACTING_JAR, PERCENTAGE_BASE);
				resultPath = extractAllMatchingEntries(jarFile, dictionaryDirectory,
						PATTERN_EXTRACT_PATH, PATTERN_RETURN_PATH, PATTERN_DELETE_FROM_PATH);
			} catch (IOException e) {
				handleException(e);
				return;
			}
			if (interrupted()) {
				handleException(new InterruptedException(
						getString(R.string.msg_installation_aborted)));
				return;
			}
			// clean up
			File zipDeleteFile = new File(zipFile);
			if (!zipDeleteFile.delete()) {
				Log.v(DictionaryForMIDs.LOG_TAG, "Failed to delete zip: " + zipFile);
			}
			File jarDeleteFile = new File(jarFile);
			if (!jarDeleteFile.delete()) {
				Log.v(DictionaryForMIDs.LOG_TAG, "Failed to delete jar: " + jarFile);
			}
			handleResult(dictionaryItem, resultPath);
		}

		/**
		 * Downloads the file specified by url.
		 *
		 * @param downloadUrl
		 *            the url of the file to download
		 * @param destinationFile
		 *            the file to save the download to
		 * @throws IOException
		 *             if an input or output exception occurred
		 */
		private void downloadFile(final String downloadUrl, final String destinationFile)
				throws IOException {
			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(downloadUrl);

			final File outputFile = new File(destinationFile);
			createParentDirectories(outputFile);
			FileOutputStream outputStream;
			outputStream = new FileOutputStream(outputFile);

			final HttpResponse response = client.execute(httpGet);

			if (isInterrupted()) {
				outputStream.close();
				return;
			}

			final HttpEntity entity = response.getEntity();
			InputStream inputStream = null;
			try {
				if (entity != null) {
					inputStream = entity.getContent();
					CopyStreamStatusCallback callback = new CopyStreamStatusCallback() {

						@Override
						public long getSkipBetweenUpdates() {
							return entity.getContentLength() * 2 / PERCENTAGE_BASE;
						}

						@Override
						public void onUpdate(final long copiedLength) {
							int percentage = (int) (copiedLength * PERCENTAGE_BASE / entity
									.getContentLength());
							handleUpdate(STATUS_DOWNLOADING, percentage);
						}

					};
					copyStreams(inputStream, outputStream, callback);
				}
			} finally {
				try {
					outputStream.close();
					if (inputStream != null) {
						inputStream.close();
					}
				} catch (IOException e) {
					// ignore exceptions here
					Log.v(DictionaryForMIDs.LOG_TAG, "Exception while closing stream: " + e);
				}
			}
		}

		/**
		 * Extracts all matching entries of an archive.
		 *
		 * @param sourceFile
		 *            the archive to extract
		 * @param destinationDirectory
		 *            the directory to save the extracted files
		 * @param filePattern
		 *            the pattern of files to extract
		 * @param selectPathPattern
		 *            the pattern of the file which path is returned
		 * @param pathDeletePattern
		 *            the pattern that is deleted from extracted file names
		 * @return the path of the file that matches the selectPathPattern
		 * @throws IOException
		 *             if an input or output exception occurred
		 */
		public String extractAllMatchingEntries(final String sourceFile,
				final String destinationDirectory, final String filePattern,
				final String selectPathPattern, final String pathDeletePattern) throws IOException {
			ZipFile zipFile = new ZipFile(sourceFile);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			String resultPath = null;
			int elementIndex = -1;
			final float elementProgressSize = (float) PERCENTAGE_BASE / (float) zipFile.size();
			while (entries.hasMoreElements()) {
				elementIndex++;
				final int currentStart = (int) (elementIndex * elementProgressSize);
				if (lastSendType != STATUS_EXTRACTING_DICTIONARY
						|| lastSendPercentage + PERCENTAGE_BASE / 1000 < currentStart) {
					handleUpdate(STATUS_EXTRACTING_DICTIONARY, currentStart);
				}
				final ZipEntry entry = entries.nextElement();
				String fileName = entry.getName().replaceFirst(pathDeletePattern, "");
				File file = new File(destinationDirectory + File.separator + fileName);
				final boolean fileNameMatchesPattern = entry.getName().matches(filePattern);
				if (!fileNameMatchesPattern) {
					continue;
				}
				if (entry.isDirectory()) {
					continue;
				}
				createParentDirectories(file);
				createFile(file);
				InputStream input = zipFile.getInputStream(entry);
				FileOutputStream output = new FileOutputStream(file);
				if (elementProgressSize < 1 || entry.getSize() < 10000) {
					copyStreams(input, output, null);
				} else {
					CopyStreamStatusCallback callback = new CopyStreamStatusCallback() {

						@Override
						public long getSkipBetweenUpdates() {
							return (long) (elementProgressSize * 3 / PERCENTAGE_BASE);
						}

						@Override
						public void onUpdate(final long copiedLength) {
							final int currentFilePercentage = (int) (copiedLength
									* elementProgressSize / entry.getSize());
							final int percentage = currentStart + currentFilePercentage;
							handleUpdate(STATUS_EXTRACTING_DICTIONARY, percentage);
						}

					};
					copyStreams(input, output, callback);
				}

				if (file.getName().matches(selectPathPattern)) {
					resultPath = file.getParent();
				}
			}
			return resultPath;
		}

		/**
		 * Extracts the first matching entry from the archive.
		 *
		 * @param sourceFile
		 *            the archive to extract
		 * @param destinationFile
		 *            the directory to save the extracted file
		 * @param filePattern
		 *            the pattern of the file to extract
		 * @throws IOException
		 *             if an input or output exception occurred
		 */
		public void extractFirstMatchingEntry(final String sourceFile,
				final String destinationFile, final String filePattern) throws IOException {
			ZipFile zipFile = new ZipFile(sourceFile);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				final boolean fileNameMatchesPattern = entry.getName().matches(filePattern);
				if (!fileNameMatchesPattern) {
					continue;
				}
				File extractedFile = new File(destinationFile);
				if (entry.isDirectory()) {
					continue;
				}
				createParentDirectories(extractedFile);
				createFile(extractedFile);
				InputStream input = zipFile.getInputStream(entry);
				FileOutputStream output = new FileOutputStream(extractedFile);

				CopyStreamStatusCallback callback = new CopyStreamStatusCallback() {

					@Override
					public long getSkipBetweenUpdates() {
						return entry.getSize() * 2 / PERCENTAGE_BASE;
					}

					@Override
					public void onUpdate(final long copiedLength) {
						final int percentage = (int) (copiedLength * PERCENTAGE_BASE / entry
								.getSize());
						handleUpdate(STATUS_EXTRACTING_JAR, percentage);
					}

				};
				copyStreams(input, output, callback);
				return;
			}
			final String message = getString(R.string.msg_compressed_entry_not_found, sourceFile,
					filePattern);
			throw new FileNotFoundException(message);
		}

		/**
		 * Copies an input stream into an output stream.
		 *
		 * @param input
		 *            the input stream to copy
		 * @param output
		 *            the output stream to write to
		 * @param statusCallback
		 *            the callback object to call or null
		 * @throws IOException
		 *             if an input or output exception occurred
		 */
		public void copyStreams(final InputStream input, final OutputStream output,
				final CopyStreamStatusCallback statusCallback) throws IOException {
			byte[] buffer = new byte[COPY_BUFFER_SIZE];
			int length;
			long copiedLength = 0;
			long modulo = 0;
			while ((length = input.read(buffer)) >= 0) {
				if (isInterrupted()) {
					return;
				}
				output.write(buffer, 0, length);
				copiedLength += length;
				// Send current status to caller
				if (statusCallback != null && copiedLength >= modulo) {
					statusCallback.onUpdate(copiedLength);
					while (copiedLength >= modulo) {
						long add = statusCallback.getSkipBetweenUpdates();
						if (add <= 0) {
							add = 1;
						}
						modulo += add;
					}
				}
			}
		}
	}

	/**
	 * An interface that defines communication between the thread copying data
	 * and the GUI.
	 *
	 */
	private interface CopyStreamStatusCallback {

		/**
		 * The method that is called after when some data has been read.
		 *
		 * @param copiedLength
		 *            the length of the copied data
		 */
		void onUpdate(long copiedLength);

		/**
		 * The amount of data to copy before calling the callback again. Must be
		 * greater than 0.
		 *
		 * @return the amount of data to copy before calling the callback again
		 */
		long getSkipBetweenUpdates();
	}

	/**
	 * Starts the service to download and install a new dictionary. Must be
	 * called from UI thread.
	 *
	 * @param context
	 *            the context to use
	 * @param dictionaryItem
	 *            the dictionary item to install
	 * @return true if the service was started, false if another installation is
	 *         already active
	 */
	public static boolean startDictionaryInstallation(final Context context,
			final DownloadDictionaryItem dictionaryItem) {
		if (DictionaryInstallationService.isRunning()) {
			return false;
		}

		Intent intent = new Intent(context, DictionaryInstallationService.class);
		intent.putExtra(DictionaryInstallationService.BUNDLE_DOWNLOAD_DICTIONARY_ITEM,
				dictionaryItem);
		context.startService(intent);

		return true;

	}
}
