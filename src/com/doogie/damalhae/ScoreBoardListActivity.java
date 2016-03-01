package com.doogie.damalhae;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Comparator;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

public class ScoreBoardListActivity extends ListActivity {
	Bitmap [] mBitmaps;
	File mFolder;
	private BaseAdapter mAdapter;
	private BaseAdapter mJpgAdapter;
	private ListView mJpgListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.score_list);
		Intent intent = getIntent();
		mJpgListView = (ListView)findViewById(R.id.jpgScoreList);
		mFolder = new File (intent.getStringExtra("folder"));
		mAdapter = new ScoreBoardAdapter(this, mFolder, 9999);
		mJpgAdapter = new ScoreBoardAdapter(this, mFolder, 5);
		setListAdapter(mAdapter);
		mJpgListView.setAdapter(mJpgAdapter);
		mAdapter.notifyDataSetChanged();
		mJpgAdapter.notifyDataSetChanged();
	}
	
	private class ScoreBoardAdapter extends BaseAdapter{
		File [] mFiles;
		Context mContext;
		int mMax;
		
		public ScoreBoardAdapter(Context context, File mCurrDir, int max) {
			mContext = context;
			mMax = max;
			mFiles = mCurrDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					if (pathname.getAbsolutePath().endsWith(".jpg")) {
						pathname.renameTo(new File(pathname.getParentFile(),".score_" + pathname.getName().replace(".jpg", "")));
					}  
					return false;
				}
			});

			mFiles = mCurrDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.getName().startsWith(".score_");
				}
			});
			
			Arrays.sort(mFiles, new Comparator() {
				public int compare(Object lhs, Object rhs) {
					return ((File)rhs).getName().compareTo(((File)lhs).getName());			
				}
			});

			mBitmaps = new Bitmap[mFiles.length-1];
			mBitmaps[0] = BitmapFactory.decodeFile(mFiles[0].getAbsolutePath());

			for (int pos=2;pos<mFiles.length;pos++) {
				mBitmaps[pos-1] = BitmapFactory.decodeFile(mFiles[pos].getAbsolutePath());
			}

		}

		
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mBitmaps.length>mMax?mMax:mBitmaps.length;
		}
		
		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return mBitmaps[position];
		}
		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.score_row, parent, false);
			ImageView imageView = (ImageView) rowView.findViewById(R.id.score_image);
			imageView.setDrawingCacheEnabled(false);
			imageView.setImageBitmap(mBitmaps[position]);
			return rowView;
		}
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		new SaveData().execute();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		for (Bitmap bitmap : mBitmaps) {
			bitmap.recycle();
		}
		mFolder = null;
		mAdapter.notifyDataSetInvalidated();
		super.onDestroy();
	}
	
	void saveScoreboard() {
			View view = mJpgListView;
			view.setDrawingCacheEnabled(true);
			Bitmap b = null;
			while (b == null) {
				b = view.getDrawingCache();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Utils.reverseBitmap(b);
			File file = new File(mFolder.getParentFile(),"scoreboard.jpg");
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
	
	class SaveData extends AsyncTask<Void, Void, Void> {
		ProgressDialog progressDialog;
		//declare other objects as per your need
		@Override
		protected void onPreExecute()
		{
			getListView().setVisibility(View.GONE);
			mJpgListView.setVisibility(View.VISIBLE);
			mJpgAdapter.notifyDataSetChanged();
			mAdapter.notifyDataSetChanged();
			progressDialog= ProgressDialog.show(ScoreBoardListActivity.this, "Saving...","Saving Scoreboard", true);

			//do initialization of required objects objects here                
		};      
		@Override
		protected Void doInBackground(Void... params)
		{
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}       
		@Override
		protected void onPostExecute(Void result)
		{
			super.onPostExecute(result);
			progressDialog.dismiss();
			saveScoreboard();
			finish();
		};
	}

}
