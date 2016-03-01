package com.doogie.damalhae;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.util.Log;

public class CourseInfo implements Serializable{
	public static final int MAX_LEVEL = 9;
	final static String SAMI_ENC = "EUN-KR";
	private static final String TAG = "CourseInfo";

	private String mCourseFilePath;
	private String mCourseName;
	private String mCourseDirPath;
	private String mCourseDataFilePath;
	private long mDuration;
	private long mCurrStart;
	private SentenceInfo mCurrSentInfo;
	private int mCurrLevel;
	private long mLastStart;
	private String mLanguage;
	private SentenceInfo mLastSentInfo;
	private ArrayList<SentenceInfo> mSentList;
	private ArrayList<String> mMeaningList;
	private HashMap<Long, SentenceInfo> mSentStartMap;
	private HashMap<Long, SentenceInfo> mSentEndMap;
	private int [] mLevelCnt = null;
	private int [] mTypeCnt = null;
	private String mCourseDivideFilePath;
	private String mCourseSamiFilePath;
	public CourseInfo(String courseName, String courseFilePath, String lang) {
		mSentList = new ArrayList<SentenceInfo>();
		mMeaningList = new ArrayList<String>();
		mLevelCnt = new int[MAX_LEVEL+2];
		mTypeCnt = new int[SentenceInfo.TYPE_MAX + 2];
		mCourseName = courseName;
		mCurrLevel = 1;
		mSentStartMap = new HashMap<Long, SentenceInfo>();
		mSentEndMap = new HashMap<Long, SentenceInfo>();
		mCurrStart = 0;
		mCourseFilePath = courseFilePath;
		mLanguage = lang;
		if (courseFilePath != null) {
			mCourseSamiFilePath = Utils.getSamiFilePath(mCourseFilePath);
			mDuration = Utils.getDuration(mCourseFilePath);
		}

		initCourseInfo(courseName);
		if (!restoreCourseInfo()) {
			if (mCourseFilePath == null) {
				FileFilter filter = new FileFilter() {

					@Override
					public boolean accept(File pathname) {
						return Utils.isSupportedFile(pathname);
					}
				};
				File [] files = (new File (Constant.DATA_PATH + "/" + courseName)).listFiles(filter);
				if (files.length >0 ) {
					mCourseFilePath = files[0].getAbsolutePath();
					mCourseSamiFilePath = Utils.getSamiFilePath(mCourseFilePath);
					mDuration = Utils.getDuration(mCourseFilePath);
				}
			}

			add(0,mDuration);

			if (!restoreDivide()) {
				if (courseFilePath != null) {

					if (!restoreSAMI(mCourseSamiFilePath, lang)) {
						mCourseSamiFilePath = Utils.getLrcFilePath(mCourseFilePath);
						restoreLRC(mCourseSamiFilePath, lang);
					}
				}
			}
			storeCourseInfo();
		} else {
		}
		mCurrSentInfo = mSentStartMap.get(0L);
	}

	public CourseInfo(String courseName) {
		this(courseName, null);
	}

	public CourseInfo(String courseName, String courseFilePath) {
		this(courseName, courseFilePath, "KRCC");
	}
	public int getCurrLevel() { 
		return mCurrLevel;
	}

	public void setCurrLevel(int mCurrLevel) {
		this.mCurrLevel = mCurrLevel;
			makeDateDir(Utils.getDate());
	}

	public boolean divide(long start, long divide) {
		Log.d(TAG, "divide (" + start +", "+ divide +") called");
		SentenceInfo oldInfo = mSentStartMap.get(start);
		long oldEnd = oldInfo.getEnd();
		if (divide<oldEnd && start<divide) {
			oldInfo.setEnd(divide);
			mSentEndMap.put(divide, oldInfo);
			add(divide, oldEnd);
			return true;
		}	
		return false;
	}

	public boolean divide(SentenceInfo info, long divide) {
		long start = info.getStart();
		long end = info.getEnd();
		if (divide<end && start<divide) {
			info.setEnd(divide);
			mSentEndMap.put(divide, info);
			add(divide, end);
			updateCourseInfo();
			return true;
		}	
		return false;
	}

	public boolean divide(long start, long divide, int type) {
		SentenceInfo oldInfo = mSentStartMap.get(start);
		long oldEnd = oldInfo.getEnd();
		if (divide<oldEnd && start<divide) {
			oldInfo.setEnd(divide);
			add(divide, oldEnd);
			oldInfo.setType(type);
			updateCourseInfo();
			return true;
		}	
		return false;
	}


