package com.doogie.damalhae;


import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.SortedMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.MediaColumns;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends Activity implements OnClickListener {


	public final static String TAG = "MainActivity";
	private final static int REQ_EDIT_CODE = 1;
	private final static int REQ_REC_CODE = 2;
	private final static int REQ_PRACTICE_CODE = 3;
	private final static int REQ_IMPORT_CODE = 4;
	private final static int REQ_PLAYER_CODE = 5;


	File mFile = null;
	File [] mFiles = null;
	int mFileIndex = 0;

	Button practiceButton;
	Button modifyButton;
	Button importButton;
	Button playerButton;
	Button editButton;
	

	//	private File mCourseFile;
	//	private String mCourseName;
	private CourseInfo mCourseInfo;
	private SharedPreferences mPref;
	private SharedPreferences mSettingsPref;
	
	private TextToSpeech mTts = null;
	private boolean mDoingTts;
	private boolean mTtsInit;

	int [] mLevelCount = new int[Constant.LEVEL_MAX+1]; 
	ArrayList<FileInfo> mFileInfoArray;

	String mCourseDirName;
	File mCourseDir;
	private Spinner mFolderspinner;
	private ArrayAdapter<String> mFolderAdapter;
	ArrayList<String> mFolderList;
	boolean isFinished = false;
	Handler mHandler;
	SortedMap<String, File> fileMap; 


	String mImportedFilePath;
	private ProgressDialog progressdDialog;
	public boolean mIsImporting;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		mPref = getPreferences(Activity.MODE_PRIVATE);
		mSettingsPref =  getSharedPreferences("settings", Activity.MODE_PRIVATE);
		mHandler = new Handler();
		Utils.makeInitDir();
		initView();
		mFolderspinner = (Spinner)findViewById(R.id.courseSpinner); // null
		mFolderList = new ArrayList<String>();


		mFolderAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item, mFolderList);
		mFolderAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);   
		mFolderspinner.setAdapter(mFolderAdapter); // NullPointerException!!
		mFolderspinner.setOnItemSelectedListener(new OnItemSelectedListener() {


			public void onItemSelected(AdapterView<?> parent, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				onUpdate(arg2);
				setCoursePref(arg2);
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}

		});
		/*
		 */
		mFolderspinner.setSelection(getCoursePref());
	}


	void onUpdate (int position) {
		String courseName = ((String)mFolderspinner.getItemAtPosition(position));
		mCourseInfo = new CourseInfo(courseName);
		setLevelListString();
		initButtons();
	}

	void initView()
	{
		importButton = (Button)findViewById(R.id.importButton);
		modifyButton = (Button)findViewById(R.id.modifyButton);

		practiceButton = (Button)findViewById(R.id.practiceButton);
		playerButton = (Button)findViewById(R.id.playerButton);
		editButton = (Button)findViewById(R.id.editButton);
		importButton.setOnClickListener(this);
		modifyButton.setOnClickListener(this);
		practiceButton.setOnClickListener(this);
		playerButton.setOnClickListener(this);
		editButton.setOnClickListener(this);
		findViewById(R.id.howtoButton).setOnClickListener(this);
		findViewById(R.id.singButton).setOnClickListener(this);
		findViewById(R.id.introButton).setOnClickListener(this);
		findViewById(R.id.scoreButton).setOnClickListener(this);
		findViewById(R.id.recordingPlayButton).setOnClickListener(this);
		
	}


	private boolean makeCourseDir(File [] files) {
		for (File file : files) {
			File courseDir = Utils.getCourseDir(file);
			courseDir.mkdirs();
			for (int i=0;i<=Constant.INDEX_RECORDING_DIR;i++) {
				Utils.getDir(courseDir, i).mkdirs();
			}
			File samiFile = Utils.getSamiFile(file);
			File newFile = new File (courseDir, file.getName());
			File newSamiFile = new File (courseDir, samiFile.getName());
			file.renameTo(newFile);
			if (samiFile.exists()) {
				samiFile.renameTo(newSamiFile);
			}
			new CourseInfo(courseDir.getName(), newFile.getAbsolutePath());
		}
		return true;
	}




	private String makeCourseDir(File file) {
		File samFile = null;
		String courseName = null;;
		String [] langs = null;

		samFile = Utils.getSamiFile(file);;
		if (samFile.exists()) {
			langs = Utils.getLangs(samFile);
			if (langs != null) {
				for (int i=0;i<langs.length;i++) {
					File courseDir = Utils.getCourseDir(file, langs[i]);
					courseName = courseDir.getName();
					if (courseDir.list().length==1) {
						new CourseInfo(courseName, file.getAbsolutePath(), langs[i]);
					}
				}
				return courseName;
			} 
		}
		samFile = Utils.getLrcFile(file);
		if (samFile.exists()) {
			langs = Utils.getLangCount(samFile);
			String oldCourseName = null;
			if (langs != null) {
				for (int i=0;i<langs.length;i++) {
					File courseDir = Utils.getCourseDir(file, langs[i]);
					oldCourseName = courseName;
					courseName = courseDir.getName();
					if (courseDir.list().length==1) {
						new CourseInfo(courseName, file.getAbsolutePath(), langs[i]);
					}
				}
			} 
			if (oldCourseName == null) {
				oldCourseName = courseName;
			}
			return oldCourseName;

		}
		return null;
	}




	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		File file = new File(Constant.JFK_SAMI_FILEPATH);
		if (!file.exists() || file.length()<1000) {
			new LoadData().execute();
		}
		updateCourseList();
		initSettignsPref();
	}
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	void initSettignsPref() {
		for (int i=1;i<=9;i++) {
			if (!mSettingsPref.contains("max_sent_" + i)) {
				setSettignsPrefInt("max_sent_" + i, getMaxSentDef(i));
			}

			if (!mSettingsPref.contains("min_sent_" + i)) {
				setSettignsPrefInt("min_sent_" + i, getMinSentDef(i));
			}
			Constant.autoLevelUpCnt = getLevelMinSettingsPref(1);
		}
	}
	
	int getMaxSentDef(int level) {
		int def = 999;
		switch (level) {
		case 1:
			def = 10;
			break;
		case 2:
		case 3:
		case 4:
		case 5:
			def = 3<<(level-1);
			break;
		case 7:
			def = 5;
			break;
		}
		return def;
	}

	int getMinSentDef(int level) {
		int def = 0;
		switch (level) {
		case 1:
			def = 10;
			break;
		case 2:
		case 3:
		case 4:
		case 5:
			def = 3<<(level-2);
			break;
		}
		return def;
	}

	
	
	
	@Override
	protected void onPause() {
		super.onPause();
//		saveScoreboard();
	}
	void saveScoreboard() {
		if (mCourseInfo != null) {
			View view = findViewById(R.id.scoreboard);
			if (view.getVisibility() != View.VISIBLE) {
				return;
			}
			view.setDrawingCacheEnabled(true);
			Bitmap b = view.getDrawingCache();
			Utils.reverseBitmap(b);
			File file = mCourseInfo.getScoreboardFile();
			boolean isExist = file.exists(); 

			FileOutputStream fos;
			try {
				fos = new FileOutputStream(file);
				b.compress(CompressFormat.JPEG, 95,fos);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!isExist) {
					Intent intent =
							new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
					intent.setData(Uri.fromFile(file));
					sendBroadcast(intent);
			}
		}
		
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		isFinished = true;
		super.onDestroy();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem importing = menu.add("가져오기");
		importing.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				try {
					Intent intent;
					intent = new Intent(MainActivity.this, FileListActivity.class);
					String [] exts = {Constant.EXT_AVI, Constant.EXT_MP3, Constant.EXT_MP4, Constant.EXT_MKV, Constant.EXT_DMH, Constant.EXT_WMV};
					intent.putExtra("exts", exts);
					startActivityForResult(intent,REQ_IMPORT_CODE);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			}
		});

		
		MenuItem deleting = menu.add("과정삭제");
		deleting.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				try {
					if (mFolderList.size()>1) {
						confirmDeleteCourse();
					} else {
						Toast.makeText(getApplicationContext(), "마지막 남은 코스는 삭제할수 없습니다.", Toast.LENGTH_SHORT).show();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			}
		});

		MenuItem edit = menu.add("자막 제작(편집)");
		edit.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent();
				intent = new Intent();
				Bundle extras = new Bundle();
				extras.putSerializable("courseInfo", mCourseInfo);
				intent.putExtras(extras);
				intent.setClass(MainActivity.this, ModifyMp3Activity.class);
				startActivityForResult(intent, REQ_EDIT_CODE);
				return true;
			}
		});

		MenuItem help = menu.add("설정");
		help.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent();
				intent = new Intent();
				intent.setClass(MainActivity.this, SettignsActivity.class);
				startActivity(intent);
				return true;
			}
		});

		
