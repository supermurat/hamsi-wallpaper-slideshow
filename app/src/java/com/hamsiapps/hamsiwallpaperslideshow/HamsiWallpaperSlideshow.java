/*
 *    Copyright (C) 2016 Murat Demir <mopened@gmail.com>
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.hamsiapps.hamsiwallpaperslideshow;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.File;
import java.io.FileFilter;

public class HamsiWallpaperSlideshow extends WallpaperService {

    private static final String TAG = "HamsiWallpaperSlideshow";
    public static final String SHARED_PREFS_NAME = "preferences";
    public static final FileFilter ImageFilter = new FileFilter() {
        public boolean accept(File dir) {
            if (dir.isDirectory()) {
                return false;
            }

            String ext = BitmapUtil.getExtension(dir.getName());
            if (ext != null) {
                return ext.equals("jpg") || ext.equals("jpeg")
                        || ext.equals("png") || ext.equals("gif");
            }
            return false;
        }
    };
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new WallpaperEngine();
    }

    class WallpaperEngine extends Engine implements OnSharedPreferenceChangeListener {

        // Canvas stuff
        private final Paint mPaint = new Paint();
        private final Matrix mScaler = new Matrix();
        // Double tap listener
        private final GestureDetector doubleTapDetector;
        private final Runnable mWorker = new Runnable() {
            public void run() {
                if (mDuration > 0)
                    drawFrame();
            }
        };
        private int mWidth = 0;
        private int mHeight = 0;
        private int mMinWidth = 0;
        private int mMinHeight = 0;
        private float mXOffset = 0;
        private float mYOffset = 0;
        private boolean mVisible = false;
        private String mBitmapPath = null;
        private int mIndex = -1;
        private long mLastDrawTime = 0;
        private boolean mStorageReady = true;
        private BroadcastReceiver mReceiver;
        // Preferences
        private SharedPreferences mPrefs = null;
        private String mFolder = null;
        private int mDuration = 0;
        private boolean mRandom = false;
        private boolean mRotate = false;
        private boolean mFitInScreen = false;
        private boolean mScroll = false;
        private boolean mRecurse = false;
        private boolean mTouchEvents = false;
        private boolean mScreenWake = false;

        WallpaperEngine() {
            try {
                final Paint paint = mPaint;
                paint.setColor(Color.WHITE);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setAntiAlias(true);
                paint.setTextSize(18f);

                mPrefs = getSharedPreferences(SHARED_PREFS_NAME, 0);
                mPrefs.registerOnSharedPreferenceChangeListener(this);

                // Read the preferences
                onSharedPreferenceChanged(mPrefs, null);
                if (Build.VERSION.SDK_INT >= 15 ) {
                    setOffsetNotificationsEnabled(true);
                }
                setTouchEventsEnabled(true);
            } catch (Exception ex) {
                Log.e("HamsiWallpaperSlideshow", "WallpaperEngine: ", ex);
            }

            doubleTapDetector = new GestureDetector(HamsiWallpaperSlideshow.this, new SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    try {
                        if (mTouchEvents) {
                            mLastDrawTime = 0;
                            drawFrame(false, true);
                            return true;
                        }
                    } catch (Exception ex) {
                        Log.e("HamsiWallpaperSlideshow", "doubleTapDetector:onDoubleTap: ", ex);
                    }
                    return false;
                }
            });
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            try {
                super.onCreate(surfaceHolder);
                // Register receiver for media events
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
                filter.addAction(Intent.ACTION_MEDIA_CHECKING);
                filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
                filter.addAction(Intent.ACTION_MEDIA_EJECT);
                filter.addAction(Intent.ACTION_MEDIA_NOFS);
                filter.addAction(Intent.ACTION_MEDIA_REMOVED);
                filter.addAction(Intent.ACTION_MEDIA_SHARED);
                filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
                filter.addDataScheme("file");
                mReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                                || action.equals(Intent.ACTION_MEDIA_CHECKING)) {
                            mStorageReady = true;
                            setTouchEventsEnabled(true);
                            drawFrame();
                        } else {
                            mStorageReady = false;
                            setTouchEventsEnabled(false);
                            mHandler.removeCallbacks(mWorker);
                        }
                    }
                };
                registerReceiver(mReceiver, filter);

                // Register receiver for screen on events
                registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        System.out.println(Intent.ACTION_SCREEN_ON);
                        if (mScreenWake) {
                            mLastDrawTime = 0;
                            drawFrame();
                        }
                    }
                }, new IntentFilter(Intent.ACTION_SCREEN_ON));

                /* mStorageReady = (Environment.getExternalStorageState() ==
                    Environment.MEDIA_MOUNTED || Environment.getExternalStorageState() ==
                        Environment.MEDIA_CHECKING); */
                setTouchEventsEnabled(mStorageReady);

                checkForPermissions();
            } catch (Exception ex) {
                Log.e("HamsiWallpaperSlideshow", "onCreate: ", ex);
            }
        }

        protected void checkForPermissions() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        Intent dialogIntent = new Intent(getBaseContext(),
                                com.hamsiapps.hamsiwallpaperslideshow.SettingsActivity.class);
                        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(dialogIntent);
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Got exception ", ex);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mWorker);
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            try {
                mVisible = visible;
                if (visible) {
                    drawFrame();
                } else {
                    mHandler.removeCallbacks(mWorker);
                }
            } catch (Exception ex) {
                Log.e("HamsiWallpaperSlideshow", "onVisibilityChanged: ", ex);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format,
                                     int width, int height) {
            try {
                super.onSurfaceChanged(holder, format, width, height);
                mWidth = width;
                mHeight = height;
                mMinWidth = width * 2; // cheap hack for scrolling
                mMinHeight = height;
                drawFrame(true);
            } catch (Exception ex) {
                Log.e("HamsiWallpaperSlideshow", "onSurfaceChanged: ", ex);
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            mLastDrawTime = 0;
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mWorker);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                                     float xStep, float yStep, int xPixels, int yPixels) {
            try {
                mXOffset = xOffset;
                mYOffset = yOffset;
                drawFrame(true);
            } catch (Exception ex) {
                Log.e("HamsiWallpaperSlideshow", "onOffsetsChanged: ", ex);
            }
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            this.doubleTapDetector.onTouchEvent(event);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {
            try {
                final Resources res = getResources();

                if (key == null) {
                    mFolder = sharedPreferences.getString(
                            res.getString(R.string.preferences_folder_key),
                            res.getString(R.string.preferences_folder_default));
                    mDuration = Integer.valueOf(sharedPreferences.getString(
                            res.getString(R.string.preferences_duration_key),
                            res.getString(R.string.preferences_duration_default)));
                    mRandom = sharedPreferences.getBoolean(
                            res.getString(R.string.preferences_random_key),
                            Boolean.valueOf(res.getString(R.string.preferences_random_default)));
                    mRotate = sharedPreferences.getBoolean(
                            res.getString(R.string.preferences_rotate_key),
                            Boolean.valueOf(res.getString(R.string.preferences_rotate_default)));
                    mFitInScreen = sharedPreferences.getBoolean(
                            res.getString(R.string.preferences_fit_in_screen_key),
                            Boolean.valueOf(res.getString(R.string.preferences_fit_in_screen_default)));
                    mScroll = sharedPreferences.getBoolean(
                            res.getString(R.string.preferences_scroll_key),
                            Boolean.valueOf(res.getString(R.string.preferences_scroll_default)));
                    mRecurse = sharedPreferences.getBoolean(
                            res.getString(R.string.preferences_recurse_key),
                            Boolean.valueOf(res.getString(R.string.preferences_recurse_default)));
                    mTouchEvents = sharedPreferences.getBoolean(
                            res.getString(R.string.preferences_doubletap_key),
                            Boolean.valueOf(res.getString(R.string.preferences_doubletap_default)));
                    mScreenWake = sharedPreferences.getBoolean(
                            res.getString(R.string.preferences_screen_awake_key),
                            Boolean.valueOf(res.getString(R.string.preferences_screen_awake_default)));
                    mLastDrawTime = 0;
                } else if (key.equals(res.getString(R.string.preferences_folder_key))) {
                    mFolder = sharedPreferences.getString(key,
                            res.getString(R.string.preferences_folder_default));
                    mIndex = -1;
                    mLastDrawTime = 0;
                } else if (key.equals(res.getString(R.string.preferences_duration_key))) {
                    mDuration = Integer.parseInt(sharedPreferences.getString(key,
                            res.getString(R.string.preferences_duration_default)));
                } else if (key.equals(res.getString(R.string.preferences_random_key))) {
                    mRandom = sharedPreferences.getBoolean(key,
                            Boolean.valueOf(res.getString(R.string.preferences_random_default)));
                } else if (key.equals(res.getString(R.string.preferences_rotate_key))) {
                    mRotate = sharedPreferences.getBoolean(key,
                            Boolean.valueOf(res.getString(R.string.preferences_rotate_default)));
                } else if (key.equals(res.getString(R.string.preferences_fit_in_screen_key))) {
                    mFitInScreen = sharedPreferences.getBoolean(key,
                            Boolean.valueOf(res.getString(R.string.preferences_fit_in_screen_default)));
                } else if (key.equals(res.getString(R.string.preferences_scroll_key))) {
                    mScroll = sharedPreferences.getBoolean(key,
                            Boolean.valueOf(res.getString(R.string.preferences_scroll_default)));
                    if (mScroll) {
                        mLastDrawTime = 0;
                    }
                } else if (key.equals(res.getString(R.string.preferences_recurse_key))) {
                    mRecurse = sharedPreferences.getBoolean(key,
                            Boolean.valueOf(res.getString(R.string.preferences_recurse_default)));
                } else if (key.equals(res.getString(R.string.preferences_doubletap_key))) {
                    mTouchEvents = sharedPreferences.getBoolean(key,
                            Boolean.valueOf(res.getString(R.string.preferences_doubletap_default)));
                } else if (key.equals(res.getString(R.string.preferences_screen_awake_key))) {
                    mScreenWake = sharedPreferences.getBoolean(key,
                            Boolean.valueOf(res.getString(R.string.preferences_screen_awake_default)));
                }
            } catch (Exception ex) {
                Log.e("HamsiWallpaperSlideshow", "onSharedPreferenceChanged: ", ex);
            }
        }

        void drawFrame() {
            drawFrame(false, false);
        }

        void drawFrame(boolean _isSetAgain) {
            drawFrame(_isSetAgain, false);
        }

        void drawFrame(boolean _isSetAgain, boolean _isOpenApplicationIfNotExistAnyFile) {
            try{
                String state = Environment.getExternalStorageState();
                if (!state.equals(Environment.MEDIA_MOUNTED) &&
                        !state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                    return;
                }

                Bitmap mBitmap = null;

                try {
                    // Do we need to get a new image?
                    if ((mBitmapPath == null) ||
                        (mDuration > 0 && mLastDrawTime < System.currentTimeMillis() - mDuration)) {
                        // Get a list of files
                        File[] files = BitmapUtil.listFiles(new File(mFolder), mRecurse, ImageFilter);
                        if (files == null || files.length < 1) {
                            if (_isOpenApplicationIfNotExistAnyFile) {
                                Intent dialogIntent = new Intent(getBaseContext(),
                                        com.hamsiapps.hamsiwallpaperslideshow.SettingsActivity.class);
                                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                getApplication().startActivity(dialogIntent);
                                return;
                            }
                            mBitmap = getDefaultBitmap();
                            mBitmapPath = "default";
                            // Save the current time
                            mLastDrawTime = System.currentTimeMillis();
                        } else {
                            // Increment counter
                            int nFiles = files.length;
                            if (mRandom) {
                                int i = mIndex;
                                do {
                                    mIndex = (int) (Math.random() * nFiles);
                                } while (nFiles > 1 && mIndex == i);
                            } else {
                                if (++mIndex >= nFiles) {
                                    mIndex = 0;
                                }
                            }

                            mBitmapPath = files[mIndex].getAbsolutePath();
                            mBitmap = getFormattedBitmap(mBitmapPath);

                            // Save the current time
                            mLastDrawTime = System.currentTimeMillis();
                        }
                    } else if (_isSetAgain) {
                        if (mBitmapPath.equals("default")) {
                            mBitmap = getDefaultBitmap();
                        } else {
                            mBitmap = getFormattedBitmap(mBitmapPath);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                SurfaceHolder holder = getSurfaceHolder();
                Canvas c = null;
                try {
                    if (mBitmap != null) {
                        int xPos = 0;
                        int yPos = 0;
                        if (mScroll) {
                            xPos = 0 - (int) (mWidth * mXOffset);
                            yPos = 0 - (int) (mHeight * mYOffset);
                        }
                        try {
                            // Get and Lock the canvas for writing
                            c = holder.lockCanvas();
                            c.drawColor(Color.BLACK);
                            c.drawBitmap(mBitmap, xPos, yPos, mPaint);
                            mBitmap.recycle();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } finally {
                    if (c != null) holder.unlockCanvasAndPost(c);
                }

                // Reschedule the next redraw
                mHandler.removeCallbacks(mWorker);
                if (mVisible) {
                    mHandler.postDelayed(mWorker, 15000);
                }
            } catch (Exception ex) {
                Log.e("drawFrame", "Got exception ", ex);
            }
        }

        private Bitmap getDefaultBitmap() {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            Bitmap mBitmap = null;
            String note1 = getString(R.string.no_photos_found_1);
            String note2 = getString(R.string.no_photos_found_2);
            String note3 = getString(R.string.no_photos_found_3);
            int y;
            if (getRotation() == Configuration.ORIENTATION_LANDSCAPE) {
                mBitmap = getFormattedBitmap(R.drawable.hamsi_back_land);
                y = (int)(mBitmap.getHeight() / 10 * 8.6);
                note2 = note2 + " " + note3;
                note3 = "";
            } else {
                mBitmap = getFormattedBitmap(R.drawable.hamsi_back);
                y = (int)(mBitmap.getHeight() / 10 * 7);
            }
            int fontSize = (int)(mBitmap.getHeight() / 40);
            Bitmap.Config bitmapConfig = mBitmap.getConfig();
            if(bitmapConfig == null) {
                bitmapConfig = Bitmap.Config.ARGB_8888;
            }
            mBitmap = mBitmap.copy(bitmapConfig, true);

            Canvas canvas = new Canvas(mBitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.rgb(255, 255, 255));
            paint.setTextSize(fontSize);
            paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
            Rect bounds1 = new Rect();
            Rect bounds2 = new Rect();
            Rect bounds3 = new Rect();
            paint.getTextBounds(note1, 0, note1.length(), bounds1);
            paint.getTextBounds(note2, 0, note2.length(), bounds2);
            paint.getTextBounds(note3, 0, note3.length(), bounds3);
            int x1 = (mBitmap.getWidth() - bounds1.width())/2;
            int x2 = (mBitmap.getWidth() - bounds2.width())/2;
            int x3 = (mBitmap.getWidth() - bounds3.width())/2;

            canvas.drawText(note1, x1, y, paint);
            canvas.drawText(note2, x2, y + fontSize + 10, paint);
            canvas.drawText(note3, x3, y + fontSize + fontSize + 20, paint);
            return mBitmap;
        }

        private Bitmap getFormattedBitmap(Bitmap bitmap) {
            int targetWidth = (mScroll) ? mMinWidth : mWidth;
            int targetHeight = (mScroll) ? mMinHeight : mHeight;

            if (bitmap == null) {
                return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            }

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            // Rotate
            if (mRotate) {
                int orientation = getResources().getConfiguration().orientation;
                if (width > height && orientation == Configuration.ORIENTATION_PORTRAIT) {
                    bitmap = BitmapUtil.rotate(bitmap, 90, mScaler);
                } else if (height > width && orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    bitmap = BitmapUtil.rotate(bitmap, -90, mScaler);
                }
            }

            // Scale bitmap
            if (width != targetWidth || height != targetHeight) {
                bitmap = BitmapUtil.transform(mScaler, bitmap, targetWidth, targetHeight, true, mFitInScreen);
            }
            return bitmap;
        }

        private Bitmap getFormattedBitmap(String file) {
            Bitmap bitmap = BitmapUtil.getSampledBitmap(file, mMinWidth, mMinHeight,
                    BitmapUtil.ScalingLogic.FIT);
            return getFormattedBitmap(bitmap);
        }

        private Bitmap getFormattedBitmap(int id) {
            Bitmap bitmap = BitmapUtil.getSampledBitmap(getResources(), id, mMinWidth, mMinHeight,
                    BitmapUtil.ScalingLogic.FIT);
            return getFormattedBitmap(bitmap);
        }

        public int getRotation(){
            Point p = new Point();
            if (Build.VERSION.SDK_INT > 12 ) {
                ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                    .getSize(p);
            } else {
                Display getOrient = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay();
                p.x = getOrient.getWidth();
                p.y = getOrient.getHeight();
            }
            if(p.y > p.x){
                return Configuration.ORIENTATION_PORTRAIT;
            }else {
                return Configuration.ORIENTATION_LANDSCAPE;
            }
        }

    }

}