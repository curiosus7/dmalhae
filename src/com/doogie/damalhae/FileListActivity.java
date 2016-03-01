package com.doogie.damalhae;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FileListActivity extends ListActivity implements OnClickListener, OnItemClickListener{
	public static final String TAG = "FileListActivity";
	SharedPreferences mPref;
	File [] mFiles;
	List <String> mFileList;
	File mCurrDir;
	String mRootFilePath;
	TextView filePathTextView;
	TextView goParentTextView;
	FileFilter mFilter;
	boolean mIsRecordingPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_list);
		mPref = getPreferences(Activity.MODE_PRIVATE);
		Intent intent = getIntent();
		mIsRecordingPlayer = intent.getBooleanExtra("recordingPlay", false);
		mFileList = new ArrayList<String>();
		mFilter = getExtFilter(intent.getStringArrayExtra("exts"));
		filePathTextView = (TextView)findViewById(R.id.filePathTextView);
		goParentTextView = (TextView)findViewById(R.id.goParentTextView);
		goParentTextView.setOnClickListener(this);
		if (mIsRecordingPlayer) {
			mCurrDir = new File(intent.getStringExtra("folder"));
			mRootFilePath = mCurrDir.getAbsolutePath();
		} else {
			mCurrDir = new File(getDirPref());
			if (!mCurrDir.exists()) {
				mCurrDir = new File(Constant.APP_ROOT_PATH);
			}
			mRootFilePath = "/";
		}
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mFileList));
		getListView().setOnItemClickListener(this);
		update();	
	}

	FileFilter getExtFilter (final String [] exts) {
		return new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory() && !pathname.isHidden()) {
					if (!mIsRecordingPlayer || pathname.listFiles().length>0) {
						return true;
					}
				}
				
				for (int i=0;i<exts.length;i++) {
					if (pathname.getName().toLowerCase().endsWith("." + exts[i].toLowerCase())) {
						return true;
					}
				}
				return false;
			}
		};
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.goParentTextView:
			if (mCurrDir.getAbsolutePath().equals(mRootFilePath)) { 
				
			} else {

				mCurrDir = mCurrDir.getParentFile();
				update();
			}

			break;
		}
	}


	void update() {
		try {
			if (mCurrDir.getAbsolutePath().equals(mRootFilePath)) {
				goParentTextView.setVisibility(View.GONE);
			} else {
				goParentTextView.setVisibility(View.VISIBLE);
			}
			mFiles = mCurrDir.listFiles(mFilter);
			if (mFiles == null) {
				mCurrDir = mCurrDir.getParentFile();
				mFiles = mCurrDir.listFiles(mFilter);
				return;
			}

			Arrays.sort(mFiles, new Comparator() {
				public int compare(Object lhs, Object rhs) {
					return ((File)lhs).getName().compareTo(((File)rhs).getName());			
				}
			});

			mFileList.clear();
			for (int i=0;i<mFiles.length;i++) {
				String fileName = mFiles[i].getName() + ((mFiles[i].isDirectory())?"/":"");
				mFileList.add(fileName);
			}

			((ArrayAdapter)getListAdapter()).notifyDataSetChanged();
			filePathTextView.setText(mCurrDir.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int pos, long arg3) {
		if (mFiles[pos].isDirectory()) {
			mCurrDir = mFiles[pos];
			update();
		} else {
			if (mIsRecordingPlayer) {
				Intent intent = new Intent();
	            Uri data = Uri.parse(mFiles[pos].toString());
	            intent.setAction(Intent.ACTION_VIEW);
	            intent.setDataAndType(data,"video/mp4");
	            try {
	                   startActivity(intent);
	           } catch (ActivityNotFoundException e) {
	                   e.printStackTrace();
	           }
			} else {
				setDirPref(mCurrDir.getAbsolutePath());
				Intent intent = new Intent();
				intent.putExtra("file", mFiles[pos].getAbsolutePath());
				Log.d("TAG","[dOOgie debug] Result : " + intent.getStringExtra("file"));
				setResult(RESULT_OK, intent);
				finish();
			}
		}
	}
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if (mCurrDir.getAbsolutePath().equals(mRootFilePath)){ 
			super.onBackPressed();
		} else {
			mCurrDir = mCurrDir.getParentFile();
			update();
		}
	}
	void setPref(String key, int i) {
		SharedPreferences.Editor e = mPref.edit();
		e.putInt(key, i);
		e.commit();
	}
	
	void setPref(String key, String value) {
		SharedPreferences.Editor e = mPref.edit();
		e.putString(key, value);
		e.commit();
	}
	
	int getPrefInt(String key, int def) {
		return mPref.getInt(key, def);
	}
	
	String getPrefString(String key, String def) {
		return mPref.getString(key, def);
	}
	
	void setDirPref(String value) {
		setPref("dir", value);
	}
	String getDirPref() {
		return getPrefString("dir" , Constant.APP_ROOT_PATH);
	}
}
