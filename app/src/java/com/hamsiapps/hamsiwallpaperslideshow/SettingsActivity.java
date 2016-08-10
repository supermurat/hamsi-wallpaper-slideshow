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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

    private static final int DIALOG_SELECT_ALBUM = 1;

    Context mContext;
    ListPreference mPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

/*        final CheckBoxPreference scroll = (CheckBoxPreference) findPreference(
                getString(R.string.preferences_scroll_key));
        scroll.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean)newValue) {
                    new AlertDialog.Builder(mContext)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(android.R.string.dialog_alert_title)
                            .setMessage(R.string.warning_scroll)
                            .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    scroll.setChecked(false);
                                }
                            }).show();
                }
                return true;
            }
        });*/
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
}
