package com.doogie.damalhae;

import java.io.Serializable;

public class FileInfo implements Serializable {
	public String fileName;
	public boolean needRecording;
	public int level;
	public int total;
	public FileInfo(String fileName) {
		this.fileName = fileName;
		needRecording = false;
		level = 0;
		total = 0;
	}
}
