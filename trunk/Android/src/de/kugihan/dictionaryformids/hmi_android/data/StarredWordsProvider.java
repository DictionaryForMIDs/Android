package de.kugihan.dictionaryformids.hmi_android.data;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.hmi_android.DictionaryForMIDs;
import de.kugihan.dictionaryformids.translation.SingleTranslationExtension;

/**
 * Abstraction to save starred words in a database.
 *
 */
public class StarredWordsProvider extends ContentProvider {

	private static final int ALL_BY_ALL_PRIMITIVES = 6;

	private static final int COUNT_BY_DICTIONARY_NAME = 5;

	private static final int ALL_BY_DICTIONARY_NAME = 4;

	private static final int ITEM = 3;

	private static final int COUNT = 2;

	private static final int ALL = 1;

	/**
	 * Columns available in this provider's methods.
	 *
	 */
	public static class StarredWords implements BaseColumns {
		/**
		 * Name of the database column holding the dictionary id.
		 */
		public static final String DICTIONARY_NAME = "dictionaryName";

		/**
		 * Name of the database column holding the language id of the term.
		 */
		public static final String FROM_LANGUAGE_ID = "fromLanguageId";

		/**
		 * Name of the database column holding an entry's translations.
		 */
		public static final String TRANSLATION = "translation";

		/**
		 * Name of the database column holding an entry's fromText.
		 */
		public static final String FROM_TEXT = "fromText";

		/**
		 * Name of the database column holding an entry's toTexts.
		 */
		public static final String TO_TEXTS = "toTexts";
	}

	/**
	 * Authority of this content provider.
	 */
	public static final String AUTHORITY = "de.kugihan.dictionaryformids.hmi_android.data.starredwordsprovider";

	/**
	 * Uri to all items of this provider.
	 */
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	/**
	 * Uri matcher that is used by all provider methods.
	 */
	private static final UriMatcher URI_MATCHER;

	/**
	 * Name of the database.
	 */
	private static final String DATABASE_NAME = "dictionary";

	/**
	 * Name of the table.
	 */
	private static final String DATABASE_TABLE = "words";

	/**
	 * Version of the database layout.
	 */
	private static final int DATABASE_VERSION = 2;

	/**
	 * Query to create the initial database.
	 */
	private static final String DATABASE_CREATE_QUERY = "CREATE TABLE " + DATABASE_TABLE + " ("
			+ StarredWords._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + StarredWords.TRANSLATION
			+ " BLOB NOT NULL, " + StarredWords.FROM_TEXT + " TEXT, " + StarredWords.TO_TEXTS
			+ " TEXT, " + StarredWords.DICTIONARY_NAME + " TEXT, " + StarredWords.FROM_LANGUAGE_ID
			+ " INTEGER);";

	/**
	 * Handle to the database helper.
	 */
	private DatabaseHelper databaseHelper;

	/**
	 * Helper to manage database updates.
	 *
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE_QUERY);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// continue using old database
			// TODO remove dropping
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			db.execSQL(DATABASE_CREATE_QUERY);
		}
	}

	/**
	 * Creates content values for a given translation for insertion into
	 * database.
	 *
	 * @param dictionaryId
	 *            the id of the dictionary the translation belongs to
	 * @param translation
	 *            the translation result to save
	 * @return the content values corresponding to the given translation
	 */
	public static ContentValues getContentValues(final String dictionaryId,
			final SingleTranslationExtension translation) {
		final byte[] object;
		final String fromTextAsString;
		final String toTextsAsString;
		try {
			object = translation.serialize();
			fromTextAsString = translation.getFromTextAsString();
			toTextsAsString = translation.getToTextsAsString();
		} catch (IOException e) {
			Log.d(DictionaryForMIDs.LOG_TAG, "Serialization failed", e);
			return null;
		} catch (DictionaryException e) {
			Log.d(DictionaryForMIDs.LOG_TAG, "Parsing failed", e);
			return null;
		}

		final ContentValues initialValues = new ContentValues();
		initialValues.put(StarredWords.DICTIONARY_NAME, dictionaryId);
		initialValues.put(StarredWords.TRANSLATION, object);
		initialValues.put(StarredWords.FROM_LANGUAGE_ID, translation.getFromText()
				.getLanguageIndex());
		initialValues.put(StarredWords.FROM_TEXT, fromTextAsString);
		initialValues.put(StarredWords.TO_TEXTS, toTextsAsString);
		return initialValues;
	}

