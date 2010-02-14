/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.dataaccess.fileaccess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import de.kugihan.dictionaryformids.general.CouldNotOpenFileException;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.general.Util;

/**
 * FileDfMInputStreamAccess provides functionality for loading a dictionary from
 * file system.
 * 
 */
public class FileDfMInputStreamAccess extends DfMInputStreamAccess {

	/**
	 * Specifies the working directory, that all following path names are
	 * relative to.
	 */
	private final String directory;
	
	/**
	 * Creates a new InputStream that uses the files in the specified directory.
	 * 
	 * @param baseDirectory
	 *            the directory that includes the dictionary
	 */
	public FileDfMInputStreamAccess(final String baseDirectory) {
		final boolean doesNotEndWithSeperator = baseDirectory != null
				&& !baseDirectory.endsWith(File.separator);
		if (doesNotEndWithSeperator) {
			directory = baseDirectory + File.separator;
		} else {
			directory = baseDirectory;
		}
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public final boolean fileExists(final String fileName)
			throws DictionaryException {
		final File file = new File(directory + fileName);
		return file.exists();
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public final InputStream getInputStream(final String fileName)
			throws DictionaryException {
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(directory + fileName);
		} catch (FileNotFoundException e) {
			Util.getUtil().log("File not found:" + fileName, Util.logLevel3);
			throw new CouldNotOpenFileException(
					"Resource file could not be opened: " + directory
							+ fileName);
		}
		return stream;
	}

}
