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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {

    private static final int DIALOG_SELECT_ALBUM = 1;

    Context mContext;
    ListPreference mPath;
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            mContext = this;

            getPreferenceManager().setSharedPreferencesName(HamsiWallpaperSlideshow.SHARED_PREFS_NAME);
            addPreferencesFromResource(R.xml.preferences);

            mPath = (ListPreference) findPreference(getString(
                    R.string.preferences_folder_key));
            mPath.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mPath.getDialog().hide();
                    Intent intent = new Intent(SettingsActivity.this, SelectFolderActivity.class);
                    startActivityForResult(intent, DIALOG_SELECT_ALBUM);
                    return true;
                }
            });

            checkForPermissions();
        } catch (Exception ex) {
            Log.e(TAG, "Got exception ", ex);
        }
    }

    protected final void onActivityResult(final int requestCode,
                                          final int resultCode, final Intent i) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case DIALOG_SELECT_ALBUM:
                    SharedPreferences prefs = getSharedPreferences(
                            HamsiWallpaperSlideshow.SHARED_PREFS_NAME, 1);
                    Editor editor = prefs.edit();
                    editor.putString(getResources().getString(R.string.preferences_folder_key), i.getStringExtra("folder"));
                    editor.commit();
                    break;
            }
        }
    }

    protected void checkForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 5);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

    }

}
