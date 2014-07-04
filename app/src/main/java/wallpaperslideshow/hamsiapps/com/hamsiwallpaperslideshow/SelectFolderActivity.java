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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;

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
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

public class SelectFolderActivity extends ListActivity {

	private class SearchFoldersTask extends AsyncTask<Void, Void, String[]> {
		Context mContext = null;
		ProgressDialog mProgressDialog = null;

		public SearchFoldersTask(final Context context) {
			mContext = context;
		}

		@Override
		protected String[] doInBackground(final Void... params) {
			// Search external storage for all public folders
			final ArrayList<File> folders = new ArrayList<File>();
			listDirectories(folders, Environment.getExternalStorageDirectory(),
					mFolderFilter);

			// Filter for folder only containing images
			final ArrayList<String> result = new ArrayList<String>();
			for (final File f : folders) {
				if (f.listFiles(mImageFilter).length > 0) {
					result.add(f.toString());
				}
			}

			final String[] temp = new String[result.size()];
			result.toArray(temp);
			return temp;
		}

		private void listDirectories(final Collection<File> files, final File directory,
				final FileFilter filter) {
			final File[] found = directory.listFiles(filter);
			if (found != null) {
				for (int i = 0; i < found.length; i++) {
					files.add(found[i]);
					if (found[i].isDirectory()) {
						listDirectories(files, found[i], filter);
					}
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
				});
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

	public static FileFilter mImageFilter = new FileFilter() {
		public boolean accept(final File file) {
			final String name = file.getName();
			final String ext = BitmapUtil.getExtension(name);

			if (ext == null)
				return false;

			if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png") && !ext.equals("gif"))
				return false;

			return !name.toLowerCase().equals("albumart.jpg");
		}
	};

	private final FileFilter mFolderFilter = new FileFilter() {
		public boolean accept(final File file) {
			if (!file.isDirectory())
				return false;
			else if (file.getName().startsWith("."))
				return false;
			else
				return !new File(file, ".nomedia").exists();
		}
	};

	private final AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(final AdapterView<?> arg0, final View arg1, final int arg2, final long arg3) {
			final TextView text2 = (TextView)arg1.findViewById(R.id.text2);
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

}
