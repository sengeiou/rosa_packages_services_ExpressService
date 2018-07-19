package com.ubtrobot.service.express.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.util.Log;
import com.ubtech.utilcode.utils.CloseUtils;
import com.ubtech.utilcode.utils.FileUtils;
import com.ubtech.utilcode.utils.thread.ThreadPool;
import com.ubtrobot.mini.properties.sdk.PropertiesApi;
import com.ubtrobot.mini.spi.ConvertUtil;
import com.ubtrobot.mini.spi.SpiRefresher;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;

/**
 * Created by logic on 17-11-17.
 *
 * @author logic
 */

public final class PngUtils {

  public static void copyToCache(final Context context) {
    ThreadPool.runOnNonUIThread(new Runnable() {
      @Override public void run() {
        try {
          AssetManager am = context.getAssets();
          String[] names = am.list("");
          for (String name : names) {
            if (name.endsWith(".gif") || name.endsWith(".png") || name.endsWith(".json")) {
              copyFileFromAssets(context, am, name);
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private static void copyFileFromAssets(Context context, AssetManager am, String name) {
    InputStream is = null;
    FileOutputStream fos = null;
    File file = new File(context.getCacheDir(), name);
    if (file.exists()) return;
    try {
      is = am.open(name);
      if (FileUtils.createOrExistsFile(file)) {
        fos = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
          fos.write(buffer, 0, read);
        }
        fos.flush();
      } else {
        Log.w("Logic", "fail to create file " + file.getAbsolutePath());
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      CloseUtils.closeIO(fos, is);
    }
  }

  public static void refreshWidthPng(File file) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), options);
    bitmap = ConvertUtil.convertBitmapAndRecycle(bitmap);

    if (bitmap.getWidth() == 480) {
      Bitmap left = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth() / 2, bitmap.getHeight());
      Bitmap right = Bitmap.createBitmap(bitmap, bitmap.getWidth() / 2, 0, bitmap.getWidth() / 2,
          bitmap.getHeight());
      bitmap.recycle();
      ByteBuffer left_b = ConvertUtil.convertBmpToBuffer(0, left);
      ByteBuffer right_b = ConvertUtil.convertBmpToBuffer(1, right);
      SpiRefresher.get().refresh(left_b);
      SpiRefresher.get().refresh(right_b);
      left.recycle();
      right.recycle();
    } else if (bitmap.getWidth() == 240) {
      ByteBuffer buf = ConvertUtil.convertBmpToBuffer(2, bitmap);
      SpiRefresher.get().refresh(buf);
      bitmap.recycle();
    } else {
      Log.w("Logic", "bitmap must be 480*240 or 240*240 .");
    }
  }

  public static void refreshWidthPng(Resources resources, @RawRes int rawId) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    Bitmap bitmap = BitmapFactory.decodeResource(resources, rawId, options);
    bitmap = ConvertUtil.convertBitmapAndRecycle(bitmap);

    if (bitmap.getWidth() == 480) {
      Bitmap left = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth() / 2, bitmap.getHeight());
      Bitmap right = Bitmap.createBitmap(bitmap, bitmap.getWidth() / 2, 0, bitmap.getWidth() / 2,
          bitmap.getHeight());
      bitmap.recycle();
      ByteBuffer left_b = ConvertUtil.convertBmpToBuffer(0, left);
      ByteBuffer right_b = ConvertUtil.convertBmpToBuffer(1, right);
      SpiRefresher.get().refresh(left_b);
      SpiRefresher.get().refresh(right_b);
      left.recycle();
      right.recycle();
    } else if (bitmap.getWidth() == 240) {
      ByteBuffer buf = ConvertUtil.convertBmpToBuffer(2, bitmap);
      SpiRefresher.get().refresh(buf);
      bitmap.recycle();
    } else {
      Log.w("Logic", "bitmap must be 480*240 or 240*240 .");
    }
  }

  public static void main(String args[]) {
    List<File> files =
        FileUtils.listFilesInDirWithFilter(new File(Environment.getExternalStorageDirectory(), "mini_test"),
            ".jpg");
    if (files == null) return;
    for (int i = 0; i < files.size(); ++i) {
      BitmapFactory.Options opt = new BitmapFactory.Options();
      opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
      ByteBuffer buffer = ConvertUtil.convertBmpToBuffer(2,
          ConvertUtil.convertBitmap(BitmapFactory.decodeFile(files.get(i).getPath(), opt)));
      File fileO = new File(files.get(i).getPath().replace(".jpg", ""));
      FileUtils.createOrExistsFile(fileO);
      FileOutputStream fos = null;
      try {
        fos = new FileOutputStream(fileO);
        fos.write(buffer.array());
        fos.flush();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        CloseUtils.closeIOQuietly(fos);
      }
    }
  }

  public static void main2(String args[]) {
    doJpegMerge("booting_002_045");
    doJpegMerge("booting_046_095");
    doJpegMerge("shutdown_res");
  }

  @SuppressLint("NewApi") public static void doJpegMerge(String dirName) {
    File booting1 = new File(Environment.getExternalStorageDirectory(), dirName);
    List<File> files = FileUtils.listFilesInDirWithFilter(booting1, ".jpg");
    if (files == null) return;

    Log.d("Logic", "files.size = " + files.size());
    files.sort(new Comparator<File>() {
      @Override public int compare(File o1, File o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    File fileO = new File(booting1, "booting_all");
    fileO.delete();
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(fileO, true);
      for (int i = 0; i < files.size(); ++i) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
        ByteBuffer buffer = ConvertUtil.convertBmpToBuffer(2,
            ConvertUtil.convertBitmap(BitmapFactory.decodeFile(files.get(i).getPath(), opt)));
        FileUtils.createOrExistsFile(fileO);
        try {
          fos.write(buffer.array());
          fos.flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } finally {
      CloseUtils.closeIOQuietly(fos);
    }
  }

  public static void main3(String filePath) {
    BitmapFactory.Options opt = new BitmapFactory.Options();
    opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
    convertBmpToStaticBuffer(2, BitmapFactory.decodeFile((filePath), opt));
  }

  @NonNull public static ByteBuffer convertBmpToStaticBuffer(int eyeMode, Bitmap bitmap) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = Bitmap.Config.RGB_565;
    bitmap = BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.size(), options);
    CloseUtils.closeIOQuietly(bos);
    int size = bitmap.getByteCount() + 1;
    ByteBuffer buf = ByteBuffer.allocate(size);
    bitmap.copyPixelsToBuffer(buf);
    byte[] raw = new byte[size - 1];
    buf.rewind();
    buf.get(raw);
    if (raw.length % 2 == 0) {
      for (int b = 0; b < raw.length; b += 2) {
        byte temp = raw[b + 1];
        raw[b + 1] = raw[b];
        raw[b] = temp;
      }
    }

    buf.rewind();
    buf.put(raw);
    Log.d("PrintArray", "unsigned char  logcat[240*240*2] = \n {");
    StringBuilder var18 = new StringBuilder();
    var18.append("unsigned char  logcat[240*240*2] = \n {");

    for (int filePath = 0; filePath < raw.length; ++filePath) {
      var18.append(String.format("0x%2x", raw[filePath]));
      if (filePath == raw.length - 1) {
        var18.append("\n }");
      } else {
        var18.append(", ");
        if (filePath % 10 == 0) {
          var18.append("\n");
        }
      }
    }

    String var19 =
        Environment.getExternalStorageDirectory().getAbsolutePath() + "/signal_res/temp.txt";
    File fileO = new File(var19);
    FileUtils.createOrExistsFile(fileO);
    FileOutputStream fos = null;

    try {
      fos = new FileOutputStream(fileO);
      fos.write(var18.toString().getBytes());
      fos.flush();
    } catch (IOException var16) {
      var16.printStackTrace();
    } finally {
      CloseUtils.closeIOQuietly(fos);
    }

    buf.put((byte) eyeMode);
    buf.rewind();
    return buf;
  }
}
