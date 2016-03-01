package com.doogie.damalhae;

import com.doogie.damalhae.CourseInfo.SamiData;

import android.os.Environment;

public class Constant {
	public static final String [] DIRNAME = {"recording", ".scoreboard"};
	public static final int INDEX_RECORDING_DIR = 0;
	public static final int INDEX_SCOREBOARD_DIR = 1;
	final static int PREV_DURATION = 1500;
	public static int autoLevelUpCnt = 10;
	public static final String APP_NAME = "DaMalHae";
	public static final String APP_ROOT_PATH = Environment.getExternalStorageDirectory() + "/" + APP_NAME;
	public static final String DATA_PATH = APP_ROOT_PATH + "/data";
	public static final String SAMPLE_DIR = "sample";
	public static final String BACKUP_DIR = "backup";
	public static final String BACKUP_PATH = APP_ROOT_PATH + "/" + BACKUP_DIR;
	public static final String SAMPLE_PATH = APP_ROOT_PATH + "/" + SAMPLE_DIR;
	public static final String DELETED_SUBTITLE = "___deleted___";
	public static final String EXT_DMH = "dmh";
	public static final String EXT_DATA = "dat";
	public static final String EXT_SAMI = "smi";
	public static final String EXT_LRC = "lrc";
	public static final String EXT_AVI = "avi";	
	public static final String EXT_MKV = "mkv";
	public static final String [] MEDIA_EXTS = {Constant.EXT_AVI, Constant.EXT_MP3, Constant.EXT_MP4, Constant.EXT_MKV};
	public static final String [] LOADABLE_EXTS = {Constant.EXT_AVI, Constant.EXT_MP3, Constant.EXT_MP4, Constant.EXT_MKV, Constant.EXT_DMH};


	public static final String HELLO_FILEPATH = SAMPLE_PATH + "/다국어_안녕하세요.mp3";
	public static final String THANK_FILEPATH = SAMPLE_PATH + "/다국어_감사합니다.mp3";
	public static final String JFK_FILEPATH = SAMPLE_PATH + "/JFK_취임연설.mp3";
	public static final String TSC_FILEPATH = SAMPLE_PATH + "/중국어_밴드_등업용.mp3";
	public static final String TSC_ENG_FILEPATH = SAMPLE_PATH + "/영어_밴드_등업용.mp3";
	public static final String TSC_CHA_BASIC_FILEPATH = SAMPLE_PATH + "/중국어_기초_밴드_등업용.mp3";
	//public static final String COURSENAME_POSTFIXS = {"KRCC"

	public static final String HELLO_SAMI_FILEPATH = SAMPLE_PATH + "/다국어_안녕하세요.smi";
	public static final String THANK_SAMI_FILEPATH = SAMPLE_PATH + "/다국어_감사합니다.smi";
	public static final String JFK_SAMI_FILEPATH = SAMPLE_PATH + "/JFK_취임연설.smi";
	public static final String TSC_SAMI_FILEPATH = SAMPLE_PATH + "/중국어_밴드_등업용.smi";
	public static final String TSC_ENG_SAMI_FILEPATH = SAMPLE_PATH + "/영어_밴드_등업용.smi";
	public static final String TSC_CHA_BASIC_SAMI_FILEPATH = SAMPLE_PATH + "/중국어_기초_밴드_등업용.smi";


	
	public static final String LEVEL_FILENAME = "level.ser";
	public static final String COURSE_INFO_FILENAME = "info.ser";
	public static final String EXT_MP3 = "mp3";
	public static final String EXT_MP4 = "mp4";
	public static final int LEVEL_MAX = 5;
	public static final String EXT_TXT = "txt";
	protected static final String EXT_WMV = "wmv"; 

}