	/**
	 * Gets the row id of a translation result.
	 *
	 * @param values
	 *            the content values describing the item to retrieve (as created
	 *            by getContentValues())
	 * @return the id of the entry or null in case it could not be found
	 */
	public Long getItemId(final ContentValues values) {
		Long itemId = null;

		// query table using string conditions
		final String selection = StarredWords.DICTIONARY_NAME + " = ? AND "
				+ StarredWords.FROM_LANGUAGE_ID + " = ? AND " + StarredWords.FROM_TEXT
				+ " = ? AND " + StarredWords.TO_TEXTS + " = ?";
		final String[] selectionArgs = { values.getAsString(StarredWords.DICTIONARY_NAME),
				values.getAsString(StarredWords.FROM_LANGUAGE_ID),
				values.getAsString(StarredWords.FROM_TEXT),
				values.getAsString(StarredWords.TO_TEXTS) };

		final Cursor cursor = query(CONTENT_URI, new String[] { StarredWords._ID,
				StarredWords.TRANSLATION }, selection, selectionArgs, null);

		final byte[] compareBlob = values.getAsByteArray(StarredWords.TRANSLATION);
		itemId = findBlob(StarredWords.TRANSLATION, compareBlob, cursor);
		cursor.close();

		return itemId;
	}

	/**
	 * Gets the row id of a translation result.
	 *
	 * @param resolver
	 *            resolver to be used for querying for the translation
	 * @param translationExtension
	 *            the translation to be searched for
	 * @param dictionaryName
	 *            the name of the dictionary
	 * @return the id of the entry or null in case it could not be found
	 */
	public static Long getItemId(final ContentResolver resolver,
			final SingleTranslationExtension translationExtension, final String dictionaryName) {
		final String fromTextAsString;
		final String toTextsAsString;
		try {
			fromTextAsString = translationExtension.getFromTextAsString();
			toTextsAsString = translationExtension.getToTextsAsString();
		} catch (DictionaryException e) {
			Log.d(DictionaryForMIDs.LOG_TAG, "Parsing failed", e);
			return null;
		}

		final String selection = StarredWords.DICTIONARY_NAME + " = ? AND "
				+ StarredWords.FROM_LANGUAGE_ID + " = ? AND " + StarredWords.FROM_TEXT
				+ " = ? AND " + StarredWords.TO_TEXTS + " = ?";
		final String[] selectionArgs = { dictionaryName,
				Integer.toString(translationExtension.getFromText().getLanguageIndex()),
				fromTextAsString, toTextsAsString };
		final Cursor cursor = resolver.query(StarredWordsProvider.CONTENT_URI, new String[] {
				StarredWords._ID, StarredWords.TRANSLATION }, selection, selectionArgs, null);

		if (cursor == null) {
			return null;
		}

		final byte[] compareBlob;
		try {
			compareBlob = translationExtension.serialize();
		} catch (IOException e) {
			Log.d(DictionaryForMIDs.LOG_TAG, "Serialization failed", e);
			return null;
		} catch (DictionaryException e) {
			Log.d(DictionaryForMIDs.LOG_TAG, "Parsing failed", e);
			return null;
		}

		final Long itemId = findBlob(StarredWords.TRANSLATION, compareBlob, cursor);
		cursor.close();

		return itemId;
	}

	/**
	 * Searches a cursor for the specified blob and returns the row id.
	 *
	 * @param column
	 *            the title of the column to search for the blob
	 * @param blobToFind
	 *            the blob to find
	 * @param cursor
	 *            the cursor to search
	 * @return the id of the row including the blob or null if it could not be
	 *         found
	 */
	public static Long findBlob(final String column, final byte[] blobToFind, final Cursor cursor) {
		Long itemId = null;
		// compare byte conditions
		while (cursor.moveToNext()) {
			final byte[] savedObject = cursor.getBlob(cursor.getColumnIndex(column));
			if (Arrays.equals(blobToFind, savedObject)) {
				itemId = cursor.getLong(cursor.getColumnIndex(StarredWords._ID));
				break;
			}
		}
		return itemId;
	}

	/**
	 * Gets the id of an item from a cursor.
	 *
	 * @param cursor
	 *            the cursor pointing to the item
	 * @return the id of the item
	 */
	public static long getItemId(final Cursor cursor) {
		return cursor.getLong(cursor.getColumnIndexOrThrow(StarredWords._ID));
	}

