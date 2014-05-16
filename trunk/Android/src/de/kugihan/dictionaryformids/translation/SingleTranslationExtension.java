package de.kugihan.dictionaryformids.translation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.dataaccess.content.FontStyle;
import de.kugihan.dictionaryformids.dataaccess.content.RGBColour;
import de.kugihan.dictionaryformids.dataaccess.content.SelectionMode;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.hmi_common.content.ContentParser;
import de.kugihan.dictionaryformids.hmi_common.content.StringColourItemText;
import de.kugihan.dictionaryformids.hmi_common.content.StringColourItemTextPart;

public class SingleTranslationExtension extends SingleTranslation {

	/**
	 * The fromText items or null if getFromText() should be used.
	 */
	private StringColourItemText fromText = null;

	/**
	 * The toTexts items or null if getToTexts() should be used.
	 */
	private Vector<StringColourItemText> toTexts = null;

	public boolean isStarred() {
		return isStarred != null && isStarred;
	}

	public boolean isStarredLoaded() {
		return isStarred != null;
	}

	public void setStarred(Boolean isStarred) {
		this.isStarred = isStarred;
	}

	/**
	 * True if this translation has been starred, null if not checked yet.
	 */
	private Boolean isStarred = null;

	public DictionaryDataFile getDictionary() {
		return dictionaryDataFile;
	}

	/**
	 * The dictionary in which this result was found.
	 */
	private DictionaryDataFile dictionaryDataFile = null;

	/**
	 * Creates an instance from the given translation object.
	 *
	 * @param translation
	 *            the object's data to use for initialization
	 */
	public SingleTranslationExtension(final SingleTranslation translation, final DictionaryDataFile dataFile) {
		super(translation.fromText, translation.toTexts, translation.foundAtBeginOfExpression,
				translation.primarySortNumber, translation.directoryFileLocation);
		fromText = null;
		toTexts = null;
		dictionaryDataFile = dataFile;
	}

	/**
	 * Creates an instance from the given parameters.
	 *
	 * @param fromTextParam
	 *            the fromText to use
	 * @param toTextsParam
	 *            the toTexts to use
	 * @param foundAtBeginOfExpressionParam
	 *            see SingleTranslation()
	 * @param primarySortNumberParam
	 *            SingleTranslation()
	 * @param directoryFileLocationParam
	 *            SingleTranslation()
	 */
	public SingleTranslationExtension(final StringColourItemText fromTextParam,
			final Vector<StringColourItemText> toTextsParam,
			final boolean foundAtBeginOfExpressionParam, final int primarySortNumberParam,
			final DirectoryFileLocation directoryFileLocationParam, final DictionaryDataFile dataFile) {
		super(new TextOfLanguage("", 0, dataFile), null, foundAtBeginOfExpressionParam,
				primarySortNumberParam, directoryFileLocationParam);
		fromText = fromTextParam;
		toTexts = toTextsParam;
		dictionaryDataFile = dataFile;
	}

	/**
	 * Creates a textual representation of toTexts for searches.
	 *
	 * @return the textual representation including all toTexts
	 * @throws DictionaryException
	 *             if parsing fails
	 */
	public String getToTextsAsString() throws DictionaryException {
		return getToTextsAsString("\0");
	}

	/**
	 * Creates a textual representation of toTexts joined by the specified
	 * separator.
	 *
	 * @param separator
	 *            string to be added between toTexts
	 * @return the textual representation including all toTexts
	 * @throws DictionaryException
	 *             if parsing fails
	 */
	public String getToTextsAsString(final String separator) throws DictionaryException {
		final Vector<StringColourItemText> texts = getToTextsAsColourItemTexts();

		final StringBuffer toTextsBuffer = new StringBuffer();
		for (int i = 0; i < texts.size(); i++) {
			if (i > 0) {
				toTextsBuffer.append(separator);
			}
			final StringColourItemText text = texts.elementAt(i);
			for (int j = 0; j < text.size(); j++) {
				toTextsBuffer.append(text.getItemTextPart(j).getText());
			}
		}

		return toTextsBuffer.toString();
	}

