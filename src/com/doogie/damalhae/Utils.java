package com.doogie.damalhae;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;

public class Utils {

	protected static final String TAG = Utils.class.getSimpleName();

	public static File getMp4File (File file) {
		return new File(file.getAbsolutePath().replace(".mp3", ".mp4"));
	}
	public static Uri getMp4Uri (Uri uri) {
		return Uri.parse(uri.getPath().replace(".mp3", ".mp4"));
	}
	public static boolean logicalXOR(boolean x, boolean y) {
		return ( ( x || y ) && ! ( x && y ) );
	}
	public static void makeInitDir() {
		new File(Constant.APP_ROOT_PATH).mkdirs();
		new File(Constant.DATA_PATH).mkdirs();
		new File(Constant.BACKUP_PATH).mkdirs();
		new File(Constant.APP_ROOT_PATH, Constant.SAMPLE_DIR).mkdirs();
	}
	/*
	public static String getMeaningMp4FromSoundMp3(String sound) {
		return sound.replace("/"+Constant.DIRNAME[Constant.INDEX_SOUND_DIR]+"/","/"+Constant.DIRNAME[Constant.INDEX_MEANING_DIR]+"/").replace(".mp3", ".mp4");
	}
	public static String getMeaningMp4PathFromSoundMp3File(File file) {
		return file.getAbsolutePath().replace("/"+Constant.DIRNAME[Constant.INDEX_SOUND_DIR]+"/","/"+Constant.DIRNAME[Constant.INDEX_MEANING_DIR]+"/").replace(".mp3",".mp4");
	}
	public static File getMeaningMp4FileFromCourseNameNFileName(File courseDir, String fileName) {
		return new File(courseDir, "/"+ Constant.DIRNAME[Constant.INDEX_MEANING_DIR] + "/" + fileName.replace("." + Constant.EXT_MP3,"."+Constant.EXT_MP4));
	}
	public static File getMeaningMp4FileFromSoundMp3File(File file) {
		return new File(file.getAbsolutePath().replace("/"+Constant.DIRNAME[Constant.INDEX_SOUND_DIR]+"/","/"+Constant.DIRNAME[Constant.INDEX_MEANING_DIR]+"/").replace(".mp3",".mp4"));
	}
	public static File getMeaningMp4FileFromMp3FileName(File courseDir, String fileName) {
		return new File(courseDir.getAbsoluteFile() + "/" + Constant.DIRNAME[Constant.INDEX_MEANING_DIR] + "/" + fileName.replace("mp3", "mp4"));
	}

	public static String getSoundPath (String coursePath) {
		return coursePath + "/" + Constant.DIRNAME[(Constant.INDEX_SOUND_DIR)];
	}
	public static String getMeaningPath (String coursePath) {
		return coursePath + "/" + Constant.DIRNAME[(Constant.INDEX_MEANING_DIR)];
	}
	public static File getSoundDir (File courseDir) {
		return new File(courseDir.getAbsoluteFile() + "/" + Constant.DIRNAME[Constant.INDEX_SOUND_DIR]);
	}
*/
	public static File [] getSortedFilesWithExtInDir (File dir, final String ext) {
		File [] files;
		FileFilter filter = new FileFilter() {
			public boolean accept(File pathname) {
				if (pathname.getAbsolutePath().endsWith("." + ext)) {
					return true;
				}
				return false;
			}
		};
		files = dir.listFiles(filter);
		Arrays.sort(files, new Comparator() {
			public int compare(Object lhs, Object rhs) {
				return ((File)lhs).getName().compareTo(((File)rhs).getName());			
			}
		});
		return files;
	}
	public static File getCourseInfoFile(File courseDir) {
		return new File(courseDir.getAbsoluteFile() +"/" + courseDir.getName() + "_" + Constant.COURSE_INFO_FILENAME);
	}

	public static File getCourseInfoFile (String courseDirPath) {
		return new File (courseDirPath + "/" + new File(courseDirPath).getName() + "_" + Constant.COURSE_INFO_FILENAME);
	}

	public static String getDateTime () {
		SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		Date currentTime = new Date ( );
		String mTime = mSimpleDateFormat.format ( currentTime );
		return mTime;
	}
	
	public static String getDate () {
		SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMdd");
		Date currentTime = new Date ( );
		String mTime = mSimpleDateFormat.format ( currentTime );
		return mTime;
	}

