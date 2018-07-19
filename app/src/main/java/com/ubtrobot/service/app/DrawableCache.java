package com.ubtrobot.service.app;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import com.airbnb.lottie.LottieDrawable;
import com.ubtech.utilcode.utils.thread.HandlerUtils;
import com.ubtrobot.cpdrawable.CircleProgressDrawable;
import com.ubtrobot.express.protos.Express;
import com.ubtrobot.giflib.GifDrawable;
import com.ubtrobot.mini.spi.IAnimationDrawable;

/**
 * 缓存
 * Created by logic on 18-1-5.
 */

public final class DrawableCache {

  private final Context context;
  private final LruCache<Express.ExpressInfo, IAnimationDrawable> mCache;

  public DrawableCache(Context context) {
    this.context = context;
    //final int memoryCache = getMemoryCacheSize();
    mCache = new LruCache<Express.ExpressInfo, IAnimationDrawable>(10) {
      @Override protected int sizeOf(Express.ExpressInfo key, IAnimationDrawable dr) {
        return 1/*getDrawableSize(dr) / 1024*/;
      }

      @Override protected void entryRemoved(boolean evicted, Express.ExpressInfo key, IAnimationDrawable oldValue,
          IAnimationDrawable newValue) {
        super.entryRemoved(evicted, key, oldValue, newValue);
        if (oldValue instanceof GifDrawable) {
          ((GifDrawable) oldValue).recycle();
        } else if (oldValue instanceof LottieDrawable) {
          final LottieDrawable drawable = ((LottieDrawable) oldValue);
          HandlerUtils.runUITask(new Runnable() {
            @Override public void run() {
              // FIXME MainThread Run
              drawable.cancelAnimation();
              drawable.clearComposition();
            }
          });
        } else if (oldValue instanceof CircleProgressDrawable) {
          final CircleProgressDrawable drawable = (CircleProgressDrawable) oldValue;
          drawable.clearAnimation();
        }
      }
    };
  }

  private int getDrawableSize(Drawable dr) {
    return dr.getIntrinsicHeight() * dr.getIntrinsicWidth() * 4;
  }

  public void addItem(Express.ExpressInfo key, IAnimationDrawable drawable) {
    if (getItem(key) == null) {
      mCache.put(key, drawable);
    }
  }

  public IAnimationDrawable getItem(Express.ExpressInfo key) {
    return mCache.get(key);
  }

  public IAnimationDrawable removeItem(Express.ExpressInfo key) {
    return mCache.remove(key);
  }

  private int getMemoryCacheSize() {
    ActivityManager am = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
    int memoryClass = 1;
    if (am != null) {
      memoryClass = am.getMemoryClass();
    }
    return 1024 * 1024 * memoryClass / 16;
  }
}