/*
		MenuItem help = menu.add("도움말");
		help.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent i = new Intent(Intent.ACTION_VIEW, 
						Uri.parse("http://blog.naver.com/curiosus/40186498831"));
				startActivity(i);
				return true;
			}
		});

		
		MenuItem recording = menu.add("녹음모드변경");
		recording.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				mRecording = !mRecording;
				return true;
			}
		});
		 */
		return true;
	}
	void copyResourceToFile(int resId, File file) throws IOException {


		InputStream in = getResources().openRawResource(resId);
		FileOutputStream out = new FileOutputStream(file);
		byte[] buff = new byte[1024];
		int read = 0;

		try {
			while ((read = in.read(buff)) > 0) {
				out.write(buff, 0, read);
			}
		} finally {
			in.close();
			out.close();
		}
	}

	public void onClick(View v) {
		Intent intent = null;
		intent = new Intent();
		Bundle extras = new Bundle();
		extras.putSerializable("courseInfo", mCourseInfo);
		intent.putExtras(extras);
		switch (v.getId()) {
		case R.id.importButton:


			break;
			/*
		case R.id.exportButton:
			exportButton.setEnabled(false);
			new Thread (new Runnable() {

				public void run() {
					// TODO Auto-generated method stub
					try {
						File zipFile = new File(Constant.EXPORT_PATH, mCourseDirName + "." + Constant.EXT_DMH);
						ZipUtils.zip(mCourseDir.getAbsolutePath(), zipFile.getAbsolutePath());
					} catch (Exception e) {
						// TODO: handle eudpxception
						e.printStackTrace();
					} finally {
						mHandler.post(new Runnable() {
							public void run() {
								exportButton.setEnabled(true);
							}
						});
					}

				}
			}).start();
			break;
		case R.id.createButton:
			intent.setClass(MainActivity.this, RecMeaningActivity.class);
			startActivityForResult(intent, REQ_REC_CODE);
			break;
			 */
		case R.id.modifyButton:

			break;

		case R.id.practiceButton:
			intent.setClass(MainActivity.this, PracticeActivity.class);
			startActivityForResult(intent, REQ_PRACTICE_CODE);
			break;

		case R.id.singButton:
			intent.setClass(MainActivity.this, SingActivity.class);
			startActivityForResult(intent, REQ_PRACTICE_CODE);
			break;
		case R.id.playerButton:
			intent.setClass(MainActivity.this, PlayerActivity.class);
			startActivityForResult(intent, REQ_PLAYER_CODE);
			break;

		case R.id.editButton:
			intent.setClass(MainActivity.this, ModifyMp3Activity.class);
			startActivityForResult(intent, REQ_EDIT_CODE);
			break;
			
		case R.id.howtoButton:
			showHowToDialog();
			break;
			
		case R.id.scoreButton:
//			saveScoreboard();
			intent.setClass(MainActivity.this, ScoreBoardListActivity.class);
			intent.putExtra("folder", mCourseInfo.getScoreboardDir().getAbsolutePath());
			startActivity(intent);
			break;

		case R.id.recordingPlayButton:
			try {
				intent = new Intent(MainActivity.this, FileListActivity.class);
				String [] exts = {Constant.EXT_MP4};
				intent.putExtra("exts", exts);
				intent.putExtra("recordingPlay", true);
				intent.putExtra("folder", mCourseInfo.getRecordingDirPath());
				startActivity(intent);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

			
		case R.id.introButton:
			Intent i = new Intent(Intent.ACTION_VIEW, 
					Uri.parse("http://blog.naver.com/curiosus/40187007035"));
			startActivity(i);
			break;
		}
	}
	/*
	private class ImportThread extends Thread {
		String mImportCourseName;
		public ImportThread(String  importCourseName) {
			mImportCourseName = importCourseName;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {

				File zipFile = new File (Constant.IMPORT_PATH, mImportCourseName + "." + Constant.EXT_DMH);
				File targetDir = new File(Constant.DATA_PATH, mImportCourseName);
				targetDir.mkdir();
				mFolderList.add(targetDir.getName());
				ZipUtils.unzip(zipFile, targetDir, false);
				mFolderAdapter.notifyDataSetChanged();
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			} finally {

				mHandler.post(new Runnable() {
					public void run() {
						importButton.setEnabled(true);
					}
				});
			}
		}

	}
	 */
	/*
	int getMeaningFileNum (File mCourseDir) {
		File dir = new File (mCourseDir, Constant.DIRNAME[Constant.INDEX_MEANING_DIR]);
		FileFilter filter = new FileFilter() {
			public boolean accept(File pathname) {
				if (pathname.getName().endsWith("."+Constant.EXT_MP4)) {
					return true;
				}
				// TODO Auto-generated method stub
				return false;
			}
		};

		File [] files = dir.listFiles(filter);
		return files.length;

	}
	 */
	void initButtons() {
		practiceButton.setEnabled(mCourseInfo.getLevelCnt(CourseInfo.MAX_LEVEL +1) > 0);
		
		if (mCourseInfo.getLevelCnt(CourseInfo.MAX_LEVEL +1) == 0 && mCourseInfo.getTypeCnt(SentenceInfo.TYPE_SUBTITLE_BLANK) == 1) {
			playerButton.setVisibility(View.GONE);
			editButton.setVisibility(View.VISIBLE);

		} else {
			playerButton.setVisibility(View.VISIBLE);
			editButton.setVisibility(View.GONE);
			
		}
			
		findViewById(R.id.scoreButton).setEnabled((mCourseInfo.getScoreboardDir().listFiles().length>0));
		findViewById(R.id.recordingPlayButton).setEnabled(mCourseInfo.hasRecordingFile());
	}

	void setText(int resId, String text) {
		((TextView)findViewById(resId)).setText(text);
	}
	void setExpText(int exp, String text) {
		switch (exp) {
		case 0:
			setText(R.id.expCount0,text);
			break;
		case 1:
			setText(R.id.expCount1,text);
			break;
		case 2:
			setText(R.id.expCount2,text);
			break;
		case 3:
			setText(R.id.expCount3,text);
			break;
		case 4:
			setText(R.id.expCount4,text);
			break;
		case 5:
			setText(R.id.expCount5,text);
			break;
		case 6:
			setText(R.id.expCount6, text);
			break;
		case 7:
			setText(R.id.expCount7,text);
			break;
		case 8:
			setText(R.id.expCount8,text);
			break;
		case 9:
			setText(R.id.expCount9,text);
			break;

		}

	}

	void setLevelListString () {
		int levelMax = CourseInfo.MAX_LEVEL;
		int totalSentence = mCourseInfo.getTypeCnt(SentenceInfo.TYPE_SOUND) - mCourseInfo.getTypeCnt(SentenceInfo.TYPE_SUBTITLE_BLANK);
		int totalLevel = mCourseInfo.getLevelCnt(CourseInfo.MAX_LEVEL+1);

		mCourseInfo.autoLevelUp(Constant.autoLevelUpCnt);
		for (int i=0;i<= levelMax;i++) {
			int levelCnt = mCourseInfo.getLevelCnt(i);
			setExpText(i, "" + levelCnt);
		}
		setText(R.id.expCountNo, "" + mCourseInfo.getTypeCnt(SentenceInfo.TYPE_SUBTITLE_BLANK));
		setText(R.id.dateTime, Utils.getDateTimeStr());
		setText(R.id.courseNameTextView, mCourseInfo.getCourseName() + " 진도표");
		String str = getResources().getString(R.string.total_level);

		if (totalSentence != 0) {
			
			int percent = totalLevel * 100/(totalSentence * (levelMax+1));
			int prepercent = totalLevel * 1000/(totalSentence * (levelMax+1)) - percent * 10;
			setText(R.id.levelListView, String.format(str, totalLevel, totalSentence * (levelMax+1), percent, prepercent));
		} else {
			setText(R.id.levelListView, String.format(str, totalLevel, totalSentence * (levelMax+1), 0, 0));
		}
		return;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub

		switch (requestCode) {
		case REQ_EDIT_CODE:
		case REQ_REC_CODE:
		case REQ_PRACTICE_CODE:
		case REQ_PLAYER_CODE:
			if (resultCode == RESULT_OK) {
				mCourseInfo = (CourseInfo)data.getExtras().get("courseInfo");
				setLevelListString();
				initButtons();
			}
			break;
		case REQ_IMPORT_CODE:
			if (resultCode == RESULT_OK) {

				String filePath = data.getStringExtra("file");
				if (Utils.getExt(filePath).equals(Constant.EXT_DMH)) {
					new UnzipData().execute(filePath);
				} else {
					if (isAlreadyCourse(filePath)) {
						overImportCourse(filePath);
					} else {
						mImportedFilePath = filePath;
						new ImportData().execute(filePath);
					}
				}
			}
			break;
		}
	}

	boolean isAlreadyCourse(String filePath) {
		String name = Utils.getFirstName(new File(filePath));
		File [] courses = getCourseDirs();
		for (File dir : courses) {
			if (Utils.getBaseCourseName(dir.getName()).equals(name)) {
				return true;
			}
		}
		return false;
	}

	File [] getCourseDirs() {


		File dir = new File(Constant.DATA_PATH);
		FileFilter filter = new FileFilter() {
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					return true;
				}
				// TODO Auto-generated method stub
				return false;
			}
		};
		File [] files = dir.listFiles(filter);
		Arrays.sort(files, new Comparator() {
			public int compare(Object lhs, Object rhs) {
				return ((File)lhs).getName().compareTo(((File)rhs).getName());			
			}
		});
		return files;
	}
	void getAlsongCache() {

	}

	void showHowToDialog() {
		String myLongText = 
				"  1. 가져오기\n" +
						"     : 미디어 파일 (MP3, AVI 등)을 선택\n" +
						"      1) 선택된 파일을 코스 리스트에 추가\n"+
						"      2) 자막파일(SMI, LRC)이 있으면 자막정보 추가\n"+
						"      3) 알송가사 지원\n"+
						"         : 알송에서 먼저 플레이한 후 가져오기 해야함\n\n"+
						"  2. 자막 제작/편집\n"+
						"      1) 문장별로 구간 나누기/합치기\n"+
						"      2) 문장별로 자막추가 및 편집\n" +
						"      3) 자세한 사용법은 자막편집 도움말 참조 \n\n"+ 
						"  3. 통문장 플레이어\n"+
						"      1) 각 단계별 문장을 자막과 함께 플레이\n"+
						"      2) 단계올리기/내리기가능\n"+
						"      3) 자세한 사용법은 플레이어내 도움말 참조\n\n"+
						"  4. 통문장 학습\n" +
						"	   1) 각 문장별로 5단계 연습\n" +
						"	   2) 목소리 녹음 및 듣기 기능\n" +
						"      3) 자세한 사용법은 학습내 도움말 참조 \n\n";
		final TextView myView = new TextView(getApplicationContext());
		myView.setText(myLongText);
		myView.setTextSize(14);
		final AlertDialog d = new AlertDialog.Builder(MainActivity.this)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			}
		})

		.setTitle("도움말")
		.setView(myView)
		.create();
		d.show();

	}

	void updateCourseList () {
		updateCourseList(getCoursePref());
	}

	void updateCourseList (int selection) {
		try {
		File dir = new File(Constant.DATA_PATH);
		FileFilter filter = new FileFilter() {
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					return true;
				}
				// TODO Auto-generated method stub
				return false;
			}
		};
		File [] files = dir.listFiles(filter);
		Arrays.sort(files, new Comparator() {
			public int compare(Object lhs, Object rhs) {
				return ((File)lhs).getName().compareTo(((File)rhs).getName());			
			}
		});
		mFolderList.clear();
		for (File file : files) {
			mFolderList.add(file.getName());
		}
		mFolderAdapter.notifyDataSetChanged();
	
			mFolderspinner.setSelection(selection%mFolderList.size());
		}	catch (ArithmeticException e){
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	
	}

	
	
	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaColumns.DATA };
		try {
			Cursor cursor = managedQuery(contentUri, proj, null, null, null);
			int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
			cursor.moveToFirst();
			if (column_index == -1) {
				return null;
			}
			return cursor.getString(column_index);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void initTts() {
		mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				mTts.setLanguage(Locale.KOREAN);
				mTts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

					@Override
					public void onUtteranceCompleted(String utteranceId) {
						// TODO Auto-generated method stub
						mDoingTts = false;
					}
				});
				mTtsInit = true;
			}
		});

	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		setCoursePref(mFolderspinner.getSelectedItemPosition());
		super.onBackPressed();

	}
	/*
	void recordTts(CourseInfo courseInfo) {
		SentenceInfo info = courseInfo.setCurrStart(0);
		courseInfo.mergeNext(0);
		while (info.getEnd() <= courseInfo.getDuration()) {
			String subtitle = info.getMeaningStr();
			if (subtitle != null && !subtitle.trim().equals("")) {
				mDoingTts = true;
				if (!mTtsInit) {
					initTts();
					while (!mTtsInit) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				int result = mTts.synthesizeToFile(subtitle, null, courseInfo.getCurrMeaningFilePath());
				if (result != TextToSpeech.SUCCESS) {
					mTtsInit = false;
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					initTts();
					while (!mTtsInit) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					result = mTts.synthesizeToFile(subtitle, null, courseInfo.getCurrMeaningFilePath());
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				while (mDoingTts) {

					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	 */
	void setPref(String key, int i) {
		SharedPreferences.Editor e = mPref.edit();
		e.putInt(key, i);
		e.commit();
	}

	int getPrefInt(String key, int def) {
		return mPref.getInt(key, def);
	}

	void setCoursePref(int course) {
		setPref("course", course);
	}
	int getCoursePref() {
		return getPrefInt("course" , 0);
	}

	class LoadData extends AsyncTask<Void, Void, Void> {
		ProgressDialog progressDialog;
		//declare other objects as per your need
		@Override
		protected void onPreExecute()
		{
			progressDialog= ProgressDialog.show(MainActivity.this, "Loading...","Loading Data", true);

			//do initialization of required objects objects here                
		};      
		@Override
		protected Void doInBackground(Void... params)
		{   
			File dir = new File(Constant.DATA_PATH);
			File [] files;

			try {
				Utils.copyResToFile(Constant.JFK_FILEPATH, MainActivity.this, R.raw.jfk);

				Utils.copyResToFile(Constant.JFK_SAMI_FILEPATH, MainActivity.this, R.raw.jfk_smi);

				makeCourseDir(new File (Constant.JFK_FILEPATH));
				

				//		updateCourseList();
			} catch (Exception e) {
				e.printStackTrace();
			}


			FileFilter filter = new FileFilter() {
				public boolean accept(File pathname) {
					if (pathname.isDirectory()) {
						return true;
					}
					// TODO Auto-generated method stub
					return false;
				}
			};
			files = dir.listFiles(filter);
			Arrays.sort(files, new Comparator() {
				public int compare(Object lhs, Object rhs) {
					return ((File)lhs).getName().compareTo(((File)rhs).getName());			
				}
			});
			for (File file : files) {
				mFolderList.add(file.getName());
			}

			//do loading operation here  
			return null;
		}       
		@Override
		protected void onPostExecute(Void result)
		{
			super.onPostExecute(result);
			Log.d(TAG, "[dOOgie debug] LoadData, onPostExecute called");
			progressDialog.dismiss();
			updateCourseList();
			mFolderspinner.setSelection(getCoursePref());
			onUpdate(getCoursePref());
		};
	}

	class DeleteData extends AsyncTask<Void, Void, Void> {
		ProgressDialog progressDialog;
		//declare other objects as per your need
		@Override
		protected void onPreExecute()
		{
			progressDialog= ProgressDialog.show(MainActivity.this, "Deleting...","Deleting Course", true);

			//do initialization of required objects objects here                
		};      
		@Override
		protected Void doInBackground(Void... params)
		{
			File file = new File(mCourseInfo.getCourseDirPath());
			File newFile = new File(Constant.BACKUP_PATH + "/" + mCourseInfo.getCourseName() +"_" + System.currentTimeMillis());
			file.renameTo(newFile);
			return null;
		}       
		@Override
		protected void onPostExecute(Void result)
		{
			super.onPostExecute(result);
			updateCourseList(mFolderspinner.getSelectedItemPosition());
			onUpdate(mFolderspinner.getSelectedItemPosition());
			progressDialog.dismiss();
		};
	}

	
	class ImportData extends AsyncTask<String, Void, String> {
		ProgressDialog progressDialog;
		//declare other objects as per your need
		@Override
		protected void onPreExecute()
		{
			progressDialog= ProgressDialog.show(MainActivity.this, "Loading...","Loading Data", true);
			mIsImporting = true;
			//do initialization of required objects objects here                
		};      
		@Override
		protected String doInBackground(String... params)
		{   
			String courseName = makeCourseDir(new File (params[0]));
			Log.d(TAG, "[dOOgie Debug] courseName :" + courseName);
			return courseName;
		}       
		@Override
		protected void onPostExecute(String result)
		{

			super.onPostExecute(result);
			Log.d(TAG, "[dOOgie Debug] ImportData, onPostExecute called");
			Log.d(TAG, "[dOOgie Debug] result :" + result);
			File file = new File(mImportedFilePath);
			if (result == null) {
				File alsongFile = Utils.getLatestAlsongFile();
				if (alsongFile != null) {
					progressDialog.dismiss();
					importAlsongLyc(alsongFile);
					return;
				} else {
					File courseDir = Utils.getCourseDir(file, "");
					String courseName = courseDir.getName();
					if (courseDir.list().length==1) {
						new CourseInfo(courseDir.getName(), mImportedFilePath);
					}
					updateCourseList();
					int course = mFolderList.indexOf(courseName);
					mFolderspinner.setSelection(course);
					onUpdate(course);
				}
			} else {

				mCourseInfo.storeCourseInfo();
				updateCourseList();
				int course = mFolderList.indexOf(result);
				mFolderspinner.setSelection(course);
				onUpdate(course);
			}
			progressDialog.dismiss();
			mIsImporting = false;
		};
	}
	
	class UnzipData extends AsyncTask<String, Void, String> {
		ProgressDialog progressDialog;
		UnZipManager manager;
		//declare other objects as per your need
		@Override
		protected void onPreExecute() {
			progressDialog= ProgressDialog.show(MainActivity.this, "Unziping and Loading...","Unziping and Loading Data", true);
		};      
		@Override
		protected String doInBackground(String... params) {
			String filePath = params[0];
			String location = new File(filePath).getParent();
			try {
				manager = new UnZipManager(filePath, location);
			} catch (IOException e) {
				return null;
			}
				File [] files = manager.getCourseDir().listFiles(Utils.getExtsFilter(Constant.MEDIA_EXTS));
				String courseName = null;
				for (File file : files) {
						mImportedFilePath = file.getAbsolutePath();
						courseName = makeCourseDir(file);
				}
				return courseName;
		}       
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (result != null) {
				mCourseInfo.storeCourseInfo();
				updateCourseList();
				int course = mFolderList.indexOf(result);
				mFolderspinner.setSelection(course);
				onUpdate(course);
			}
			progressDialog.dismiss();
		}
	}

	void importAlsongLyc(final File file) {
		String myLongText = Utils.getLycInfo(file);

		final TextView myView = new TextView(getApplicationContext());
		myView.setText(myLongText);
		myView.setTextSize(14);
		final AlertDialog d = new AlertDialog.Builder(MainActivity.this)
		.setPositiveButton("가져오기", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String courseName = null;
				File mediaFile = new File(mImportedFilePath);
				File lrcFile = Utils.getLrcFile(mediaFile);
				Utils.copyLyc2Lrc(file, lrcFile);
				String [] langs = Utils.getLangCount(lrcFile);
				for (int i=0;i<langs.length;i++) {
					File courseDir = Utils.getCourseDir(mediaFile, langs[i]);
					courseName = courseDir.getName();
					if (courseDir.list().length==1) {
						new CourseInfo(courseName, mediaFile.getAbsolutePath(), langs[i]);
					}
				}
				updateCourseList();
				int course = mFolderList.indexOf(courseName)-langs.length+1;
				mFolderspinner.setSelection(course);
				mCourseInfo.levelUpAll(0);
				onUpdate(course);
			}
		})
		.setNegativeButton("취소", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				File mediaFile = new File(mImportedFilePath);
				File courseDir = Utils.getCourseDir(mediaFile, "");
				String courseName = courseDir.getName();
				if (courseDir.list().length==1) {
					new CourseInfo(courseDir.getName(), mImportedFilePath);
				}
				updateCourseList();
				int course = mFolderList.indexOf(courseName);
				mFolderspinner.setSelection(course);
				onUpdate(course);				
			}
		})

		.setTitle("알송가사 가져오기")
		.setView(myView)
		.create();
		d.show();

	}

	
	
	void overImportCourse (final String filePath) {
		final TextView myView = new TextView(getApplicationContext());
		myView.setText("선택하신 파일은 이미 만들어진 코스가 있습니다.\n그래도 가져오기시겠습니까 ?");
		myView.setTextSize(14);
		final AlertDialog d = new AlertDialog.Builder(MainActivity.this)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				mImportedFilePath = filePath;
				new ImportData().execute(filePath);
			}
		})
		.setNegativeButton("No", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {}
		})

		.setTitle("중복된 가져오기!")
		.setView(myView)
		.create();
		d.show();

	}
	void confirmDeleteCourse () {
		final TextView myView = new TextView(getApplicationContext());
		myView.setText("현재 코스를 정말 지우시겠습니까?");
		myView.setTextSize(14);
		final AlertDialog d = new AlertDialog.Builder(MainActivity.this)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				new DeleteData().execute();
			}
		})
		.setNegativeButton("No", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {}
		})

		.setTitle("코스 삭제!")
		.setView(myView)
		.create();
		d.show();

	}
		
	void setSettignsPrefInt(String key, int i) {
		SharedPreferences.Editor e = mSettingsPref.edit();
		e.putInt(key, i);
		e.commit();
	}
	
	int getLevelMaxSettingsPref (int level) {
		return getSettingsPrefInt("max_sent_"+level, 1000);
	}
	
	int getLevelMinSettingsPref (int level) {
		return getSettingsPrefInt("min_sent_"+level, 0);
	}

	int getSettingsPrefInt(String key, int def) {
		return mSettingsPref.getInt(key, def);
	}

}

