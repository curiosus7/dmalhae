package com.doogie.damalhae;

import java.io.Serializable;

public class SentenceInfo implements Serializable, Comparable<SentenceInfo> {
	public final static int TYPE_INVALID = 0;
	public final static int TYPE_SOUND = 1;
	public final static int TYPE_MEANING_PREV = 2;
//	public final static int TYPE_MEANING_NEXT = 3;
	public final static int TYPE_SUBTITLE_BLANK = 3;
	
	public final static int TYPE_MAX = 3;
	
	
	public final static int MEANING_TYPE_FILE = 0;
	public final static int MEANING_TYPE_PREV = 1;
	public final static int MEANING_TYPE_NEXT = 2;
	
	private long mStart;
	private long mEnd;

	private int mType;
	private int mMeaningType;
	private int mLevel;
	private boolean mHasMeaningFile;
	private String mMeaningStr;
	private String mSoundStr;


	public SentenceInfo (long start, long end) {
		mStart = start;
		mEnd = end;
		mType = TYPE_SOUND;
		mMeaningType = MEANING_TYPE_FILE;
		mHasMeaningFile = false;
//		mMeaningStr = "";
	}

	public long getStart() {
		return mStart;
	}

	public int getType() {
		return mType;
	}

	public void setType(int mType) {
		this.mType = mType;
	}


	public String getMeaningStr() {
		return mMeaningStr;
	}

	public void setMeaningStr(String meaningStr) {
		this.mMeaningStr = meaningStr;
	}

	public String getSoundStr() {
		return mSoundStr;
	}

	public void setSoundStr(String mSoundStr) {
		this.mSoundStr = mSoundStr;
	}

	public void setStart(long mStart) {
		this.mStart = mStart;
	}


	public long getEnd() {
		return mEnd;
	}


	public void setEnd(long mEnd) {
		this.mEnd = mEnd;
	}


	
	public int getLevel() {
		return mLevel;
	}

	public void setLevel(int mLevel) {
		this.mLevel = mLevel;
	}




	public int getMeaningType() {
		return mMeaningType;
	}


	public void setMeaningType(int mMeaningType) {
		this.mMeaningType = mMeaningType;
	}


	public int getDuration() {
		// TODO Auto-generated method stub
		return (int) (mEnd - mStart);
	}

	public void levelDown() {
		mLevel--;
	}

	public void levelUp() {
		mHasMeaningFile = true;
		mLevel++;
	}

	public boolean isHasMeaningFile() {
		return mHasMeaningFile;
	}

	public boolean hasMeaningFile() {
		return mHasMeaningFile;
	}

	public void setHasMeaningFile(boolean mHasMeaningFile) {
		this.mHasMeaningFile = mHasMeaningFile;
	}

	@Override
	public int compareTo(SentenceInfo info) {
		return new Long(this.mStart).compareTo(new Long(info.mStart));
	}
	public boolean isTtsable () {
		return (mMeaningStr != null && !mMeaningStr.trim().equals(""));
	}
	
}
