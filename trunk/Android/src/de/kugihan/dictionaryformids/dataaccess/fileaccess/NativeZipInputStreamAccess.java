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
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.general.CouldNotOpenFileException;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.general.Util;

/**
 * NativeZipInputStreamAccess provides functionality for loading a dictionary from
 * a zip archive.
 *
 */
public class NativeZipInputStreamAccess extends DfMInputStreamAccess {

	/**
	 * Path of the zip-file to which the class is attached.
	 */
	private final String zipfile;

	/**
	 * The path prefix that will be added to all requested files. This allows a
	 * dictionary to be located in a subdirectory of the archive.
	 */
	private String dictionaryRoot = null;
	
	/**
	 * Constructor attaches this class to the specified zip-file. All following
	 * tasks will be using this file.
	 * 
	 * @param zipFilePath
	 *            the zip-file represented by the class
	 */
	public NativeZipInputStreamAccess(final String zipFilePath) {
		zipfile = zipFilePath;
	}
	
	/**
	 * Creates an input stream for this instance's zipfile.
	 * 
	 * @return the InputStream of this instance's zipfile
	 * @throws IOException
	 *             if an input/output exception occured
	 */
	private InputStream openFileConnectionStream() throws IOException {
		return new FileInputStream(zipfile);
	}
	
	/**
	 * Creates a zip input stream for this instance's zipfile.
	 * 
	 * @return the ZipInputStream of this instance's zipfile
	 * @throws IOException
	 *             if an input/output exception occured
	 */
	private ZipInputStream openZipStream() throws IOException {
		return new ZipInputStream(openFileConnectionStream());
	}

	/**
	 * Looks for the given dictionary file in the dictionary directory specified
	 * in {@link DictionaryDataFile}. If the file cannot be found in that
	 * subdirectory, the root directory of the ZIP-archive will be used.
	 * 
	 * @param fileName
	 *            the dictionary file that is to be opened
	 * @throws DictionaryException
	 *             if an input error occurred
	 */
	private void initializeDictionaryRoot(final String fileName)
			throws DictionaryException {
		dictionaryRoot = DictionaryDataFile.pathNameDataFiles + File.separator;
		// check if file exists using newly set dictionaryRoot
		final boolean isRootInSubDirectory = fileExists(fileName);
		if (!isRootInSubDirectory) {
			dictionaryRoot = "";
		}
	}

	/**
	 * Gets the {@link InputStream} of the specified file in the current
	 * zip-file.
	 * 
	 * @param fileName
	 *            the name of the included file
	 * @return the {@link InputStream} or null
	 * @throws DictionaryException
	 *             if an input error occurred
	 */
	private InputStream getInputStreamInternal(final String fileName)
			throws DictionaryException {
		// find out where the dictionary is located on first run
		if (dictionaryRoot == null) {
			initializeDictionaryRoot(fileName);
		}
		// add path prefix to fileName
		final String absolutePath = dictionaryRoot + fileName;
		ZipInputStream zipStream;
    	ZipEntry zippedFile = null;
		try {
	    	zipStream = openZipStream(); 
	    	while ((zippedFile = zipStream.getNextEntry()) != null) {
	    		if (absolutePath.equals(zippedFile.getName())) {
	    			break;
	    		} 
	    	}
		} catch (IOException ioe) {
			 throw new CouldNotOpenFileException(ioe);
		}
		if (zippedFile == null) {
			return null;
		} else {
			return zipStream;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final InputStream getInputStream(final String fileName)
			throws DictionaryException {
		final InputStream zipStream = getInputStreamInternal(fileName);
		if (zipStream == null) {
			Util.getUtil().log("File not found:" + fileName, Util.logLevel3);
			throw new CouldNotOpenFileException(
					"Resource file could not be opened: " + fileName);
		}		
		return zipStream;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean fileExists(final String fileName) 
			throws DictionaryException {
		final InputStream inputStream = getInputStreamInternal(fileName);
		final boolean fileExists = (inputStream != null);
		if (fileExists) {
			try {
				inputStream.close();
			} catch (IOException e) {
				// ignore this exception
			}
		}
    	return fileExists;
	}

	/**
	 * Checks if the current zip-file includes a jar file, which hints on an
	 * included dictionary.
	 * 
	 * @return true if jar file was found
	 * @throws DictionaryException if there was an error opening the file
	 */
	public final boolean hasJarDictionary() throws DictionaryException {
		try {
	    	final ZipInputStream zipStream = openZipStream(); 
	    	ZipEntry zippedFile = null;
	    	while ((zippedFile = zipStream.getNextEntry()) != null) {
	    		if (zippedFile.getName().endsWith(".jar")) {
	    			return true;
	    		}
	    	}
	    	return false;
		} catch (IOException ioe) {
			 throw new CouldNotOpenFileException(ioe);
		}
	}

}
