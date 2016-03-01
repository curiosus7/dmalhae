package com.doogie.damalhae;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.util.Log;

public class UnZipManager {
	File mCourseDir;

	/**
	 * Unzip a zip file.  Will overwrite existing files.
	 * 
	 * @param zipFile Full path of the zip file you'd like to unzip.
	 * @param location Full path of the directory you'd like to unzip to (will be created if it doesn't exist).
	 * @throws IOException
	 */
	int BUFFER_SIZE =4000; 
	public UnZipManager(String zipFile, String location) throws IOException {
		int size;
		byte[] buffer = new byte[BUFFER_SIZE];

		try {
			if ( !location.endsWith("/") ) {
				location += "/";
			}
			File f = new File(location);
			if(!f.isDirectory()) {
				f.mkdirs();
			}
			ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), BUFFER_SIZE));
			try {
				ZipEntry ze = null;
				while ((ze = zin.getNextEntry()) != null) {
					String path = location + ze.getName();
					File unzipFile = new File(path);

					if (ze.isDirectory()) {
						if(!unzipFile.isDirectory()) {
							unzipFile.mkdirs();
							mCourseDir = unzipFile;
						}
					} else {
						// check for and create parent directories if they don't exist
						File parentDir = unzipFile.getParentFile();
								if ( null != parentDir ) {
									mCourseDir = parentDir;
									if ( !parentDir.isDirectory() ) {
										parentDir.mkdirs();
									}
								}

								// unzip the file
								FileOutputStream out = new FileOutputStream(unzipFile, false);
								BufferedOutputStream fout = new BufferedOutputStream(out, BUFFER_SIZE);
								try {
									while ( (size = zin.read(buffer, 0, BUFFER_SIZE)) != -1 ) {
										fout.write(buffer, 0, size);
									}

									zin.closeEntry();
								}
								finally {
									fout.flush();
									fout.close();
								}
					}
				}
			}
			finally {
				zin.close();
			}
		}
		catch (Exception e) {
			Log.e("Directory", "Unzip exception", e);
		}
	}
	public File getCourseDir() {
		return mCourseDir;
	}
}


