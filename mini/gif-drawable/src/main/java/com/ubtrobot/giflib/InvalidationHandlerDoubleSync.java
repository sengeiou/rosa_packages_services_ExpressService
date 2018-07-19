package com.ubtrobot.giflib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import com.ubtech.utilcode.utils.CloseUtils;
import com.ubtrobot.mini.spi.SpiRefresher;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * Created by logic on 17-11-20.
 * @author logic--
 */

final class InvalidationHandlerDoubleSync extends Handler {

  static final int MSG_TYPE_INVALIDATION = -1;

  private final WeakReference<GifDrawableDoubleSync> mDrawableRef;

  public InvalidationHandlerDoubleSync(final GifDrawableDoubleSync gifDrawable) {
    super(Looper.getMainLooper());
    mDrawableRef = new WeakReference<>(gifDrawable);
  }

  @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB_MR1) @Override
  public void handleMessage(final Message msg) {
    final GifDrawableDoubleSync gifDrawable = mDrawableRef.get();
    if (gifDrawable == null) {
      return;
    }

    if (msg.what == MSG_TYPE_INVALIDATION) {
      //gifDrawable.invalidateSelf();
      final Bitmap bitmap1 = gifDrawable.mBuffer1;
      final Bitmap bitmap2 = gifDrawable.mBuffer2;
      if (bitmap1.isRecycled() || bitmap2.isRecycled()) return;
      ByteBuffer buf1 = convertBmpToBuffer(0, bitmap1);
      ByteBuffer buf2 = convertBmpToBuffer(1, bitmap2);
      SpiRefresher.get().refresh(buf1);
      SpiRefresher.get().refresh(buf2);
    } else {
      for (AnimationListener listener : gifDrawable.mListeners) {
        listener.onAnimationCompleted(msg.what);
      }
    }
  }

  @NonNull private ByteBuffer convertBmpToBuffer(int eyeMode, Bitmap bitmap) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inPreferredConfig = Bitmap.Config.RGB_565;
    bitmap = BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.size(), opts);
    CloseUtils.closeIOQuietly(bos);

    int size = bitmap.getByteCount() + 1;
    ByteBuffer buf = ByteBuffer.allocate(size);
    bitmap.copyPixelsToBuffer(buf);

    byte[] raw = new byte[size - 1];
    byte temp;
    buf.rewind();
    buf.get(raw);
    if ((raw.length % 2) == 0) {
      for (int i = 0; i < raw.length; i = i + 2) {
        temp = raw[i + 1];
        raw[i + 1] = raw[i];
        raw[i] = temp;
      }
    }
    buf.rewind();
    buf.put(raw);
    buf.put((byte) eyeMode);
    buf.rewind();
    return buf;
  }
}