	/**
	 * Gets the dictionary id of an item from a cursor.
	 *
	 * @param cursor
	 *            the cursor pointing to the item
	 * @return the dictionary id of the item
	 */
	public static String getDictionaryId(final Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(StarredWords.DICTIONARY_NAME));
	}

	/**
	 * Gets the language id of the fromText from a cursor.
	 *
	 * @param cursor
	 *            the cursor pointing to the item
	 * @return the language id of the item
	 */
	public static int getFromLanguageId(final Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(StarredWords.FROM_LANGUAGE_ID));
	}

	/**
	 * Gets the translation from a cursor.
	 *
	 * @param cursor
	 *            the cursor pointing to the item
	 * @return the translation
	 */
	public static SingleTranslationExtension getTranslation(final Cursor cursor) {
		final byte[] data = cursor.getBlob(cursor.getColumnIndex(StarredWords.TRANSLATION));
		try {
			return SingleTranslationExtension.unserialize(data);
		} catch (IOException e) {
			Log.d(DictionaryForMIDs.LOG_TAG, "IOException on unserialize from Cursor", e);
		} catch (ClassNotFoundException e) {
			Log.d(DictionaryForMIDs.LOG_TAG, "ClassNotFound on unserialize from Cursor", e);
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int result = 0;
		final SQLiteDatabase db = databaseHelper.getWritableDatabase();
		final int match = URI_MATCHER.match(uri);
		switch (match) {
		case ITEM:
			final String id = uri.getPathSegments().get(0);
			String where = StarredWords._ID + " = " + id;
			if (!TextUtils.isEmpty(selection)) {
				where += " AND (" + selection + ")";
			}
			result = db.delete(DATABASE_TABLE, where, selectionArgs);
			break;

		case ALL:
			result = db.delete(DATABASE_TABLE, selection, selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Invalid uri: " + match);
		}
		if (result > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType(Uri uri) {
		final int match = URI_MATCHER.match(uri);
		switch (match) {
		case ALL:
		case ALL_BY_DICTIONARY_NAME:
			return "vnd.android.cursor.dir";
		case ITEM:
		case COUNT:
		case COUNT_BY_DICTIONARY_NAME:
			return "vnd.android.cursor.item";
		default:
			throw new IllegalArgumentException("Invalid uri: " + match);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (URI_MATCHER.match(uri) != ALL) {
			throw new IllegalArgumentException("Invalid uri");
		}
		if (values == null) {
			throw new NullPointerException("Values must be specified");
		}

		// check if item already exists
		final Long existingId = getItemId(values);
		if (existingId != null) {
			// return existing item's id to prevent duplicates
			return ContentUris.withAppendedId(CONTENT_URI, existingId);
		}

		final SQLiteDatabase db = databaseHelper.getWritableDatabase();
		final long rowId = db.insert(DATABASE_TABLE, null, values);
		final Uri result = ContentUris.withAppendedId(CONTENT_URI, rowId);
		getContext().getContentResolver().notifyChange(result, null);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreate() {
		databaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(DATABASE_TABLE);
		final int match = URI_MATCHER.match(uri);
		switch (match) {
		case ALL:
			// no need to refine the query
			break;

		case ALL_BY_DICTIONARY_NAME:
			builder.appendWhere(StarredWords.DICTIONARY_NAME + "=");
			builder.appendWhereEscapeString(uri.getPathSegments().get(1));
			break;

		case ALL_BY_ALL_PRIMITIVES:
			if (selection != null || selectionArgs != null) {
				throw new IllegalArgumentException("selection and selectionArgs must be null.");
			}
			selection = StarredWords.DICTIONARY_NAME + " = ? AND "
					+ StarredWords.FROM_LANGUAGE_ID + " = ? AND "
					+ StarredWords.FROM_TEXT + " = ? AND "
					+ StarredWords.TO_TEXTS + " = ?";
			final List<String> segments = uri.getPathSegments();
			selectionArgs = new String[] { segments.get(2), segments.get(1), segments.get(3),
					segments.get(4) };
			break;

		case ITEM:
			builder.appendWhere(StarredWords._ID + "=");
			builder.appendWhereEscapeString(uri.getPathSegments().get(0));
			break;

		case COUNT_BY_DICTIONARY_NAME:
			builder.appendWhere(StarredWords.DICTIONARY_NAME + "=");
			builder.appendWhereEscapeString(uri.getPathSegments().get(1));
			// continue with COUNT case
		case COUNT:
			if (projection != null) {
				throw new IllegalArgumentException("Projection must be null for count queries.");
			}
			projection = new String[] { "COUNT(*)" };
			break;

		default:
			throw new IllegalArgumentException("Invalid uri: " + match);
		}
		final SQLiteDatabase db = databaseHelper.getReadableDatabase();
		final Cursor cursor = builder.query(db, projection, selection, selectionArgs, null, null,
				null);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (URI_MATCHER.match(uri) != ITEM) {
			throw new IllegalArgumentException("Invalid uri");
		}
		String whereClause = StarredWords._ID + "=" + uri.getPathSegments().get(0);
		if (!TextUtils.isEmpty(selection)) {
			whereClause += " AND (" + selection + ")";
		}
		final SQLiteDatabase db = databaseHelper.getWritableDatabase();
		final int rows = db.update(DATABASE_TABLE, values, whereClause, selectionArgs);
		if (rows > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return rows;
	}

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "#", ITEM);
		URI_MATCHER.addURI(AUTHORITY, "", ALL);
		URI_MATCHER.addURI(AUTHORITY, null, ALL);
		URI_MATCHER.addURI(AUTHORITY, "allByDictionaryName/*", ALL_BY_DICTIONARY_NAME);
		URI_MATCHER.addURI(AUTHORITY, "allByAllColumns/#/*/*/*", ALL_BY_ALL_PRIMITIVES);
		URI_MATCHER.addURI(AUTHORITY, "count", COUNT);
		URI_MATCHER.addURI(AUTHORITY, "countByDictionaryName/*", COUNT_BY_DICTIONARY_NAME);
	}

}