	public SentenceInfo getSentenceInfo (long start) {
		return mSentStartMap.get(start);
	}

	public boolean mergePrev (long start) {
		if (start !=0) {
			mergePrevStr(start);
			remove(start);
			updateCourseInfo();
			return true;
		}
		return false;
	}

	private void mergePrevStr (long start) {
		SentenceInfo mergedInfo = mSentStartMap.get(start);
		SentenceInfo mergingInfo = mSentEndMap.get(start);
		String mergedStr = mergedInfo.getMeaningStr();
		String mergingStr = mergingInfo.getMeaningStr();
		mergingInfo.setMeaningStr(Utils.mergeString(mergingStr, mergedStr));
	}

	public boolean mergeNext (long start) {
		if (start != mLastStart) {
			return mergePrev(mSentStartMap.get(start).getEnd());
		}
		return false;
	}



	public void add (long start, long end) {
		SentenceInfo addInfo = new SentenceInfo(start,end);
		mSentList.add(addInfo);
		mSentStartMap.put(start, addInfo);
		mSentEndMap.put(end, addInfo);
		Collections.sort(mSentList);
		updateLastStart();
	}

	public void remove (long start) {
		SentenceInfo currInfo = mSentStartMap.get(start);
		SentenceInfo prevInfo = mSentEndMap.get(start);
		prevInfo.setEnd(currInfo.getEnd());
		mSentEndMap.remove(start);
		mSentStartMap.remove(start);
		mSentEndMap.put(prevInfo.getEnd(), prevInfo);
		mSentList.remove(currInfo);
		mCurrStart = prevInfo.getStart();
		mCurrSentInfo = prevInfo;
		Collections.sort(mSentList);
		updateLastStart();	
	}

	public void storeCourseInfo() {
		File dataFile = new File(mCourseDataFilePath);
		File tempFile = new File(dataFile.getAbsolutePath()+".temp");
		File oldFile = new File(dataFile.getAbsoluteFile()+".old");
		updateCourseInfo();
		try {
			FileOutputStream fos = new FileOutputStream(tempFile);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(mCourseFilePath);
			oos.writeLong(mDuration);
			oos.writeObject(mLanguage);
			oos.writeObject(mSentList);
			oos.writeObject(mMeaningList);
			oos.close();
		} catch (Exception e)  {
			e.printStackTrace();
			return;
		}
		if (dataFile.exists()) {
			oldFile.delete();
			dataFile.renameTo(oldFile);
		}
		tempFile.renameTo(dataFile);

		if (!storeDivide()) {
			return;
		}
			Log.d(TAG, "[dOOgie Debug] mCourseFilePath :" + mCourseFilePath);
/*
			try {
				mCourseFilePath.length();
			} catch (NullPointerException e){
				e.printStackTrace();
			}
*/
			File courseFileDir = (new File(mCourseFilePath)).getParentFile();
			File file = new File(courseFileDir, mCourseName + ".smi");
			storeSAMI(file.getAbsolutePath(), mLanguage); 
	} 

