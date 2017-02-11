/*******************************************************************************
 * DictionaryForMIDs - a free multi-language dictionary for mobile devices.
 * Copyright (C) 2005, 2006, 2009 Gert Nuber (dict@kugihan.de) and
 * Achim Weimert (achim.weimert@gmail.com)
 * 
 * GPL applies - see file COPYING for copyright statement.
 ******************************************************************************/
package de.kugihan.dictionaryformids.dataaccess.fileaccess;

import android.content.res.AssetManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import de.kugihan.dictionaryformids.general.CouldNotOpenFileException;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.general.Util;

/**
 * AssetDfMInputStreamAccess provides functionality for loading a dictionary
 * from the applications included assets.
 * 
 */
public class AssetDfMInputStreamAccess extends DfMInputStreamAccess {

	/**
	 * The asset manager to use.
	 */
	private final AssetManager assetManager;

	/**
	 * The base directory in the assets folder that includes the dictionary.
	 */
	private final String directory;

	/**
	 * Creates an InputStream for loading a dictionary from the application's
	 * assets.
	 * 
	 * @param currentAssetManager
	 *            the asset manager that provides the assets to use
	 * @param dictionaryDirectory
	 *            the directory that includes the dictionary files
	 */
	public AssetDfMInputStreamAccess(final AssetManager currentAssetManager,
			final String dictionaryDirectory) {
		assetManager = currentAssetManager;
		directory = dictionaryDirectory;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean fileExists(final String asset)
			throws DictionaryException {
		boolean successfullyOpenedFile;
		InputStream in = null;
		String assetPath = getPath(asset);
		try {
			in = assetManager.open(assetPath);
			successfullyOpenedFile = true;
		} catch (IOException e) {
			successfullyOpenedFile = false;
		}
		if (successfullyOpenedFile) {
			try {
				in.close();
			} catch (IOException e) {
				Util.getUtil().log(
						"Asset file could not be closed:" + assetPath,
						Util.logLevel3);
				throw new DictionaryException(
						"Asset file could not be closed: " + assetPath);
			}
		}
		return successfullyOpenedFile;
	}

	/**
	 * Returns the path of the asset according to the base directory.
	 * 
	 * @param asset
	 *            the path of a dictionary file
	 * @return the absolute path to the asset
	 */
	private String getPath(final String asset) {
		return directory + File.separator + asset;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final InputStream getInputStream(final String asset)
			throws DictionaryException {
		InputStream in;
		final String assetPath = getPath(asset);
		try {
			in = assetManager.open(assetPath);
		} catch (IOException e) {
			Util.getUtil().log("Asset file not found:" + assetPath,
					Util.logLevel3);
			throw new CouldNotOpenFileException(
					"Asset file could not be opened: " + assetPath);
		}
		return in;
	}

}
