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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 串口文件读写接口
 * @author Logic
 */
interface ISpitUtil {
  String SPI_SERIAL_FILE = "/dev/st7789v";

  void open() throws IOException;

  void close();

  void write(ByteBuffer out) throws IOException;

  int read(ByteBuffer in) throws IOException;
}