	public boolean restoreCourseInfo() {
		File dataFile = new File(mCourseDataFilePath);
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(dataFile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ois = new ObjectInputStream(bis);
			String courseFilePath = null;
			long duration;
			courseFilePath = (String)ois.readObject();
			if (mCourseFilePath != null && !mCourseFilePath.equals(courseFilePath)) {
				return false;
			}
			if (courseFilePath == null) {
				return false;
			}
			mCourseFilePath = courseFilePath;

			mCourseSamiFilePath = Utils.getSamiFilePath(mCourseFilePath);
			mDuration = Utils.getDuration(mCourseFilePath);
			duration = ois.readLong();
			if (mDuration != duration) {
				return false;
			}
			mLanguage = (String)ois.readObject();
			mSentList = (ArrayList)ois.readObject();
			mMeaningList = (ArrayList)ois.readObject();
		} catch (FileNotFoundException e) {
			return false;
		}		catch (StreamCorruptedException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			try {
				if (ois != null) {
					ois.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		updateCourseInfo();
		return true;
	}

	void initCourseInfo(String courseName) {
		mCourseDirPath = Constant.DATA_PATH + "/" + courseName; 
		//		mCourseFilePath = mCourseDirPath + "/" + courseName + "." + Constant.EXT_MP3;
		mCourseDataFilePath = mCourseDirPath + "/" + courseName + "." + Constant.EXT_DATA;
		mCourseDivideFilePath = mCourseDirPath + "/" + courseName + "_divide.dat";
		//		mCourseSamiFilePath = mCourseDirPath + "/" + courseName + "." + Constant.EXT_SAMI;
		makeScoreboardDir();
	}

	public void updateCourseInfo() {
		mSentStartMap.clear();
		mSentEndMap.clear();
		for (int i=0;i<mLevelCnt.length;i++) {
			mLevelCnt[i] = 0;
		}
		for (int i=0;i<mTypeCnt.length;i++) {
			mTypeCnt[i] = 0;
		}

		Iterator<SentenceInfo> it = mSentList.iterator();
		while (it.hasNext()) {
			SentenceInfo info = it.next();
			int type = info.getType();
			int level = info.getLevel();
			mSentStartMap.put(info.getStart(), info);
			mSentEndMap.put(info.getEnd(), info);
			info.setHasMeaningFile(hasMeaningFile(info));
			String subtitle = info.getMeaningStr();
			if (subtitle == null || "".equals(subtitle.trim())) {
				subtitle = "";
			}
			if (!info.hasMeaningFile() && !info.isTtsable() ) {
				info.setLevel(0);
			} else if (level == 0) {
				//info.setLevel(1);
			}
			level = info.getLevel();
			mTypeCnt[type]++;
			if ("".equals(subtitle) && type == SentenceInfo.TYPE_SOUND) {
				mTypeCnt[SentenceInfo.TYPE_SUBTITLE_BLANK]++;
			}
			mTypeCnt[SentenceInfo.TYPE_MAX + 1]++;
			if (type == SentenceInfo.TYPE_SOUND && !subtitle.equals("")) {
				mLevelCnt[level]++;
				mLevelCnt[MAX_LEVEL+1]+=(level>2)?level+1:level;
			}
		}
		mSentStartMap.put(mDuration, mSentStartMap.get(0L));
		mSentEndMap.put(0L, mSentEndMap.get(mDuration));
		mLastStart = mSentEndMap.get(mDuration).getStart();
	}


	SentenceInfo getCurrent() {
		return mSentStartMap.get(mCurrStart);
	}
	SentenceInfo getNext(long start) {
		return mSentStartMap.get(mSentStartMap.get(start).getEnd());
	}

	SentenceInfo getNext(SentenceInfo info) {
		return mSentStartMap.get(info.getEnd());
	}


	SentenceInfo getNext(long start, int type) {
		SentenceInfo info = null;
		long seek = start;
		do {
			info = getNext(seek);
			seek = info.getStart();
			if (seek == start)
				break;
		} while (info.getType()!=type);
		return info;
	}
	SentenceInfo getNext(SentenceInfo info, int type) {
		SentenceInfo retInfo = info;
		do {
			retInfo = getNext(retInfo);
			if (retInfo.equals(info))
				break;
		} while (retInfo.getType()!=type);
		return retInfo;
	}


	SentenceInfo getPrev(long start) {
		return mSentEndMap.get(start);
	}



	SentenceInfo getPrev(long start, int type) {
		SentenceInfo info;
		long seek = start;
		do {
			info = getPrev(seek);
			seek = info.getStart();
			if (seek == start)
				break;
		} while (info.getType()!=type);
		return info;
	}

	SentenceInfo getPositionSentInfo(long pos) {
		SentenceInfo info = mSentStartMap.get(0L);
		while (true) {
			long start = info.getStart();
			long end = info.getEnd();
			if ( end == mDuration || pos >= start && pos < end) {
				return info;
			}
			info = mSentStartMap.get(end);
		}
	}
	SentenceInfo goNext() {
		mCurrStart = getCurrent().getEnd();
		mCurrSentInfo = getNext(mCurrSentInfo);
		return mSentStartMap.get(mCurrStart);
	}
	


	SentenceInfo goPrev() {
		SentenceInfo prevInfo = mSentEndMap.get(mCurrStart);
		mCurrStart = prevInfo.getStart();
		mCurrSentInfo = prevInfo;
		return prevInfo;
	}

	SentenceInfo goNext(int type) {
		SentenceInfo info = getNext(mCurrStart, type);
		mCurrStart = info.getStart();
		mCurrSentInfo = info;
		return info;
	}

	SentenceInfo goPrev(int type) {
		SentenceInfo info = getPrev(mCurrStart, type);
		mCurrStart = info.getStart();
		mCurrSentInfo = info;
		return info;
	}

	SentenceInfo goNextValid() {
		return null;
	}

	SentenceInfo goPrevValid() {
		return null;
	}

	public void updateLastStart() {
		mLastStart = mSentEndMap.get(mDuration).getStart();
	}


	public void updateMeaningType() {

	}


	public String getCourseFilePath() {
		return mCourseFilePath;
	}

	public long getDuration() {
		return mDuration;
	}

	public long getCurrStart() {
		return mCurrStart;
	}

	public SentenceInfo setCurrStart(long mCurrStart) {
		this.mCurrStart = mCurrStart;
		mCurrSentInfo = mSentStartMap.get(mCurrStart);
		return mCurrSentInfo;
	}


	public ArrayList<SentenceInfo> getSentList() {
		return mSentList;
	}

	public void setSentList(ArrayList<SentenceInfo> mSentList) {
		this.mSentList = mSentList;
	}

	public HashMap<Long, SentenceInfo> getSentStartMap() {
		return mSentStartMap;
	}

	public void setSentStartMap(HashMap<Long, SentenceInfo> mSentStartMap) {
		this.mSentStartMap = mSentStartMap;
	}

	public HashMap<Long, SentenceInfo> getSentEndMap() {
		return mSentEndMap;
	}

	public void setSentEndMap(HashMap<Long, SentenceInfo> mSentEndMap) {
		this.mSentEndMap = mSentEndMap;
	}

	public long getLastStart() {
		return mLastStart;
	}

	public SentenceInfo goLast() {
		mCurrStart = mLastStart;
		mCurrSentInfo = mSentStartMap.get(mLastStart);
		return mSentStartMap.get(mLastStart);
	}

	public void levelUpAll (int level) {
		if (level >= MAX_LEVEL) {
			return;
		}
		SentenceInfo info = getNext(mLastStart, SentenceInfo.TYPE_SOUND);
		SentenceInfo oldInfo = info;
		do {
			if (info.getLevel() == level) {
				levelUp(info);
			}
			info = getNext(info, SentenceInfo.TYPE_SOUND);

		} while (!info.equals(oldInfo));
	}
	public void levelDownAll() {
		levelDownAll(mCurrLevel);
	}

	public void levelUpAll() {
		levelUpAll(mCurrLevel);
	}
	
	public void levelUpFromTo(long start, long end, int level) {
		long curr = start;
		while (curr<=end) {
			SentenceInfo info = mSentStartMap.get(curr);
			if (info.getLevel() == level && info.getType() == SentenceInfo.TYPE_SOUND) {
				levelUp(info);
			}
			curr = info.getEnd();
		}
	}
	
	
	

	public void levelDownAll(int level) {
		if (level == 0) {
			return;
		}
		SentenceInfo info = getNext(mLastStart, SentenceInfo.TYPE_SOUND);
		SentenceInfo oldInfo = info;
		do {
			if (info.getLevel() == level) {
				levelDown(info);
			}
			info = getNext(info, SentenceInfo.TYPE_SOUND);

		} while (!info.equals(oldInfo));
	}


	public  void levelUp (SentenceInfo info) {
		int level = info.getLevel();
		
		if (info.getType() == SentenceInfo.TYPE_INVALID 
				|| !info.isTtsable() 
				|| level==MAX_LEVEL)
			return;
		info.levelUp();
		mLevelCnt[level]--;
		mLevelCnt[level+1]++;
		mLevelCnt[MAX_LEVEL+1]++;
		storeCourseInfo();
	}

	public void levelDown (SentenceInfo info) { 
		int level = info.getLevel();
		if (level==0)
			return;
		info.levelDown();
		mLevelCnt[level]--;
		mLevelCnt[level-1]++;
		mLevelCnt[MAX_LEVEL+1]--;
		storeCourseInfo();
	}

	public void setLastStart(long mLastStart) {
		this.mLastStart = mLastStart;
	}

	public SentenceInfo goNextForPlaying() {
		SentenceInfo info;
		Set<SentenceInfo> infoSet  = new HashSet<SentenceInfo>();
		infoSet.add(getCurrent());
		do {
			info = getNext(mCurrStart, SentenceInfo.TYPE_SOUND);
			if (infoSet.contains(info)) {
				return getCurrent();
			} else {
				infoSet.add(info);
			}
		} while (mCurrLevel != 0 && info.getLevel() != mCurrLevel);
		return setCurrent(info);
	}

	public SentenceInfo setCurrent(SentenceInfo info) {
		mCurrStart = info.getStart();
		mCurrSentInfo = info;
		return info;
	}

	public int getLevelCnt(int i) {
		return mLevelCnt[i];
	}
	public int getCurrLevelCnt() {
		return mLevelCnt[mCurrLevel];
	}
	
	public int getCurrLevelPosCnt() {
		int cnt = 0;
		SentenceInfo info = getSentenceInfo(0L);
		while (info.getStart()<mCurrStart) {
		if (info.getLevel()==mCurrLevel) {
			cnt++;
		}
			info = getNext(info);
		}
		return cnt+1;
	}

	public int getTypeCnt(int i) {
		return mTypeCnt[i];
	}


	public boolean hasMeaningFile(SentenceInfo info) {

		return (info.isTtsable());
	}
	/*
	String getMeaningFilePath (SentenceInfo info) {
		return mCourseDirPath + "/" + Constant.DIRNAME[Constant.INDEX_MEANING_DIR] + "/" 
				+ mCourseName + "_" + info.getStart() + "." + Constant.EXT_MP4; 
	}
	 */
	/*
	String getCurrMeaningFilePath() {
		return getMeaningFilePath(getCurrent());
	}
	 */

	public String getCourseName() {
		return mCourseName;
	}

	public String getCourseDirPath() {
		return mCourseDirPath;
	}


	public String getCourseDataFilePath() {
		return mCourseDataFilePath;
	}
	public int getSize() {
		return mSentList.size();
	}
	public int getIndex(SentenceInfo info) {
		return mSentList.indexOf(info);
	}

	public int getCurrIndex() {
		return getIndex(mCurrSentInfo);

	}

	String getDivideStr() {

		StringBuffer sb = new StringBuffer();
		if (mSentList.size()<2) {
			return "";
		}
		for (int i=mSentList.size() - 1 ;i>1;i--) {
			sb.append("" + mSentList.get(i).getStart() + "," );
		}
		sb.append("" + mSentList.get(1).getStart());
		return sb.toString();
	}

	void divide(String info) throws NumberFormatException{
		String [] data = info.split(",");
		for (int i=0;i<data.length;i++) {
			divide(0,Long.valueOf(data[i]));
		}
	}

	void divide(List<SubtitleData> samiList) {
		Collections.sort(samiList);
		Iterator<SubtitleData> it = samiList.iterator();
		long prevEnd = mDuration;
		while (it.hasNext()) {
			SubtitleData samiData = it.next();
			long end = samiData.start;
			String subtitle = samiData.subtitle.trim();
			if (prevEnd - end < 1000 && subtitle.equals(""))
				continue;
			if (samiData.start == 0 || divide(0L, samiData.start)) {
				SentenceInfo info = mSentStartMap.get(samiData.start);
				info.setMeaningStr(subtitle);
				if (subtitle.equals(Constant.DELETED_SUBTITLE)) {
					info.setType(SentenceInfo.TYPE_INVALID);
				}
//				if (subtitle.equals("")) {
//					info.setType(SentenceInfoTYPE_SOUNDINVALID);
//				}
				prevEnd = end;
			}

		}
	}



	boolean storeDivide () {
		File dataFile = new File(mCourseDivideFilePath);
		File tempFile = new File(dataFile.getAbsolutePath()+".temp");
		File oldFile = new File(dataFile.getAbsoluteFile()+".old");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(tempFile);
			fos.write(getDivideStr().getBytes());
		} catch (Exception e)  {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (fos!=null) 
					fos.close();
			} catch (Exception e) {

			}
		}
		if (dataFile.exists()) {
			oldFile.delete();
			dataFile.renameTo(oldFile);
		}
		tempFile.renameTo(dataFile);
		return true;
	}

	String getSamiStr(String lang) {
		StringBuffer sami = new StringBuffer();
		sami.append("<SAMI>\r\n");
		sami.append("<HEAD>\r\n");
		sami.append("<Title>" + mCourseName + "</Title>\r\n");
		sami.append("<STYLE TYPE=\"text/css\">\r\n");
		sami.append("<!--\r\n" +
				"P {margin-left:8pt; margin-right:8pt; margin-bottom:2pt; margin-top:2pt;\r\n");
		sami.append("text-align:center; font-size:20pt; font-family:arial, sans-serif;\r\n");
		sami.append("font-weight:normal; color:white;}\r\n");
		sami.append(".KRCC {Name:Korean; Lang:kr-KR; SAMIType:CC;}\r\n");
		sami.append(".ENCC {Name:English; Lang:en-US; SAMIType:CC;}\r\n");
		sami.append("#STDPrn {Name:Standard Print;}\r\n");
		sami.append("#LargePrn {Name:Large Print; font-size:20pt;}\r\n");
		sami.append("#SmallPrn {Name:Small Print; font-size:10pt;}\r\n");		
		sami.append("\r\n");
		SentenceInfo info = getSentenceInfo(0);
		while (true) {
			sami.append("<SYNC Start=" + info.getStart() + "><P Class=" + mLanguage + ">");
			if (info.isTtsable()) {
				sami.append("\r\n" + info.getMeaningStr() + "\r\n");
			} else {
				sami.append("&nbsp;\r\n");
			}
			if (info.getEnd() == mDuration) {
				break;
			}
			info = getNext(info);
		}
		sami.append("</BODY>\r\n</SAMI>");
		return sami.toString();
	}


	boolean storeSAMI(String filePath, String lang) {
		File dataFile = new File(filePath);
		File tempFile = new File(dataFile.getAbsolutePath()+".temp");
		File oldFile = new File(dataFile.getAbsoluteFile()+".old");

		FileOutputStream fos = null;
		OutputStreamWriter out = null;
		BufferedWriter bw = null;
		String line = null;
		try {
			fos = new FileOutputStream(tempFile);
			out = new OutputStreamWriter(fos, "EUC-KR");
			bw = new BufferedWriter(out);
			StringBuffer sami = new StringBuffer();
			bw.write(getSamiStr(lang));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			try {
				if (bw != null)
					bw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (dataFile.exists()) {
			oldFile.delete();
			dataFile.renameTo(oldFile);
		}
		tempFile.renameTo(dataFile);
		return true;

	}


	boolean restoreDivide() {
		File dataFile = new File(mCourseDivideFilePath);
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		byte [] buf = new byte[5000];
		try {
			fis = new FileInputStream(dataFile);
			fis.read(buf);
			divide(new String (buf));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			try {
				if (fis != null)
					fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;

	}

	class SubtitleData implements Comparable<SubtitleData>{
		long start;
		String subtitle;
		@Override
		public int compareTo(SubtitleData another) {
			return new Long(another.start).compareTo(new Long(this.start));
		}
	}
	
	class LrcData extends SubtitleData implements Serializable {
		int nationCode;
		public LrcData(String line, int nationCode) {
			subtitle ="";
			this.nationCode = nationCode;
			parse(line);
		}
		void parse(String line) {
			start = Utils.getTime(line);
			
			while (line.contains("  ")) {
				line = line.replace("  ", " ");
			}
			int pos = line.indexOf("]");
			subtitle = line.substring(pos+1).trim(); 
		}

	}
	
	class SamiData extends SubtitleData implements Serializable{
		String nationCode;
		public SamiData(String line) {
			subtitle ="";
			parse(line);
		}



		void parse(String line) {
			String [] entities = null; 
			while (line.contains("  ")) {
				line = line.replace("  ", " ");
			}
			if (line.toUpperCase(Locale.ENGLISH).contains("</BODY>")) {
				line = line.substring(0,line.toUpperCase(Locale.ENGLISH).indexOf("</BODY>"));
			}
			entities = line.replace("<br>", "\n")
					.replace("<BR>", "\n")
					.replace("&nbsp;"," ")
					.replace("-", " ")
					.split(">");
			for (int i=0;i<entities.length;i++) {
				parseEntity(entities[i]);
			}
		}

		void parseEntity(String entity) {
			if (!entity.contains("<")) {
				subtitle = entity;
				return;
			}
			if (entity.toUpperCase().contains("<SYNC")) {
				String [] words = entity.split(" ");
				for (int i=0;i<words.length;i++) {
					String word = words[i].toUpperCase();
					if (word.contains("START")) {
						start = Integer.valueOf(word.substring(word.toUpperCase().indexOf("START=")+6).trim());
						return;
					}
				}
			}
			if (entity.toUpperCase().contains("<P")) {
				nationCode = entity.substring(entity.indexOf("=")+1).trim();
				return;
			}
			return;
		}
	}

	boolean restoreSAMI(String filePath)
	{
		return restoreSAMI(filePath, "KRCC");
	}

	boolean restoreSAMI(String filePath, String lang) {
		File dataFile = new File(filePath);
		List<SubtitleData> samiData = new ArrayList<SubtitleData>();
		FileInputStream fis = null;
		InputStreamReader in = null;
		BufferedReader br = null;
		String line = null;
		byte [] buf = new byte[5000];
		try {
			fis = new FileInputStream(dataFile);
			in = new InputStreamReader(fis, "EUC-KR");
			br = new BufferedReader(in);
			StringBuffer sami = new StringBuffer();
			do {
				line = br.readLine();
				if (line == null || line.toUpperCase().contains("<SYNC")) {
					String samiLine = sami.toString();
					if (samiLine.toUpperCase().contains("<SYNC") && samiLine.toUpperCase().contains(lang)) {
						samiData.add(new SamiData(samiLine));
					}
					if (line == null) {
						break;
					} else {
						sami = new StringBuffer();
					}
				}
				sami.append(line);


			} while (true);

			Collections.sort(samiData);
			divide(samiData);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			try {
				if (br != null)
					br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;

	}

	boolean restoreLRC(String filePath, String lang) {
		Map <Integer, Integer> timeMap = new HashMap<Integer,Integer>();
		File dataFile = new File(filePath);
		List<SubtitleData> lrcData = new ArrayList<SubtitleData>();
		FileInputStream fis = null;
		InputStreamReader in = null;
		BufferedReader br = null;
		String line = null;

		int cnt = 0;
		try {
			cnt = new Integer(lang);
		} catch (NumberFormatException e) {
			return false;
		}
		
		byte [] buf = new byte[5000];
		try {
			fis = new FileInputStream(dataFile);
			in = new InputStreamReader(fis, "EUC-KR");
			br = new BufferedReader(in);
			StringBuffer sami = new StringBuffer();
			do {
				line = br.readLine();
				if (line == null) {
					break;
				}
				int time = Utils.getTime(line); 
				if (time < 0) {
					continue;
				}
				if (timeMap.containsKey(time)) {
					timeMap.put(time, timeMap.get(time)+1); 
				} else {
					timeMap.put(time, 1);
				}
				if (timeMap.get(time)==cnt) {
					lrcData.add(new LrcData(line, cnt));
				}
				sami.append(line);
			} while (true);

			Collections.sort(lrcData);
			divide(lrcData);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			try {
				if (br != null)
					br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;

	}

	
	public ArrayList<String> getMeaningList() {
		return mMeaningList;
	}

	public String getLanguage() {
		return mLanguage;
	}

	public void setLanguage(String mLanguage) {
		this.mLanguage = mLanguage;
	}
	boolean isDateDirExist(String date) {
		return (new File(mCourseDirPath + "/" + Constant.DIRNAME[Constant.INDEX_RECORDING_DIR] + "/" + date)).exists();
	}

	void makeDateDir(String date) {
		File dateDir = new File(mCourseDirPath + "/" + Constant.DIRNAME[Constant.INDEX_RECORDING_DIR] + "/" + date);
		if (!dateDir.exists()) {
			dateDir.mkdirs();
		}
	}

	void makeScoreboardDir() {
		File dir = getScoreboardDir();
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	public File getScoreboardDir() {
		return new File(mCourseDirPath + "/" + Constant.DIRNAME[Constant.INDEX_SCOREBOARD_DIR]);
	}
	
	public File getScoreboardFile() {
		return new File(getScoreboardDir(), ".score_" + Utils.getDate());
	}
	
	public File getTodayScoreboardFile() {
		return new File(getScoreboardDir(), ".score_" + "99999999");
	}
	
	
	

	File getDateDir(String date) {
		return (new File(mCourseDirPath + "/" + Constant.DIRNAME[Constant.INDEX_RECORDING_DIR] + "/" + date));
	}

	String getDateDirPath(String date) {
		return (mCourseDirPath + "/" + Constant.DIRNAME[Constant.INDEX_RECORDING_DIR] + "/" + date);
	}
	
	public String getRecordingDirPath() {
		return mCourseDirPath + "/" + Constant.DIRNAME[Constant.INDEX_RECORDING_DIR];
	}
	
	File getRecordingDir() {
		return new File(getRecordingDirPath());
	}
	

	File getDateDir() {
		return getDateDir(Utils.getDate());
	}

	String getDateDirPath() {
		return getDateDirPath(Utils.getDate());
	}

	String getBeforeFilePath(String date) {
		return getDateDirPath(date) + "/" + String.format("%08d_01.mp4", mCurrStart);
	}

	String getAfterFilePath(String date) {
		return getDateDirPath(date) + "/" + String.format("%08d_02.mp4", mCurrStart);
	}

	String getRecordingFilePath(int level, int mode) {
		return getDateDirPath() + "/" + String.format("S%03d_L%1d_M%1d_%s.mp4", getCurrIndex() + 1, level, mode, getMeaningFileName());
	}

	String getMeaningFileName() {
		String ret =
		mCurrSentInfo.getMeaningStr()
			.replace(" ", "_")
			.replace("\n", "_")
			.replace("\r", "_")
			.replace("!", "_")
			.replace("\\", "_")
			.replace("/", "_")
			.replace(":", "_")
			.replace("*", "_")
			.replace("?", "_")
			.replace("\"", "_")
			.replace("<", "_")
			.replace(">", "_")
			.replace("|", "_")
			.replace("^", "_")
			.replace("$", "_")
			.replace("&", "_")
			.replace(".", "_")
			.replace("__","_")
			.replace("__","_")
			.replace("__","_")
			.replace("__","_")
			.replace("__","_")
			.replace("__","_")
			.replace("__","_")
			.replace("__","_")
			.replace("__","_");
		if (ret.endsWith("_"))  {
			ret = ret.substring(0, ret.length()-1);
		}
		return ret;
	}
	
	String getBeforeFilePath() {
		return getDateDirPath() + "/" + String.format("%08d_01.mp4", mCurrStart);
	}

	String getAfterFilePath() {
		return getDateDirPath() + "/" + String.format("%08d_02.mp4", mCurrStart);
	}

	void removeBeforeFile(long start) {
		File file = new File(getDateDirPath() + "/" + String.format("%08d_01.mp4", start));
		if (file.exists()) {
			file.delete();
		}
	}
	void removeRecordingFile (int mode) {
		File file = new File(getRecordingFilePath(mCurrLevel, mode));
		if (file.exists()) {
			file.delete();
		}
	}
	void removeRecordedFiles() {
		File [] files = getDateDir().listFiles();
		for (File file : files) {
			if (file.isFile()) {
				file.delete();
			}
		}
	}

	void removeAfterFile(long start) {
		File file = new File(getDateDirPath() + "/" + String.format("%08d_02.mp4", start));
		if (file.exists()) {
			file.delete();
		}
	}
	
	void setType(int type) {
		int oldType = mCurrSentInfo.getType();
		if (oldType == type) return;
		mCurrSentInfo.setType(type);
		mTypeCnt[oldType]--;
		mTypeCnt[type]++;
		if (type == SentenceInfo.TYPE_SOUND && oldType == SentenceInfo.TYPE_INVALID) {
			mTypeCnt[SentenceInfo.TYPE_SUBTITLE_BLANK]++;
		}
		if (oldType == SentenceInfo.TYPE_SOUND && type == SentenceInfo.TYPE_INVALID) {
			mTypeCnt[SentenceInfo.TYPE_SUBTITLE_BLANK]--;
		}

	}
	
	void setMeaningStr(String str) {
		mCurrSentInfo.setMeaningStr(str);
	}
	
	int getType() {
		return mCurrSentInfo.getType();
	
	}
	//Make level 1 cnt  
	void autoLevelUp(int cnt) {
		SentenceInfo info = mSentStartMap.get(0L); 
		while (mLevelCnt[1]<cnt) {
			if (info.getLevel()== 0) {
				levelUp(info);
			}
			info = getNext(info);
			if (info.getStart() ==0) {
				return;
			}
		}
	}
	
	boolean hasRecordingFile() {
		
		try {
			File [] files = getRecordingDir().listFiles();
			for (File file : files) {
				if (file.isDirectory() && file.listFiles(Utils.getFileNameFilter("mp4")).length>0) {
					return true;
				}
			}
		} catch (Exception e) {
			return false;
		}

		return false;
	}

	public void levelSet(SentenceInfo info, int level) { 
		int oldLevel = info.getLevel();
		if (oldLevel==0)
			return;
		info.setLevel(level);
		mLevelCnt[oldLevel]--;
		mLevelCnt[level]++;
		mLevelCnt[MAX_LEVEL+1]--;
		storeCourseInfo();
	}
	
}

