/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.hmi_android.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * DownloadDictionaryItem is the class that represents a downloadable
 * dictionary.
 * 
 */
public final class DownloadDictionaryItem implements Parcelable {
	
	/**
	 * The dictionaryId of the dictionary.
	 */
	private int dictionaryId;

	/**
	 * The name of the dictionary.
	 */
	private String dictionaryName;

	/**
	 * The link of the dictionary.
	 */
	private String dictionaryLink;

	/**
	 * The file name of the dictionary.
	 */
	private String dictionaryFileName;

	/**
	 * The size of the dictionary.
	 */
	private long dictionarySize;

	/**
	 * Constructor to fill all standard parameters.
	 * 
	 * @param id
	 *            the id of the dictionary
	 * @param name
	 *            the name of the dictionary
	 * @param link
	 *            the link of the dictionary
	 * @param fileName
	 *            the file name of the dictionary
	 * @param size
	 *            the size of the dictionary
	 */
	public DownloadDictionaryItem(final int id, final String name,
			final String link, final String fileName, final long size) {
		dictionaryId = id;
		dictionaryName = name;
		dictionaryLink = link;
		dictionaryFileName = fileName;
		dictionarySize = size;
	}
	
	/**
	 * Returns the dictionaryId of the dictionary.
	 * 
	 * @return the dictionaryId of the dictionary
	 */
	public int getId() {
		return dictionaryId;
	}

	/**
	 * Returns the name of the dictionary.
	 * 
	 * @return the name of the dictionary
	 */
	public String getName() {
		return dictionaryName;
	}

	/**
	 * Returns the link of the dictionary.
	 * 
	 * @return the link of the dictionary
	 */
	public String getLink() {
		return dictionaryLink;
	}

	/**
	 * Returns the file name of the dictionary.
	 * 
	 * @return the file name of the dictionary
	 */
	public String getFileName() {
		return dictionaryFileName;
	}

	/**
	 * Returns the size of the dictionary.
	 * 
	 * @return the size of the dictionary
	 */
	public long getSize() {
		return dictionarySize;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeToParcel(final Parcel out, final int flags) {
		out.writeString(dictionaryName);
		out.writeString(dictionaryLink);
		out.writeString(dictionaryFileName);
		out.writeLong(dictionarySize);
	}

	/**
	 * Needed by Parcelable.
	 */
	public static final Parcelable.Creator<DownloadDictionaryItem> CREATOR = new Parcelable.Creator<DownloadDictionaryItem>() {
		public DownloadDictionaryItem createFromParcel(final Parcel in) {
			return new DownloadDictionaryItem(in);
		}

		public DownloadDictionaryItem[] newArray(final int size) {
			return new DownloadDictionaryItem[size];
		}
	};

	/**
	 * Constructor to create an instance from a parcel.
	 * 
	 * @param in
	 *            the parcel to construct an instance
	 */
	private DownloadDictionaryItem(final Parcel in) {
		dictionaryName = in.readString();
		dictionaryLink = in.readString();
		dictionaryFileName = in.readString();
		dictionarySize = in.readLong();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return dictionaryName;
	}
}
