package com.doogie.damalhae;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.SoundPool;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class ModifyMp3Activity extends BaseActivity implements OnClickListener,
OnSeekBarChangeListener, OnSeekCompleteListener,
SurfaceHolder.Callback, OnItemClickListener, OnItemLongClickListener {

	final static int STATE_SOUND_PLAY = 0;
	final static int STATE_AUTO_PLAY = 1;
	final static int STATE_MEANING_REC = 2;

	private final static int REQ_IMPORT_CODE = 4;

	final static int STATE_CONTINUE = 9;
	final static int STATE_STOP = 10;

	final static int UPDATE_PERIOD = 70;
	final static int SILENCE_BIAS = 500;
	final static int SCALE_MAX = 301;
	final static int CUTTING_PAUSE = 100;
	final static int BEEP_DELAY = 0;

	final static int DIALOG_AUTO_SETTING = 1;
	final static int DIALOG_DELETE_TXT = 2;
	private static final String TAG = "ModifyMp3Activity";

	private Visualizer mVisualizer;
	private long mAutoLastStart;
	private int mMuteCnt;
	private int mSoundCnt;
	private int mCuttingMuteLen;
	private long mAutoEnd = 0;


	SharedPreferences mPref;

	int mCurrCnt;
	int mOldTotalCnt;

	//private CourseInfo mCourseInfo;
	// private File mCourseFile;
	//private SentenceInfo mCurrSentInfo;

	private ProgressDialog progressDialog;

	boolean mIsPlaying = false;
	boolean mIsRecording = false;
	Handler mHandler = new Handler();
	Boolean mIsInit = true;

	TextView playTextView;
	TextView playAllTextView;
	TextView titleTextView;
	// TextView actionTextView;
	TextView autoTextView;

	SeekBar playSeekBar;
	SeekBar playAllSeekBar;

	SeekBar muteSeekBar;
	int mMuteHold;

	Button stopModeButton;

//	Button prevButton;
//	Button nextButton;
	Button prevSecButton;
	Button nextSecButton;
	Button importButton;

	//Button pauseButton;
	Button autoButton;
	Button invalidButton;

	Button stopButton;
	Button divideButton;
	Button mergePrevButton;
	Button mergeNextButton;

	private Spinner mExpSpinner;
	private ArrayAdapter<String> mExpAdapter;
	ArrayList<String> mExpList;

	private SurfaceView mVideoView;
	private SurfaceHolder mVideoHolder;

	private ArrayAdapter<String> mMeaningAdapter;
	private ListView mMeaningListView;

	String mCourseName;
	File[] mFiles = null;
	File mCurrFile = null;
	Uri mCurrUri = null;
	int mCurrIndex = 0;
	int mPrevIndex = 0;
	File mPrevFile = null;
	ArrayList<FileInfo> mFileInfoArray;
	int mCurrLevel = 0;
	Intent mIntent = null;
	Uri mUri = null;
	PowerManager.WakeLock mWl;
	private boolean mIsWakeLocked = false;

	boolean mIsDestroyed = false;

	int mCurrState = STATE_STOP;
	int mPrevState = STATE_STOP;
	int mOffset;
	int mCount;

	private boolean mStopMode;

	private SoundPool mSoundPool;
	Boolean mIsPrepared = false;
	int mDuration = 0;
	Random mRnd = new Random();
	Runnable mRun = new Runnable() {

		public void run() {
			updateUiOnState();
		}
	};
	private int mBeepSound;

	private ArrayList<String> mTypeList;
	// private Spinner mTypeSpinner;
	private ArrayAdapter<String> mTypeAdapter;

	private ArrayList<String> mAutoList;
	private Spinner mAutoSpinner;
	private ArrayAdapter<String> mAutoAdapter;
	private long mLastPosition;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.modify);
		mPref = getPreferences(Activity.MODE_PRIVATE);
		initView();
		initData();
	}

	void initData() {

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		Intent intent = getIntent();
		mUri = intent.getData();
		Bundle extras = intent.getExtras();
		mCourseInfo = (CourseInfo) extras.get("courseInfo");
		setLevelListString();
		mSoundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
		mCourseName = mCourseInfo.getCourseName();
		mCourseInfo.setCurrStart(mCourseInfo.getLastStart());
		mCurrSentInfo = mCourseInfo.getCurrent();
		initMeaningList();
		initlevelSpinner(mCourseInfo.MAX_LEVEL);
		initTypeSpinner();
	}

	void setCurrState(int state) {
		mPrevState = mCurrState;
		mCurrState = state;
		initUiForState(state);
	}

	void setVisibility(int visible) {
		titleTextView.setVisibility(visible);
		playTextView.setVisibility(visible);
		playAllTextView.setVisibility(visible);
		playSeekBar.setVisibility(visible);
		playAllSeekBar.setVisibility(visible);
	}

	void initView() {
		importButton = (Button) findViewById(R.id.importButton);
		playTextView = (TextView) findViewById(R.id.playTextView);
		playAllTextView = (TextView) findViewById(R.id.playAllTextView);
		// actionTextView = (TextView)findViewById(R.id.actionTextView);
		titleTextView = (TextView) findViewById(R.id.titleTextView);
//		titleTextView.setSelected(true);
		// actionTextView.setSelected(true);

		mMeaningListView = (ListView) findViewById(R.id.meaningList);

		mVideoView = (SurfaceView) findViewById(R.id.videoView);
		mVideoHolder = mVideoView.getHolder();
		mVideoHolder.addCallback(this);
		mVideoHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		importButton.setOnClickListener(this);

		autoTextView = (TextView) findViewById(R.id.autoTextView);

		prevButton = (Button) findViewById(R.id.prevButton);
		nextButton = (Button) findViewById(R.id.nextButton);
		prevSecButton = (Button) findViewById(R.id.prevSecButton);
		nextSecButton = (Button) findViewById(R.id.nextSecButton);

		pauseButton = (Button) findViewById(R.id.pauseButton);
		autoButton = (Button) findViewById(R.id.autoButton);
		invalidButton = (Button) findViewById(R.id.invalidButton);
		stopButton = (Button) findViewById(R.id.stopButton);
		divideButton = (Button) findViewById(R.id.divideButton);
		mergePrevButton = (Button) findViewById(R.id.mergePrevButton);
		mergeNextButton = (Button) findViewById(R.id.mergeNextButton);

		stopModeButton = (Button) findViewById(R.id.stopModeButton);
		playSeekBar = (SeekBar) findViewById(R.id.playSeekBar);
		playAllSeekBar = (SeekBar) findViewById(R.id.playAllSeekBar);
		pauseButton.setOnClickListener(this);
		autoButton.setOnClickListener(this);
		invalidButton.setOnClickListener(this);
		stopButton.setOnClickListener(this);
		prevButton.setOnClickListener(this);
		nextButton.setOnClickListener(this);

		prevSecButton.setOnClickListener(this);
		nextSecButton.setOnClickListener(this);
		stopModeButton.setOnClickListener(this);
		divideButton.setOnClickListener(this);
		mergePrevButton.setOnClickListener(this);
		mergeNextButton.setOnClickListener(this);
		playSeekBar.setOnSeekBarChangeListener(this);
		playAllSeekBar.setOnSeekBarChangeListener(this);
		setOnClickListener(R.id.chooseMeaning);
		setOnClickListener(R.id.editMeaning);
		setOnClickListener(R.id.removeMeaning);
		setOnClickListener(R.id.removeAllMeaning);
		setOnClickListener(R.id.removeSubtitle);
		setOnClickListener(R.id.editSubtitle);

		muteSeekBar = (SeekBar) findViewById(R.id.muteSeekBar);
		muteSeekBar.setMax(20);
		muteSeekBar.setProgress(0);
		muteSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				mMuteHold = progress;
			}
		});
	}

	void initUiForState(int state) {
		playSeekBar.setProgress(0);
		try {
			playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
		} catch (Exception e) {
				Log.e(TAG, "playAllSeekBar : " + playAllSeekBar);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		}
		switch (state) {
		case STATE_SOUND_PLAY:
			if (!isPlaying()) {
				soundPlayerPause();
			}
			nextButton.setEnabled(true);
			prevButton.setEnabled(true);
			nextSecButton.setEnabled(true);
			prevSecButton.setEnabled(true);
			pauseButton.setEnabled(true);
			mCurrSentInfo = mCourseInfo.getCurrent();
			mAutoLastStart = mCurrSentInfo.getStart();
			mAutoEnd = mCurrSentInfo.getEnd();
			setEditButtonEnabled(true);
			setInvalidButton();
			// mTypeSpinner.setSelection(mCurrSentInfo.getType());
			soundPlayerSeekTo((int) mCurrSentInfo.getStart());
			playSeekBar.setMax(mCurrSentInfo.getDuration());
			playSeekBar.setProgress(0);
			playAllSeekBar.setMax(mSoundPlayer.getDuration());
			playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
			setTitleTextView();
			setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
			setPlayAllTextView(playAllSeekBar.getProgress(),
					playAllSeekBar.getMax());

			break;
		case STATE_AUTO_PLAY:
			setEditButtonEnabled(false);
			nextButton.setEnabled(false);
			prevButton.setEnabled(false);
			nextSecButton.setEnabled(false);
			prevSecButton.setEnabled(false);
			mCurrSentInfo = mCourseInfo.getCurrent();
			mAutoLastStart = mCurrSentInfo.getStart();
			pauseButton.setEnabled(false);
			// mTypeSpinner.setSelection(mCurrSentInfo.getType());
			soundPlayerSeekTo((int) mCurrSentInfo.getStart());
			autoButton.setEnabled(false);
			playSeekBar.setProgress(0);
			playSeekBar.setMax(mCurrSentInfo.getDuration());
			playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
			playAllSeekBar.setMax(mSoundPlayer.getDuration());

			setTitleTextView();
			setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
			setPlayAllTextView(playAllSeekBar.getProgress(),
					playAllSeekBar.getMax());

			break;

		case STATE_STOP:
			mCurrCnt = 0;
			mOldTotalCnt = mCourseInfo.getCurrLevelCnt();
			playTextView.setText("");
			playAllTextView.setText("");
			nextButton.setEnabled(false);
			prevButton.setEnabled(false);
			nextSecButton.setEnabled(false);
			prevSecButton.setEnabled(false);
			divideButton.setEnabled(false);
			mExpSpinner.setEnabled(true);
			mergeNextButton.setEnabled(false);
			mergePrevButton.setEnabled(false);
			setAutoButtonEnabled(true);
			// mTypeSpinner.setEnabled(false);
			mCourseInfo.setCurrStart(0L);
			mCurrSentInfo = mCourseInfo.getCurrent();
			setIsPlaying(false);
			goPrevState();
			pauseButton.setText("▶");
			break;
		}

		String oriText = mCurrSentInfo.getMeaningStr();


		// Set an EditText view to get user input
		if (oriText == null || oriText.trim().equals("")) {
			setText(R.id.editSubtitle,"자막입력");
		} else {
			setText(R.id.editSubtitle,"자막수정");
		}
	
	}

	private void setInvalidButton() {
		if (mCurrSentInfo.getType() == SentenceInfo.TYPE_INVALID) {
			mCurrSentInfo.setMeaningStr(Constant.DELETED_SUBTITLE);
			invalidButton.setText("되살리기");
			invalidButton.setEnabled(true);
			setEnabled(R.id.removeSubtitle, false);
			setSubtitleTextView();
		} else {
			invalidButton.setText("지우기");
			if (mCurrSentInfo.getMeaningStr() == null) {
				mCurrSentInfo.setMeaningStr("");
			}
			boolean hasMeaning = !"".equals(mCurrSentInfo.getMeaningStr().trim());
			invalidButton.setEnabled(!hasMeaning);
			setEnabled(R.id.removeSubtitle, hasMeaning);
			setSubtitleTextView();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		mBeepSound = mSoundPool.load(this, R.raw.beep, 1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem autoDivide = menu.add("자동나누기");
		MenuItem openTxt = menu.add("문장가져오기");
		MenuItem subEdit = menu.add("문장추가");
		autoDivide.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				showDialog(DIALOG_AUTO_SETTING);
				return false;
			}
		});
		openTxt.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				try {
					try {

						Intent intent = new Intent(ModifyMp3Activity.this,
								FileListActivity.class);
						String[] exts = { Constant.EXT_TXT };
						intent.putExtra("exts", exts);
						startActivityForResult(intent, REQ_IMPORT_CODE);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			}
		});
		subEdit.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				editMeaning();
				return false;
			}
		});
		MenuItem help = menu.add("도움말");
		help.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent i = new Intent(Intent.ACTION_VIEW, 
						Uri.parse("http://blog.naver.com/curiosus/40186502136"));
				startActivity(i);
				return true;
			}
		});
		return true;
	}

	@Override
	protected void onDestroy() {
		mIsDestroyed = true;
		aquireWakeLock(false);
		mSoundPlayer.reset();
		mSoundPlayer.release();
		super.onDestroy();
	}

	void updateUiOnState() {
		if (mCurrState == STATE_STOP) {
			setIsPlaying(false);
			initUiForState(mCurrState);
			return;
		}
		if (isPlaying()) {
			mExpSpinner.setEnabled(false);
			switch (mCurrState) {
			case STATE_SOUND_PLAY:
				setEditButtonEnabled(true);
				playSeekBar.setProgress((int) (mSoundPlayer
						.getCurrentPosition() - mCurrSentInfo.getStart()));
				playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
				setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
				setPlayAllTextView(playAllSeekBar.getProgress(),
						playAllSeekBar.getMax());
				long currPosition = mSoundPlayer.getCurrentPosition();
				if (!isSoundOnRange()) {
					if (mStopMode || (mCourseInfo.getMeaningList() != null
							&& mCourseInfo.getMeaningList().size() > 0
							&& mCurrSentInfo.getType() != SentenceInfo.TYPE_INVALID
							&& (mCurrSentInfo.getMeaningStr() == null || mCurrSentInfo
							.getMeaningStr().trim().equals("")))) {
						setIsPlaying(false);
						soundPlayerPause();
						pauseButton.setText(">");
						setAutoButtonEnabled(true);
					} else {
						goNextState();

					}
				}
				mLastPosition = currPosition;
				break;
			case STATE_AUTO_PLAY:
				playSeekBar.setProgress((int) (mSoundPlayer
						.getCurrentPosition() - mCurrSentInfo.getStart()));
				playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
				setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
				setPlayAllTextView(playAllSeekBar.getProgress(),
						playAllSeekBar.getMax());
				if (!isAutoSoundOnRange()) {
					goNextState();
				}
				break;

			}
			mHandler.postDelayed(mRun, UPDATE_PERIOD);
		}
	}

	// Set up New Sentence
	//

	private void setPlayTextView(int progress, int max) {
		playTextView.setText(Utils.getProgeressTimeText(progress, max));
	}

	private void setPlayAllTextView(int progress, int max) {
		playAllTextView.setText(Utils.getProgeressTimeText(progress, max));
	}

	public void onClick(View v) {
		Intent intent = null;
		switch (v.getId()) {

		case R.id.importButton:
			try {
				try {

					intent = new Intent(this, FileListActivity.class);
					String[] exts = { Constant.EXT_TXT };
					intent.putExtra("exts", exts);
					startActivityForResult(intent, REQ_IMPORT_CODE);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			break;

		case R.id.pauseButton:
			if (pauseButton.getText().equals(">")) {
				goNextState();
				setIsPlaying(false);
			}
			if (isPlaying()) {
				setIsPlaying(false);
				// pauseButton.setText("▶");
				if (mCurrState == STATE_SOUND_PLAY) {
					soundPlayerPause();
					// mTypeSpinner.setEnabled(true);
					setAutoButtonEnabled(true);
				}
			} else {
				setIsPlaying(true);
				// pauseButton.setText("Pause ");
				if (mCurrState == STATE_STOP) {
					setCurrState(STATE_SOUND_PLAY);
				}
				setAutoButtonEnabled(false);
				soundPlayerStart();
				mHandler.postDelayed(mRun, UPDATE_PERIOD);

			}
			break;
		case R.id.divideButton:
			if (isPlaying()) {
				setIsPlaying(false);
				soundPlayerPause();
				// pauseButton.setText("▶");
				// mTypeSpinner.setEnabled(true);
			}
			mCourseInfo.divide(mCurrSentInfo.getStart(),
					mSoundPlayer.getCurrentPosition());
			// mTypeSpinner.dispatchTouchEvent(MotionEvent.obtain(0, 0,
			// MotionEvent.ACTION_DOWN, 5.0f, 5.0f, 0));
			// mTypeSpinner.dispatchTouchEvent(MotionEvent.obtain(0, 0,
			// MotionEvent.ACTION_UP, 5.0f, 5.0f, 0));
			setCurrState(STATE_SOUND_PLAY);

			// (mFileInfoArray.get(mPrevIndex).level)++;
			// levelUpButton.setEnabled(false);
			break;
		case R.id.mergePrevButton:
			if (isPlaying()) {
				setIsPlaying(false);
				soundPlayerPause();
				// pauseButton.setText("▶");
				// mTypeSpinner.setEnabled(true);
			}
			mCourseInfo.mergePrev(mCurrSentInfo.getStart());
			// mTypeSpinner.dispatchTouchEvent(MotionEvent.obtain(0, 0,
			// MotionEvent.ACTION_DOWN, 5.0f, 5.0f, 0));
			// mTypeSpinner.dispatchTouchEvent(MotionEvent.obtain(0, 0,
			// MotionEvent.ACTION_UP, 5.0f, 5.0f, 0));
			setCurrState(STATE_SOUND_PLAY);

			// (mFileInfoArray.get(mPrevIndex).level)++;
			// levelUpButton.setEnabled(false);
			break;

		case R.id.mergeNextButton:
			if (isPlaying()) {
				setIsPlaying(false);
				soundPlayerPause();
				// pauseButton.setText("▶");
				// mTypeSpinner.setEnabled(true);
			}
			mCourseInfo.mergeNext(mCurrSentInfo.getStart());
			// mTypeSpinner.dispatchTouchEvent(MotionEvent.obtain(0, 0,
			// MotionEvent.ACTION_DOWN, 5.0f, 5.0f, 0));
			// mTypeSpinner.dispatchTouchEvent(MotionEvent.obtain(0, 0,
			// MotionEvent.ACTION_UP, 5.0f, 5.0f, 0));
			setCurrState(STATE_SOUND_PLAY);

			// (mFileInfoArray.get(mPrevIndex).level)++;
			// levelUpButton.setEnabled(false);
			break;

		case R.id.stopButton:
			if (mCurrState != STATE_STOP) {
				mCourseInfo.updateCourseInfo();
				mCourseInfo.storeCourseInfo();
				mCurrState = STATE_STOP;
				mHandler.postDelayed(mRun, UPDATE_PERIOD);
			}

			break;
		case R.id.autoButton:
			showDialog(DIALOG_AUTO_SETTING);

			break;

		case R.id.invalidButton:
			onClickInvalidButton();
			if (pauseButton.getText().equals("║") || pauseButton.getText().equals(">")) {
				goNextState();
				setIsPlaying(true);
				setAutoButtonEnabled(false);
				soundPlayerStart();
				mHandler.postDelayed(mRun, UPDATE_PERIOD);
			}
			break;
		case R.id.prevButton:
			if (playSeekBar.getProgress() > Constant.PREV_DURATION) {
				soundPlayerSeekTo((int) mCurrSentInfo.getStart());
				playSeekBar.setProgress(0);
				playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
			} else {
				goPrevState();
			}
			mHandler.postDelayed(mRun, UPDATE_PERIOD);
			break;
		case R.id.prevSecButton:
			soundPlayerSeekTo(mSoundPlayer.getCurrentPosition() - 500);
			if (mSoundPlayer.getCurrentPosition() < mCurrSentInfo.getStart()) {
				soundPlayerSeekTo((int) mCurrSentInfo.getStart());
			}
			playSeekBar
			.setProgress((int) (mSoundPlayer.getCurrentPosition() - mCurrSentInfo
					.getStart()));
			playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
			setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
			setPlayAllTextView(playAllSeekBar.getProgress(),
					playAllSeekBar.getMax());
			break;
		case R.id.nextSecButton:
			soundPlayerSeekTo(mSoundPlayer.getCurrentPosition() + 500);
			playSeekBar
			.setProgress((int) (mSoundPlayer.getCurrentPosition() - mCurrSentInfo
					.getStart()));
			playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
			setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
			setPlayAllTextView(playAllSeekBar.getProgress(),
					playAllSeekBar.getMax());
			break;

		case R.id.nextButton:
			goNextState();
			break;

		case R.id.stopModeButton:
			if (mStopMode) {
				mStopMode = false;
				stopModeButton.setText("자동멈춤");
			} else {
				mStopMode = true;
				stopModeButton.setText("멈춤없음");
			}
			break;

		case R.id.chooseMeaning:
			chooseMeaning(0);
			break;

		case R.id.editMeaning:
			editMeaning(0);
			break;
		case R.id.removeMeaning:
			removeMeaning(0);
			break;

		case R.id.removeAllMeaning:
			removeAllMeaning();

			break;
		case R.id.removeSubtitle:
			removeSubtitle();
			break;
		case R.id.editSubtitle:
			editSubtitle();
			break;

		}
		setLevelListString();
	}

	private void onClickInvalidButton() {
		if (mCourseInfo.getType() == SentenceInfo.TYPE_INVALID) {
			mCourseInfo.setType(SentenceInfo.TYPE_SOUND);
			mCourseInfo.setMeaningStr("");
			setSubtitleTextView();
			invalidButton.setText("지우기");
			setLevelListString();
		} else {
			mCourseInfo.setType(SentenceInfo.TYPE_INVALID);
			mCourseInfo.setMeaningStr(Constant.DELETED_SUBTITLE);
			setSubtitleTextView();
			invalidButton.setText("되살리기");
			setLevelListString();
		}
	}

	public void onSeekComplete(MediaPlayer arg0) {

	}

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		switch (seekBar.getId()) {
		case R.id.playSeekBar:
			if (fromUser && (mCurrState == STATE_SOUND_PLAY)) {
				soundPlayerSeekTo((int) (mCurrSentInfo.getStart() + progress));
				setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
			}
			break;
		case R.id.playAllSeekBar:
			if(fromUser && (mCurrState == STATE_SOUND_PLAY)) {
				goPositionSession(mCourseInfo.getPositionSentInfo(progress));
				return;
			} 
			break;

		}
	}
	
	void goPositionSession(SentenceInfo info) {
		mCurrSentInfo = mCourseInfo.setCurrent(info);
		goPrevSession();
		goNextSession();
	}



	

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	public void setVisibility(int resId, int visibility) {
		findViewById(resId).setVisibility(visibility);
	}

	public int getVisibility(int resID) {
		return findViewById(resID).getVisibility();
	}

	public void goneView(int resId) {
		setVisibility(resId, View.GONE);
	}

	public void visibleView(int resId) {
		setVisibility(resId, View.VISIBLE);
	}

	void playBeep() {
		float vol = 5 / 50.000f;
		mSoundPool.play(mBeepSound, vol, vol, 0, 0, (float) 1.0);
	}

	void initTypeSpinner() {
		mTypeList = new ArrayList<String>();
		// mTypeSpinner = (Spinner)findViewById(R.id.typeSpinner);
		mTypeList.clear();
		mTypeList.add(SentenceInfo.TYPE_INVALID, "Invalid");
		mTypeList.add(SentenceInfo.TYPE_SOUND, "Sound");
		mTypeList.add(SentenceInfo.TYPE_MEANING_PREV, "Meaning Prev");
//		mTypeList.add(SentenceInfo.TYPE_MEANING_NEXT, "Meaning Next");

		mTypeAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, mTypeList);
		mTypeAdapter
		.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// mTypeSpinner.setAdapter(mTypeAdapter); // NullPointerException!!
		/*
		 * mTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
		 * 
		 * public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
		 * long arg3) { mCurrSentInfo.setType(arg2);
		 * mCourseInfo.updateCourseInfo(); setLevelListString(); if (mCurrState
		 * == STATE_SOUND_PLAY) { // setIsPlaying(true); //
		 * pauseButton.setText("║"); // soundPlayerStart();
		 * mHandler.postDelayed(mRun, UPDATE_PERIOD); } } public void
		 * onNothingSelected(AdapterView<?> arg0) {
		 * 
		 * }`
		 * 
		 * 
		 * });
		 */
	}

	void initAutoSpinner(View view) {
		mAutoList = new ArrayList<String>();
		mAutoSpinner = (Spinner) view.findViewById(R.id.autoSpinner);
		mAutoList.clear();
		for (int i = 0; i < 10; i++) {
			mAutoList.add(i, "" + (i * 2 + 2) / 10 + "." + (i * 2 + 2) % 10);
		}

		mAutoAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, mAutoList);
		mAutoAdapter
		.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAutoSpinner.setAdapter(mAutoAdapter); // NullPointerException!!
		mAutoSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				mCuttingMuteLen = arg2 * 2 + 2;
				setAutoMutePref(arg2);
			}

			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});
		mAutoSpinner.setSelection(getAutoMutePref());
	}

	void initlevelSpinner(int max) {
		mExpList = new ArrayList<String>();
		mExpSpinner = (Spinner) findViewById(R.id.expSpinner);

		mExpList.add("무자막");
		mExpList.add("자막");
		mExpList.add("무자막+자막");
		mExpList.add("지워진것");
		mExpList.add("전체");
		mExpAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, mExpList);
		mExpAdapter
		.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mExpSpinner.setAdapter(mExpAdapter); // NullPointerException!!
		if (mCourseInfo.getLevelCnt(mCourseInfo.getCurrLevel()) == 0) {
			mCourseInfo.setCurrLevel(4);
		}
		mExpSpinner.setSelection(0);
		mExpSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				mCourseInfo.setCurrLevel(arg2);
				setCurrState(STATE_STOP);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}

		});
	}

	void initVisualizer() {
		final int freq = (Visualizer.getMaxCaptureRate() / 2);
		final int period = 1000 * 1000 / freq;

		mVisualizer = new Visualizer(mSoundPlayer.getAudioSessionId());
		mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
		mVisualizer.setDataCaptureListener(
				new Visualizer.OnDataCaptureListener() {
					public void onWaveFormDataCapture(Visualizer visualizer,
							byte[] bytes, int samplingRate) {
						if (!isPlaying()) {
							return;
						}
						int i = 0;
						long currPos = mSoundPlayer.getCurrentPosition();
						for (byte am : bytes) {
							if (am < (126 - mMuteHold)
									&& am > (-126 + mMuteHold)) {
								mSoundCnt++;
								if (isDividableRange(currPos)
										&& mMuteCnt >= (mCuttingMuteLen + 30)
										* 100 / period) {
									long divide = currPos - 1500;
									if (mCurrState == STATE_AUTO_PLAY) {
										if (mCourseInfo.divide(mAutoLastStart,
												divide,
												SentenceInfo.TYPE_INVALID)) {
											Log.d("AutoDivide",
													"AutoDivide for Muted("
															+ mMuteCnt + ") : "
															+ mAutoLastStart
															+ " ~ " + divide);
											mCurrSentInfo = mCourseInfo
													.goNext();
											playSeekBar.setMax(mCurrSentInfo
													.getDuration());
											mAutoLastStart = divide;
											setLevelListString();
										} else {
											Log.e("AutoDivide",
													"AutoDivide Fail for Mute : "
															+ mAutoLastStart
															+ " ~ " + divide);
										}
									}

								}
								Log.d("Visualizer",
										"============================ Sound ("
												+ mSoundPlayer
												.getCurrentPosition()
												+ ","
												+ (mSoundPlayer
														.getCurrentPosition() - mCurrSentInfo
														.getStart()) + ","
														+ mMuteCnt
														+ "======================");
								mMuteCnt = 0;
								return;

							}
						}
						Log.d("Visualizer",
								"==============1============== Mute ("
										+ mSoundPlayer.getCurrentPosition()
										+ ","
										+ (mSoundPlayer.getCurrentPosition() - mCurrSentInfo
												.getStart())
												+ "======================");

						mMuteCnt++;
						((TextView) findViewById(R.id.muteTextView))
						.setText(getString(R.string.muteLength)
								+ mMuteCnt * period / 1000 + "."
								+ ((mMuteCnt * period % 1000)) / 100
								+ "초");
						if (mCurrState == STATE_AUTO_PLAY) {

							if (isDividableRange(currPos)
									&& mMuteCnt == mCuttingMuteLen * 100
									/ period) {
								long divide = currPos;
								if (mCourseInfo.divide(mAutoLastStart, divide,
										SentenceInfo.TYPE_SOUND)) {
									autoTextView.setText(String
											.format(getString(R.string.autoPos),
													getTimeString(divide
															- mCurrSentInfo
															.getStart())));
									setTitleTextView();
									Log.d("AutoDivide", "AutoDivide for Sound("
											+ mMuteCnt + ") : "
											+ mAutoLastStart + " ~ " + divide);
									mCurrSentInfo = mCourseInfo.goNext();
									playSeekBar.setMax(mCurrSentInfo
											.getDuration());
									mAutoLastStart = divide;
									setLevelListString();
									soundPlayerPause();
									soundPlayerSeekTo(divide);
									mHandler.postDelayed(new Runnable() {

										@Override
										public void run() {
											soundPlayerStart();
										}
									}, CUTTING_PAUSE);
								} else {
									Log.e("AutoDivide",
											"AutoDivide Fail for Sound : "
													+ mAutoLastStart + " ~ "
													+ divide);
								}
							}
						}
					}

					public void onFftDataCapture(Visualizer visualizer,
							byte[] bytes, int samplingRate) {
					}
				}, freq, true, false);
		mVisualizer.setEnabled(false);
	}

	void soundPlayerPause() {
		if (mSoundPlayer.isPlaying()) {
			mVisualizer.setEnabled(false);
			mSoundPlayer.pause();
			pauseButton.setText("▶");
		}
	}

	void soundPlayerStart() {
		if (mMeaningAdapter.getCount()==0 || mCurrState == STATE_AUTO_PLAY) {
			mVisualizer.setEnabled(true);
		}
		if (!mSoundPlayer.isPlaying()) {
			mSoundPlayer.start();
			setText(R.id.muteTextView,"");
			pauseButton.setText("║");
		}
	}

	@Override
	public void onBackPressed() {
		mVisualizer.setEnabled(false);
		mVisualizer.release();
		setIsPlaying(false);
		mHandler.removeCallbacks(mRun);

		mCourseInfo.storeCourseInfo();
		Intent intent = new Intent();
		Bundle extras = new Bundle();
		extras.putSerializable("courseInfo", mCourseInfo);
		intent.putExtras(extras);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mCourseInfo.storeCourseInfo();
	}

	void goNextState() {
		goNextSession();
	}

	void goPrevState() {
		goPrevSession();
	}

	void goNextSession() {
		int level = mCourseInfo.getCurrLevel();
		switch (mCurrState) {
		case STATE_SOUND_PLAY:
		case STATE_STOP:
			SentenceInfo oldinfo = mCurrSentInfo;

			mCurrSentInfo = goNextPlaySound();
			if (oldinfo.equals(mCurrSentInfo)
					&& mCourseInfo.getLevelCnt(mCourseInfo.getCurrLevel()) != 1) {
				mExpSpinner.setSelection(4);
				return;
			}
			int levelCnt = mCourseInfo.getCurrLevelCnt();

			if (mOldTotalCnt != levelCnt) {
				mCurrCnt += levelCnt - mOldTotalCnt;
				mOldTotalCnt = levelCnt;
			}
			if (mCurrCnt == levelCnt) {
				mCurrCnt = 1;
			} else {
				mCurrCnt++;
			}
			setCurrState(STATE_SOUND_PLAY);

			break;
		case STATE_AUTO_PLAY:
			mVisualizer.setEnabled(false);
			mCourseInfo.updateCourseInfo();
			setLevelListString();
			mCourseInfo.storeCourseInfo();
			mCurrState = STATE_STOP;
			mHandler.postDelayed(mRun, UPDATE_PERIOD);
			break;
		}
	}

	void goPrevSession() {
		if (mSoundPlayer.isPlaying()) {
			// soundPlayerPause();
		}
		mCurrSentInfo = goPrevPlaySound();
		if (mCurrCnt == 1) {
			mCurrCnt = mCourseInfo.getCurrLevelCnt();
		} else {
			mCurrCnt--;
		}

		setCurrState(STATE_SOUND_PLAY);
	}

	void setAutoButtonEnabled(boolean enabled) {
		autoButton.setEnabled(enabled);
	}

	boolean isSoundOnRange() {
		long currPosition = mSoundPlayer.getCurrentPosition();
		if (currPosition > mCurrSentInfo.getEnd()) {
			return false;
		}
		if (mCourseInfo.getDuration() == mCurrSentInfo.getEnd()) {
			if (currPosition + 1000 < mLastPosition) {
				return false;
			}
		}
		return true;
	}

	boolean isAutoSoundOnRange() {
		long currPosition = mSoundPlayer.getCurrentPosition();
		return !(currPosition > mAutoEnd || (mCourseInfo.getDuration() == mAutoEnd && currPosition < mCurrSentInfo
				.getStart()));
	}

	void soundPlayerSeekTo(long msec) {
		mSoundPlayer.seekTo((int) msec);
		mLastPosition = msec;
		if (isPlaying()) {
			pauseButton.setText("║");
		} else {
			pauseButton.setText("▶");
		}
	}
	
	void soundPlayerSeekTo(int msec) {
		mSoundPlayer.seekTo((int) msec);
		mLastPosition = msec;
		if (isPlaying()) {
			pauseButton.setText("║");
		} else {
			pauseButton.setText("▶");
		}
	}

	void setTitleTextView() {
		titleTextView.setText(mCourseName);
		setOrderTextView(mCourseInfo.getCurrIndex() + 1, mCourseInfo.getSize());
		setLevelTextView();
		setSubtitleTextView();
	}

	private void setSubtitleTextView() {
		String subtitle = mCurrSentInfo.getMeaningStr();
		if (subtitle == null) {
			subtitle = "";
			mCurrSentInfo.setMeaningStr("");
		}
		setText(R.id.subtitleTextView, subtitle.replace("\n", " "));
	}

	void setEditButtonEnabled(boolean enabled) {
		divideButton.setEnabled(enabled
				&& mCurrSentInfo.getStart() != mSoundPlayer
				.getCurrentPosition());
		mergePrevButton.setEnabled(enabled && mCurrSentInfo.getStart() != 0);
		mergeNextButton.setEnabled(enabled
				&& mCurrSentInfo.getEnd() != mCourseInfo.getDuration());
	}

	String getTimeString(long time) {
		return (String.format("%05d", time));
	}

	void setLevelTextView() {
		String str = " 단계 : " + mCurrSentInfo.getLevel();
		if (mCourseInfo.getType() == SentenceInfo.TYPE_INVALID) {
			str += " (지워짐)";
		} else {
			str += "       ";
		}
		setText(R.id.levelTextView, str);
	}

	void setOrderTextView(int order, int total) {
		setText(R.id.orderTextView, Utils.getProgeressText(order, total));
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		switch (id) {
		case DIALOG_AUTO_SETTING:
			final LinearLayout linear = (LinearLayout) inflater.inflate(
					R.layout.autosetdialog, null);
			initAutoSpinner(linear);
			return new AlertDialog.Builder(ModifyMp3Activity.this)
			.setTitle("자동 나누기 세팅")
			.setView(linear)
			.setPositiveButton("확인",
					new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog,
						int which) {
					setIsPlaying(true);
					setCurrState(STATE_AUTO_PLAY);
					soundPlayerStart();
					mHandler.postDelayed(mRun, UPDATE_PERIOD);
				}
			}).setNegativeButton("취소", null).create();
		case DIALOG_DELETE_TXT:
			return new AlertDialog.Builder(ModifyMp3Activity.this)
			.setTitle("문장이 삭제되었습니다.")
			.setPositiveButton("확인",
					new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog,
						int which) {
					meaningAdapterNotifyDataSetChanged();
				}
			}).create();

		}
		return null;

	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mSoundPlayer = new SoundPlayer();
		try {
			mSoundPlayer.setDataSource(mCourseInfo.getCourseFilePath());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mSoundPlayer.setDisplay(holder);
		mSoundPlayer.setLooping(true);
		mSoundPlayer.setOnCompletionListener(new OnCompletionListener() {

			public void onCompletion(MediaPlayer mp) {

				goNextState();
				mCourseInfo.storeCourseInfo();
			}
		});
		mSoundPlayer
		.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

			public void onPrepared(MediaPlayer mp) {
				mHandler.postDelayed(mRun, UPDATE_PERIOD);
			}
		});

		try {
			mSoundPlayer.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		initVisualizer();

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int videoWidth = mSoundPlayer.getVideoWidth();
		int videoHeight = mSoundPlayer.getVideoHeight();

		if (videoWidth == 0) {
			mVideoView.setVisibility(View.GONE);
		}
		int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
		android.view.ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();

		// Set the width of the SurfaceView to the width of the screen

		// Set the height of the SurfaceView to match the aspect ratio of the
		// video
		// be sure to cast these as floats otherwise the calculation will likely
		// be 0
		lp.width = mVideoView.getWidth();
		lp.height = (int) (((float) videoHeight / (float) videoWidth) * (float) lp.width);

		// Commit the layout parameters
		mVideoView.setLayoutParams(lp);

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub

	}

	void initMeaningList() {
		mMeaningAdapter = new ArrayAdapter<String>(this,
				R.layout.simple_text_item, mCourseInfo.getMeaningList());
		mMeaningListView.setAdapter(mMeaningAdapter);
		mMeaningListView.setOnItemClickListener(this);
		mMeaningListView.setOnItemLongClickListener(this);
		meaningAdapterNotifyDataSetChanged();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {

		chooseMeaning(pos);
		/*
		 * if (mCurrState==STATE_MEANING_REC) {
		 * mCourseInfo.levelUp(mCurrSentInfo); setLevelListString();
		 * mCurrSentInfo = goNextRecSound(); pauseButton.setText("Pasue");
		 * mTypeSpinner.setEnabled(false); playBeep(); mHandler.postDelayed(new
		 * Runnable() {
		 * 
		 * @Override public void run() { setIsPlaying(true);
		 * setCurrState(STATE_SOUND_PLAY); mHandler.postDelayed(mRun,
		 * UPDATE_PERIOD); mSoundPlayerStart(); } }, 600); }
		 */
	}

	void addSubtitle(String meaning) {
		ArrayList<String> meaningList = mCourseInfo.getMeaningList();
		if (meaning == null || meaning.trim().equals("")) {
			return;
		} else {
			String [] strs = meaning.split("\n");
			for (String str : strs) {
				meaningList.add(str);
			}
		}
		meaningAdapterNotifyDataSetChanged();
	}

	void replaceSubtitle(int pos, String meaning) {
		ArrayList<String> meaningList = mCourseInfo.getMeaningList();
		if (meaning == null || meaning.trim().equals("")) {
			return;
		} else {
			if (pos>=0) {
				meaningList.remove(pos);
			}
			String [] strs = meaning.split("\n");
			for (int i=0; i<strs.length;i++) {
				if (pos+i<0) {
					mCurrSentInfo.setMeaningStr(strs[i]);
				} else {
					meaningList.add(pos +i, strs[i]);
				}
			}
		}
		meaningAdapterNotifyDataSetChanged();
	}

	void removeSubtitle() {
		ArrayList<String> meaningList = mCourseInfo.getMeaningList();
		String meaning = mCurrSentInfo.getMeaningStr();
		if (meaning == null || meaning.trim().equals("")) {
			return;
		} else {
			String str = meaning;
			mCurrSentInfo.setMeaningStr("");
			meaningList.add(str);
		}
		meaningAdapterNotifyDataSetChanged();
		setSubtitleTextView();
		setInvalidButton();
		/*
		isPlaying()
		setIsPlaying(true);
		// pauseButton.setText("Pause ");
		setAutoButtonEnabled(false);
		soundPlayerStart();
		mHandler.postDelayed(mRun, UPDATE_PERIOD);
		soundPlayerSeekTo((int) mCurrSentInfo.getStart());
		playSeekBar.setProgress(0);
		playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
*/		
	}

	void chooseMeaning(int pos) {
		ArrayList<String> meaningList = mCourseInfo.getMeaningList();
		String meaning = mCurrSentInfo.getMeaningStr();
		if (Constant.DELETED_SUBTITLE.equals(meaning)) {
			onClickInvalidButton();
			meaning = mCurrSentInfo.getMeaningStr();
		}
		if (meaning == null || meaning.trim().equals("")) {
			mCurrSentInfo.setMeaningStr(meaningList.remove(pos));
		} else {
			String str = meaning;
			mCurrSentInfo.setMeaningStr(meaningList.get(pos));
			meaningList.set(pos, str);
		}

		meaningAdapterNotifyDataSetChanged();
		setSubtitleTextView();
		setIsPlaying(true);
		// pauseButton.setText("Pause ");
		setAutoButtonEnabled(false);
		soundPlayerStart();
		mHandler.postDelayed(mRun, UPDATE_PERIOD);
		goNextState();

	}

	void editMeaning(int pos) {
		editMeaning(pos, mCourseInfo.getMeaningList().get(pos));
	}

	void removeMeaning(int pos) {
		ArrayList<String> meaningList = mCourseInfo.getMeaningList();
		meaningList.add(meaningList.remove(pos));
		meaningAdapterNotifyDataSetChanged();
	}

	void removeAllMeaning() {
		ArrayList<String> meaningList = mCourseInfo.getMeaningList();
		while (meaningList.size() > 0) {
			meaningList.remove(0);
		}
		meaningAdapterNotifyDataSetChanged();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos,
			long id) {
		ArrayList<String> meaningList = mCourseInfo.getMeaningList();
		meaningList.remove(pos);
		showDialog(DIALOG_DELETE_TXT);
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQ_IMPORT_CODE:
			if (resultCode == RESULT_OK) {
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						progressDialog = ProgressDialog.show(
								ModifyMp3Activity.this, "", "Importing...");
					}
				});
				String filePath = data.getStringExtra("file");

				String ext = Utils.getExt(filePath).toLowerCase();
				if (ext.equals(Constant.EXT_AVI)
						|| ext.equals(Constant.EXT_MP3)
						|| ext.equals(Constant.EXT_MP4)
						|| ext.equals(Constant.EXT_MKV)) {
					// makeCourseDir(new File (filePath));
					// updateCourseList();

				} else if (ext.equals(Constant.EXT_DMH)) {

				} else if (ext.equals(Constant.EXT_TXT)) {
					Utils.readTxtToList(mCourseInfo.getMeaningList(), new File(
							filePath));
					meaningAdapterNotifyDataSetChanged();
				}
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						progressDialog.dismiss();
					}
				});

			}

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

	boolean isDividableRange(long pos) {
		return (pos > 1000L && pos < mCourseInfo.getDuration() - 1000L);
	}

	void meaningAdapterNotifyDataSetChanged() {
		if (mMeaningAdapter.getCount() > 0) {
			setVisibility(R.id.scoreboard, View.GONE);
			setVisibility(R.id.meaningGroup, View.VISIBLE);
		} else {
			setVisibility(R.id.scoreboard, View.VISIBLE);
			setVisibility(R.id.meaningGroup, View.GONE);
		}
		mMeaningAdapter.notifyDataSetChanged();

	}

	void aquireWakeLock(boolean aquire) {
		if (!mIsWakeLocked && aquire) {
			mIsWakeLocked = true;
			mWl.acquire();
			return;
		}
		if (mIsWakeLocked && !aquire) {
			mIsWakeLocked = false;
			mWl.release();
		}
	}

	void setIsPlaying(boolean play) {
		mIsPlaying = play;
		aquireWakeLock(play);
	}

	boolean isPlaying() {
		return mIsPlaying;
	}

	private SentenceInfo goPrevPlaySound() {
		SentenceInfo info = null;
		do {
			info = mCourseInfo.goPrev();
			if (mCurrSentInfo.equals(info)) {
				return info;
			}
		} while (!isPlaySound(info));
		return info;
	}

	private SentenceInfo goNextPlaySound() {
		SentenceInfo info = null;
		Set<SentenceInfo> infoSet = new HashSet<SentenceInfo>();
		infoSet.add(mCurrSentInfo);
		do {
			info = mCourseInfo.goNext();
			if (infoSet.contains(info)) {
				return mCurrSentInfo;
			} else {
				infoSet.add(info);
			}
		} while (!isPlaySound(info));
		return info;
	}

	private boolean isPlaySound(SentenceInfo info) {

		switch (mCourseInfo.getCurrLevel()) {
		case 4:
			return true;
		case 3:
			if (info.getType()==SentenceInfo.TYPE_INVALID) {
				return true;
			}
			break;
		case 2:
			if (info.getType()!=SentenceInfo.TYPE_INVALID) {
				return true;
			}
			break;
		case 1:
			if (info.getType()!=SentenceInfo.TYPE_INVALID && info.isHasMeaningFile() && info.isTtsable()) {
				return true;
			}
			break;
		case 0:
			if (info.getType()!=SentenceInfo.TYPE_INVALID && (!info.isHasMeaningFile() || !info.isTtsable())) {
				return true;
			}
			break;
		}
		return false;

	}

	void setOnClickListener(int resId) {
		findViewById(resId).setOnClickListener(this);
	}

	View getView(int resId) {
		return findViewById(resId);
	}

	void editSubtitle() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		String oriText = mCurrSentInfo.getMeaningStr();
		 if("║".equals(pauseButton.getText())) {
			 pauseButton.performClick();
		 }

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		if (oriText == null || oriText.trim().equals("")) {
			input.setText("");
			alert.setTitle("자막입력");
		} else {
			input.setText(oriText);
			alert.setTitle("자막수정");
		}


		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String text = input.getEditableText().toString();
				replaceSubtitle(-1, text);
				setSubtitleTextView();

				// Do something with value!
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});

		alert.show();


	}

	void editMeaning(final int pos, final String oriText) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		if (pos < 0) {
			alert.setTitle("문장추가");
		} else {
			alert.setTitle("문장수정");
		}

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setText(oriText);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String text = input.getEditableText().toString();
				if (pos < 0) {
					addSubtitle(text);
				} else {
					replaceSubtitle(pos, text);
				}
				// Do something with value!
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});

		alert.show();
	}

	void editMeaning() {
		editMeaning(-1, "");
	}
	void showHowToDialog() {
		String myLongText = 
				"  1. Quick Start\n" +
						"      1) 해석이 있는 텍스트 파일(.txt) 준비\n" +
						"         : 줄바꾸기로 문장 구분\n"+
						"      2) 문장자동나누기\n"+
						"        a. 자동나누기 선택\n" +
						"        b. 무음길이 선택\n"+
						"             : 0.6초 혹은 미디어에 적당한 값\n"+
						"        c. 확인 선택\n"+
						"             : 자동나누기 진행됨\n"+
						"      4) 텍스트 불러와서 각 문장에 연결\n"+
						"        a. 문장불러오기 선택\n"+
						"        b. 텍스트 파일 선택\n"+
						"        c. Play 선택\n"+
						"        d. 플레이되는 문장 확인\n"+
						"        e. 첫문장과 일치하면 첫문장 선택 누름\n"+
						"        f. 필요없는 부분이면 지우기 선택\n"+
						"        g. 구간 수정을 위해 나누기/합치기 이용\n"+
						"        h. 문장추가/문장수정도 적절히 활용\n";
		final TextView myView = new TextView(getApplicationContext());
		myView.setText(myLongText);
		myView.setTextSize(14);
		final AlertDialog d = new AlertDialog.Builder(ModifyMp3Activity.this)
		.setPositiveButton(android.R.string.ok, null)
		.setTitle("도움말")
		.setView(myView)
		.create();
		d.show();

	}



	void setPrefInt(String key, int i) {
		SharedPreferences.Editor e = mPref.edit();
		e.putInt(key, i);
		e.commit();
	}

	void setPrefBoolean(String key, boolean value) {
		SharedPreferences.Editor e = mPref.edit();
		e.putBoolean(key, value);
		e.commit();

	}

	int getPrefInt(String key, int def) {
		return mPref.getInt(key, def);
	}

	boolean getPrefBoolean(String key, boolean def) {
		return mPref.getBoolean(key, def);
	}

	void setAutoMutePref(int mute) {
		setPrefInt("auto_mute", mute);
	}
	int getAutoMutePref() {
		return getPrefInt("auto_mute", 2);
	}
}
