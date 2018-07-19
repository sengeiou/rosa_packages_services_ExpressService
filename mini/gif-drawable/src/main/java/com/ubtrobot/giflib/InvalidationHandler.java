package com.ubtrobot.giflib;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import com.ubtrobot.mini.spi.ConvertUtil;
import com.ubtrobot.mini.spi.SpiRefresher;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

class InvalidationHandler extends Handler {

  static final int MSG_TYPE_INVALIDATION = -1;

  private final WeakReference<GifDrawable> mDrawableRef;

  public InvalidationHandler(final GifDrawable gifDrawable) {
    super(Looper.getMainLooper());
    mDrawableRef = new WeakReference<>(gifDrawable);
  }

  @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB_MR1) @Override
  public void handleMessage(final Message msg) {
    final GifDrawable gifDrawable = mDrawableRef.get();
    if (gifDrawable == null) {
      return;
    }

    if (msg.what == MSG_TYPE_INVALIDATION) {
      //gifDrawable.invalidateSelf();
      final Bitmap bitmap = gifDrawable.mBuffer;
      if (bitmap.isRecycled()) return;
      Bitmap temp = ConvertUtil.convertBitmap(bitmap);
      if (bitmap.getWidth() == 480) {
        final int width = bitmap.getWidth() / 2;
        final int height = bitmap.getHeight();
        Bitmap right = Bitmap.createBitmap(temp, 0, 0, width, height);
        Bitmap left = Bitmap.createBitmap(temp, width, 0, width, height);
        ByteBuffer bb_right = ConvertUtil.convertBmpToBuffer(1, right);
        ByteBuffer bb_left = ConvertUtil.convertBmpToBuffer(0, left);
        SpiRefresher.get().refresh(bb_right);
        SpiRefresher.get().refresh(bb_left);
        right.recycle();
        left.recycle();
      } else if (bitmap.getWidth() == 240) {
        ByteBuffer bb = ConvertUtil.convertBmpToBuffer(2, temp);
        SpiRefresher.get().refresh(bb);
      }
      temp.recycle();
    } else {
      //回调需调用
      for (AnimationListener listener : gifDrawable.mListeners) {
        listener.onAnimationCompleted(msg.what);
      }
    }
  }
}
