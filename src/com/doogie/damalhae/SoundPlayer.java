package com.doogie.damalhae;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.media.MediaPlayer;
import android.util.Log;

public class SoundPlayer extends MediaPlayer {
	public void seekTo (int msec)  throws IllegalStateException {
		
		Class c = MediaPlayer.class;
		try {
			Method m = c.getMethod("seekTo", int.class, int.class);
			m.invoke(this, msec, 1);
		} catch (SecurityException e) {
			super.seekTo(msec);
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			super.seekTo(msec);
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			super.seekTo(msec);
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			super.seekTo(msec);
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			super.seekTo(msec);
			e.printStackTrace();
		} 
	}

}
