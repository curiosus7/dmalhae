/**
 * 	121229	On Level 2, Level Buttons are not enabled.						fixed
 * 	121229	At meaning silence, time goes twice faster than usual.  		fixed
 * 
 */


package com.doogie.damalhae;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class PracticeActivity extends BaseActivity
implements OnClickListener, OnLongClickListener, OnSeekBarChangeListener, 
OnSeekCompleteListener, SensorEventListener, SurfaceHolder.Callback, OnKeyListener {





	final static String TAG="PracticeActivity";

	final static int STATE_SOUND_PLAY = 0;
	//	final static int STATE_MEANING_REC = 2;
	final static int STATE_MEANING_PLAY = 3;
	final static int STATE_SOUND_SILENCE = 4;
	final static int STATE_MEANING_SILENCE = 5;
	final static int STATE_SUBTITLE_CLICK = 6;
	//	final static int STATE_BEFORE_REC = 6;
	//	final static int STATE_AFTER_REC = 7;
	//	final static int STATE_BEFORE_PLAY = 8;
	//	final static int STATE_AFTER_PLAY = 9;

	static PracticeActivity mInstance;
	final static int START_MARGIN = 0;

	final static int STATE_CONTINUE = 9;
	final static int STATE_STOP = 10;

	final static int UPDATE_PERIOD = 100;
	final static int SILENCE_BIAS = 500;
	final static int SCALE_MAX = 301;
	int mPeriod = UPDATE_PERIOD;

	SharedPreferences mPref;
	SharedPreferences mSettingsPref;

	//private CourseInfo mCourseInfo;
	//	private File mCourseFile;
	//private SentenceInfo mCurrSentInfo;
	private SentenceInfo mOldSentInfo;
	private boolean mNeedRemoveFile = false;


	private TextToSpeech mTts = null;
	private boolean mTtsInit;

	boolean mIsPlaying = false;
	boolean mIsRecording = false;
	Handler mHandler = new Handler();
	Boolean mIsInit = true;

	MediaPlayer mMeaningPlayer;
	MediaRecorder mRecorder;
	TextView playTextView;
	TextView titleTextView;

	SeekBar playSeekBar;

	TextView playAllTextView;
	SeekBar playAllSeekBar;



	//Button prevButton;
	//Button nextButton;
	Button prevSecButton;
	Button nextSecButton;

	//	Button levelUpButton;
	//	Button levelDownButton;

	//	Button pauseButton;
	Button stopButton;
	Button removeMeaningButton;
	Button viewModeButton;
	Button stopModeButton;
	View recordView;


	private Spinner mExpSpinner;
	private ArrayAdapter<String> mExpAdapter;
	ArrayList<String> mExpList;

	private Spinner muteSpinner;
	private ArrayAdapter<String> muteAdapter;
	ArrayList<String> muteList;

	//	String mCourseName;
	//	File mCourseDir;
	File [] mFiles = null;
	File mCurrFile = null;
	Uri mCurrUri = null;
	int mCurrIndex = 0;
	int mPrevIndex = 0;
	File mPrevFile = null;
	ArrayList<FileInfo> mFileInfoArray;
	Intent mIntent = null;
	Uri mUri = null;
	PowerManager.WakeLock mWl; 
	boolean mIsDestroyed = false;
	int mCurrState = STATE_STOP;
	int mPrevState = STATE_STOP;
	int mCount;
	//	int mCurrCnt;
	int mOldTotalCnt;
	private SurfaceView mVideoView;
	private SurfaceHolder mVideoHolder;

	//	private boolean mSoundPlayerPrepared;
	Boolean mIsPrepared = false;
	int mDuration = 0;
	Random mRnd;
	Runnable mRun;
	//	private SoundPool mSoundPool;
	//	private int mBeepSound;
	//	private int mLevelUpSound;
	//	private int mLevelDownSound;

	private long mSilenceStart;

	//	private boolean mViewMode = false;
	//	private boolean mStopMode = true;

	//	protected boolean mRecording = false;
	private MenuItem mAutoLevelModeMenuItem;
	private MenuItem mShakeUpModeMenuItem;
	private MenuItem mRandModeMenuItem;
	private MenuItem mTransModeMenuItem;

	private MenuItem mRecordingMenuItem;

	private List <SentenceInfo> mRandSentInfoList;




	static PracticeActivity getInstance() {
		return mInstance;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mInstance = this;
		setContentView(R.layout.practice);
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		am.registerMediaButtonEventReceiver(new ComponentName(getPackageName(), RemoteControlReceiver.class.getName()));

		mPref = getPreferences(Activity.MODE_PRIVATE);
		mSettingsPref = getSharedPreferences("settings", MODE_PRIVATE);
		initView();
		initData();
	}


	void initData() {
		mRnd = new Random(System.currentTimeMillis());
		mRun = new Runnable() {

			public void run() {
				// TODO Auto-generated method stub
				updateUiOnState();
			}
		};
		mRandSentInfoList = new ArrayList <SentenceInfo>();
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); 
		accelerormeterSensor = sensorManager 
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		setTtsMode(getTtsModePref());
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");

		Intent intent = getIntent();
		mUri = intent.getData();
		Bundle extras = intent.getExtras();
		mCourseInfo = (CourseInfo)extras.get("courseInfo");
		setLevelListString();
		//		mSoundPool = new SoundPool(4,AudioManager.STREAM_MUSIC, 100);
		mRecorder = new MediaRecorder();
		mMeaningPlayer = new MediaPlayer();

		mMeaningPlayer.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				if (mCurrState == STATE_MEANING_PLAY) {
					if (!mTts.isSpeaking() && !mSoundPlayer.isPlaying() && mp.isPlaying()) {
						goNextState();
					}
				}
			}
		});
		mMeaningPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				playSeekBar.setProgress(0);
				playSeekBar.setMax(mp.getDuration());
				setTitleTextView();
				setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
				if (mIsPlaying) {
					voicePlayerStart();
					mHandler.post(mRun);
				}
			}
		});

		initlevelSpinner(mCourseInfo.MAX_LEVEL);
		initMuteSpinner(200);

		initIconVisility();

	}

	void setCurrState (int state)
	{
		mPrevState = mCurrState;
		mCurrState = state;
		initUiForState(state);
	}
	void setVisibility (int visible) { 
		titleTextView.setVisibility(visible);
		playTextView.setVisibility(visible);
		playSeekBar.setVisibility(visible);
		playAllTextView.setVisibility(visible);
		playAllSeekBar.setVisibility(visible);

	}
	void initView() {
		playTextView = (TextView)findViewById(R.id.playTextView);
		titleTextView = (TextView)findViewById(R.id.titleTextView);
		//titleTextView.setSelected(true);
		mVideoView = (SurfaceView)findViewById(R.id.videoView);
		mVideoHolder = mVideoView.getHolder();
		mVideoHolder.addCallback(this);
		mVideoHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		setSelected(R.id.descriptionTextView, true);


		prevButton = (Button)findViewById(R.id.prevButton);
		nextButton = (Button)findViewById(R.id.nextButton);
		prevSecButton = (Button)findViewById(R.id.prevSecButton);
		nextSecButton = (Button)findViewById(R.id.nextSecButton);

		levelUpButton = (Button)findViewById(R.id.levelUpButton);
		levelDownButton = (Button)findViewById(R.id.levelDownButton);


		pauseButton = (Button)findViewById(R.id.pauseButton);
		stopButton = (Button)findViewById(R.id.stopButton);
		removeMeaningButton = (Button)findViewById(R.id.removeMeaningButton);
		viewModeButton = (Button)findViewById(R.id.viewModeButton);
		stopModeButton = (Button)findViewById(R.id.stopModeButton);
		recordView = findViewById(R.id.recordView);

		playSeekBar = (SeekBar)findViewById(R.id.playSeekBar);
		playSeekBar.setBackgroundColor(Color.BLACK);

		playAllTextView = (TextView)findViewById(R.id.playAllTextView);
		playAllSeekBar = (SeekBar)findViewById(R.id.playAllSeekBar);

		pauseButton.setOnClickListener(this);
		stopButton.setOnClickListener(this);
		removeMeaningButton.setOnClickListener(this);
		viewModeButton.setOnClickListener(this);
		stopModeButton.setOnClickListener(this);
		prevButton.setOnClickListener(this);
		nextButton.setOnClickListener(this);
		setSubtitleOnClickLisener(this);

		prevSecButton.setOnClickListener(this);
		nextSecButton.setOnClickListener(this);

		levelUpButton.setOnClickListener(this);
		levelDownButton.setOnClickListener(this);

		levelUpButton.setOnLongClickListener(this);
		levelDownButton.setOnLongClickListener(this);
		removeMeaningButton.setOnLongClickListener(this);

		playSeekBar.setOnSeekBarChangeListener(this);
		playAllSeekBar.setOnSeekBarChangeListener(this);

		visibleView(R.id.expGroupLayout);


	}

	void initUiForState(int state)
	{
		Log.d (TAG, "iniUiForState("+state+") called");
		String filePath = null;
		setDescriptionText();
		playSeekBar.setProgress(0);
		int level = mCourseInfo.getCurrLevel();
		switch (state) {
		case STATE_SOUND_PLAY:
		case STATE_SUBTITLE_CLICK:

			if (level==0 || level==CourseInfo.MAX_LEVEL+1) {
				setSubtitleTextView(true);
			} else {
				setSubtitleTextView(false);
			}
			setMoveButtonEnabled(true);
			int start = (int)mCurrSentInfo.getStart();
			int currPos = mSoundPlayer.getCurrentPosition();
			if  ((level != CourseInfo.MAX_LEVEL + 1 && level != 0) || (currPos < start - 700) || (currPos > start + 700)) {
				SoundPlayerSeekToStart();
			}

			playSeekBar.setProgress(0);
			playSeekBar.setMax(mCurrSentInfo.getDuration() + START_MARGIN);
			setTitleTextView();
			setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
			setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
			if (mIsPlaying == true) {
				soundPlayerStart();
			}
			break;

		case STATE_MEANING_PLAY:
			setSubtitleTextView(true);
			setMoveButtonEnabled(true);
			resetSilenceStart(); 
			playSeekBar.setProgress(0);
			playSeekBar.setMax(mCurrSentInfo.getDuration());
			setTitleTextView();
			setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
			if (mIsPlaying) {
				playMeaning();
				resetSilenceStart(); 
			} else {

			}
			break;

		case STATE_STOP:

			mOldTotalCnt = mCourseInfo.getCurrLevelCnt();
			playTextView.setText("");
			titleTextView.setText(mCourseInfo.getCourseName() + "\n ( " + 0 + " ~ " + mCourseInfo.getDuration() + " )");
			nextButton.setEnabled(false);
			prevButton.setEnabled(false);
			nextSecButton.setEnabled(false);
			prevSecButton.setEnabled(false);
			initIconVisility();
			long prefPos = getPosPref(mCourseInfo.getCurrLevel());
			mCurrSentInfo = mCourseInfo.setCurrStart(prefPos);
			if (mCurrSentInfo == null) {
				mCurrSentInfo = mCourseInfo.goLast();
			}

			if (!mIsPlaying)
			{
				pauseButton.setText("▶");
			}
			mExpSpinner.setEnabled(true);
			setLevelButtonEnabled(true);
			//			setRecordingMode(mCourseInfo.getCurrLevel()==5);
			setRemoveButtonEnabled(isRecordingMode());
			setSubtitleTextView(false);
			mSilenceStart = 0;
			goRandNextSession();
			break;

		case STATE_MEANING_SILENCE:
			filePath = mCourseInfo.getRecordingFilePath(level, state);
			setSubtitleTextView(true);
			setMoveButtonEnabled(true);
			playSeekBar.setProgress(0);
			playSeekBar.setMax(mCurrSentInfo.getDuration() * getSilenceRatio(mCurrSentInfo.getLevel(), state) / 100 + SILENCE_BIAS);

			if (isRecordingMode() && !prepareMeaning(filePath)) {
				prepareRecorder(filePath);
				startRecording();
			}
			if (mIsPlaying) {
				resetSilenceStart();
			}
			break;
		case STATE_SOUND_SILENCE:
			filePath = mCourseInfo.getRecordingFilePath(level, state);
			playSeekBar.setProgress(0);
			playSeekBar.setMax(mCurrSentInfo.getDuration() * getSilenceRatio(mCurrSentInfo.getLevel(), state) / 100 + SILENCE_BIAS);
			if (isRecordingMode() && ! prepareMeaning(filePath)) {
				prepareRecorder(filePath);
				startRecording();
			}
			if (mIsPlaying) {
				resetSilenceStart();
			}
			break;

		}
	}

	void setSelected(int resId, boolean selected) {
		findViewById(resId).setSelected(selected);
	}
	void setVisibility(int resId, int visibility) {
		findViewById(resId).setVisibility(visibility);
	}

	void setStopView (boolean stop) {
		int width = mSoundPlayer.getVideoHeight();
		int height = mSoundPlayer.getVideoHeight();

		if (width == 0) {
			setVisibility(R.id.scoreboard, View.VISIBLE);
			return;
		} 
		if (stop) {
			setVisibility(R.id.scoreboard, View.VISIBLE);
		} else {
			setVisibility(R.id.scoreboard, View.GONE);
		}
	}


	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mCourseInfo.storeCourseInfo();
	}








	@Override
	protected void onStart() {
		super.onStart();
		if (accelerormeterSensor != null) {
			sensorManager.registerListener(this, accelerormeterSensor, 
					SensorManager.SENSOR_DELAY_GAME); 
		} 

		//		mBeepSound = mSoundPool.load(this,R.raw.beep,1);
		//		mLevelUpSound = mSoundPool.load(this, R.raw.level_up,1);
		//		mLevelDownSound = mSoundPool.load(this,  R.raw.level_down,1);
		aquireWakeLock(false);
		initTts();
	}
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		aquireWakeLock(false);
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
//		MenuItem levelUpAll = menu.add("일괄단계올리기");
		mAutoLevelModeMenuItem = menu.add("");
		setAutoLevelMode(getAutoLevelModePref());
		mAutoLevelModeMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				setAutoLevelMode(!isAutoLevelMode());
				return false;
			}
		});

		mShakeUpModeMenuItem = menu.add("");
		setShakeUpMode(getShakeUpModePref());
		mShakeUpModeMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				setShakeUpMode(!isShakeUpMode());
				return false;
			}
		});
		mRandModeMenuItem = menu.add("");
		setRandMode(getRanModedPref(mCourseInfo.getCurrLevel()));
		mRandModeMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				setRandMode(!isRandMode());
				return false;
			}
		});

		mTransModeMenuItem = menu.add("");
		setTransMode(getTransModePref(mCourseInfo.getCurrLevel()));
		mTransModeMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				setTransMode(!isTransMode());
				return false;
			}
		});

