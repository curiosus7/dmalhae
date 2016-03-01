package com.doogie.damalhae;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class PhoneReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
			String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
			if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
				intent=new Intent();
				intent.setAction("com.doogie.damalhae.PHONE_RINGING");
				//				intent.putExtra("url",uri.toString());
				context.sendBroadcast(intent);			
			}
		} else {
			intent=new Intent();
			intent.setAction("com.doogie.damalhae.PHONE_RINGING");
			//				intent.putExtra("url",uri.toString());
			context.sendBroadcast(intent);			
			
		}
	}
}
	