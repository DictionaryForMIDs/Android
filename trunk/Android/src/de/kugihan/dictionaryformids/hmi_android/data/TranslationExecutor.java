package de.kugihan.dictionaryformids.hmi_android.data;

import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.translation.TranslationExecutionCallback;
import de.kugihan.dictionaryformids.translation.TranslationParameters;
import de.kugihan.dictionaryformids.translation.TranslationParametersBatch;

public interface TranslationExecutor {

	public void setTranslationExecutionCallback(
			TranslationExecutionCallback translationResultHMIObjParam);

	public void executeTranslation(TranslationParameters translationParametersObj)
			throws DictionaryException;

	public void executeTranslationBatch(TranslationParametersBatch translationParametersBatchObj)
			throws DictionaryException;

	public void cancelLastTranslation();

}
