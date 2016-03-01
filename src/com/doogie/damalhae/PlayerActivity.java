/**
 * 	121229	On Level 2, Level Buttons are not enabled.						fixed
 * 	121229	At meaning silence, time goes twice faster than usual.  		fixed
 * 
 */


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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class PlayerActivity extends BaseActivity implements OnClickListener, OnLongClickListener, OnSeekBarChangeListener, OnSeekCompleteListener, SensorEventListener, SurfaceHolder.Callback{

	final static String TAG="PracticeActivity";

	final static int STATE_SOUND_PLAY = 0;
	//	final static int STATE_MEANING_REC = 2;
	final static int STATE_MEANING_PLAY = 3;
	final static int STATE_SOUND_SILENCE = 4;
	final static int STATE_MEANING_SILENCE = 5;
	final static int STATE_BEFORE_REC = 6;
	final static int STATE_AFTER_REC = 7;
	final static int STATE_BEFORE_PLAY = 8;
	final static int STATE_AFTER_PLAY = 9;

	final static int START_MARGIN = 0;

	final static int STATE_CONTINUE = 9;
	final static int STATE_STOP = 10;

	final static int UPDATE_PERIOD = 100;
	final static int SILENCE_BIAS = 500;
	final static int SCALE_MAX = 301;
	int mPeriod = UPDATE_PERIOD;
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
	TextView playAllTextView;
	TextView titleTextView;

	SeekBar playSeekBar;
	SeekBar playAllSeekBar;

	//Button prevButton;
	//Button nextButton;
	Button prevSecButton;
	Button nextSecButton;

//	Button levelUpButton;
//	Button levelDownButton;
	Button levelUpStartButton;
	Button skipButton;

//	Button pauseButton;
	Button stopButton;
	Button removeMeaningButton;

	SharedPreferences mPref;

	
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
	int mCurrCnt;
	int mOldTotalCnt;
	private SurfaceView mVideoView;
	private SurfaceHolder mVideoHolder;

//	private SoundPool mSoundPool;
	//	private boolean mSoundPlayerPrepared;
	Boolean mIsPrepared = false;
	int mDuration = 0;
	Random mRnd = new Random();
	Runnable mRun = new Runnable() {

		public void run() {
			// TODO Auto-generated method stub
			updateUiOnState();
		}
	};
	private int mBeepSound;

	private long mSilenceStart;

	private long mlevelUpStart;

	private boolean mSkip;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);
		mPref = getPreferences(Activity.MODE_PRIVATE);
		initView();
		initData();
		initTts();
	}

	void initData() {
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); 
		accelerormeterSensor = sensorManager 
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

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
				if (mCurrState != STATE_MEANING_SILENCE) {
					goNextState();
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
					mVoicePlayerStart();
				}
				mHandler.post(mRun);
			}
		});
		initSkipButton();
		initlevelSpinner(mCourseInfo.MAX_LEVEL);
		initMuteSpinner(200);
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
		playAllTextView.setVisibility(visible);

		playSeekBar.setVisibility(visible);
		playAllSeekBar.setVisibility(visible);

	}
	void initView() {
		playTextView = (TextView)findViewById(R.id.playTextView);
		playAllTextView = (TextView)findViewById(R.id.playAllTextView);

		titleTextView = (TextView)findViewById(R.id.titleTextView);
//		titleTextView.setSelected(true);
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
		levelUpStartButton = (Button)findViewById(R.id.levelUpStartButton);
		skipButton = (Button)findViewById(R.id.skipButton);


		pauseButton = (Button)findViewById(R.id.pauseButton);
		stopButton = (Button)findViewById(R.id.stopButton);
		removeMeaningButton = (Button)findViewById(R.id.removeMeaningButton);

		playSeekBar = (SeekBar)findViewById(R.id.playSeekBar);
		playAllSeekBar = (SeekBar)findViewById(R.id.playAllSeekBar);
		playSeekBar.setBackgroundColor(Color.DKGRAY);
		pauseButton.setOnClickListener(this);
		stopButton.setOnClickListener(this);
		removeMeaningButton.setOnClickListener(this);
		prevButton.setOnClickListener(this);
		nextButton.setOnClickListener(this);

		prevSecButton.setOnClickListener(this);
		nextSecButton.setOnClickListener(this);

		levelUpButton.setOnClickListener(this);
		levelDownButton.setOnClickListener(this);
		levelUpStartButton.setOnClickListener(this);
		skipButton.setOnClickListener(this);
		setOnClickListener(R.id.stopModeButton);


		levelUpButton.setOnLongClickListener(this);
		levelDownButton.setOnLongClickListener(this);
		removeMeaningButton.setOnLongClickListener(this);

		playSeekBar.setOnSeekBarChangeListener(this);
		playAllSeekBar.setOnSeekBarChangeListener(this);
		visibleView(R.id.expGroupLayout);


	}
	void setOnClickListener(int resId) {
		findViewById(resId).setOnClickListener(this);
	}
	void initUiForState(int state)
	{
		Log.d (TAG, "iniUiForState("+state+") called");
		String filePath = null;
		setDescriptionText();
		playSeekBar.setProgress(0);
		switch (state) {
		case STATE_SOUND_PLAY:
			setLevelButtonEnabled(true);
			setSubtitleTextView(true);
			setMoveButtonEnabled(true);
			int start = (int)mCurrSentInfo.getStart();
			int currPos = mSoundPlayer.getCurrentPosition();
			if  ((currPos < start - 700) || (currPos > start + 700)) {
				SoundPlayerSeekToStart();
			}

			playSeekBar.setProgress(0);
			playSeekBar.setMax(mCurrSentInfo.getDuration() + START_MARGIN);
			setTitleTextView();
			setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
			if (mIsPlaying == true) {
				mSoundPlayerStart();
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
			mCurrCnt = 0;
			mOldTotalCnt = mCourseInfo.getCurrLevelCnt();
			playTextView.setText("");
			playAllTextView.setText("");

			titleTextView.setText(mCourseInfo.getCourseName() + "\n ( " + 0 + " ~ " + mCourseInfo.getDuration() + " )");
			nextButton.setEnabled(false);
			prevButton.setEnabled(false);
			nextSecButton.setEnabled(false);
			prevSecButton.setEnabled(false);
			mCurrSentInfo = mCourseInfo.goLast();
			if (!mIsPlaying)
			{
				pauseButton.setText("▶");
			}
			mExpSpinner.setEnabled(true);
			setLevelButtonEnabled(false);
			if (mCourseInfo.getCurrLevel()==5) {
				setRemoveButtonEnabled(true);
			} else {
				setRemoveButtonEnabled(false);
			}
			goNextSession();
			break;

		case STATE_MEANING_SILENCE:
			setSubtitleTextView(false);
			playSeekBar.setProgress(0);
			playSeekBar.setMax(mCurrSentInfo.getDuration() * getSilenceRatio(mCurrSentInfo.getLevel(), state) / 100 + SILENCE_BIAS);
			resetSilenceStart(); 
			break;

		case STATE_BEFORE_REC:
			filePath = mCourseInfo.getBeforeFilePath();
			if (!prepareMeaning(filePath)) {
				prepareRecorder(mCourseInfo.getBeforeFilePath());
				setSubtitleTextView(false);
				playSeekBar.setProgress(0);
				playSeekBar.setMax(mCurrSentInfo.getDuration() * getSilenceRatio(mCurrSentInfo.getLevel(), state) / 100 + SILENCE_BIAS);
				resetSilenceStart();
				startRecording();
			} else {

			}
			break;

		case STATE_SOUND_SILENCE:
			playSeekBar.setProgress(0);
			playSeekBar.setMax(mCurrSentInfo.getDuration() * getSilenceRatio(mCurrSentInfo.getLevel(), state) / 100 + SILENCE_BIAS);
			resetSilenceStart();

			break;

		case STATE_AFTER_REC:
			filePath = mCourseInfo.getAfterFilePath();
			if (!prepareMeaning(filePath)) {
				prepareRecorder(mCourseInfo.getAfterFilePath());
				playSeekBar.setProgress(0);
				playSeekBar.setMax(mCurrSentInfo.getDuration() * getSilenceRatio(mCurrSentInfo.getLevel(), state) / 100 + SILENCE_BIAS);
				resetSilenceStart();
				startRecording();
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
	protected void onStart() {
		super.onStart();
		if (accelerormeterSensor != null) {
			sensorManager.registerListener(this, accelerormeterSensor, 
					SensorManager.SENSOR_DELAY_GAME); 
		} 

//		mBeepSound = mSoundPool.load(this,R.raw.beep,1);
		aquireWakeLock(false);
	}
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		aquireWakeLock(false);
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem levelUpAll = menu.add("일괄단계올리기");
		levelUpAll.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				mCourseInfo.levelUpAll();
				setLevelListString();
				return true;
			}
		});
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub

		setIsPlaying(false);
		mSoundPlayer.release();
		mIsDestroyed = true;
		super.onDestroy();
	}

	void updateUiOnState() {
		Log.d(TAG, "[updateUiOnState] mCurrState : " + mCurrState + ", mIsPlaying : " + mIsPlaying + "[" + playSeekBar.getProgress() + "/" + playSeekBar.getMax() + "]");
		long value;
		if (mCurrState == STATE_STOP) {
			setIsPlaying(false);
			mSoundPlayerPause();
			mMeaningPlayerPause();
			mVoicePlayerPause();
			initUiForState(mCurrState);
			return;
		}
		if (true) {
			mExpSpinner.setEnabled(false);
			switch (mCurrState) {
			case STATE_SOUND_PLAY:
				int currPosition = mSoundPlayer.getCurrentPosition();
				playSeekBar.setProgress(currPosition - (int)mCurrSentInfo.getStart());
				playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
				setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
				setPlayAllTextView(playAllSeekBar.getProgress(),playAllSeekBar.getMax());

				if (!isSoundOnVaildRange(currPosition)) {
					goNextState();
				}
				mLastPosition = currPosition;
				break;
			case STATE_MEANING_SILENCE:
				value = System.currentTimeMillis() - mSilenceStart;
				if (value > playSeekBar.getMax()) {
					goNextState();
				} else {
					playSeekBar.setProgress((int)value);
					setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
				}
				break;
			case STATE_SOUND_SILENCE:
				value = System.currentTimeMillis() - mSilenceStart;
				if (value > playSeekBar.getMax()) {
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
			if (isNextState()) {
				pauseButton.setText("║");
				goNextSession();
			} else {
				switch (mCurrState) {
				case STATE_SOUND_PLAY:
					if (mIsPlaying) {
						setIsPlaying(false);
						pauseButton.setText("▶");
						mSoundPlayerPause();
					} else {
						pauseButton.setText("║");
						mSoundPlayerStart();
						setIsPlaying(true);
					}
					break;
				case STATE_MEANING_PLAY:
					if (mIsPlaying) {
						setIsPlaying(false);
						pauseButton.setText("▶");
						mMeaningPlayerPause();
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
					} else {
						pauseButton.setText("║");
						setIsPlaying(true);
					}
					break;
				}
			}
			break;
		case R.id.stopButton:
			if (mCurrState != STATE_STOP) {
				mCurrState = STATE_STOP;
				updateUiOnState();
			}			
			break;

		case R.id.prevButton:
			if (isNextState()) {
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
			break;
		case R.id.nextSecButton:
			soundPlayerSeekTo(mSoundPlayer.getCurrentPosition()+500);
			playSeekBar.setProgress((int) (mSoundPlayer.getCurrentPosition()-mCurrSentInfo.getStart()));
			setPlayTextView(playSeekBar.getProgress(), playSeekBar.getMax());
			break;

		case R.id.nextButton:
			if (isNextState()) {
				pauseButton.setText("║");
				goNextSession();
			} else {
				goNextState();
			}
			break;
		case R.id.levelUpButton:
			if (mCurrSentInfo.getLevel() == mCourseInfo.MAX_LEVEL) {
				mCourseInfo.levelSet(mCurrSentInfo, 0);
			} else {
				mCourseInfo.levelUp(mCurrSentInfo);
			}
			setLevelListString();
			setLevelButtonEnabled(false);
			if (isNextState()) {
				pauseButton.setText("║");
				goNextSession();
			}
			break;
		case R.id.levelDownButton:
			if (mCurrSentInfo.getLevel() == 1) {
				mCourseInfo.levelSet(mCurrSentInfo, 9);
			} else {
				mCourseInfo.levelDown(mCurrSentInfo);
			}
			setLevelListString();
			setLevelButtonEnabled(false);
			
			if (isNextState()) {
				pauseButton.setText("║");
				goNextSession();
			}
			
			break;

		case R.id.levelUpStartButton:
			if (levelUpStartButton.getText().equals("구간단계올리기시작")) {
				mlevelUpStart = mCurrSentInfo.getStart();
				levelUpStartButton.setText("구간단계올리기끝");
			} else {
				mCourseInfo.levelUpFromTo(mlevelUpStart, mCurrSentInfo.getStart(), 0);
				levelUpStartButton.setText("구간단계올리기시작");
				setLevelListString();
			}

			break;

		case R.id.skipButton:
			if (skipButton.getText().equals("무자막지나가기")) {
				mSkip = true;
				skipButton.setText("무자막재생하기");
			} else {
				mSkip = false;
				skipButton.setText("무자막지나가기");
			}

			break;

		case R.id.removeMeaningButton:
			if (mCurrState == STATE_BEFORE_REC || mCurrState == STATE_AFTER_REC) {
				if (mIsRecording) {
					stopRecording();
				} else {
					mVoicePlayerStop();
				}
				stopRecording();
				mVoicePlayerPause();
			}
			setCurrState(STATE_MEANING_PLAY);
			removeMeaningFile(mCurrSentInfo);
			break;
		case R.id.stopModeButton:
			setStopMode(!isStopMode());
		}

	}

	private void setStopMode(boolean b) {
		String str = b?"멈춤없음":"자동멈춤";
		((Button)findViewById(R.id.stopModeButton)).setText(str);
	}

	private boolean isStopMode() {
		return ((Button)findViewById(R.id.stopModeButton)).getText().equals("멈춤없음");

	}

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
				if (isNextState()) {
					pauseButton.setText("║");
					mSoundPlayerStart();
					setIsPlaying(true);
				}

				return;
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

//	void playBeep(){
//		float vol = 10/50.000f;
//		mSoundPool.play(mBeepSound, vol, vol, 0, 0, (float)1.0);
//	}

	void mSoundPlayerPause() {
		if (mSoundPlayer.isPlaying()) {
			mSoundPlayer.pause();
		}
	}
	void mSoundPlayerStart() {
		if (!mSoundPlayer.isPlaying())
			mSoundPlayer.start();
	}

	void mMeaningPlayerPause() {
		mTts.stop();
	}
	void mVoicePlayerStart() {
		mMeaningPlayer.start();
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
		playSeekBar.setBackgroundColor(Color.DKGRAY);
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
		if (mSkip == true && (((!info.hasMeaningFile() && !info.isTtsable() )) || info.getType() == SentenceInfo.TYPE_INVALID)) {
			return false;
		}
		if (mCourseInfo.getCurrLevel() == CourseInfo.MAX_LEVEL+1) {
			return true;
		}
		if (info.getMeaningType() != SentenceInfo.MEANING_TYPE_FILE || (mSkip == true && (!info.hasMeaningFile() && !info.isTtsable() ))){
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
				// TODO Auto-generated method stub
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}


		});
	}

	void initSkipButton() {
		if (mCourseInfo.getLevelCnt(CourseInfo.MAX_LEVEL+1) == 0) {
			skipButton.setEnabled(false);
			mCourseInfo.setCurrLevel(CourseInfo.MAX_LEVEL + 1);
			setCurrLevelPref(CourseInfo.MAX_LEVEL+1);
		}
	}
	void initlevelSpinner (int max) {
		mExpList = new ArrayList<String>();
		mExpSpinner = (Spinner)findViewById(R.id.expSpinner);
		for (int i=0;i<=max;i++) {
			mExpList.add(i,new String(i + " 단계 "));
		}
		mExpList.add(max+1,new String("전체 단계"));
		mExpAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mExpList);
		mExpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);   
		mExpSpinner.setAdapter(mExpAdapter); // NullPointerException!!
		int cnt = 0;
		while (mCourseInfo.getLevelCnt(mCourseInfo.getCurrLevel()) == 0 && mCourseInfo.getCurrLevel()<=CourseInfo.MAX_LEVEL) {
			mCourseInfo.setCurrLevel((mCourseInfo.getCurrLevel() + 1) % (CourseInfo.MAX_LEVEL + 1));
			cnt++;
			if (cnt>10) {
				mCourseInfo.setCurrLevel(CourseInfo.MAX_LEVEL + 1);
			}
		}
		mExpSpinner.setSelection(getCurrLevelPref());
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

	//	121229	On level 2, level buttons are not enabled, fixed
	//		
	void goNextState() {
		Log.d(TAG, "goNextState called");
		if (isStopMode()) {
			setNextState();
			mSoundPlayer.seekTo(mSoundPlayer.getCurrentPosition()+mOffset);
			mSoundPlayerPause();
		} else {
			goNextSession();
		}
	}

	void goPrevState() {
		if (mSoundPlayer.getCurrentPosition() - mCourseInfo.getCurrStart() > 1000) {
			setCurrState(STATE_SOUND_PLAY);
		} else {
			//soundPlayerPause();
			goPrevSession();
		}
	}


	void goNextSession() {
		int level = mCourseInfo.getCurrLevel();
		//		if (level != 0 && mSoundPlayer.isPlaying()) {
		//			mSoundPlayerPause();
		//		}
		SentenceInfo oldinfo = mCurrSentInfo;

		mCurrSentInfo = goNextPlaySound();
		if (oldinfo.equals( mCurrSentInfo) && mCourseInfo.getLevelCnt(mCourseInfo.getCurrLevel())!=1) {
			mExpSpinner.setSelection((mCourseInfo.getCurrLevel()+1)%(CourseInfo.MAX_LEVEL+1));
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

	}

	void goPositionSession(SentenceInfo info) {
		mCurrSentInfo = mCourseInfo.setCurrent(info);
		if (isPlaySound (mCurrSentInfo)){
			setCurrState(STATE_SOUND_PLAY);
		} else {
			goPrevSession();
		}
	}




	void goPrevSession() {
		if (mSoundPlayer.isPlaying()) {
			mSoundPlayerPause();
		}
		mCurrSentInfo = goPrevPlaySound();
		if (mCurrCnt==1) {
			mCurrCnt = mCourseInfo.getCurrLevelCnt();
		}
		else {
			mCurrCnt--;
		}

		setCurrState(STATE_SOUND_PLAY);
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
		int level = mCurrSentInfo.getLevel();
		if (level == 0) {
			//levelUpStartButton.setVisibility(View.VISIBLE);
			//levelDownButton.setVisibility(View.GONE);
		} else {
			levelUpStartButton.setVisibility(View.GONE);
			levelDownButton.setVisibility(View.VISIBLE);
			levelUpStartButton.setText("구간단계올리기시작");
		}

		levelUpButton.setEnabled(enable);
		levelDownButton.setEnabled(enable);
		levelUpStartButton.setEnabled(enable);
		if (mCourseInfo.getCurrLevel()==5) {
			setRemoveButtonEnabled(enable);
		}
		if (mCurrSentInfo.getLevel()==0) {
			levelDownButton.setEnabled(false);

		}
		if (mCurrSentInfo.getLevel()==mCourseInfo.MAX_LEVEL) {
			//levelUpButton.setEnabled(false);
		}

	}

	void setRemoveButtonEnabled(boolean enable) {
		removeMeaningButton.setEnabled(enable);
	}


	public void onBackPressed() {
		setCurrLevelPref(mCourseInfo.getCurrLevel());
		mCourseInfo.storeCourseInfo();
		mHandler.removeCallbacks(mRun);
		mHandler.removeCallbacks(mRun);
		setIsPlaying(false);
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

	
	void removeMeaningFile (SentenceInfo info) {
		//		Utils.removeFile(mCourseInfo.getCourseName(), mCourseInfo.getMeaningFilePath(info));
		if (info == null) {
			return;
		}
		mCourseInfo.removeAfterFile(info.getStart());
		mCourseInfo.removeBeforeFile(info.getStart());
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
					if (mShakeCount==3) {
						mVib.vibrate(500);
						levelUpButton.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 5.0f, 5.0f, 0)); 
						levelUpButton.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 5.0f, 5.0f, 0));
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
			mCourseInfo.levelDownAll();
			setLevelListString();
			showDialog(v.getId());
			break;
		case R.id.levelUpButton:
			mCourseInfo.levelUpAll();
			setLevelListString();
			showDialog(v.getId());
			break;
		case R.id.removeMeaningButton:
			mHandler.removeCallbacks(mRun);
			mHandler.removeCallbacks(mRun);
			mHandler.removeCallbacks(mRun);
			mHandler.removeCallbacks(mRun);

			if (mCurrState == STATE_BEFORE_REC || mCurrState == STATE_AFTER_REC) {
				if (mIsRecording) {
					stopRecording();
				} else {
					mVoicePlayerStop();
				}
				goPrevState();
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
		}
	}

	
	void setOrderTextView(int order, int total) {
		setText(R.id.orderTextView, Utils.getProgeressText(order, total));
	}
	private void setPlayTextView(int progress, int max) {
		playTextView.setText(Utils.getProgeressTimeText(progress, max));
	}

	private void setPlayAllTextView(int progress, int max) {
		playAllTextView.setText(Utils.getProgeressTimeText(progress, max));
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
			setLevelOrderTextView(mCurrCnt, mCourseInfo.getCurrLevelCnt());
		}
	}

	int getSilenceRatio (int level, int state) {
		int ratio = 0;
		switch (level) {
		case 1:
			if (state == STATE_MEANING_SILENCE) {
				ratio =  100;
			} else {
				ratio =  125;
			}
			break;
		case 2:
			if (state == STATE_MEANING_SILENCE) {
				ratio =  100;
			} else if (state == STATE_SOUND_SILENCE) {
				ratio =  125;
			}
			break;
		case 3:
			if (state == STATE_MEANING_SILENCE) {
				ratio =  100;
			} else {
				ratio =  100;
			}
			break;
		case 4:
			if (state == STATE_MEANING_SILENCE) {
				ratio =  100;
			} else {
				ratio =  125;
			}
			break;
		case 5:
			if (state == STATE_MEANING_SILENCE) {
				ratio =  100;
			} else {
				ratio =  100;
			}
			break;

		default: 
			ratio =  100;
		}
		return ratio * muteSpinner.getSelectedItemPosition() * 25 / 100;

	}

	String getPlayText (int level, int state) {
		if (mCourseInfo.getCurrLevel()==CourseInfo.MAX_LEVEL+1) {
			return ("level : " + level);
		}
		switch (level) {
		case 1:
			switch (state) {
			case STATE_SOUND_PLAY:
				return "원어를 듣고 알아들었다면 2단계로 고고싱~";
			case STATE_SOUND_SILENCE:
				return "무음시간은 %를 변경해서 원하는대로 조정하세요.";
			case STATE_MEANING_PLAY:
				return "해석을 듣고 익히고";
			case STATE_MEANING_SILENCE:
				return "흔들어서 단계올리기가 되요";
			}
		case 2:
			switch (state) {
			case STATE_SOUND_PLAY:
				return "원어민의 소리를 주의깊게 들어보세요~";
			case STATE_SOUND_SILENCE:
				return "최대한 성대모사 하며 따라하세요~";
			case STATE_MEANING_PLAY:
				return "해석을 듣고 원어를 대충 말할 수 있다면 3단계로 고고싱";
			case STATE_MEANING_SILENCE:
				return "원어를 말해보세요~";
			}
		case 3:
			switch (state) {
			case STATE_SOUND_PLAY:
				return "원어를 잘 따라할 수 있으면 4단계로 고고싱~";
			case STATE_SOUND_SILENCE:
				return "원어를 따라한 후 해석을 말해보세요~";
			case STATE_MEANING_PLAY:
				return "해석을 듣고";
			case STATE_MEANING_SILENCE:
				return "원어를 말해보세요~";
			}
		case 4:
			switch (state) {
			case STATE_SOUND_PLAY:
				return "롱클릭을 하면 단계가 일괄 이동합니다.";
			case STATE_SOUND_SILENCE:
				return "원어를 성대모사해보세요~";
			case STATE_MEANING_PLAY:
				return "해석을 듣고 원어를 잘 말할 수 있으면 5단계로 고고싱~";
			case STATE_MEANING_SILENCE:
				return "원어를 말해보세요";
			}
		case 5:
			switch (state) {
			case STATE_SOUND_PLAY:
				return "지우기를 누르면 녹음이 지워져요";
			case STATE_SOUND_SILENCE:
				return "녹음 후 다음에는 녹음이 재생되요";
			case STATE_MEANING_PLAY:
				return "해석을 들으면서 동시에 따라하고";
			case STATE_MEANING_SILENCE:
				return "원어를 말해보세요~ 녹음됩니다.";
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

	private void initTts() {
		mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				mTts.setLanguage(Utils.getLocale(mCourseInfo.getLanguage()));
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
			.setTitle("단계가 일괄적으로 내려갔습니다.")
			.setPositiveButton("확인", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			})
			.create();
		case R.id.levelUpButton:
			return new AlertDialog.Builder(this)
			.setTitle("단계가 일괄적으로 올라갔습니다.")
			.setPositiveButton("확인", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			})
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
				mSoundPlayerPause();
			} else {
				setCurrState(STATE_SOUND_PLAY);
				//				if (mIsPlaying) {
				//					mSoundPlayerStart();
				//				}
			}
			break;
		case STATE_MEANING_PLAY:
			if (on) {
				mMeaningPlayerPause();
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
	/*
	void SoundPlayerSeekToStart() {
		int pos = (int)mCurrSentInfo.getStart() + mOffset;
		SoundPlayerSeekTo(pos<0?0:pos);
	}
	*/

	void soundPlayerSeekTo(int pos) {
		mLastPosition = (long)pos;
		mSoundPlayer.seekTo(pos);
		playAllSeekBar.setMax(mSoundPlayer.getDuration());
		playAllSeekBar.setProgress(mSoundPlayer.getCurrentPosition());
		setPlayAllTextView(playAllSeekBar.getProgress(), playAllSeekBar.getMax());
		return;
	}
	boolean isNextState() {
		return (pauseButton.getText().equals(" > "));
	}
	void setNextState() {
		pauseButton.setText(" > ");
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

	
	void setCurrLevelPref (int level) {
		setPrefInt("curr_level", level);
	}

	int getCurrLevelPref() {
		return getPrefInt("curr_level", 1);
	}

}
