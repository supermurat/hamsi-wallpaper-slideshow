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

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FolderArrayAdapter extends ArrayAdapter<String> {

	private class CachedView {
		public ImageView image;
		public TextView text1;
		public TextView text2;

		public CachedView(final View v) {
			image = (ImageView) v.findViewById(R.id.image);
			text1 = (TextView) v.findViewById(R.id.text1);
			text2 = (TextView) v.findViewById(R.id.text2);
			v.setTag(this);
		}
	}

	private static boolean mHasThumbnailUtils;
	static {
		try {
			ThumbnailUtilsWrapper.checkAvailable();
			mHasThumbnailUtils = true;
		} catch (final Throwable t) {
			mHasThumbnailUtils = false;
		}
	}

	private final LayoutInflater mInflater;

	private final String[] mFolders;

	public FolderArrayAdapter(final Context context, final int layout, final String[] folders) {
		super(context, layout, folders);
		this.mInflater = LayoutInflater.from(context);
		this.mFolders = folders;
	}

	@Override
	public View getView(final int position, View view, final ViewGroup parent) {
		if (view == null) {
			view = mInflater.inflate(R.layout.select_folder_list_item, null);
			new CachedView(view);
		}

		final CachedView cache = (CachedView)view.getTag();

		final String folder = mFolders[position];
		if (folder != null) {
			cache.image.setImageResource(android.R.drawable.ic_menu_gallery);
			cache.text1.setText(new File(folder).getName());
			cache.text2.setText(folder);
			try {
				final File[] images = new File(folder).listFiles(SelectFolderActivity.mImageFilter);
				if (images.length > 0) {
					cache.text1.setText(new File(folder).getName() + " (" + images.length + ")");
					Bitmap bitmap = BitmapUtil.makeBitmap(75, 10000, images[0].getAbsolutePath(), null);
					if (mHasThumbnailUtils) {
						bitmap = ThumbnailUtilsWrapper.extractThumbnail(bitmap, 75, 75);
					} else {
						bitmap = BitmapUtil.transform(null, bitmap, 75, 75, false, true);
					}
					cache.image.setImageBitmap(bitmap);
				}
			} catch (final Exception e) { }
		}

		return view;
	};

}