package de.kugihan.dictionaryformids.hmi_android.data;

import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.translation.TranslationExecution;
import de.kugihan.dictionaryformids.translation.TranslationExecutionCallback;
import de.kugihan.dictionaryformids.translation.TranslationParameters;

public class DfMTranslationExecutor implements TranslationExecutor {

	@Override
	public void setTranslationExecutionCallback(
			TranslationExecutionCallback translationResultHMIObjParam) {
		TranslationExecution.setTranslationExecutionCallback(translationResultHMIObjParam);
	}

	@Override
	public void executeTranslation(TranslationParameters translationParametersObj)
			throws DictionaryException {
		TranslationExecution.executeTranslation(translationParametersObj);
	}

	@Override
	public void cancelLastTranslation() {
		TranslationExecution.cancelLastTranslation();
	}

}
