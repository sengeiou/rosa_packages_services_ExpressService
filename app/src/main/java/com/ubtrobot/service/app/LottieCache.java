package com.ubtrobot.service.app;

import android.util.LruCache;
import com.airbnb.lottie.LottieComposition;

/**
 * Created by logic on 18-5-28.
 *
 * @author logic
 */

public final class LottieCache {

  private final LruCache<String, LottieComposition> mCache;

  public LottieCache() {
    mCache = new LruCache<String, LottieComposition>(10) {
      @Override protected int sizeOf(String key, LottieComposition value) {
        return 1;
      }
    };
  }

  public LottieComposition get(String key) {
    return mCache.get(key);
  }

  public void put(String key, LottieComposition composition) {
    mCache.put(key, composition);
  }
}
