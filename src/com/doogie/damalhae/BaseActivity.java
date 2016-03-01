package com.doogie.damalhae;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;


public class BaseActivity extends Activity {
	CourseInfo mCourseInfo;
	SentenceInfo mCurrSentInfo;
	
	
	Button pauseButton;
	Button prevButton;
	Button nextButton;
	Button levelUpButton;
	Button levelDownButton;

	BroadcastReceiver mPhoneReceiver;

	SoundPlayer mSoundPlayer;
	private SoundPool mSoundPool;
	private int mBeepSound;
	private int mLevelUpSound;
	private int mLevelDownSound;
	int mOffset;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	    getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
	    getActionBar().hide();
		IntentFilter filter = new IntentFilter("com.doogie.damalhae.PHONE_RINGING");
		filter.addAction("com.doogie.damalhae.action.MEDIA_BUTTON");
		mPhoneReceiver = new Receiver();
		registerReceiver(mPhoneReceiver, filter);

		mSoundPool = new SoundPool(4,AudioManager.STREAM_MUSIC, 100);

	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("시작오프셋").setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				selectGroupDialog();
				return false;
			}
		});
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		mBeepSound = mSoundPool.load(this,R.raw.beep,1);
		mLevelUpSound = mSoundPool.load(this, R.raw.level_up,1);
		mLevelDownSound = mSoundPool.load(this,  R.raw.level_down,1);

	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		unregisterReceiver(mPhoneReceiver);
		super.onDestroy();

	}

	public void playBeepSound(){
		float vol = 10/50.000f;
		mSoundPool.play(mBeepSound, vol, vol, 0, 0, (float)1.0);
	}

	public void playLevelUpSound() {
		float vol = 50/50.000f;
		mSoundPool.play(mLevelUpSound, vol, vol, 0, 0, (float)1.0);
	}

	public void playLevelDownSound() {
		float vol = 50/50.000f;
		mSoundPool.play(mLevelDownSound, vol, vol, 0, 0, (float)1.0);
	}
	
	public void playLevelSound() {
		
	}


	private class Receiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			boolean isPractice = BaseActivity.this instanceof PracticeActivity;
			if ("com.doogie.damalhae.PHONE_RINGING".equals(intent.getAction())) {
				if("║".equals(pauseButton.getText())) {
					pauseButton.performClick();
				}
			} else {
				KeyEvent event = (KeyEvent) intent
						.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
				int keyCode = event.getKeyCode();
				int action = event.getAction();
				if (event.getAction() != KeyEvent.ACTION_DOWN) {
					return;
				}
				Log.i("keycode", String.valueOf(keyCode));
				Log.i("action", String.valueOf(action));
				switch (keyCode) {
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					if (isPractice) {
						if (levelDownButton != null && levelDownButton.isEnabled()) {
							playLevelDownSound();
							levelDownButton.performClick();
						}
					} else {
						if (nextButton != null && nextButton.isEnabled()) {
							playBeepSound();
							nextButton.performClick();
						}
					}
					break;
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					if (isPractice) {
						if (levelUpButton.isEnabled()) {
							playLevelUpSound();
							levelUpButton.performClick();
						}
					} else {

						if (prevButton.isEnabled()) {
							playBeepSound();
							prevButton.performClick();
						}
					}
					break;
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				case KeyEvent.KEYCODE_MEDIA_PAUSE:
				case KeyEvent.KEYCODE_MEDIA_PLAY:
					playBeepSound();
					pauseButton.performClick();
					break;
				}



			}
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
		if (totalSentence != 0) {
			int percent = totalLevel * 100/(totalSentence * (levelMax+1));
			int prepercent = totalLevel * 1000/(totalSentence * (levelMax+1)) - percent * 10;
			setText(R.id.levelListView , "진도점수 : " + totalLevel + "/" + totalSentence * (levelMax+1) + " (" + percent + "." + prepercent + "%)");
		} else {
			setText(R.id.levelListView ,"진도점수 : 0/0 (0%)");
		}
		if (Utils.getDateTimeStr().contains("23:5")) {
			saveScoreboard();
		}
		return;
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

	void setText(int resId, String text) {
		((TextView)findViewById(resId)).setText(text);
		
	}
	
	void setOnClickListener(int resId, OnClickListener listener ) {
		((View)findViewById(resId)).setOnClickListener(listener);
	}
	
	void setEnabled(int resId, boolean enabled) {
		findViewById(resId).setEnabled(enabled);
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveScoreboard();
	}
	void saveScoreboard() {
		if (mCourseInfo != null) {
			View view = findViewById(R.id.scorerow);
			if (view.getVisibility() != View.VISIBLE) {
				return;
			}
			view.setDrawingCacheEnabled(true);
			Bitmap b = view.getDrawingCache();
			if (b == null) {
				return;
			}
			//Utils.reverseBitmap(b);
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
			
			view = findViewById(R.id.scoreboard);
			if (view.getVisibility() != View.VISIBLE) {
				return;
			}
			view.setDrawingCacheEnabled(true);
			b = view.getDrawingCache();
			if (b == null) {
				return;
			}
			//Utils.reverseBitmap(b);
			file = mCourseInfo.getTodayScoreboardFile();
			isExist = file.exists(); 

			try {
				fos = new FileOutputStream(file);
				b.compress(CompressFormat.JPEG, 95,fos);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			/*
			if (!isExist) {
					Intent intent =
							new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
					intent.setData(Uri.fromFile(file));
					sendBroadcast(intent);
			}
			*/
		}
		
	}

	private void selectGroupDialog() {
		final String [] groupNameArray = {"0ms", "-50ms", "-100ms", "-150ms", "-200ms", "-250ms"
				, "-300ms", "-350ms", "-400ms", "-450ms", "-500ms"};
		Builder builder = new AlertDialog.Builder(this);  
			builder.setTitle("시작 오프셋 선택");
		builder.setSingleChoiceItems(groupNameArray, mOffset/-50,  
				new DialogInterface.OnClickListener() {  
			@Override  
			public void onClick(DialogInterface dialog, int which) {
				mOffset = which * -50;
				dialog.dismiss();
			}  
		});
		builder.setNegativeButton("cancel",  
				new DialogInterface.OnClickListener() {  
			@Override  
			public void onClick(DialogInterface dialog, int which) {  
				dialog.dismiss();  
			}  
		});  
		AlertDialog alert = builder.create();  
		alert.show();  
	}
	
	void SoundPlayerSeekToStart() {
		int pos = (int)mCurrSentInfo.getStart() + mOffset;
		soundPlayerSeekTo(pos<0?0:pos);
	}
	
	void soundPlayerSeekTo(int i){}
	

	
}


