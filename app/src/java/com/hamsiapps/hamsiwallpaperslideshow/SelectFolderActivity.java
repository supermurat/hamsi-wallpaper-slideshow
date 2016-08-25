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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;

public class SelectFolderActivity extends ListActivity {

    private static final String TAG = "SelectFolderActivity";
    public static FileFilter mImageFilter = new FileFilter() {
        public boolean accept(final File file) {
            try {
                String name = file.getName();
                String ext = BitmapUtil.getExtension(name);

                if (ext == null)
                    return false;

                if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png") && !ext.equals("gif"))
                    return false;

                return !name.toLowerCase().equals("albumart.jpg");
            } catch (Exception ex) {
                // Ignore and continue
                Log.e(TAG, "BUG:mImageFilter ", ex);
            }
            return false;
        }
    };
    private final FileFilter mFolderFilter = new FileFilter() {
        public boolean accept(final File file) {
            try {
                if (!file.isDirectory())
                    return false;
                else if (file.getName().startsWith("."))
                    return false;
                else
                    return !new File(file, ".nomedia").exists();
            } catch (Exception ex) {
                // Ignore and continue
                Log.e(TAG, "BUG:mFolderFilter ", ex);
            }
            return false;
        }
    };
    private final AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(final AdapterView<?> arg0, final View arg1, final int arg2, final long arg3) {
            final TextView text2 = (TextView) arg1.findViewById(R.id.text2);
            final Intent intent = new Intent();
            intent.putExtra("folder", text2.getText().toString());
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_folder_activity);
        setResult(Activity.RESULT_CANCELED);

        getListView().setOnItemClickListener(mItemClickListener);

        new SearchFoldersTask(this).execute();
    }

    private class SearchFoldersTask extends AsyncTask<Void, Void, String[]> {
        Context mContext = null;
        ProgressDialog mProgressDialog = null;

        SearchFoldersTask(final Context context) {
            mContext = context;
        }

        @Override
        protected String[] doInBackground(final Void... params) {
            try {
                // Search external storage for all public folders
                final ArrayList<File> folders = new ArrayList<File>();
                listDirectories(folders, Environment.getExternalStorageDirectory());

                // Filter for folder only containing images
                final ArrayList<String> result = new ArrayList<String>();
                listDirectoriesThatHaveImages(result, folders);

                if (folders.size() == 0 || result.size() == 0) {
                    folders.clear();
                    result.clear();
                    listDirectories(folders, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getParentFile());
                    listDirectoriesThatHaveImages(result, folders);
                }

                final String[] temp = new String[result.size()];
                result.toArray(temp);
                return temp;
            } catch (Exception ex) {
                // Ignore and continue
                Log.e(TAG, "BUG:doInBackground ", ex);
            }
            return new String[0];
        }

        private void listDirectories(final Collection<File> files, final File directory) {
            try {
                if (directory.isDirectory() && directory.exists()) {
                    final File[] found = directory.listFiles(mFolderFilter);
                    if (found != null) {
                        for (int i = 0; i < found.length; i++) {
                            files.add(found[i]);
                            listDirectories(files, found[i]);
                        }
                    }
                }
            } catch (Exception ex) {
                // Ignore and continue
                Log.e(TAG, "BUG:listDirectories ", ex);
            }
        }

        private void listDirectoriesThatHaveImages(final ArrayList<String> result, final ArrayList<File> folders) {
            for (final File f : folders) {
                try {
                    if (f.isDirectory() && f.exists()) {
                        final File[] found = f.listFiles(mImageFilter);
                        if (found != null && found.length > 0) {
                            result.add(f.toString());
                        }
                    }
                } catch (Exception ex) {
                    // Ignore and continue
                    Log.e(TAG, "BUG:listDirectoriesThatHaveImages ", ex);
                }
            }
        }

        @Override
        protected void onPostExecute(final String[] result) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            if (result != null && result.length > 0) {
                final ListActivity activity = (ListActivity) mContext;
                activity.setListAdapter(new FolderArrayAdapter(mContext,
                        R.layout.select_folder_list_item, result));
            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                        .setTitle("No photos")
                        .setMessage("There were no folders containing photos found.")
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog,
                                                        final int which) {
                                        dialog.cancel();
                                        finish();
                                    }
                                }
                        );
                builder.show();
            }
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = ProgressDialog.show(mContext, "Please wait",
                    "Searching folders, this can take some time to complete...",
                    true);
            mProgressDialog.setCancelable(true);
        }
    }

}