	/**
	 * Creates a textual representation of fromText.
	 *
	 * @return the textual representation of fromText
	 * @throws DictionaryException
	 *             in case parsing fails
	 */
	public String getFromTextAsString() throws DictionaryException {
		final StringColourItemText text = getFromTextAsColourItemText();

		final StringBuffer fromTextBuffer = new StringBuffer();
		for (int i = 0; i < text.size(); i++) {
			fromTextBuffer.append(text.getItemTextPart(i).getText());
		}

		return fromTextBuffer.toString();
	}

	/**
	 * Returns the StringColourItemText of the current translation's fromText.
	 *
	 * @return the StringColourItemText of the current translation's fromText
	 * @throws DictionaryException
	 *             in case of parsing errors
	 */
	public StringColourItemText getFromTextAsColourItemText() throws DictionaryException {
		if (fromText == null) {
			fromText = getStringColourItemText(getFromText(), true);
		}
		return fromText;
	}

	/**
	 * Returns all the StringColourItemText of the current translation's
	 * toTexts.
	 *
	 * @return all the StringColourItemText of the current translation's toTexts
	 * @throws DictionaryException
	 *             in case of parsing errors
	 */
	public Vector<StringColourItemText> getToTextsAsColourItemTexts() throws DictionaryException {
		if (toTexts == null) {
			toTexts = getStringColourItemTexts(getToTexts(), false);
		}
		return toTexts;
	}