	public static File getFile (File courseDir, String currFile, String ext, int type) {
		String currExt = currFile.substring(currFile.lastIndexOf("."), currFile.length());
		return new File(courseDir.getAbsoluteFile() +"/" + Constant.DIRNAME[type]+"/" + currFile.replace(currExt, "." + ext));
	}

	public static File getFile (File courseDir, String currFile, String ext, int type, String date) {
		String currExt = currFile.substring(currFile.lastIndexOf("."), currFile.length());

		return new File(courseDir.getAbsoluteFile() +"/" + Constant.DIRNAME[Constant.INDEX_RECORDING_DIR] + "/" + date + "/" + Constant.DIRNAME[type]+"/"   + currFile.replace(currExt,"." + ext));
	}
	public static String getFilePath (File courseDir, String currFile, String ext, int type, String date) {
		String currExt = currFile.substring(currFile.lastIndexOf("."), currFile.length());
		return courseDir.getAbsoluteFile() +"/" + Constant.DIRNAME[Constant.INDEX_RECORDING_DIR] + "/" + date + "/" + Constant.DIRNAME[type]+"/"   + currFile.replace(currExt, "." + ext);
	}

	public static String getFilePath (File courseDir, String currFile, String ext, int type) {
		String currExt = currFile.substring(currFile.lastIndexOf("."), currFile.length());
		return (courseDir.getAbsoluteFile() +"/" + Constant.DIRNAME[type]+"/" + currFile.replace(currExt, "." + ext));
	}

	public static File getDir (File courseDir, int type) {
			return new File(courseDir.getAbsoluteFile() +"/" + Constant.DIRNAME[type]);
	}
	
	public static void remove(File file) {
		if (!file.exists()) {
			return;
		}
		if (file.isFile()) {
			file.delete();
		}
		if (file.isDirectory()) {
			File [] files = file.listFiles();
			for (File f : files) {
				remove(f);
			}
			file.delete();
		}
	}
	
	
	
/*
	public static void removeFile (String courseName, String filePath) {
		File file = new File(filePath);
		file.renameTo(new File(Constant.DATA_PATH + "/" + courseName + "/" +  Constant.DIRNAME[Constant.INDEX_REMOVED_DIR] + "/" + file.getName()));
	}
	*/
	public static File getDir (File courseDir,int type, String date) {
		return new File(courseDir.getAbsoluteFile() +"/" + Constant.DIRNAME[Constant.INDEX_RECORDING_DIR] + "/" + date + "/" + Constant.DIRNAME[type]);
	}


	public static File getDir (File courseDir, String date) {
		return new File(courseDir.getAbsoluteFile() +"/" + Constant.DIRNAME[Constant.INDEX_RECORDING_DIR] + "/" + date);
	}


	public static void copyFile(File src, File dst) throws IOException
	{
		FileChannel inChannel = new FileInputStream(src).getChannel();
		FileChannel outChannel = new FileOutputStream(dst).getChannel();
		try
		{
			inChannel.transferTo(0, inChannel.size(), outChannel);
		}
		finally
		{
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
		}
	}

	public static void moveFile(File src, File dst) throws IOException
	{
		copyFile(src, dst);
		src.delete();  
	}
	
	public static FilenameFilter getFileNameFilter(final String ext) {
		return
		new FilenameFilter() {
			@Override
			public boolean accept(File file, String filename) {
				return (filename.toLowerCase().endsWith("."+ ext.toLowerCase()));
			}
		};
	}

	public static File getCourseDir (File file, String lang) {
		File courseDir;
		if (lang.equals("")) {
			courseDir = new File (Constant.DATA_PATH + "/" + getFirstName(file));
		} else {
			courseDir = new File (Constant.DATA_PATH + "/" + getFirstName(file) + "_" + lang);
		}
		courseDir.mkdirs();
		for (int i=0;i<=Constant.INDEX_RECORDING_DIR;i++) {
			Utils.getDir(courseDir, i).mkdirs();
		}
		return courseDir;
	}

	public static File getCourseDir (File file) {
		return getCourseDir(file, "");
	}	
	public static boolean isSupportedFile(File file) {
		String ext = getExt(file);
		if (ext.equals(Constant.EXT_MP3) 
				|| ext.equals(Constant.EXT_MP4)
				|| ext.equals(Constant.EXT_AVI)) {
			return true;
		}
		return false;
	}

	public static boolean copyResToFile(final String outputFile,
			final Context context, final int resId) throws IOException {
		if (new File(outputFile).exists()) {
			return false;
		} else {
			createFile(outputFile, context, resId);
			return true;
		}
	}

