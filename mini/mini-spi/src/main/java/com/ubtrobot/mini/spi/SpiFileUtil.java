/*
 *
 *  *
 *  *  *
 *  *  * Copyright (c) 2008-2017 UBT Corporation.  All rights reserved.  Redistribution,
 *  *  *  modification, and use in source and binary forms are not permitted unless otherwise authorized by UBT.
 *  *  *
 *  *
 *
 */

package com.ubtrobot.mini.spi;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * alpha2串口文件读写
 *
 * @author Logic
 */

class SpiFileUtil implements ISpitUtil {

  private FileOutputStream ouStream;
  private FileInputStream inStream;
  private final String mFileName;

  SpiFileUtil() {
    mFileName = ISpitUtil.SPI_SERIAL_FILE;
  }

  @Override synchronized public void open() throws IOException, SecurityException {
    if (ouStream == null) {
      this.ouStream = new FileOutputStream(new File(mFileName));
    }
    if (inStream == null) {
      this.inStream = new FileInputStream(new File(mFileName));
    }
  }

  @Override public void close() {
    try {
      if (ouStream != null) {
        ouStream.close();
      }
      ouStream = null;
      if (inStream != null) {
        inStream.close();
      }
      inStream = null;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override public void write(ByteBuffer out) throws IOException {
    if (ouStream == null) return;
    if (out == null || !out.hasRemaining()) return;
    if (out.remaining() != 240 * 240 * 2 + 1) {
      Log.w("Express", "error data...size =" + out.remaining());
      return;
    }
    byte[] bytes = new byte[out.remaining()];
    out.get(bytes);
    ouStream.write(bytes);
    ouStream.flush();
  }

  @Override public int read(ByteBuffer in) throws IOException {
    if (in == null || inStream == null) return 0;
    int available = inStream.available();
    if (available > 0) {
      byte[] bytes = new byte[available];
      int read = inStream.read(bytes);
      if (read <= 0) return 0;
      in.put(bytes);
      in.position(0);
      in.limit(read);
    }
    return available;
  }
}
