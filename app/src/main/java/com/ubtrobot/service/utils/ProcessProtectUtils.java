package com.ubtrobot.service.utils;

import android.content.Context;
import android.content.Intent;

/**
 * @author : kevin.liu@ubtrobot.com
 * @description :
 * @date : 2018/5/16
 * @modifier :
 * @modify time :
 */
public class ProcessProtectUtils {

  public final static String PROCESS_REGISTER_ACTION = "com.ubt.process.register.action";
  private final static String PKG_NAME = "pkg_name";
  private final static String PID = "pid";
  private final static String RESTART_COMPONENT_NAME = "restart_comp_name"; //需要重启的组件的名称
  private final static String RESTART_COMPONENT_TYPE = "restart_comp_type";

  public static void sendProcessRegisterBroadcast(Context context) {
    Intent intent = new Intent();
    intent.setAction(PROCESS_REGISTER_ACTION);
    intent.putExtra(PID, android.os.Process.myPid());
    intent.putExtra(PKG_NAME, context.getPackageName());
    intent.putExtra(RESTART_COMPONENT_NAME, "com.ubtrobot.service.ExpressService");
    intent.putExtra(RESTART_COMPONENT_TYPE, "Service");
    context.sendBroadcast(intent);
  }
}
