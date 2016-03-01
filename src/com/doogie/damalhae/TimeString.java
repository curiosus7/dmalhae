package com.doogie.damalhae;

public class TimeString {
	public int hour;
	public int minute;
	public int sec;
	public int remain;
	public int size;

	public TimeString(int millisec) {
		hour = millisec/1000/60/60;
		minute = millisec/1000/60 - hour*60;
		sec = millisec / 1000 - millisec/1000/60*60;
		remain = millisec - millisec/1000*1000;
		if (hour>0) {
			size = 4;
		} else if (minute>0) {
			size = 3;
		} else if (sec>5) {
			size = 2;
		} else {
			size = 1;
		}
	}
	public String getFullSecStr() {
		return String.format("%01d:%02d:%02d", hour, minute, sec);
	}
	
	public String getFullMilliStr(int divide) { 
		return getFullSecStr() + "." + remain/divide;
	}
	public String getString() {
		return getString(size);
	}
	public String getString(int size) {
		switch (size) {
		case 4:
			return String.format("%01d:%02d:%02d", hour, minute, sec);
		case 3:
			return String.format("%02d:%02d", minute, sec);
		case 2:
			return String.format("%02d", sec);
		}
		return String.format("%02d.%d", sec, remain/100);
	}
	public int getSize() {
		return size;
	}
}

