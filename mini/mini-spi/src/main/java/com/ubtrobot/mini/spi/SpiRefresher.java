package com.ubtrobot.mini.spi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import com.ubtech.utilcode.utils.ByteBufferList;
import com.ubtech.utilcode.utils.thread.ThreadPool;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by logic on 17-11-10.
 *
 * @author logic
 */

public final class SpiRefresher {
  private static final String TAG = "SpiRefresher";
  private static final int SPI_MAX_BYTES = 115201;
  private ByteBuffer normal_buffer;
  private WeakReference<Context> contextRef;
  private HandlerThread mWorker;
  private SpiHandler mHandler;
  private ISpitUtil util;
  private AtomicBoolean inited = new AtomicBoolean(false);

  private SpiRefresher() {
  }

  public static SpiRefresher get() {
    return Holder._instance;
  }

  public void init() throws IOException {
    if (!inited.getAndSet(true)) {
      util = new SpiFileUtil();
      ByteBufferList.setMaxItemSize(SPI_MAX_BYTES);
      util.open();
      mWorker = new HandlerThread("SpiRefresher");
      mWorker.start();
      mHandler = new SpiHandler(mWorker.getLooper());
      try {
        @SuppressLint("PrivateApi") final Class<?> activityThread =
            Class.forName("android.app.ActivityThread");
        final Method currentApplicationMethod =
            activityThread.getDeclaredMethod("currentApplication");
        contextRef = new WeakReference<>((Context) currentApplicationMethod.invoke(null));
      } catch (Exception e) {
        e.printStackTrace();
      }
      ThreadPool.runOnNonUIThread(new Runnable() {
        @Override public void run() {
          loadNormalBmp();
        }
      });
    }
  }

  private void loadNormalBmp() {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    Bitmap bitmap =
        BitmapFactory.decodeStream(contextRef.get().getResources().openRawResource(R.raw.normal),
            null, options);
    Bitmap tempBmp = bitmap.copy(Bitmap.Config.RGB_565, false);
    bitmap.recycle();

    int size = tempBmp.getByteCount() + 1;
    normal_buffer = ByteBufferList.obtain(size);
    tempBmp.copyPixelsToBuffer(normal_buffer);
    tempBmp.recycle();

    byte temp;
    normal_buffer.rewind();
    int length = size - 1;
    if ((length % 2) == 0) {
      for (int i = 0; i < length; i = i + 2) {
        temp = normal_buffer.get(i + 1);
        normal_buffer.put(i + 1, normal_buffer.get(i));
        normal_buffer.put(i, temp);
      }
    }
    normal_buffer.put(length, (byte) 2);
    normal_buffer.rewind();
    Log.w(TAG, "normal_buffer.remaining = " + normal_buffer.remaining());
  }

  private void handleRefreshRequest(ByteBuffer b) {
    try {
      util.write(b);
      ByteBufferList.reclaim(b);
    } catch (IOException e) {
      Log.e(TAG, e.getMessage() + "");
      writeNormalBuffer();
    }
  }

  private void handleSyncRefreshRequest(final @NonNull BufferMessage bufferMessage) {
    try {

      util.write(bufferMessage.buffer);
      synchronized (bufferMessage) {
        bufferMessage.response = true;
        bufferMessage.notifyAll();
      }
      ByteBufferList.reclaim(bufferMessage.buffer);
    } catch (IOException e) {
      Log.e(TAG, e.getMessage() + "");
      writeNormalBuffer();
    }
  }

  private void writeNormalBuffer() {
    try {
      ByteBuffer temp = normal_buffer.duplicate();
      temp.put(temp.remaining() - 1, (byte) 3);
      util.write(temp);
    } catch (IOException e1) {
      Log.e(TAG, e1.getMessage() + "");
    }
  }

  public void refresh(ByteBuffer buffer) {
    mHandler.sendMessage(mHandler.obtainMessage(0, buffer));
  }

  public void refreshSync(@NonNull ByteBuffer buffer) {
    final BufferMessage bufferMessage = new BufferMessage();
    bufferMessage.buffer = buffer;
    bufferMessage.response = false;
    mHandler.sendMessage(mHandler.obtainMessage(3, bufferMessage));
    try {
      synchronized (bufferMessage) {
        if (!bufferMessage.response) {
          bufferMessage.wait();
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void refresh(Bitmap bitmap) {
    mHandler.sendMessage(mHandler.obtainMessage(2, bitmap));
  }

  public void reset() {
    mHandler.removeMessages(0);
  }

  public void normal() {
    mHandler.sendEmptyMessage(1);
  }

  public void close() {
    mWorker.quitSafely();
    util.close();
  }

  private void handleRefreshRequest(Bitmap bmp) {
    if (bmp.getWidth() == 480) {
      final int width = bmp.getWidth() / 2;
      final int height = bmp.getHeight();
      Bitmap right = Bitmap.createBitmap(bmp, 0, 0, width, height);
      Bitmap left = Bitmap.createBitmap(bmp, width, 0, width, height);
      ByteBuffer bb_right = ConvertUtil.convertBmpToBuffer(1, right);
      ByteBuffer bb_left = ConvertUtil.convertBmpToBuffer(0, left);
      try {
        util.write(bb_right);
        util.write(bb_left);
        ByteBufferList.reclaim(bb_left);
        ByteBufferList.reclaim(bb_right);
      } catch (IOException e) {
        Log.e(TAG, e.getMessage() + "");
        writeNormalBuffer();
      }
      right.recycle();
      left.recycle();
    } else if (bmp.getWidth() == 240) {
      ByteBuffer bb = ConvertUtil.convertBmpToBuffer(2, bmp);
      try {
        util.write(bb);
        ByteBufferList.reclaim(bb);
      } catch (IOException e) {
        Log.e(TAG, e.getMessage() + "");
        writeNormalBuffer();
      }
    }
    bmp.recycle();
  }

  private static class Holder {
    @SuppressLint("StaticFieldLeak") private static final SpiRefresher _instance =
        new SpiRefresher();
  }

  private static class BufferMessage {
    ByteBuffer buffer;
    boolean response;
  }

  private class SpiHandler extends Handler {

    SpiHandler(Looper looper) {
      super(looper);
    }

    @Override public void handleMessage(Message msg) {
      switch (msg.what) {
        case 0:
          handleRefreshRequest((ByteBuffer) msg.obj);
          break;
        case 2:
          handleRefreshRequest((Bitmap) msg.obj);
          break;
        case 1:
          try {
            util.write(normal_buffer.duplicate());
          } catch (IOException e) {
            Log.e(TAG, e.getMessage() + "");
          }
          break;
        case 3:
          handleSyncRefreshRequest((BufferMessage) msg.obj);
          break;
        default:
          break;
      }
    }
  }
}
