/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fruit.launcher.wallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;

import java.io.IOException;
import java.util.ArrayList;

import com.fruit.launcher.wallpaper.Configurator;
import com.fruit.launcher.wallpaper.R;


public class WallpaperChooser extends Activity implements
		AdapterView.OnItemSelectedListener, OnClickListener {

	private static final int MAX_WALLPAPER_COUNT = 36;

	private static final String TAG = "Launcher.WallpaperChooser";

	private Gallery mGallery;
	private ImageView mImageView;
	private boolean mIsWallpaperSet;

	private Bitmap mBitmap;

	private ArrayList<Integer> mThumbs;
	private ArrayList<Integer> mImages;
	private WallpaperLoader mLoader;
	private Resources mResources = null;
	
	private Button btn_set = null;
	private Handler handler = null; 

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		findWallpapers();

		setContentView(R.layout.wallpaper_chooser);

		mGallery = (Gallery) findViewById(R.id.gallery);
		mGallery.setAdapter(new ImageAdapter(this));
		mGallery.setOnItemSelectedListener(this);
		mGallery.setCallbackDuringFling(false);

		btn_set = (Button)findViewById(R.id.set);
		btn_set.setOnClickListener(this);		

		mImageView = (ImageView) findViewById(R.id.wallpaper);
		
		handler = new Handler();
	}

	private void findWallpapers() {
		mThumbs = new ArrayList<Integer>(MAX_WALLPAPER_COUNT);
		mImages = new ArrayList<Integer>(MAX_WALLPAPER_COUNT);

		// Context.getPackageName() may return the "original" package name,
		// com.fruit.launcher; Resources needs the real package name,
		// com.fruit.launcher. So we ask Resources for what it thinks the
		// package name should be.
		mResources = addConfigWallpapers(Configurator.CONFIG_ARRAY_WALLPAPER);
		if (mResources == null) {
			mResources = addWallpapers(Configurator.CONFIG_ARRAY_WALLPAPER);
		}
	}

	private Resources addConfigWallpapers(String arrayName) {
		final String packageName = Configurator.getConfigPackageName();
		final Resources resources = Configurator.getConfigResources(this);
		final String[] extras = Configurator.getConfigPackageArray(resources,
				arrayName);
		if (extras != null) {
			for (String extra : extras) {
				int res = resources.getIdentifier(extra, "drawable",
						packageName);
				if (res != 0) {
					final int thumbRes = resources.getIdentifier(extra
							+ "_small", "drawable", packageName);

					if (thumbRes != 0) {
						mThumbs.add(thumbRes);
						mImages.add(res);
					}
				}
			}
			return resources;
		}
		return null;
	}

	private Resources addWallpapers(String arrayName) {
		final String packageName = getPackageName();
		final Resources resources = getResources();
		final int listId = resources.getIdentifier(arrayName, "array",
				packageName);
		final String[] extras = resources.getStringArray(listId);

		for (String extra : extras) {
			int res = resources.getIdentifier(extra, "drawable", packageName);
			if (res != 0) {
				final int thumbRes = resources.getIdentifier(extra + "_small",
						"drawable", packageName);

				if (thumbRes != 0) {
					mThumbs.add(thumbRes);
					mImages.add(res);
				}
			}
		}
		return resources;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mIsWallpaperSet = false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mLoader != null
				&& mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
			mLoader.cancel(true);
			mLoader = null;
		}
	}

	@Override
	public void onItemSelected(AdapterView parent, View v, int position, long id) {
		if (mLoader != null
				&& mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
			mLoader.cancel();
		}
		mLoader = (WallpaperLoader) new WallpaperLoader().execute(position);
	}

	/*
	 * When using touch if you tap an image it triggers both the onItemClick and
	 * the onTouchEvent causing the wallpaper to be set twice. Ensure we only
	 * set the wallpaper once.
	 */
	private void selectWallpaper(int position) {
		if (mIsWallpaperSet) {
			return;
		}

		mIsWallpaperSet = true;
		try {
			WallpaperManager wpm = (WallpaperManager) getSystemService(WALLPAPER_SERVICE);
			// wpm.setResource(mImages.get(position));
			Bitmap bmp = loadWallpaperBitmap(mImages.get(position));
			if (bmp != null) {
				wpm.setBitmap(bmp);
				setResult(RESULT_OK);
			} else {
				Log.e(TAG, "selectWallpaper error! id=" + mImages.get(position));
				setResult(RESULT_CANCELED);
			}
			finish();
		} catch (IOException e) {
			Log.e(TAG, "Failed to set wallpaper: " + e);
		}
	}

	private Bitmap loadWallpaperBitmap(int resId) {
		final Resources res = mResources;
		Bitmap bitmap = null;

		if (resId > 0) {
			BitmapFactory.Options option = new BitmapFactory.Options();
			option.inDither = false;
			option.inPreferredConfig = Bitmap.Config.ARGB_8888;
			//option.inSampleSize = 2;
			try {
				bitmap = BitmapFactory.decodeResource(res, resId, option);
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
				bitmap = null;
			}
		}
		return bitmap;
	}

	@Override
	public void onNothingSelected(AdapterView parent) {

	}

	private class ImageAdapter extends BaseAdapter {

		private LayoutInflater mLayoutInflater;

		ImageAdapter(WallpaperChooser context) {
			mLayoutInflater = context.getLayoutInflater();
		}

		@Override
		public int getCount() {
			return mThumbs.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView image;

			if (convertView == null) {
				image = (ImageView) mLayoutInflater.inflate(
						R.layout.wallpaper_item, parent, false);
			} else {
				image = (ImageView) convertView;
			}

			int thumbRes = mThumbs.get(position);
			Drawable thumbDrawable = mResources.getDrawable(thumbRes);
			if (thumbDrawable != null) {
				thumbDrawable.setDither(true);
				image.setImageDrawable(thumbDrawable);
			} else {
				Log.e(TAG, "Error decoding thumbnail resId=" + thumbRes
						+ " for wallpaper #" + position);
			}
			return image;
		}
	}
	
    Runnable runnableUi = new Runnable(){  
    	
        @Override  
        public void run() {  
        	//update UI
            btn_set.setText(R.string.finish_setting);  
            btn_set.setEnabled(true);
        }  
          
    };  
    
	private void handleClick(){
		
        new Thread(){  
        	
        	@Override
            public void run(){  
        		selectWallpaper(mGallery.getSelectedItemPosition());        		
                handler.post(runnableUi);   
            } 
        	
        }.start();          
		
	}

	@Override
	public void onClick(View v) {
		Button b = (Button)v;	
		//b.setFocusable(true);
		b.setEnabled(false);
		b.setText(getText(R.string.is_setting));
		
		handleClick();
	}

	class WallpaperLoader extends AsyncTask<Integer, Void, Bitmap> {
		BitmapFactory.Options mOptions;

		WallpaperLoader() {
			mOptions = new BitmapFactory.Options();
			mOptions.inDither = false;
			mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
			mOptions.inSampleSize = 2;
		}

		@Override
		protected Bitmap doInBackground(Integer... params) {
			if (isCancelled()) {
				return null;
			}
			try {
				return BitmapFactory.decodeResource(mResources,
						mImages.get(params[0]), mOptions);
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(Bitmap b) {
			if (b == null) {
				return;
			}

			if (!isCancelled() && !mOptions.mCancel) {
				// Help the GC
				if (mBitmap != null) {
					mBitmap.recycle();
				}

				final ImageView view = mImageView;
				view.setImageBitmap(b);

				mBitmap = b;

				final Drawable drawable = view.getDrawable();
				drawable.setFilterBitmap(true);
				drawable.setDither(true);

				view.postInvalidate();

				mLoader = null;
			} else {
				b.recycle();
			}
		}

		void cancel() {
			mOptions.requestCancelDecode();
			super.cancel(true);
		}
	}
}