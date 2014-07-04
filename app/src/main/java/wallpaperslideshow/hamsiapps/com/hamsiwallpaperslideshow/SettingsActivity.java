/*
 *    Copyright (C) 2014 Murat Demir <mopened@gmail.com>
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

package wallpaperslideshow.hamsiapps.com.hamsiwallpaperslideshow;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class SettingsActivity extends PreferenceActivity {
	
	private static final int DIALOG_SELECT_ALBUM = 1;
	
	Context mContext;
	ListPreference mPath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		
		getPreferenceManager().setSharedPreferencesName(
				HamsiWallpaperSlideshow.SHARED_PREFS_NAME);
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

		Preference about = (Preference) findPreference(
				getString(R.string.preferences_about_key));
		about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				String versionName;
				try {
					PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
					versionName = pi.versionName;
				} catch (PackageManager.NameNotFoundException e) {
					versionName = "";
				}
				LayoutInflater inflater = getLayoutInflater();
				View aboutView = inflater.inflate(R.layout.about_dialog, null);
				TextView aboutText = (TextView) aboutView.findViewById(R.id.text1);
				aboutText.setText(Html.fromHtml(getString(R.string.about_text)
						.replaceAll("\\{VersionName\\}", versionName)));
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setIcon(R.drawable.hamsi_app_icon)
					.setTitle(R.string.app_name)
					.setView(aboutView)
					.setPositiveButton(android.R.string.ok, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
				builder.show();
				return false;
			}
		});

		final CheckBoxPreference scroll = (CheckBoxPreference) findPreference(
				getString(R.string.preferences_scroll_key));
		scroll.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if ((Boolean) newValue == true) {
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
		});
	}

	protected final void onActivityResult(final int requestCode,
            final int resultCode, final Intent i) {
	    if (resultCode == RESULT_OK) {
	            switch (requestCode) {
	            case DIALOG_SELECT_ALBUM:
	            	SharedPreferences prefs = getSharedPreferences(
	            			HamsiWallpaperSlideshow.SHARED_PREFS_NAME, 1);
                    Editor editor = prefs.edit();
                    editor.putString(getResources().getString(R.string.preferences_folder_key),
                    		i.getStringExtra("folder"));
                    editor.commit();
                    break;
	            }
	    }
	}
}
