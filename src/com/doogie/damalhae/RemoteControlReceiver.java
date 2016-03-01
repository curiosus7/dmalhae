package com.doogie.damalhae;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    	
		String intentAction = intent.getAction();
//		PracticeActivity activity = PracticeActivity.getInstance();
		if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
			return;
		}
		Intent sendIntent = new Intent();

		sendIntent.setAction("com.doogie.damalhae.action.MEDIA_BUTTON");
		sendIntent.putExtra(Intent.EXTRA_KEY_EVENT, (KeyEvent) intent
				.getParcelableExtra(Intent.EXTRA_KEY_EVENT));

		context.sendBroadcast(sendIntent);			


		if(PracticeActivity.getInstance() == null) {
			return;
		}
		/*
		KeyEvent event = (KeyEvent) intent
				.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		int keyCode = event.getKeyCode();
		int action = event.getAction();
		if (event.getAction() != KeyEvent.ACTION_DOWN) {
			return;
		}
		Log.i("keycode", String.valueOf(keyCode));
		Log.i("action", String.valueOf(action));
		//onKeyDown(keyCode, event)
		switch (keyCode) {
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			if (activity.levelDownButton.isEnabled()) {
				activity.playLevelDownSound();
				activity.levelDownButton.performClick();
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			if (activity.levelUpButton.isEnabled()) {
				activity.playLevelUpSound();
				activity.levelUpButton.performClick();
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
		case KeyEvent.KEYCODE_MEDIA_PAUSE:
		case KeyEvent.KEYCODE_MEDIA_PLAY:
			activity.playBeepSound();
			activity.pauseButton.performClick();
			break;
		}

		*/
	}
}
