package com.ubtrobot.giflib;

import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Default executor for rendering tasks - {@link ScheduledThreadPoolExecutor}
 * with 1 worker thread and {@link DiscardPolicy}.
 */
final class GifRenderingExecutor extends ScheduledThreadPoolExecutor {

  private GifRenderingExecutor() {
    super(1, new DiscardPolicy());
  }

  static GifRenderingExecutor getInstance() {
    return InstanceHolder.INSTANCE;
  }

  // Lazy initialization via inner-class holder
  private static final class InstanceHolder {
    private static final GifRenderingExecutor INSTANCE = new GifRenderingExecutor();
  }
}