	public static void registerResToCourse(final String outputFile,
			final Context context, final int resId) {
		
	}

	public static void createFile(final String outputFile,
			final Context context, final int resId)
					throws IOException {

		final OutputStream outputStream = new FileOutputStream(outputFile);

		final Resources resources = context.getResources();
		final byte[] largeBuffer = new byte[1024 * 4];
		int totalBytes = 0;
		int bytesRead = 0;


		final InputStream inputStream = resources.openRawResource(resId);
		while ((bytesRead = inputStream.read(largeBuffer)) > 0) {
			if (largeBuffer.length == bytesRead) {
				outputStream.write(largeBuffer);
			} else {
				final byte[] shortBuffer = new byte[bytesRead];
				System.arraycopy(largeBuffer, 0, shortBuffer, 0, bytesRead);
				outputStream.write(shortBuffer);
			}
			totalBytes += bytesRead;
		}
		inputStream.close();


		outputStream.flush();
		outputStream.close();
	}

	
	public static String  getProgeressText(int progress, int max) {
		return String.format("[%03d/%03d]", progress, max);
	}
	
	public static String  getProgeressTimeText(int progress, int max) {
		TimeString  pTime = new TimeString(progress);
		TimeString mTime = new TimeString(max);
		
		return "["+pTime.getString(mTime.size)+ "/" + mTime.getString() + "]";
	}
	
	public static String getExt(File file) {
		String filePath = file.getAbsolutePath();
		return filePath.substring(filePath.lastIndexOf(".")+1);
	}

	public static String getExt(String filePath) {
		return filePath.substring(filePath.lastIndexOf(".")+1);
	}


	public static File getSamiFile (File file) { 
		String filePath = file.getAbsolutePath();
		return new File (getSamiFilePath(filePath));
	}
	
	public static File getLrcFile (File file) { 
		String filePath = file.getAbsolutePath();
		return new File (getLrcFilePath(filePath));
	}
	public static long getDuration(String filePath) {
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		mmr.setDataSource(filePath);
		String durationString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		if (durationString != null)
			return Long.parseLong(durationString);
		return 0;
	}
	public static String getSamiFilePath(String filePath) {
		return  (filePath.substring(0,filePath.lastIndexOf(".")+1) + Constant.EXT_SAMI);
	}

	public static String getLrcFilePath(String filePath) {
		return  (filePath.substring(0,filePath.lastIndexOf(".")+1) + Constant.EXT_LRC);
	}

	public static String getFirstName(String fileName) {
		return (fileName.substring(0, fileName.lastIndexOf(".")));
	}

	public static String getFirstName(File file) {
		return getFirstName(file.getName());
	}
	public static String getBaseCourseName(String courseName) {
		if (courseName.endsWith("CC")) {
			int index = -1;
			for (int i=courseName.length()-3;i>=courseName.length()-7;i--) {
				if (courseName.charAt(i) == '_') {
					index = i;
					break;
				}
			}
			if (index == courseName.length()-5 || index == courseName.length()-7) {
				return courseName.substring(0, index);
			} else {
				return courseName;
			}
		} else {
			return courseName;
		}
	}	
	public static String mergeString (String a, String b) {
		if (Constant.DELETED_SUBTITLE.equals(a)) { a="";}
		if (Constant.DELETED_SUBTITLE.equals(b)) { b="";}
		
		if (a == null || "".equals(a)) {
			return b;
		} 
		if (b == null || "".equals(b)){
			return a;
		}
		return a + "\n" + b;
	}

