package com.ubtrobot.giflib;

/**
 * Created by logic on 17-11-20.
 * @author logic--
 */

abstract class SafeRunnableSync implements Runnable {
  final GifDrawableDoubleSync mGifDrawable;

  SafeRunnableSync(GifDrawableDoubleSync gifDrawable) {
    mGifDrawable = gifDrawable;
  }

  @Override public final void run() {
    try {
      if (!mGifDrawable.isRecycled()) {
        doWork();
      }
    } catch (Throwable throwable) {
      final Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
          Thread.getDefaultUncaughtExceptionHandler();
      if (uncaughtExceptionHandler != null) {
        uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), throwable);
      }
      throw throwable;
    }
  }

  abstract void doWork();
}
