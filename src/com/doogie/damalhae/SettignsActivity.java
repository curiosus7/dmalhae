package com.doogie.damalhae;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

public class SettignsActivity extends Activity {
	SharedPreferences mSettingsPref;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.level_move);
		mSettingsPref = getSharedPreferences("settings", MODE_PRIVATE);
		
	}
	
	@Override
	protected void onResume() {
		initEditTexts();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		setSettingsPrefs();
		
	}
	
	int getLevelMaxEditTextId(int level) {
		switch (level) {
			case 1:
				return R.id.editTextLevel1Min;
			case 2:
				return R.id.editTextLevel2Max;
			case 3:
				return R.id.editTextLevel3Max;
			case 4:
				return R.id.editTextLevel4Max;
			case 5:
				return R.id.editTextLevel5Max;
			case 6:
				return R.id.editTextLevel6Max;
			case 7:
				return R.id.editTextLevel7Max;
			case 8:
				return R.id.editTextLevel8Max;
			case 9:
				return R.id.editTextLevel9Max;
		}
		return 0;
	}
	
	int getLevelMinEditTextId(int level) {
		switch (level) {
			case 1:
				return R.id.editTextLevel1Min;
			case 2:
				return R.id.editTextLevel2Min;
			case 3:
				return R.id.editTextLevel3Min;
			case 4:
				return R.id.editTextLevel4Min;
			case 5:
				return R.id.editTextLevel5Min;
			case 6:
				return R.id.editTextLevel6Min;
			case 7:
				return R.id.editTextLevel7Min;
			case 8:
				return R.id.editTextLevel8Min;
			case 9:
				return R.id.editTextLevel9Min;
		}
		return 0;
	}
	
	void initEditTexts() {
		for (int level=1; level<=9;level++) {
			getLevelMaxEditText(level).setText(""+getLevelMaxSettingsPref(level));
			getLevelMinEditText(level).setText(""+getLevelMinSettingsPref(level));
		}
		
	}

	void setSettingsPrefs() {
		for (int level=1; level<=9;level++) {
			setLevelMaxSettingsPref(level, getLevelMaxEditTextValue(level));
			setLevelMinSettingsPref(level, getLevelMinEditTextValue(level));
		}
	}
	EditText getLevelMaxEditText(int level) {
		return (EditText)findViewById(getLevelMaxEditTextId(level));
	}

	EditText getLevelMinEditText(int level) {
		return (EditText)findViewById(getLevelMinEditTextId(level));
	}
	
	int getLevelMaxEditTextValue(int level) {
		return Integer.valueOf(getLevelMaxEditText(level).getText().toString());
	}

	int getLevelMinEditTextValue(int level) {
		return Integer.valueOf(getLevelMinEditText(level).getText().toString());
	}

	
	
	int getLevelMaxSettingsPref (int level) {
		return getSettingsPrefInt("max_sent_"+level, 1000);
	}
	
	int getLevelMinSettingsPref (int level) {
		return getSettingsPrefInt("min_sent_"+level, 0);
	}

	void setLevelMaxSettingsPref (int level, int value) {
		setSettingsPrefInt("max_sent_"+level, value);
	}
	
	void setLevelMinSettingsPref (int level, int value) {
		setSettingsPrefInt("min_sent_"+level, value);
	}

	
	int getSettingsPrefInt(String key, int def) {
		return mSettingsPref.getInt(key, def);
	}

	void setSettingsPrefInt(String key, int i) {
		SharedPreferences.Editor e = mSettingsPref.edit();
		e.putInt(key, i);
		e.commit();
	}

}

	