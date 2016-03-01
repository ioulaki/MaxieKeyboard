/******************************************************************************
 * Based on code provided as a Copyright 2011 KeyPoint Technologies (UK) Ltd.   
 * All rights reserved. This program and the accompanying materials   
 * are made available under the terms of the Eclipse Public License v1.0  
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html

 *           
 * Contributors: 
 * KeyPoint Technologies (UK) Ltd - Initial API and implementation
 * Andreas Komninos, University of Strathclyde - Additional code implementation
 * http://www.komninos.info
 * http://mobiquitous.cis.strath.ac.uk
 *****************************************************************************/
package com.strathclyde.corehandler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

public class CopyAssets {
	/**
	 * Initiates copy of core files from assets
	 * @param filePath
	 * @param assetMgr
	 */
	public static void atxAssestCopy(String filePath, AssetManager assetMgr) {

		String path = filePath+"/Profile/Profile";
		File atxFile = new File(path);
		if(atxFile.exists()) {
			return;
		}

		AssetManager am = assetMgr;
		try {
			AssetFileDescriptor af = am.openFd("Profile.zip");
			long filesize = af.getLength();
			InputStream isd = am.open("Profile.zip");
			OutputStream os = new FileOutputStream(filePath+"/Profile.zip"); 
			byte[] b = new byte[(int)filesize]; 
			int length;
			while ((length = isd.read(b))>0) { os.write(b,0,length);}
			isd.close();
			os.close(); 
		} catch (IOException e) {
			e.printStackTrace();
		}

		String zipPath = filePath + "/Profile.zip";
		File atxZipFile = new File(zipPath);
		if(atxZipFile.exists()) {
			unzip(zipPath);
		}

		atxZipFile.delete();
	}

	/**
	 * Reads and unzips a zipped file 
	 * @param zipFileName
	 */
	private static void unzip(String zipFileName) {
		try {
			File file = new File(zipFileName);
			ZipFile zipFile = new ZipFile(file);

			// create a directory named the same as the zip file in the 
			// same directory as the zip file.
			File zipDir = new File(file.getParentFile(), "Profile");
			zipDir.mkdir();

			Enumeration<?> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();

				String nme = entry.getName();
				// File for current file or directory
				File entryDestination = new File(zipDir, nme);

				// This file may be in a subfolder in the Zip bundle
				// This line ensures the parent folders are all
				// created.
				entryDestination.getParentFile().mkdirs();

				// Directories are included as seperate entries 
				// in the zip file.
				if(!entry.isDirectory()) {
					generateFile(entryDestination, entry, zipFile);
				} else {
					entryDestination.mkdirs();
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param entryDestination
	 * @param entry
	 * @param zipFile
	 */
	private static void generateFile(File destination, ZipEntry entry, ZipFile owner) {
		InputStream in = null;
		OutputStream out = null;

		InputStream rawIn;
		try {
			rawIn = owner.getInputStream(entry);
			in = new BufferedInputStream(rawIn, 1024);
			FileOutputStream rawOut = new FileOutputStream(destination);
			out = new BufferedOutputStream(rawOut, 1024);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