/*
		levelUpAll.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				mCourseInfo.levelUpAll();
				goRandNextSession();
				setLevelListString();
				return true;
			}
		});
*/
		mRecordingMenuItem = menu.add("");
		setRecordingMode(getRecordingPref(mCourseInfo.getCurrLevel()));
		mRecordingMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				setRecordingMode(!isRecordingMode());
				return true;
			}
		});
		/*
		MenuItem del = menu.add("문장지우기");
		del.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				mCourseInfo.setType(SentenceInfo.TYPE_INVALID);
				setLevelListString();
				if (isNextMode()) {
					pauseButton.setText("║");
				}
				goRandNextSession();
				return true;
			}			
		});
		 */

		MenuItem help = menu.add("도움말");
		help.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent i = new Intent(Intent.ACTION_VIEW, 
						Uri.parse("http://blog.naver.com/curiosus/40186499195"));
				startActivity(i);
				return true;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		mInstance = null;
		setIsPlaying(false);
		mSoundPlayer.release();
		mIsDestroyed = true;
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		am.unregisterMediaButtonEventReceiver(new ComponentName(getPackageName(), RemoteControlReceiver.class.getName()));
		super.onDestroy();
	}

	void updateUiOnState() {
		Log.d(TAG, "[updateUiOnState] mCurrState : " + mCurrState + ", mIsPlaying : " + mIsPlaying + "[" + playSeekBar.getProgress() + "/" + playSeekBar.getMax() + "]");
		long value;
		if (mCurrState == STATE_STOP) {
			setIsPlaying(false);
			soundPlayerPause();
			meaningPlayerPause();
			mVoicePlayerPause();
			stopRecording();
			initUiForState(mCurrState);
			return;
		}
		if (true) {
			mExpSpinner.setEnabled(false);
			switch (mCurrState) {
			case STATE_SOUND_PLAY:
			case STATE_SUBTITLE_CLICK:
				int currPosition = mSoundPlayer.getCurrentPosition();
				playSeekBar.setProgress(currPosition - (int)mCurrSentInfo.getStart());
				setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
				playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
				setPlayAllTextView(playAllSeekBar.getProgress(), playAllSeekBar.getMax());
				if (!isSoundOnVaildRange(currPosition) && isPlaying()) {
					goNextState();
				}
				mLastPosition = currPosition;
				break;
			case STATE_MEANING_SILENCE:
				value = System.currentTimeMillis() - mSilenceStart;
				if (isPlaying() && !mMeaningPlayer.isPlaying() && value > playSeekBar.getMax()) {
					goNextState();
				} else {
					playSeekBar.setProgress((int)value);
					setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
				}
				break;
			case STATE_SOUND_SILENCE:
				value = System.currentTimeMillis() - mSilenceStart;
				if (!mMeaningPlayer.isPlaying() && value > playSeekBar.getMax()) {
					goNextState();
				} else {
					playSeekBar.setProgress((int)value);
					setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
				}
				break;

			case STATE_MEANING_PLAY:
				if (mIsPlaying) {
					value = System.currentTimeMillis() - mSilenceStart;
					if (value > 500 && !mTts.isSpeaking()) {
						goNextState();

					} else { 
						playSeekBar.setProgress((int)value);
						setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
					}
				}
				break;
				/*
			case STATE_BEFORE_REC:
				if (mMeaningPlayer.isPlaying()) {
					playSeekBar.setProgress(mMeaningPlayer.getCurrentPosition());
					setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
				} else {
					value = System.currentTimeMillis() - mSilenceStart;
					if (value > playSeekBar.getMax()) {
						goNextState();
					} else {
						playSeekBar.setProgress((int)value);
						setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
					}
				}
				break;

			case STATE_AFTER_REC:
				if (mMeaningPlayer.isPlaying()) {
					playSeekBar.setProgress(mMeaningPlayer.getCurrentPosition());
					setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
				} else {
					value = System.currentTimeMillis() - mSilenceStart;
					if (value > playSeekBar.getMax()) {
						goNextState();
					} else {
						playSeekBar.setProgress((int)value);
						setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
					}
				}
				break;
				 */
			}
			mHandler.postDelayed(mRun, mPeriod);
		}
	}


	// Set up New Sentence
	//




	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()) {

		case R.id.pauseButton:
			if (isNextMode()) {
				pauseButton.setText("║");
				goRandNextSession();
			} else {
				switch (mCurrState) {
				case STATE_SOUND_PLAY:
					if (mIsPlaying) {
						setIsPlaying(false);
						pauseButton.setText("▶");
						soundPlayerPause();
					} else {
						pauseButton.setText("║");
						soundPlayerStart();
						setIsPlaying(true);
					}
					break;
				case STATE_MEANING_PLAY:
					if (mIsPlaying) {
						setIsPlaying(false);
						pauseButton.setText("▶");
						meaningPlayerPause();
					} else {
						pauseButton.setText("║");
						if (!mTts.isSpeaking()) {
							resetSilenceStart();
							playMeaning();
						}
						setIsPlaying(true);
					}
					break;

				default:

					if (mIsPlaying) {
						setIsPlaying(false);
						pauseButton.setText("▶");
						mVoicePlayerPause();
					} else {
						if (mSilenceStart==0) {
							resetSilenceStart();
						}
						if (isRecordingMode() && !mIsRecording) {
							voicePlayerStart();
						}
						pauseButton.setText("║");
						setIsPlaying(true);
					}
					break;
				}
			}
			break;
		case R.id.stopButton:
			if (mCurrState != STATE_STOP) {
				if (!mIsPlaying) {
					setPosPref(mCourseInfo.getCurrLevel(), 0L);
				}	else {
					setPosPref(mCourseInfo.getCurrLevel(), mCourseInfo.getPrev(mCurrSentInfo.getStart()).getStart());
				}
				mCurrState = STATE_STOP;
				updateUiOnState();
			}			
			break;

		case R.id.prevButton:
			if (isNextMode()) {
				pauseButton.setText("║");
			}
			goPrevState();
			break;
		case R.id.prevSecButton:
			soundPlayerSeekTo(mSoundPlayer.getCurrentPosition()-500);
			if (mSoundPlayer.getCurrentPosition()<mCurrSentInfo.getStart()) {
				SoundPlayerSeekToStart();
			}
			playSeekBar.setProgress((int) (mSoundPlayer.getCurrentPosition()-mCurrSentInfo.getStart()));
			setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
			if (isNextMode()) {
				pauseButton.setText("║");
				soundPlayerStart();
				setIsPlaying(true);
			}
			break;
		case R.id.nextSecButton:
			soundPlayerSeekTo(mSoundPlayer.getCurrentPosition()+500);
			playSeekBar.setProgress((int) (mSoundPlayer.getCurrentPosition()-mCurrSentInfo.getStart()));
			setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
			break;

		case R.id.nextButton:
			if (isNextMode()) {
				pauseButton.setText("║");
				goRandNextSession();
			} else {
				goNextState();
			}
			break;
		case R.id.levelUpButton:
			//			playLevelUpSound();
			mRandSentInfoList.remove(mOldSentInfo);
			mCourseInfo.levelUp(mOldSentInfo);
			setLevelListString();
			setLevelButtonEnabled(false);
			if (isNextMode()) {
				pauseButton.setText("║");
				goRandNextSession();
			} else if (!isPlaying()) {
				goRandNextSession();
			}
			break;
		case R.id.levelDownButton:
			//			playLevelDownSound();
			mRandSentInfoList.remove(mOldSentInfo);
			mCourseInfo.levelDown(mOldSentInfo);
			setLevelListString();
			setLevelButtonEnabled(false);
			if (isNextMode()) {
				pauseButton.setText("║");
				goRandNextSession();
			} else if (!isPlaying()) {
				goRandNextSession();
			}
			break;
		case R.id.removeMeaningButton:
			if (isRecordingMode()) {
				if (mIsRecording) {
					stopRecording();
				} else {
					mVoicePlayerStop();
				}
				stopRecording();
				mVoicePlayerStop();
				soundPlayerPause();
				removeMeaningFile(mCurrSentInfo);
				if (isTransMode()) {
					if (!isTtsMode()) {
						setCurrState(STATE_MEANING_SILENCE);
					} else {
						setCurrState(STATE_MEANING_PLAY);
					}
				} else {
					setCurrState(STATE_SOUND_PLAY);
				}
				/*
				switch (mCurrSentInfo.getLevel()) {
				case 1:
					setCurrState(STATE_SOUND_PLAY);
					break;
				case 2:
				case 3:
				case 4:
				case 5:
					setCurrState(STATE_MEANING_PLAY);
					break;
				}
				 */


			}
			/*
			if (mViewMode) {
				setCurrState();
			} else {
				setCurrState(STATE_MEANING_PLAY);
			}
			 */

			break;

		case R.id.viewModeButton:
			setTtsMode(!isTtsMode());
			setStopMode(!isTtsMode());
			if(!isTtsMode()) {
				if (mCurrState == STATE_MEANING_PLAY) {
					meaningPlayerPause();
					mOldSentInfo = mCurrSentInfo;
					setLevelButtonEnabled(true);
					goNextState();
				}
			}
			break;
		case R.id.stopModeButton:
			setStopMode(!isStopMode());
			if (isNextMode()) {
				pauseButton.setText("║");
			}
			break;
		case R.id.subtitleTextView:
			if(!isPlaying()) {
				setCurrState(STATE_SUBTITLE_CLICK);
				soundPlayerStart();
				setIsPlaying(true);
			}
		}
		

	}
	/*
	void initForViewMode(boolean ttsMode) {
		if (ttsMode) {
			mViewMode = true;
			mStopMode = true;
			viewModeButton.setText("TTS켜기");
			stopModeButton.setText("멈춤없음");
		} else {
			mViewMode = false;
			mStopMode = false;
			viewModeButton.setText("TTS끄기");
			stopModeButton.setText("자동멈춤");
		}
		setViewModePref(viewMode);
	}
	 */
	public void onSeekComplete(MediaPlayer arg0) {
		// TODO Auto-generated method stub

	}


	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		switch(seekBar.getId()) {
		case R.id.playSeekBar:
			if(fromUser && (mCurrState == STATE_SOUND_PLAY)) {
				soundPlayerSeekTo((int) (getSeekValue(mCurrSentInfo.getStart())+progress));
				setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
				if (isNextMode()) {
					pauseButton.setText("║");
					soundPlayerStart();
					setIsPlaying(true);
				}
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





	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}


	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}
	public void setView (int resId, int visibility) {
		findViewById(resId).setVisibility(visibility);
	}  
	public void goneView(int resId) {
		setView(resId, View.GONE);
	}
	public void visibleView(int resId) {
		setView(resId, View.VISIBLE);
	}

	//	public void playBeepSound(){
	//		float vol = 10/50.000f;
	//		mSoundPool.play(mBeepSound, vol, vol, 0, 0, (float)1.0);
	//	}
	//	
	//	public void playLevelUpSound() {
	//		float vol = 50/50.000f;
	//		mSoundPool.play(mLevelUpSound, vol, vol, 0, 0, (float)1.0);
	//	}
	//
	//	public void playLevelDownSound() {
	//		float vol = 50/50.000f;
	//		mSoundPool.play(mLevelDownSound, vol, vol, 0, 0, (float)1.0);
	//	}

	void soundPlayerPause() {
		if (mSoundPlayer.isPlaying()) {
			mSoundPlayer.pause();
		}
	}
	void soundPlayerStart() {
		if (!mSoundPlayer.isPlaying())
			mSoundPlayer.start();
	}

	void meaningPlayerPause() {
		mTts.stop();
	}
	void voicePlayerStart() {
		if (!mMeaningPlayer.isPlaying()) {
			mMeaningPlayer.start();
		}
	}
	void mVoicePlayerPause() {
		if (mMeaningPlayer.isPlaying()) {
			mMeaningPlayer.pause();
		}
	}

	void mVoicePlayerStop() {
		mMeaningPlayer.stop();
	}

	void startRecording() {
		playSeekBar.setBackgroundColor(Color.RED);
		mRecorder.start();
		mIsRecording = true;
	}

	void stopRecording() {
		playSeekBar.setBackgroundColor(Color.BLACK);
		if (mIsRecording) {
			mRecorder.stop();
			mIsRecording = false;
		}
	}


	void setMoveButtonEnabled (boolean enable) {
		prevButton.setEnabled(enable);
		nextButton.setEnabled(enable);
		prevSecButton.setEnabled(enable);
		nextSecButton.setEnabled(enable);
	}

	private boolean prepareRecorder(String file) {
		mRecorder.reset();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		mRecorder.setOutputFile(file);

		try {
			mRecorder.prepare();
		} catch (Exception e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private SentenceInfo goNextPlaySound() {
		SentenceInfo info = null;
		Set<SentenceInfo> infoSet = new HashSet<SentenceInfo>();
		infoSet.add(mCurrSentInfo);
		do {
			if (mCourseInfo.getCurrLevel()==CourseInfo.MAX_LEVEL+1) {
				info = mCourseInfo.goNext();
			} else {
				info = mCourseInfo.goNext(SentenceInfo.TYPE_SOUND);
			}
			if (infoSet.contains(info)) {
				return mCurrSentInfo;
			} else {
				infoSet.add(info);
			}
		} while (!isPlaySound(info));
		return info;
	}

	private boolean isPlaySound(SentenceInfo info) {
		if (mCourseInfo.getCurrLevel() == CourseInfo.MAX_LEVEL+1) {
			return true;
		}
		if (info.getMeaningType() != SentenceInfo.MEANING_TYPE_FILE || (!info.hasMeaningFile() && !info.isTtsable() )){
			return false;
		}
		if (info.getLevel() == mCourseInfo.getCurrLevel()) {
			return true;
		}
		return false;
	} 


	private SentenceInfo goPrevPlaySound() {
		SentenceInfo info = null;
		do {
			if (mCourseInfo.getCurrLevel()==CourseInfo.MAX_LEVEL+1) {
				info = mCourseInfo.goPrev();
			} else {
				info = mCourseInfo.goPrev(SentenceInfo.TYPE_SOUND);
			}
			if (mCurrSentInfo.equals(info)) {
				return info;
			}
		} while (!isPlaySound(info));
		return info;
	}

	/*
	private SentenceInfo goPrevRecSound() {
		// TODO Auto-generated method stub
		SentenceInfo info = null;
		do {
			info = mCourseInfo.goPrev(SentenceInfo.TYPE_SOUND);
			if (mCurrSentInfo.equals(info)) {
				return info;
			}
		} while (mCurrSentInfo.getMeaningType() == SentenceInfo.MEANING_TYPE_FILE && !(new File (mCourseInfo.getCurrMeaningFilePath()).exists()));
		return info;
	}
	 */
	void initMuteSpinner (int max) {
		muteList = new ArrayList<String>();
		muteSpinner = (Spinner)findViewById(R.id.muteSpinner);
		for (int i=0;i<=max/25;i++) {
			muteList.add(i, (25 * i) + "%");
		}
		muteAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, muteList);
		muteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);   
		muteSpinner.setAdapter(muteAdapter); // NullPointerException!!

		muteSpinner.setSelection(4);
		muteSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				setMutePref(mCourseInfo.getCurrLevel(), arg2);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}


		});
	}

	void initlevelSpinner (int max) {
		mExpList = new ArrayList<String>();
		mExpSpinner = (Spinner)findViewById(R.id.expSpinner);
		for (int i=0;i<max;i++) {
			mExpList.add(i,new String( (i+1) + " 단계"));
		}
		mExpAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mExpList);
		mExpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);   
		mExpSpinner.setAdapter(mExpAdapter); // NullPointerException!!
		while (mCourseInfo.getLevelCnt(mCourseInfo.getCurrLevel()) == 0 ) {
			mCourseInfo.setCurrLevel((mCourseInfo.getCurrLevel() %  CourseInfo.MAX_LEVEL)+1);
		}

		mExpSpinner.setSelection(getCurrLevelPref()-1);
		mExpSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				muteSpinner.setSelection(getMutePref(arg2+1));
				mCourseInfo.setCurrLevel(arg2+1);
				mRandSentInfoList.clear();
				setCurrState(STATE_STOP);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}


		});
	}

	//	121229	On level 2, level buttons are not enabled, fixed
	//
	void goNextState() {
		Log.d(TAG, "goNextState called");
		int level = mCourseInfo.getCurrLevel();
		if (mCurrState == STATE_STOP) {
			goRandNextSession();
		}
		if (isTransMode()) {
			switch (mCurrState) {
			case STATE_MEANING_PLAY:
				if (mNeedRemoveFile) {
					removeMeaningFile(mOldSentInfo);
					mNeedRemoveFile = false;
				}
				mOldSentInfo = mCurrSentInfo;
				setLevelButtonEnabled(true);
				if (playSeekBar.getProgress() > 500) {
					meaningPlayerPause();
					if (isDescription(mCurrSentInfo)) {
						setCurrState(STATE_SOUND_PLAY);
					} else {
						setCurrState(STATE_MEANING_SILENCE);

					}
				}
				else {
					meaningPlayerPause();
					goRandNextSession();
				}


				break;
			case STATE_MEANING_SILENCE:
				stopRecording();
				mVoicePlayerStop();
				setCurrState(STATE_SOUND_PLAY);
				break;
			case STATE_SOUND_PLAY:
				soundPlayerPause();
				if (false) {
					//	setCurrState(STATE_AFTER_REC);
				} else {
					if (isStopMode()) {
						if (mCurrSentInfo.getLevel() != mCourseInfo.getCurrLevel()) {
							goRandNextSession();
						} else {
							pauseButton.setText(" > ");
						}
					} else {
						if (isDescription(mCurrSentInfo)) {
							goRandNextSession();
						} else {
							setCurrState(STATE_SOUND_SILENCE);
						}
					}

				}
				break;
			case STATE_SUBTITLE_CLICK:
				boolean oldIsPlaying = isPlaying();
				soundPlayerPause();
				setIsPlaying(false);
				if (!getLevelButtonEnabled() || !oldIsPlaying) {
					goRandNextSession();
				}
				break;
				
			case STATE_SOUND_SILENCE:
				if (mIsRecording) {
					stopRecording();
					setCurrState(STATE_MEANING_SILENCE);
				} else {
					mVoicePlayerStop();
					goRandNextSession();
				}
				break;
				/*		case STATE_BEFORE_REC:

					setCurrState(STATE_SOUND_PLAY);
					break;
				case STATE_AFTER_REC:
					stopRecording();
					goNextSession();
					break;
				 */
			}
		} else {
			switch (mCurrState) {
			case STATE_MEANING_PLAY:
				meaningPlayerPause();
				if (isStopMode()) {
					setSubtitleTextView(true);
					pauseButton.setText(" > ");
				} else {
					if (isDescription(mCurrSentInfo)) {
						goRandNextSession();
					} else {
						setCurrState(STATE_MEANING_SILENCE);
					}
				}
				break;
			case STATE_MEANING_SILENCE:
				if (mIsRecording) {
					stopRecording();
					setCurrState(STATE_SOUND_PLAY);
				} else {
					mVoicePlayerStop();
					goRandNextSession();
				}
				break;
			case STATE_SOUND_PLAY:
				/*				
					    if (mNeedRemoveFile) {
						removeMeaningFile(mOldSentInfo);
						mNeedRemoveFile = false;
					}
				 */
				mOldSentInfo = mCurrSentInfo;
				setLevelButtonEnabled(true);
				soundPlayerPause();
				if (isDescription(mCurrSentInfo)) {
					setCurrState(STATE_MEANING_PLAY);
				}else {
					setCurrState(STATE_SOUND_SILENCE);
				}
				break;
			case STATE_SOUND_SILENCE:
				mVoicePlayerStop();
				if (mIsRecording) {
					stopRecording();
					setCurrState(STATE_MEANING_SILENCE);
				} else {
					if (isRecordingMode()) {
						setCurrState(STATE_MEANING_SILENCE);
						//goRandNextSession();
					} else {
						if (!isTtsMode()) {
							if (!isStopMode() || mCurrSentInfo.getLevel() != mCourseInfo.getCurrLevel()) {
								setCurrState(STATE_MEANING_SILENCE);
							} else {
								setSubtitleTextView(true);
								pauseButton.setText(" > ");
							}
						} else {
							setCurrState(STATE_MEANING_PLAY);
						}
					}
				}
				break;
			}
		}




	}

	void goPrevState() {
		int level = mCourseInfo.getCurrLevel();
		if (mCurrState == STATE_STOP) {
			goPrevSession();
		}
		if (isTransMode()) {
			switch (mCurrState) {
			case STATE_MEANING_PLAY:
				int pos = playSeekBar.getProgress(); 
				if (pos > 500)
					setCurrState(STATE_MEANING_PLAY);
				else {
					meaningPlayerPause();
					goPrevSession();
				}
				break;
			case STATE_MEANING_SILENCE:
				stopRecording();
				mVoicePlayerStop();
				if (playSeekBar.getProgress() > 1000) {
					setCurrState(STATE_MEANING_SILENCE);
				} else {
					if (!isTtsMode()) {
						goPrevSession();
					} else {
						setCurrState(STATE_MEANING_PLAY);
					}
				}
				break;
			case STATE_SOUND_PLAY:
				if (mSoundPlayer.getCurrentPosition() - mCourseInfo.getCurrStart() > 1000) {
					setCurrState(STATE_SOUND_PLAY);
				} else {
					soundPlayerPause();
					goPrevSession();
				}
				break;

			case STATE_SUBTITLE_CLICK:
				if (!isPlaying()) {
					goPrevSession();
				}
				break;

			case STATE_SOUND_SILENCE:
				stopRecording();
				mVoicePlayerStop();
				setCurrState(STATE_SOUND_PLAY);
				break;
				/*
			case STATE_BEFORE_REC:
				stopRecording();
				mVoicePlayerPause();
				if (mViewMode) {
					setCurrState(STATE_BEFORE_REC);
				} else {
					setCurrState(STATE_MEANING_PLAY);
				}

				break;
			case STATE_AFTER_REC:
				stopRecording();
				mVoicePlayerPause();
				setCurrState(STATE_SOUND_PLAY);
				break;
				 */
			}
		} else {
			switch (mCurrState) {
			case STATE_MEANING_PLAY:
				if (mMeaningPlayer.getCurrentPosition() < 1000)
					setCurrState(STATE_MEANING_PLAY);
				else {
					meaningPlayerPause();
					setCurrState(STATE_SOUND_PLAY);
				}
				break;
			case STATE_MEANING_SILENCE:
				stopRecording();
				mVoicePlayerStop();
				if (playSeekBar.getProgress() > 1000) {
					setCurrState(STATE_MEANING_SILENCE);
				} else {
					if (!isTtsMode()) {
						setCurrState(STATE_SOUND_PLAY);
					} else {
						setCurrState(STATE_MEANING_PLAY);
					}
				}
				break;
			case STATE_SOUND_PLAY:
				if (mSoundPlayer.getCurrentPosition() - mCourseInfo.getCurrStart() > 1000) {
					setCurrState(STATE_SOUND_PLAY);
				} else {
					soundPlayerPause();
					goPrevSession();
				}
				break;
			case STATE_SOUND_SILENCE:
				stopRecording();
				mVoicePlayerStop();
				setCurrState(STATE_SOUND_PLAY);
				break;
			}
		}
	}


	void goNextSession() {
		int level = mCourseInfo.getCurrLevel();
		SentenceInfo oldinfo = mCurrSentInfo;

		soundPlayerPause();
		mMeaningPlayer.pause();
		mVoicePlayerPause();
		stopRecording();

		mCurrSentInfo = goNextPlaySound();
		if (mOldSentInfo == null || !isPlaying() || isStopMode()) {
			mOldSentInfo = mCurrSentInfo;
			setLevelButtonEnabled(true);
		}

		if (oldinfo.equals( mCurrSentInfo) && mCourseInfo.getLevelCnt(mCourseInfo.getCurrLevel())!=1) {
			mExpSpinner.setSelection((mCourseInfo.getCurrLevel())%CourseInfo.MAX_LEVEL);
			return;
		}
		int levelCnt = mCourseInfo.getCurrLevelCnt();

		//		if (mOldTotalCnt != levelCnt) {
		//			mCurrCnt += levelCnt - mOldTotalCnt;
		//			mOldTotalCnt = levelCnt;
		//		}
		//		if (mCurrCnt == levelCnt) {
		//			mCurrCnt = 1;
		//		} else {
		//			mCurrCnt++;
		//		}

		int start = (int)mCurrSentInfo.getStart();
		int currPos = mSoundPlayer.getCurrentPosition();
		if  ((level != CourseInfo.MAX_LEVEL + 1 && level != 0) || (currPos < start - 700) || (currPos > start + 700)) {
			SoundPlayerSeekToStart();
		}
		if (isTransMode()) {
			File file = new File(mCourseInfo.getRecordingFilePath(level, STATE_MEANING_SILENCE));
			// 녹음파일이 있는 경우에도 해석 읽기 생략
			if (!isTtsMode() || (isRecordingMode() && file.exists() && file.length() > 0L)) {
				mOldSentInfo = mCurrSentInfo;
				setLevelButtonEnabled(true);
				setCurrState(STATE_MEANING_SILENCE);
			} else {
				setCurrState(STATE_MEANING_PLAY);
			}
		} else {
			setCurrState(STATE_SOUND_PLAY);
		}
	}




	void goPrevSession() {
		int level = mCourseInfo.getCurrLevel();

		if (mSoundPlayer.isPlaying()) {
			soundPlayerPause();
		}
		meaningPlayerPause();
		mVoicePlayerPause();
		stopRecording();
		mCurrSentInfo = goPrevPlaySound();
		mOldSentInfo = mCurrSentInfo;
		//		if (mCurrCnt==1) {
		//			mCurrCnt = mCourseInfo.getCurrLevelCnt();
		//		}
		//		else {
		//			mCurrCnt--;
		//		}
		int start = (int)mCurrSentInfo.getStart();
		int currPos = mSoundPlayer.getCurrentPosition();
		if  ((level != CourseInfo.MAX_LEVEL + 1 && level != 0) || (currPos < start - 700) || (currPos > start + 700)) {
			SoundPlayerSeekToStart();
		}

		if (isTransMode()) {
			if (!isTtsMode()) {
				setCurrState(STATE_MEANING_SILENCE);
			} else {
				setCurrState(STATE_MEANING_PLAY);
			}

		} else {
			setCurrState(STATE_SOUND_PLAY);
		}
	}

	boolean prepareMeaning () {	
		return false;
	}

	boolean prepareMeaning (String filePath) {	

		mMeaningPlayer.reset();
		File file = new File(filePath);
		if (!file.exists()) {
			return false;
		}
		try {
			mMeaningPlayer.setDataSource(filePath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return false;
		}
		mMeaningPlayer.prepareAsync();
		mHandler.removeCallbacks(mRun);
		return true;
	}


	void setLevelButtonEnabled(boolean enable) {
		levelUpButton.setEnabled(enable);
		levelDownButton.setEnabled(enable);
		if (isRecordingMode()) {
			setRemoveButtonEnabled(enable);
		}
		if (mCurrSentInfo.getLevel()==0) {
			levelDownButton.setEnabled(false);
		}
		if (mCurrSentInfo.getLevel()==mCourseInfo.MAX_LEVEL) {
			levelUpButton.setEnabled(false);
		}

	}
	
	boolean getLevelButtonEnabled() {
		return (levelUpButton.isEnabled() || levelDownButton.isEnabled());
	}

	void setRemoveButtonEnabled(boolean enable) {
		removeMeaningButton.setEnabled(enable);
	}


	@Override
	public void onBackPressed() {
		setPosPref(mCourseInfo.getCurrLevel(), mCourseInfo.getPrev(mCurrSentInfo.getStart()).getStart());
		setCurrLevelPref(mCourseInfo.getCurrLevel());
		mCourseInfo.storeCourseInfo();
		mHandler.removeCallbacks(mRun);
		mHandler.removeCallbacks(mRun);
		Intent intent = new Intent();
		Bundle extras = new Bundle();
		extras.putSerializable("courseInfo", mCourseInfo);
		intent.putExtras(extras);
		setResult(RESULT_OK, intent);
		finish();
	}

	void removeMeaningFile (SentenceInfo info) {
		//		Utils.removeFile(mCourseInfo.getCourseName(), mCourseInfo.getMeaningFilePath(info));
		mCourseInfo.removeRecordingFile(STATE_MEANING_SILENCE);
		mCourseInfo.removeRecordingFile(STATE_SOUND_SILENCE);
		mCourseInfo.updateCourseInfo();
		setLevelListString();
	}

	private long lastTime; 
	private float speed; 
	private float lastX; 
	private float lastY; 
	private float lastZ; 

	private float x, y, z; 
	private static final int SHAKE_THRESHOLD = 1500; 
	private static final int DATA_X = SensorManager.DATA_X; 
	private static final int DATA_Y = SensorManager.DATA_Y; 
	private static final int DATA_Z = SensorManager.DATA_Z; 


	private SensorManager sensorManager; 
	private Sensor accelerormeterSensor; 
	private Vibrator mVib;
	private int mShakeCount =0;

	private boolean mIsWakeLocked = false;

	private long mLastPosition;

	private Activity mRandModeMenu;


	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}


	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) { 

			long currentTime = System.currentTimeMillis(); 

			long gabOfTime = (currentTime - lastTime); 



			if (gabOfTime > 100) { 

				lastTime = currentTime; 



				x = event.values[SensorManager.DATA_X]; 
				y = event.values[SensorManager.DATA_Y]; 
				z = event.values[SensorManager.DATA_Z]; 

				speed = Math.abs(x + y + z - lastX - lastY - lastZ) /gabOfTime * 10000;
				//				Log.d(TAG, "speed : " + speed);	

				if (speed > SHAKE_THRESHOLD) { 
					mShakeCount++;
					if (mShakeCount==3
							&& isShakeUpMode()
							&& levelUpButton.isEnabled()) {
						mVib.vibrate(300);
						levelUpButton.performClick();
						mShakeCount = 0;
					} 	
				} else {
					mShakeCount = 0;
				}



				lastX = event.values[DATA_X]; 

				lastY = event.values[DATA_Y]; 

				lastZ = event.values[DATA_Z]; 

			} 

		} 



	}

	@Override
	public boolean onLongClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()) {

		case R.id.levelDownButton:
			showDialog(v.getId());
			break;
		case R.id.levelUpButton:

			showDialog(v.getId());
			break;
		case R.id.removeMeaningButton:
			mHandler.removeCallbacks(mRun);
			mHandler.removeCallbacks(mRun);
			mHandler.removeCallbacks(mRun);
			mHandler.removeCallbacks(mRun);

			if (isRecordingMode() && (mCurrState == STATE_MEANING_SILENCE || mCurrState == STATE_SOUND_SILENCE)) {
				if (mIsRecording) {
					stopRecording();
				} else {
					mVoicePlayerStop();
				}
				setCurrState(STATE_SOUND_PLAY);
			}
			pause(true);
			removeAllRecording();
			showDialog(v.getId());
			break;
		}
		return false;
	}

	private void removeAllRecording() {
		File dir = mCourseInfo.getDateDir();
		File [] files = dir.listFiles();
		for (int i=0;i<files.length;i++) {
			if (files[i].isFile()) {
				files[i].delete();
			}
		}
	}

	long getSeekValue(long seek) {
		return seek < START_MARGIN ? 0 : seek - START_MARGIN;
	}

	void setIsPlaying(boolean playing) {
		setIsPlaying(playing, 0);
	}	

	void setIsPlaying(boolean playing, int delay) {
		mIsPlaying = playing;
		if (playing) {
			aquireWakeLock(true);
			mHandler.postDelayed(mRun, delay);
		} else {
			aquireWakeLock(false);
			mHandler.removeCallbacks(mRun);
			mHandler.removeCallbacks(mRun);
			mHandler.removeCallbacks(mRun);

		}
	}
	boolean isPlaying() {
		return mIsPlaying;
	}

	void setOrderTextView(int order, int total) {
		setText(R.id.orderTextView, Utils.getProgeressText(order, total));
	}
	private void setPlayTextView(int progress, int max) {
		playTextView.setText(Utils.getProgeressTimeText(progress, max));
	}

	void setLevelOrderTextView(int order, int total) { 
		setText(R.id.levelOrderTextView, Utils.getProgeressText(order, total));
	}

	void setTitleTextView() {
		titleTextView.setText(mCourseInfo.getCourseName());
		setOrderTextView(mCourseInfo.getCurrIndex() + 1, mCourseInfo.getSize());
		if (mCourseInfo.getCurrLevel() == 0) {
			setLevelOrderTextView(mCourseInfo.getCurrIndex() + 1, mCourseInfo.getSize());
		} else {
			setLevelOrderTextView(mCourseInfo.getCurrLevelPosCnt(), mCourseInfo.getCurrLevelCnt());
		}
	}

	int getSilenceRatio (int level, int state) {
		return muteSpinner.getSelectedItemPosition() * 25;
	}

	String getPlayText (int level, int state) {
		if (mCourseInfo.getCurrLevel()==CourseInfo.MAX_LEVEL+1) {
			return ("level : " + level);
		}
		if (isTransMode()) {
			if (isRecordingMode()) {
				switch (state) {
				case STATE_SOUND_PLAY:
					return "원어 듣고 통역 맞는지 확인";
				case STATE_SOUND_SILENCE:
					return "빨간색일때 녹음되요.";
				case STATE_MEANING_PLAY:
					return "해석 듣고 원어 녹음하기";
				case STATE_MEANING_SILENCE:
					return "빨간색일때 녹음되요";
				}

			} else {
				switch (state) {
				case STATE_SOUND_PLAY:
					return "원어 듣고 말한 것과 비교";
				case STATE_SOUND_SILENCE:
					return "원어를 다시 한번 말하기";
				case STATE_MEANING_PLAY:
					return "해석 듣고 원어 말하기";
				case STATE_MEANING_SILENCE:
					return "중간공백은 %버튼을 눌러서 조절";
				}

			}
		} else {
			if (isRecordingMode()) {
				switch (state) {
				case STATE_SOUND_PLAY:
					return "듣고 따라하기 녹음";
				case STATE_SOUND_SILENCE:
					return "빨간색일때는 녹음되요.";
				case STATE_MEANING_PLAY:
					return "해석을 듣고";
				case STATE_MEANING_SILENCE:
					return "원어를 녹음하기";
				}

			} else {
				switch (state) {
				case STATE_SOUND_PLAY:
					return "원어듣고 해석 이해하기!";
				case STATE_SOUND_SILENCE:
					return "해석을 생각해 보고";
				case STATE_MEANING_PLAY:
					return "내 해석과 비교해 보고";
				case STATE_MEANING_SILENCE:
					return "원어를 떠올려서 말하기";
				}

			}
		}


		return "";
	}

	void setDescriptionText() {
		if (mCurrSentInfo != null) {
			setText(R.id.descriptionTextView, getPlayText(mCurrSentInfo.getLevel(), mCurrState));
		} else {
			setText(R.id.descriptionTextView, "");
		}
	}

	void aquireWakeLock(boolean aquire) {
		if (!mIsWakeLocked  && aquire) {
			mIsWakeLocked = true;
			mWl.acquire();
			Log.d(TAG, "WakeLock Aquired");
			return;
		}
		if (mIsWakeLocked && !aquire) {
			mIsWakeLocked = false;
			mWl.release();
			Log.d(TAG, "WakeLock Released");
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mSoundPlayer = new SoundPlayer();

		mSoundPlayer.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				if (mCurrState == STATE_SOUND_PLAY) {
					//					goNextState();
				}
			}
		});
		mSoundPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {


			public void onPrepared(MediaPlayer mp) {
				Log.d(TAG, "mSoundPlayer prepared");
			}
		});


		try {
			mSoundPlayer.setDataSource(mCourseInfo.getCourseFilePath());
		} catch (IOException e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mSoundPlayer.setDisplay(holder);
		mSoundPlayer.setLooping(true);
		try {
			mSoundPlayer.prepare();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

		//Set the width of the SurfaceView to the width of the screen

		//Set the height of the SurfaceView to match the aspect ratio of the video 
		//be sure to cast these as floats otherwise the calculation will likely be 0
		lp.width = mVideoView.getWidth();
		lp.height = (int) (((float)videoHeight / (float)videoWidth) * (float)lp.width);

		//Commit the layout parameters
		mVideoView.setLayoutParams(lp);        
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}

	private void setSubtitleTextView(boolean enable) {
		String subtitle = mCurrSentInfo.getMeaningStr();

		if (enable && subtitle != null) {
			setText(R.id.subtitleTextView, subtitle.replace("\n", " "));
		} else {
			setText(R.id.subtitleTextView, "");
		}
	}
	
	private void setSubtitleOnClickLisener(OnClickListener listener) {
		setOnClickListener(R.id.subtitleTextView, listener);
	}

	private void initTts() {
		mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				mTtsInit = true;
			}
		});

	}
	/*
	void recordMeaning() {
		CourseInfo courseInfo = mCourseInfo;
		SentenceInfo info = courseInfo.getCurrent();
		String filePath = courseInfo.getCurrMeaningFilePath();
		File file = new File (filePath);
		String subtitle = info.getMeaningStr();
		if (subtitle != null && !subtitle.trim().equals("") && (!file.exists() || file.length() == 0)) {
			if (!mTtsInit) {
				initTts();
				while (!mTtsInit) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			int result = mTts.synthesizeToFile(subtitle, null, courseInfo.getCurrMeaningFilePath());
			if (result != TextToSpeech.SUCCESS) {
				Log.e(TAG, "Fail to synthesizeToFile : Need to initialize TextToSpeech and retry");
				mTtsInit = false;
				try {
					Thread.sleep(100);
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
				if (result != TextToSpeech.SUCCESS) {
					mTtsInit = false;
					Log.e(TAG, "Fail to Retry synthesizeToFile : Need to initialize TextToSpeech");
				}

			}
		}
	}
	 */

	boolean playMeaning() {
		CourseInfo courseInfo = mCourseInfo;
		SentenceInfo info = courseInfo.getCurrent();
		//		String filePath = courseInfo.getCurrMeaningFilePath();
		//		File file = new File (filePath);
		String subtitle = info.getMeaningStr();
		Log.d(TAG, "playMeaning called : " + subtitle);
		if (subtitle != null && !subtitle.trim().equals("")) {
			if (!mTtsInit) {
				initTts();
				while (!mTtsInit) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			if (isEnglish(subtitle)) {
				mTts.setLanguage(Locale.US);
			} else {
				mTts.setLanguage(Utils.getLocale(mCourseInfo.getLanguage()));
			}

			int result = mTts.speak(subtitle, TextToSpeech.QUEUE_FLUSH, null);
			if (result != TextToSpeech.SUCCESS) {
				Log.e(TAG, "Fail to synthesizeToFile : Need to initialize TextToSpeech and retry");
				mTtsInit = false;
				try {
					Thread.sleep(100);
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
				result = mTts.speak(subtitle, TextToSpeech.QUEUE_FLUSH, null);
				if (result != TextToSpeech.SUCCESS) {
					mTtsInit = false;
					Log.e(TAG, "Fail to Retry synthesizeToFile : Need to initialize TextToSpeech");
					return false;
				}

			}

			return true;
		}
		return false;
	}

	String getTTsStr (String subtitle) {
		return null;
	}

	void resetSilenceStart() {
		mSilenceStart = System.currentTimeMillis(); 
	}
	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
		switch (id) {
		case R.id.levelDownButton:
			return new AlertDialog.Builder(this)
			.setTitle("단계를 일괄적으로 내리겠습니까?")
			.setPositiveButton("예", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					mRandSentInfoList.clear();
					mCourseInfo.levelDownAll();
					setLevelListString();
					goRandNextSession();
				}
			})
			.setNegativeButton("아니오", null)
			.create();
		case R.id.levelUpButton:
			return new AlertDialog.Builder(this)
			.setTitle("단계가 일괄적으로 올리겠습니까?")
			.setPositiveButton("확인", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					mRandSentInfoList.clear();
					mCourseInfo.levelUpAll();
					setLevelListString();
					goRandNextSession();
				}
			})
			.setNegativeButton("아니오", null)
			.create();
		case R.id.removeMeaningButton:
			return new AlertDialog.Builder(this)
			.setTitle("녹음이 일괄 삭제되었습니다.")
			.setPositiveButton("확인", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					pause(false);
					if (mIsPlaying) {
						mHandler.post(mRun);
					}
				}
			})
			.create();
		}

		return null;
	}

	void pause(boolean on) {
		switch (mCurrState) {
		case STATE_SOUND_PLAY:
			if (on) {
				soundPlayerPause();
			} else {
				setCurrState(STATE_SOUND_PLAY);
				//				if (mIsPlaying) {
				//					soundPlayerStart();
				//				}
			}
			break;
		case STATE_MEANING_PLAY:
			if (on) {
				meaningPlayerPause();
			} else {
				setCurrState(STATE_MEANING_PLAY);
			}
			break;
		}
	}

	boolean isSoundOnVaildRange (int currPosition) {
		//		long currPosition = mSoundPlayer.getCurrentPosition();
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
	
	void soundPlayerSeekTo(int pos) {
		mLastPosition = (long)pos;
		mSoundPlayer.seekTo(pos);
		playAllSeekBar.setMax(mSoundPlayer.getDuration());
		playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
		setPlayAllTextView(playAllSeekBar.getProgress(), playAllSeekBar.getMax());
		return;
	}

	/*
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		super.onKeyDown(keyCode, event);
		switch (keyCode) {
		case KeyEvent.KEYCODE_HEADSETHOOK:
			mVib.vibrate(500);
			levelUpButton.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 5.0f, 5.0f, 0)); 
			levelUpButton.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 5.0f, 5.0f, 0));
			return true;
		case KeyEvent.KEYCODE_BACK
		}
		return false;
	}
	 */

	void setPrefInt(String key, int i) {
		SharedPreferences.Editor e = mPref.edit();
		e.putInt(key, i);
		e.commit();
	}

	void setSettignsPrefInt(String key, int i) {
		SharedPreferences.Editor e = mSettingsPref.edit();
		e.putInt(key, i);
		e.commit();
	}

	
	void setPrefBoolean(String key, boolean value) {
		SharedPreferences.Editor e = mPref.edit();
		e.putBoolean(key, value);
		e.commit();

	}

	void setPrefString(String key, String value) {
		SharedPreferences.Editor e = mPref.edit();
		e.putString(key, value);
		e.commit();

	}


	int getPrefInt(String key, int def) {
		return mPref.getInt(key, def);
	}

	boolean getPrefBoolean(String key, boolean def) {
		return mPref.getBoolean(key, def);
	}

	String getPrefString(String key, String def) {
		return mPref.getString(key, def);
	}
	
	int getSettingsPrefInt(String key, int def) {
		return mSettingsPref.getInt(key, def);
	}
	

	void setMutePref(int level, int mute) {
		setPrefInt(mCourseInfo.getCourseName() + "_mute_" + level, mute);
	}
	int getMutePref(int level) {
		int def;
		switch (level) {
		case 1:
			def = 50/25;
			break;
		case 6:
			def = 2;
			break;
		case 7:
			def = 4;
			break;
		default:
			def = 9-level;
			break;
		}
		return getPrefInt(mCourseInfo.getCourseName() +"_mute_" + level, def);
	}

	int getMaxSentPref(int level) {
		int def = 1000;
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
		return getSettingsPrefInt("max_sent_" + level, def);
	}

	int getMinSentPref(int level) {
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
		return getSettingsPrefInt("min_sent_" + level, def);
	}




	void setRandModePref(int level, boolean value) {
		setPrefBoolean(mCourseInfo.getCourseName()+"_rand_" + level , value);
	}
	boolean getRanModedPref(int level) {
		boolean def = true;
		switch (level) {
		case 1:
		case 2:
		case 6:
		case 7:
			def = false;
			break;
		}
		return getPrefBoolean(mCourseInfo.getCourseName()+"_rand_" + level, def);
	}

	void setTransModePref(int level, boolean value) {
		setPrefBoolean(mCourseInfo.getCourseName()+"_trans_" + level , value);
	}
	boolean getTransModePref(int level) {
		boolean def = false;
		if (level == 1) {
			def = false;
		} else {
			def = true;
		}
		return getPrefBoolean(mCourseInfo.getCourseName()+"_trans_" + level, def);
	}


	boolean getRecordingPref(int level) {
		return getPrefBoolean(mCourseInfo.getCourseName() + "_recording_" + level, level==7);
	}


	void setRecordingPref (int level, boolean value) {
		setPrefBoolean(mCourseInfo.getCourseName() + "_recording_" + level, value);
	}


	void setPosPref(int level, long pos) {
		setPrefInt(mCourseInfo.getCourseName()+"_pos_" + level, (int)pos);
	}
	long getPosPref(int level) {
		return (long)(getPrefInt(mCourseInfo.getCourseName()+"_pos_" + level, -1));
	}

	/*
	void setViewModePref (boolean isView) {
		setPrefBoolean("viewmode", isView);
	}

	boolean getViewModePref() {
		return getPrefBoolean("viewmode", false);
	}
	 */

	void setTtsModePref (boolean value) {
		setPrefBoolean("ttsmode", value);
	}

	boolean getTtsModePref() {
		return getPrefBoolean("ttsmode", true);
	}

	void setStopModePref (boolean value) {
		setPrefBoolean("stopmode", value);
	}

	boolean getStopModePref() {
		return getPrefBoolean("stopmode", false);
	}


	void setShakeUpModePref (boolean value) {
		setPrefBoolean("shakeupmode", value);
	}

	boolean getShakeUpModePref() {
		return getPrefBoolean("shakeupmode", false);
	}

	void setAutoLevelModePref (boolean value) {
		setPrefBoolean(mCourseInfo.getCourseName() +"autolevelmode", value);
	}

	boolean getAutoLevelModePref() {
		return getPrefBoolean(mCourseInfo.getCourseName() +"autolevelmode", true);
	}


	void setCurrLevelPref (int level) {
		setPrefInt(mCourseInfo.getCourseName()+"_curr_level", level);
	}

	int getCurrLevelPref() {
		return getPrefInt(mCourseInfo.getCourseName()+"_curr_level", 1);
	}

	boolean isEnglish(String str) {
		boolean ret = false;
		try {
			ret = str.length()==(str.getBytes("UTF-8")).length;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block

		}
		return ret;
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
		final AlertDialog d = new AlertDialog.Builder(PracticeActivity.this)
		.setPositiveButton(android.R.string.ok, null)
		.setTitle("도움말")
		.setView(myView)
		.create();
		d.show();

	}
	boolean isNextMode() {
		return (pauseButton.getText().equals(" > "));
	}
	void setNextMode() {
		pauseButton.setText(" > ");
	}


	boolean isDescription(SentenceInfo info) {
		return (info.getDuration()>30000L && info.getMeaningStr().length()<10);
	}

	boolean isShakeUpMode() {
		if (mShakeUpModeMenuItem != null) {
			return mShakeUpModeMenuItem.getTitle().equals("흔들어단계올리기끄기");
		}
		return getShakeUpModePref();
	}

	void setShakeUpMode(boolean mode) {
		if (mShakeUpModeMenuItem != null) {
			if (mode) {
				mShakeUpModeMenuItem.setTitle("흔들어단계올리기끄기");
			} else {
				mShakeUpModeMenuItem.setTitle("흔들어단계올리기켜기");
			}
		}
		setShakeUpModePref(mode);
		setVisibility(R.id.sTextView, mode);
	}
	
	boolean isAutoLevelMode() {
		if (mAutoLevelModeMenuItem != null) {
			return mAutoLevelModeMenuItem.getTitle().equals("자동단계이동끄기");
		}
		return getAutoLevelModePref();
	}

	void setAutoLevelMode(boolean mode) {
		if (mAutoLevelModeMenuItem != null) {
			if (mode) {
				mAutoLevelModeMenuItem.setTitle("자동단계이동끄기");
			} else {
				mAutoLevelModeMenuItem.setTitle("자동단계이동켜기");
			}
		}
		setAutoLevelModePref(mode);
		//setVisibility(R.id.sTextView, mode);
	}

	

	boolean isRandMode() {
		if (mRandModeMenuItem != null) {
			return mRandModeMenuItem.getTitle().equals("랜덤모드끄기");
		}
		return getRanModedPref(mCourseInfo.getCurrLevel());
	}

	void setRandMode(boolean mode) {
		if (mRandModeMenuItem != null) {
			if (mode) {
				mRandModeMenuItem.setTitle("랜덤모드끄기");
			} else {
				mRandModeMenuItem.setTitle("랜덤모드켜기");
			}
		}
		setRandModePref(mCourseInfo.getCurrLevel(), mode);
		setVisibility(R.id.raTextView, mode);
	}

	boolean isTransMode() {
		if (mTransModeMenuItem != null) {
			return mTransModeMenuItem.getTitle().equals("원어 > 해석");
		}
		return getTransModePref(mCourseInfo.getCurrLevel());
	}

	void setTransMode(boolean mode) {
		if (mTransModeMenuItem != null) {

			if (mode) {
				mTransModeMenuItem.setTitle("원어 > 해석");
			} else {
				mTransModeMenuItem.setTitle("해석 > 원어");
			}
		}
		setText(R.id.transTextView, mode?"해석 > 원어":"원어 > 해석");
		setTransModePref(mCourseInfo.getCurrLevel(), mode);

	}

	boolean isRecordingMode() {
		if (mRecordingMenuItem != null) {
			return mRecordingMenuItem.getTitle().equals("녹음모드끄기");
		}
		return getRecordingPref(mCourseInfo.getCurrLevel());
	}

	void setRecordingMode(boolean mode) {
		if (mRecordingMenuItem != null) {
			if (mode) {
				mRecordingMenuItem.setTitle("녹음모드끄기");
			} else {
				mRecordingMenuItem.setTitle("녹음모드켜기");
			}
		}
		setRecordingPref(mCourseInfo.getCurrLevel(), mode);
		setVisibility(R.id.rTextView, mode);
	}

	boolean isTtsMode() {
		if (viewModeButton != null) {
			return viewModeButton.getText().equals("TTS끄기");
		}
		return getTtsModePref();
	}

	void setTtsMode(boolean mode) {
		if (viewModeButton != null) {
			if (mode) {
				viewModeButton.setText("TTS끄기");
			} else {
				viewModeButton.setText("TTS켜기");
			}
		}
		setTtsModePref(mode);
		setVisibility(R.id.tTextView, mode);
	}

	boolean isStopMode() {
		if (stopModeButton != null) {
			return stopModeButton.getText().equals("멈춤없음");
		}
		return getStopModePref();
	}

	void setStopMode(boolean mode) {
		if (stopModeButton == null) {
			return;
		}
		if (mode) {
			stopModeButton.setText("멈춤없음");
		} else {
			stopModeButton.setText("자동멈춤");
		}
		setStopModePref(mode);
		setVisibility(R.id.stopTextView, mode);
	}


	void initIconVisility() {
		setRandMode(getRanModedPref(mCourseInfo.getCurrLevel()));
		setTransMode(getTransModePref(mCourseInfo.getCurrLevel()));

		setRecordingMode(getRecordingPref(mCourseInfo.getCurrLevel()));
		setShakeUpMode(getShakeUpModePref());
		setTtsMode(getTtsModePref());
		setStopMode(getStopModePref());
	}
	/*
	void setIconPref() {
		int level = mCourseInfo.getCurrLevel();
		setRandPref(level, isRandMode());
		setRecordingPref(level, isRecordingMode());
		setShakeUpModePref(isShakeUpMode());
		setTtsModePref(isTtsMode());
		setStopModePref(isStopMode());
	}
	 */

	private void setPlayAllTextView(int progress, int max) {
		playAllTextView.setText(Utils.getProgeressTimeText(progress, max));
	}

	void goPositionSession(SentenceInfo info) {
		mCurrSentInfo = mCourseInfo.setCurrent(info);
		goPrevSession();
		goNextSession();
	}

	void goRandNextSession() {
		if (isLevelDownMoveNeeded()) {
			goToLevel(mCourseInfo.getCurrLevel()-1);
			return;
		}

		if (isLevelUpMoveNeeded()) {
			goToLevel(mCourseInfo.getCurrLevel()+1);
			return;
		}

		if (isRandMode() && !(mCourseInfo.getCurrLevelCnt() == 0)) {
			int limit_cnt = 0;
			limit_cnt++;
			int cnt = mRnd.nextInt(mCourseInfo.getCurrLevelCnt())+1;
			for (int i=0;i<cnt;i++) {
				goNextSession();
			}
			SentenceInfo oldInfo = mCurrSentInfo;
			while (mRandSentInfoList.contains(mCurrSentInfo)) {
				goNextSession();
				if (oldInfo.equals(mCurrSentInfo)) {
					clearRandSentInfoList();
				}
			}
			Log.d(TAG, "Random Next : levelCnt : " + mCourseInfo.getCurrLevelCnt() + " limit_cnt : " + limit_cnt + " RandomSentInfoCnt : " + mRandSentInfoList.size());
		} else {
			goNextSession();
		}
		if (mRandSentInfoList.size()>=mCourseInfo.getCurrLevelCnt()) {
			clearRandSentInfoList();
		}
		mRandSentInfoList.add(mCurrSentInfo);
	}

	private void goToLevel(int level) {
		if (!isLevelDownMoveNeeded(level) && !isLevelUpMoveNeeded(level)) {
			playLevelSound(level);
			try {
				Thread.sleep(2000L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		mExpSpinner.setSelection(level-1);
		return;
	}
	void clearRandSentInfoList() {
		int half = (mRandSentInfoList.size()+1)/2;
		for(int i=0; i< half ;i++) {
			mRandSentInfoList.remove(0);
		}
	}

	void setVisibility(int resId, boolean visible) {
		try {
			if (visible) {
				findViewById(resId).setAlpha(1);
			} else {
				findViewById(resId).setAlpha(0.25f);
			}
		} catch (NoSuchMethodError e) { 
			if (visible) {
				findViewById(resId).setVisibility(View.VISIBLE);
			} else {
				findViewById(resId).setVisibility(View.INVISIBLE);
			}
		}
	}

	void setIconVisibility() {

	}


	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		super.onKeyDown(keyCode, event);
		switch (keyCode) {
		/*		case KeyEvent.KEYCODE_MEDIA_NEXT:
			if (levelDownButton.isEnabled()) {
				playLevelDownSound();
				levelDownButton.performClick();
			}
			return true;
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			if (levelUpButton.isEnabled()) {
				playLevelUpSound();
				levelUpButton.performClick();
			}
			return true;
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
		case KeyEvent.KEYCODE_MEDIA_PAUSE:
		case KeyEvent.KEYCODE_MEDIA_PLAY:
			playBeepSound();
			pauseButton.performClick();
			return true;
		 */		case KeyEvent.KEYCODE_BACK:
			 setPosPref(mCourseInfo.getCurrLevel(), mCourseInfo.getPrev(mCurrSentInfo.getStart()).getStart());
			 setCurrLevelPref(mCourseInfo.getCurrLevel());
			 mCourseInfo.storeCourseInfo();
			 mHandler.removeCallbacks(mRun);
			 mHandler.removeCallbacks(mRun);
			 Intent intent = new Intent();
			 Bundle extras = new Bundle();
			 extras.putSerializable("courseInfo", mCourseInfo);
			 intent.putExtras(extras);
			 setResult(RESULT_OK, intent);
			 finish();
			 return true;
		}
		return false;
	}

	boolean isLevelUpMoveNeeded() {
		return isLevelUpMoveNeeded(mCourseInfo.getCurrLevel());
	}
	boolean isLevelUpMoveNeeded(int level) {
		
		if (!isAutoLevelMode()) {
			return false;
		}
		if (level == CourseInfo.MAX_LEVEL) {
			return false;
		}
		if (mCourseInfo.getLevelCnt(level) > getMaxSentPref(level)) {
			return false;
		}
		
		if (mCourseInfo.getLevelCnt(level + 1) >= getMaxSentPref(level+1)) {
			return true;
		}
		return false;
	}
	boolean isLevelDownMoveNeeded() 
	{
		return isLevelDownMoveNeeded(mCourseInfo.getCurrLevel());
	}

	boolean isLevelDownMoveNeeded(int level) {
		if (!isAutoLevelMode()) {
			return false;
		}

		if (level <= 1) {
			return false;
		}
		if (mCourseInfo.getLevelCnt(level) <= getMinSentPref(level) && getToLevelCnt(level-1)>0) {
			return true;
		}

		return false;
	}

	int getToLevelCnt(int level) {
		int sum = 0;
		for (int i=1;i<=level;i++) {
			sum += mCourseInfo.getLevelCnt(i);
		}
		return sum;
	}
	public void playLevelSound(int level) {
		mTts.setLanguage(Locale.US);
		mTts.speak("Level " + level, TextToSpeech.QUEUE_FLUSH, null);
	}


}