	/**
	 * Creates a byte representation of the current instance.
	 *
	 * @return a byte representation of the current instance
	 * @throws IOException
	 *             in case of writing exceptions
	 * @throws DictionaryException
	 *             in case of parsing exceptions
	 */
	public byte[] serialize() throws IOException, DictionaryException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(bos);
			oos.writeBoolean(foundAtBeginOfExpression);
			oos.writeInt(primarySortNumber);
			// directoryFileLocation
			oos.writeInt(directoryFileLocation.directoryFileNumber);
			oos.writeUTF(directoryFileLocation.postfixDictionaryFile);
			oos.writeInt(directoryFileLocation.positionInDirectoryFile);
			// fromText
			writeStringColourItemText(oos, getFromTextAsColourItemText());
			// toTexts
			writeStringColourItemTexts(oos, getToTextsAsColourItemTexts());
		} finally {
			try {
				if (oos != null) {
					oos.close();
				}
			} catch (IOException e) {
				// ignore
			}
		}
		return bos.toByteArray();
	}

	/**
	 * Serializes a Vector of StringColourItemText.
	 *
	 * @param outputStream
	 *            the stream to serialize to
	 * @param texts
	 *            the texts to serialize
	 * @throws IOException
	 *             in case of writing exceptions
	 */
	private void writeStringColourItemTexts(ObjectOutputStream outputStream,
			Vector<StringColourItemText> texts) throws IOException {
		outputStream.writeInt(texts.size());
		for (StringColourItemText text : texts) {
			writeStringColourItemText(outputStream, text);
		}
	}

	/**
	 * Serializes a StringColourItemText.
	 *
	 * @param outputStream
	 *            the stream to serialize to
	 * @param text
	 *            the text to serialize
	 * @throws IOException
	 *             in case of writing exceptions
	 */
	private void writeStringColourItemText(ObjectOutputStream outputStream,
			StringColourItemText text) throws IOException {
		outputStream.writeInt(text.size());
		for (int i = 0; i < text.size(); i++) {
			final StringColourItemTextPart part = text.getItemTextPart(i);
			outputStream.writeUTF(part.getText());
			outputStream.writeInt(part.getColour().red);
			outputStream.writeInt(part.getColour().green);
			outputStream.writeInt(part.getColour().blue);
			outputStream.writeInt(part.getStyle().style);
			outputStream.writeInt(part.getSelectionMode().mode);
		}
	}

	/**
	 * Unserialize a Vector of StringColourItemTexts.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @return the vector representation
	 * @throws IOException
	 *             in case of reading exceptions
	 */
	private static Vector<StringColourItemText> readStringColourItemTexts(
			ObjectInputStream inputStream)
			throws IOException {
		final int size = inputStream.readInt();
		final Vector<StringColourItemText> result = new Vector<StringColourItemText>(size);
		for (int i = 0; i < size; i++) {
			result.add(readStringColourItemText(inputStream));
		}
		return result;
	}

	/**
	 * Unserialize a StringColourItemText.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @return the StringColourItemText read
	 * @throws IOException
	 *             in case of reading exceptions
	 */
	private static StringColourItemText readStringColourItemText(ObjectInputStream inputStream)
			throws IOException {
		final StringColourItemText result = new StringColourItemText();
		final int size = inputStream.readInt();
		for (int i = 0; i < size; i++) {
			result.addItemTextPart(readStringColourItemTextPart(inputStream));
		}
		return result;
	}

	/**
	 * Unserialize a StringColourItemTextPart.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @return the StringColourItemTextPart read
	 * @throws IOException
	 *             in case of reading exceptions
	 */
	private static StringColourItemTextPart readStringColourItemTextPart(
			ObjectInputStream inputStream)
			throws IOException {
		final String text = inputStream.readUTF();
		final RGBColour colour = new RGBColour(inputStream.readInt(), inputStream.readInt(),
				inputStream.readInt());
		final FontStyle style = new FontStyle(inputStream.readInt());
		final SelectionMode selectionMode = new SelectionMode(inputStream.readInt());
		return new StringColourItemTextPart(text, colour, style, selectionMode);
	}

	/**
	 * Unserialize an SingleTranslationExtension from a byte array.
	 *
	 * @param data
	 *            the byte array to read from
	 * @return the unserialized instance
	 * @throws IOException
	 *             in case of reading exceptions
	 * @throws ClassNotFoundException
	 *             in case of problems when creating the instance
	 */
	public static SingleTranslationExtension unserialize(final byte[] data) throws IOException,
			ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(bis);
			final boolean foundAtBeginOfExpression = ois.readBoolean();
			final int primarySortNumber = ois.readInt();
			final DirectoryFileLocation directoryFileLocation = new DirectoryFileLocation(
					ois.readInt(), ois.readUTF(), ois.readInt());
			final StringColourItemText fromText = readStringColourItemText(ois);
			final Vector<StringColourItemText> toTexts = readStringColourItemTexts(ois);
			// TODO: last parameter
			return new SingleTranslationExtension(fromText, toTexts, foundAtBeginOfExpression,
					primarySortNumber, directoryFileLocation, null);
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Reads a StringColourItemText from a TextOfLanguage.
	 * 
	 * @param data
	 *            the TextOfLanguage to read from
	 * @param isInput
	 *            true if it is a fromText
	 * @return the StringColourItemTextPart
	 * @throws DictionaryException
	 *             in case of parsing exceptions
	 */
	private StringColourItemText getStringColourItemText(final TextOfLanguage data,
			final boolean isInput) throws DictionaryException {
		return new ContentParser().determineItemsFromContent(data, true, isInput);
	}

	/**
	 * Reads a Vector of StringColourItemTextPart from a Vector<?> of
	 * TextOfLanguage objects.
	 *
	 * @param data
	 *            a Vector of TextOfLanguage objects
	 * @param isInput
	 *            true if it is a fromText
	 * @return a Vector of StringColourItemTextPart
	 * @throws DictionaryException
	 *             in case of parsing errors
	 */
	private Vector<StringColourItemText> getStringColourItemTexts(final Vector<?> data,
			final boolean isInput) throws DictionaryException {
		final Vector<StringColourItemText> result = new Vector<StringColourItemText>();
		for (Object object : data) {
			final TextOfLanguage text = (TextOfLanguage) object;
			result.add(getStringColourItemText(text, isInput));
		}
		return result;
	}
}
