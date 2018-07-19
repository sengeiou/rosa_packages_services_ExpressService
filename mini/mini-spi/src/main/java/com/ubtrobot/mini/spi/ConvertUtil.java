package com.ubtrobot.mini.spi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import com.ubtech.utilcode.utils.ByteBufferList;
import java.nio.ByteBuffer;

/**
 * Created by logic on 18-1-3.
 *
 * @author logic
 */

public final class ConvertUtil {

  @NonNull public static Bitmap convertBitmap(Bitmap bitmap) {
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inPreferredConfig = Bitmap.Config.RGB_565;
    return bitmap.copy(Bitmap.Config.RGB_565, false);
  }

  @NonNull public static Bitmap convertBitmapAndRecycle(Bitmap bitmap) {
    Bitmap temp;
    temp = bitmap.copy(Bitmap.Config.RGB_565, false);
    bitmap.recycle();
    return temp;
  }

  @NonNull public static ByteBuffer convertBmpToBuffer(int eyeMode, Bitmap bitmap) {
    int size = bitmap.getByteCount() + 1;
    ByteBuffer buf = ByteBufferList.obtain(size);
    bitmap.copyPixelsToBuffer(buf);

    byte temp;
    buf.rewind();
    int length = size - 1;
    if ((length % 2) == 0) {
      for (int i = 0; i < length; i = i + 2) {
        temp = buf.get(i + 1);
        buf.put(i + 1, buf.get(i));
        buf.put(i, temp);
      }
    }
    buf.put(length, (byte) eyeMode);
    return buf;
  }
}