	public static void readTxtToList(ArrayList <String> list, File txtFile) {
		FileInputStream fis = null;
		InputStreamReader in = null;
		BufferedReader br = null;
		String line = null;
		try {
			fis = new FileInputStream(txtFile);
			in = new InputStreamReader(fis, "EUC-KR");
			br = new BufferedReader(in);
			do {
				line = br.readLine();
				if (line == null) {
					break;
				} else if (!line.trim().equals("")) {
					list.add(line);
				}
			} while (true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}



	static String getLang (String line) {
		String [] cols = line.toUpperCase().split(">");
		for (int i=0;i<cols.length;i++) {
			String col = cols[i];
			if (col.contains("CLASS=")) {
				int start = col.lastIndexOf("CLASS=") + 6;
				String code = col.substring(start);
				return code;
			}
		}
		return null;
	}
	
	static int getTime (String line) {
		if (!line.contains("[") && !line.contains("]")) {
			return -1;
		}
		int ret = -1;
		int start = line.indexOf("[");
		int end = line.indexOf("]");
		if ((end - start) != 9) {
			return -1;
		}
		String timeStr = line.substring(start + 1, end);
		String [] times = new String[3];
		times[0] = timeStr.substring(0, 2);
		times[1] = timeStr.substring(3, 5);
		times[2] = timeStr.substring(6);
		try {
			ret = 10 * (new Integer(times[2]) + 100 * (new Integer(times[1]) + 60 * new Integer(times[0])));
		} catch (NumberFormatException e) {
			return -1;
		}
		return ret;
	}
	 

	
	
	static int getLycLength(File file) {
		FileInputStream fis = null;
		InputStreamReader in = null;
		BufferedReader br = null;
		String line = null;
		int max = 0;

		try {
			fis = new FileInputStream(file);
			in = new InputStreamReader(fis, "UTF-8");
			br = new BufferedReader(in);
			do {
				line = br.readLine();
				if (line == null) {
					break;
				} 
				String [] lines = line.split("<br>");
				for (String ln : lines) {
					int time = getTime(ln);
					if (time < 0) {
						continue;
					}
					if (time>max) {
						max = time;
					}
				}
			} while (true);
			return max;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return max;
		} catch (IOException e) {
			e.printStackTrace();
			return max;
		}
		finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}


	}
	
	static String getLycInfo(File file) {
		FileInputStream fis = null;
		InputStreamReader in = null;
		BufferedReader br = null;
		String line = null;
		StringBuffer sb = new StringBuffer();
		int max = 0;

		try {
			fis = new FileInputStream(file);
			in = new InputStreamReader(fis, "UTF-8");
			br = new BufferedReader(in);
			do {
				line = br.readLine();
				if (line == null) {
					break;
				} 
				String [] lines = line.split("<br>");
				for (String ln : lines) {
					int time = getTime(ln);
					if (time < 0) {
						sb.append(ln+"\n");
						continue;
					}
					int pos = ln.indexOf("]");
					sb.append(ln.substring(pos+1).trim()); 
					if (time>max) {
						max = time;
					}
				}
			} while (true);
			return sb.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return sb.toString();
		}
		finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	static String [] getLangCount(File file) {

		Map <Integer, Integer> timeMap = new HashMap<Integer,Integer>();
		FileInputStream fis = null;
		InputStreamReader in = null;
		BufferedReader br = null;
		String line = null;
		int maxCount = 0;
		
		try {
			fis = new FileInputStream(file);
			in = new InputStreamReader(fis, "EUC-KR");
			br = new BufferedReader(in);
			do {
				line = br.readLine();
				if (line == null) {
					break;
				} 
				int time = getTime(line);
						
				if (time <= 0) {
					continue;
				}
				if (timeMap.containsKey(time)) {
					timeMap.put(time, timeMap.get(time)+1); 
				} else {
					timeMap.put(time, 1);
				}

			} while (true);
			for (int key : timeMap.keySet()) {
				if (maxCount < timeMap.get(key)) {
					maxCount = timeMap.get(key);
				}
			}
			String [] ret = new String[maxCount];
			for (int i=0;i<maxCount;i++) {
				ret[i] = ""+(i+1);
			}
			return ret;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	
	}
	
	
	static String [] getLangs(File file) {
		Set <String> langSet = new HashSet<String>();
		FileInputStream fis = null;
		InputStreamReader in = null;
		BufferedReader br = null;
		String line = null;
		try {
			fis = new FileInputStream(file);
			in = new InputStreamReader(fis, "EUC-KR");
			br = new BufferedReader(in);
			do {
				line = br.readLine();
				if (line == null) {
					break;
				} 
				String lang = getLang(line);
				if (lang != null) {
					langSet.add(lang);
				}
			} while (true);
			return langSet.toArray(new String[langSet.size()]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}
	static Locale getLocale(String lang) {
		if (lang.equals("KRCC")) {
			return Locale.KOREAN;
		}
		if (lang.equals("ENCC")) {
			return Locale.ENGLISH;
		}
		if (lang.equals("CNCC")) {
			return Locale.CHINESE;
		}
		return Locale.KOREAN;
	}
	String getLang(Locale locale) {
		if (locale.equals(Locale.KOREAN)) {
			return "KRCC";
		}
		if (locale.equals(Locale.ENGLISH)) {
			return "ENCC";
		}
		return "KRCC";
	}
	public static File getLatestAlsongFile() {
		
		File file = new File(Environment.getExternalStorageDirectory()+"/Alsong/lyricCache");
		if (!file.exists()) {
			return null;
		}
		File [] files = file.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {
				return filename.toLowerCase().endsWith(".lyc");
			}
		});
		if (files.length == 0) {
			return null;
		}
		File ret = files[0];
		for (File  lycFile : files) {
			if (ret.lastModified() < lycFile.lastModified()) {
				ret = lycFile;
			}
		}
		return ret;
	}
	public static void copyAlsongFile(File file) {
		// TODO Auto-generated method s
		long duration = getDuration(file.getAbsolutePath());
		File [] files = new File(Environment.getExternalStorageDirectory()+"/Alsong/lyricCache").listFiles();
		for (File  lycfile : files) {
			long lrcDuration = getLycLength(lycfile);
			if (lrcDuration < duration + 5000 &&  lrcDuration > duration - 30000) {
				copyLyc2Lrc(lycfile, getLrcFile(file));
				return;
			}
		}
	}
	public static void copyLyc2Lrc(File lycFile, File lrcFile) {
		// TODO Auto-generated method stub
		FileInputStream fis = null;
		InputStreamReader in = null;
		BufferedReader br = null;
		
		FileOutputStream fos = null;
		OutputStreamWriter out = null;
		BufferedWriter bw = null;
		String line = null;
		int maxCount = 0;

		try {
			fis = new FileInputStream(lycFile);
			in = new InputStreamReader(fis, "UTF8");
			br = new BufferedReader(in);
			String tags [] = {"[ar:","[al:","[ti:"};
			fos = new FileOutputStream(lrcFile);
			out = new OutputStreamWriter(fos, "EUC-KR");
			bw = new BufferedWriter(out);
			int tagCnt=0;
			do {
				line = br.readLine();
				if (line == null) {
					break;
				} 
				if (tagCnt<3 && !line.trim().equals("") && !line.trim().startsWith("[")) {
					bw.write(tags[tagCnt]+line+"]\r\n");
					tagCnt++;
				} else {
					bw.write(line.replace("<br>", "\r\n")+"\r\n");
				}
			} while (true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static String getDateTimeStr() {
        final Calendar c = Calendar.getInstance();
        int Year = c.get(Calendar.YEAR);
        Year = Year;
        	
        int Month = c.get(Calendar.MONTH) + 1; // 1월(0), 2월(1), ..., 12월(11)
        int Day = c.get(Calendar.DAY_OF_MONTH);
        int DayOfWeek = c.get(Calendar.DAY_OF_WEEK); // 일요일(1), 월요일(2), ..., 토요일(7)
        
        int Hour = c.get(Calendar.HOUR_OF_DAY); //HOUR는 12시간, HOUR_OF_DAY는 24시간 입니다.
        int Minute = c.get(Calendar.MINUTE);
        int Second = c.get(Calendar.SECOND);
        int AmPm = c.get(Calendar.AM_PM); // AM(0), PM(1)
        
        String stringDayOfWeek[] = { "", "일", "월", "화", "수", "목", "금", "토" }; // 일요일이 1이고 stringDayOfWeek[0]은 없으니 비워둡니다.
        String stringAmPm[] = { "오전", "오후" };
        
        String stringDayAndTimeFormat = String.format("%02d/%02d/%02d(" + stringDayOfWeek[DayOfWeek] + ")" + " %02d:%02d", Year, Month, Day, Hour, Minute);
        return stringDayAndTimeFormat;
	}
	
	public static Bitmap reverseBitmap(Bitmap bitmap) {
		int [] allpixels = new int [ bitmap.getHeight()*bitmap.getWidth()];
		int count = 0;
		bitmap.getPixels(allpixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(),bitmap.getHeight());
		for(int i =0; i<bitmap.getHeight()*bitmap.getWidth();i++){
			if (allpixels[i] == 0) {
				count++;
			}
			allpixels[i] =~allpixels[i];
		}
		if (count>allpixels.length/2) {
			bitmap.setPixels(allpixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
		}
		return bitmap;
	}
	
	static FileFilter getExtsFilter (final String [] exts) {
		return new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				for (int i=0;i<exts.length;i++) {
					if (pathname.getName().toLowerCase().endsWith("." + exts[i].toLowerCase())) {
						return true;
					}
				}
				return false;
			}
		};
	}

}





