package de.kugihan.dictionaryformids.hmi_android.thread;

import java.util.Observable;

import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.hmi_android.data.TranslationExecutor;
import de.kugihan.dictionaryformids.translation.TranslationExecutionCallback;
import de.kugihan.dictionaryformids.translation.TranslationParameters;
import de.kugihan.dictionaryformids.translation.TranslationParametersBatch;
import de.kugihan.dictionaryformids.translation.TranslationResult;

public class Translations extends Observable {

	private class TranslationHandler implements TranslationExecutionCallback {

		@Override
		public void newTranslationResult(TranslationResult resultOfTranslation) {
			setChanged();
			notifyObservers(resultOfTranslation);
			translationState.decreaseActiveCount();
		}

		@Override
		public void deletePreviousTranslationResult() {
			setChanged();
			notifyObservers(null);
		}
	}

	public static class TranslationState extends Observable {
		public boolean isActive() {
			return this.countOutstandingTranslations > 0;
		}

		private synchronized void setActive(int countOutstandingTranslations) {
			this.countOutstandingTranslations = countOutstandingTranslations;
			setChanged();
			notifyObservers(this.isActive());
		}

		private synchronized void decreaseActiveCount() {
			this.countOutstandingTranslations--;
			setChanged();
			notifyObservers(this.isActive());
		}

		private int countOutstandingTranslations = 0;
	}

	private TranslationExecutor executor = null;

	private final TranslationHandler handler = new TranslationHandler();

	public TranslationState getTranslationState() {
		return translationState;
	}

	private final TranslationState translationState = new TranslationState();

	public void setExecutor(TranslationExecutor executor) {
		if (executor == null) {
			throw new IllegalArgumentException();
		}
		this.executor = executor;
		this.executor.setTranslationExecutionCallback(handler);
	}

	public void startTranslation(TranslationParametersBatch translationParameters) {
		if (executor == null) {
			throw new IllegalStateException("Set executor first");
		}

		// TODO: is check necessary?
		final int count = translationParameters.numberOfTranslationParameters();
		for (int i = 0; i < count; i++) {
			TranslationParameters parameters = translationParameters.getTranslationParametersAt(i);
			if (!parameters.isExecuteInBackground()) {
				throw new IllegalArgumentException("All translations must be configured for background execution.");
			}
		}

		try {
			translationState.setActive(translationParameters.numberOfTranslationParameters());
			executor.executeTranslationBatch(translationParameters);
		} catch (DictionaryException exception) {
			// TODO: handle
		}
	}

	public void cancelTranslation() {
		if (executor == null) {
			throw new IllegalStateException("Set executor first");
		}

		executor.cancelLastTranslation();
	}
}
