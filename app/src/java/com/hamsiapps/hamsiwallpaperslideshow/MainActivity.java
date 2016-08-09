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

package com.hamsiapps.hamsiwallpaperslideshow;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private Activity me;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        me = this;

        TextView tvHowToEnable = (TextView) findViewById(R.id.tvHowToEnable);
        tvHowToEnable.setText(Html.fromHtml(getString(R.string.how_to_enable)));

        Button btnSetWallpaper = (Button) findViewById(R.id.btnSetWallpaper);
        btnSetWallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                if (Build.VERSION.SDK_INT > 15) {
                    i.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                    String p = HamsiWallpaperSlideshow.class.getPackage().getName();
                    String c = HamsiWallpaperSlideshow.class.getCanonicalName();
                    i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(p, c));
                } else {
                    i.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                }
                startActivityForResult(i, 0);
            }
        });

        Button btnConfigure = (Button) findViewById(R.id.btnConfigure);
        btnConfigure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(i);
            }
        });

        Button btnAbout = (Button) findViewById(R.id.btnAbout);
        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(me);
                    builder.setIcon(R.drawable.hamsi_app_icon)
                            .setTitle(R.string.app_name)
                            .setView(aboutView)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    builder.show();
                } catch (Exception ex) {
                    Log.e("MainActivity", "btnAbout:onClick: ", ex);
                }
            }
        });

        Button btnLike = (Button) findViewById(R.id.btnLike);
        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    final String my_package_name = getPackageName();
                    String url = "";
                    try {
                        me.getPackageManager().getPackageInfo("com.android.vending", 0);
                        url = "market://details?id=" + my_package_name;
                    } catch ( final Exception e ) {
                        url = "https://play.google.com/store/apps/details?id=" + my_package_name;
                    }
                    final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception ex) {
                    Log.e("MainActivity", "btnLike:onClick: ", ex);
                }
            }
        });
    }

}
