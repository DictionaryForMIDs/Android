package de.kugihan.dictionaryformids.hmi_android.data;

import java.util.Observable;
import java.util.Observer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;

/**
 * ExternalStorageState is a singleton class to query and receive updates on the
 * state of external storage.
 * 
 */
public class ExternalStorageState extends Observable {
	
	/**
	 * Handle to the singleton instance.
	 */
	private static ExternalStorageState instance = null;
	
	/**
	 * Handle to the context to attach the broadcast receiver to.
	 */
	private Context context = null;
	
	/**
	 * The receiver for external storage events.
	 */
	private BroadcastReceiver externalStorageReceiver;
	
	/**
	 * The state of the external storage.
	 */
	private boolean isExternalStorageReadable;

	/**
	 * Private constructor to prevent external instantiation.
	 * 
	 * @param applicationContext the context to attach the listener to
	 */
	private ExternalStorageState(Context applicationContext) {
		this.context = applicationContext;
		updateExternalStorageState();
	}
	
	/**
	 * Returns the active instance or creates a new instance if none exists.
	 * 
	 * @param applicationContext
	 *            the application context to attach the storage listener to
	 */
	public static ExternalStorageState createInstance(Context applicationContext) {
		if (instance == null) {
			instance = new ExternalStorageState(applicationContext);
		}
		return instance;
	}
	
	/**
	 * Returns the current instance of the class or null.
	 * 
	 * @return the current instance or null
	 */
	public static ExternalStorageState getInstance() {
		return instance;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addObserver(Observer observer) {
		super.addObserver(observer);
		startWatchingExternalStorage();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void deleteObserver(Observer observer) {
		super.deleteObserver(observer);
		if (countObservers() == 0) {
			stopWatchingExternalStorage();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void deleteObservers() {
		super.deleteObservers();
		stopWatchingExternalStorage();
	}

	/**
	 * Creates a new BroadcastReceiver and registers it for media intents.
	 */
	private void startWatchingExternalStorage() {
		if (externalStorageReceiver != null) {
			return;
		}
	    externalStorageReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	            updateExternalStorageState();
	        }
	    };
	    IntentFilter filter = new IntentFilter();
	    filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
	    filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
	    filter.addAction(Intent.ACTION_MEDIA_REMOVED);
	    filter.addDataScheme("file");
	    context.registerReceiver(externalStorageReceiver, filter);
	}

	/**
	 * Removes the BroadcastReceiver for media intents.
	 */
	private void stopWatchingExternalStorage() {
		if (externalStorageReceiver == null) {
			return;
		}
		context.unregisterReceiver(externalStorageReceiver);
	    externalStorageReceiver = null;
	}
	
	/**
	 * Reads the state of the external storage.
	 */
	private void updateExternalStorageState() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            isExternalStorageReadable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            isExternalStorageReadable = true;
        } else {
            isExternalStorageReadable = false;
        }
        setChanged();
        notifyObservers();
    }
	
	/**
	 * Returns if external storage is currently accessible for reading.
	 * 
	 * @return true if storage currently can be read from, otherwise false
	 */
	public boolean isExternalStorageReadable() {
		if (countObservers() == 0) {
			// manually update state if there are no observers
			// as we stop listening for changes in this case
			updateExternalStorageState();
		}
		return isExternalStorageReadable;
	}

}
