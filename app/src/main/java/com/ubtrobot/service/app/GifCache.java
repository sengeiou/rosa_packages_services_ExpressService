package com.ubtrobot.service.app;

import android.util.LruCache;
import com.ubtrobot.giflib.GifDecoder;

/**
 * Created by logic on 18-5-28.
 *
 * @author logic
 */

public final class GifCache {

  private final LruCache<String, GifDecoder> mCache;

  public GifCache() {
    mCache = new LruCache<String, GifDecoder>(20) {
      @Override protected int sizeOf(String key, GifDecoder value) {
        return 1;
      }

      @Override protected void entryRemoved(boolean evicted, String key, GifDecoder oldValue,
          GifDecoder newValue) {
        oldValue.recycle();
      }
    };
  }

  public GifDecoder get(String key) {
    return mCache.get(key);
  }

  public void put(String key, GifDecoder decoder) {
    mCache.put(key, decoder);
  }

  public void remove(String key) {
    mCache.remove(key);
  }
}
