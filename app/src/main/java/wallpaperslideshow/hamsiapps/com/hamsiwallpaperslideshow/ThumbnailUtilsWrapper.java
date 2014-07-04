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

import android.graphics.Bitmap;

public class ThumbnailUtilsWrapper {

	static {
		try {
			Class.forName("android.media.ThumbnailUtils");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void checkAvailable() { }

	public static Bitmap extractThumbnail(final Bitmap source, final int width,
			final int height) {
		return android.media.ThumbnailUtils.extractThumbnail(source, width,
				height, android.media.ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
	}

}
