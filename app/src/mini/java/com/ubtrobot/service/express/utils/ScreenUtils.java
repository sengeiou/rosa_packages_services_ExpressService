package com.ubtrobot.service.express.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by bob.xu on 2017/11/6.
 *
 * @author bob.xu
 */

public class ScreenUtils {
  private static final int DOUBLE_EYES_OK = 0;
  private static final int LEFT_EYE_OK = 1;
  private static final int RIGHT_EYE_OK = 2;
  private static final int DOUBLE_EYES_FAIL = -1;
  private static final String node_backlight = "/dev/lcd_st7789v_backlight";

  public ScreenUtils() {
  }

  /** 左右屏显示一样 */
  public static int refreshSame(byte[] eyes) {
    if (eyes == null || eyes.length != 240 * 240 * 2) { //参数检查
      return -1;
    }
    return refresh(eyes, 2);
  }

  /** 左右屏显示不一样 */
  public static int refreshDiff(byte[] lefteye, byte[] righteye) {
    if (lefteye == null
        || righteye == null
        || lefteye.length != 240 * 240 * 2
        || righteye.length != 240 * 240 * 2) { //参数检查
      return -1;
    }
    int resultleft;
    int resultRight;
    int result;
    resultleft = refresh(lefteye, 0);
    resultRight = refresh(righteye, 1);
    if (resultleft == 0 && resultRight == 0) {
      result = DOUBLE_EYES_OK;
    } else if (resultleft == 0 && resultRight == -1) {
      result = LEFT_EYE_OK;
    } else if (resultleft == -1 && resultRight == 0) {
      result = RIGHT_EYE_OK;
    } else {
      result = DOUBLE_EYES_FAIL;
    }
    return result;
  }

  private synchronized static int refresh(byte[] data, int mode) {
    byte[] switchData = new byte[data.length + 1];
    for (int i = 0; i < switchData.length; i++) {
      if (i == switchData.length - 1) {
        switchData[i] = (byte) mode;
      } else {
        switchData[i] = data[i];
      }
    }

    int result = 0;
    FileOutputStream fos;
    try {
      String node_spi = "/dev/st7789v";
      fos = new FileOutputStream(node_spi);
      fos.write(switchData);
      fos.close();
    } catch (Exception e) {
      e.printStackTrace();
      result = -1;
    }
    return result;
  }

  public static boolean setBrightness(int light) {
    if (light > 31 || light <= 0) {
      throw new RuntimeException("参数不合法，light 取值范围：1-31");
    }

    try {
      FileOutputStream fos = new FileOutputStream(node_backlight);
      fos.write(light);
      fos.close();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public static int getBrightness() {
    byte[] b = new byte[1];
    try {
      FileInputStream fis = new FileInputStream(node_backlight);
      if (fis.read(b) != -1) {
        fis.close();
      } else {
        throw new IOException("Can't read file: " + node_backlight);
      }
      return (int) b[0];
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }
}
