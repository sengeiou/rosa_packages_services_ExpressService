package com.ubtrobot.service.app;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.ubtech.utilcode.utils.AppUtils;
import com.ubtech.utilcode.utils.LogUtils;
import com.ubtech.utilcode.utils.ProcessUtils;
import com.ubtech.utilcode.utils.bugly.CrashReporter;
import com.ubtrobot.master.Master;
import com.ubtrobot.mini.properties.sdk.Path;
import com.ubtrobot.mini.properties.sdk.PropertiesApi;
import com.ubtrobot.service.BuildConfig;
import com.ubtrobot.service.express.ExpressServiceImpl;
import com.ubtrobot.sys.SysApi;
import com.ubtrobot.ulog.ULog;
import com.ubtrobot.ulog.logger.android.AndroidLoggerFactory;
import java.util.Map;
import timber.log.Timber;

/**
 * 表情服务application
 *
 * @author Logic
 */

public class ExpressApplication extends Application {

  @SuppressLint("StaticFieldLeak") private static Context mContext = null;

  public static Context getContext() {
    return mContext;
  }

  private synchronized void setContext(Context context) {
    ExpressApplication.mContext = context;
  }

  @Override public void onCreate() {
    super.onCreate();
    if (ProcessUtils.isMainProcess(this)) {
      //设置全局上下文
      setContext(this.getApplicationContext());
      if (BuildConfig.SDResourceEnable) {
        PropertiesApi.setRootPath(Path.DIR_MINI_FILES_SDCARD_ROOT);
      }
      //setprop log.tag.Speech VERBOSE 可打开v级别，默认是i级别
      LogUtils.init(true, true, "Emoji");
      initTimber();
      //版本信息日志
      LogUtils.I("%s", AppUtils.getAppInfo(this));
      //初始化表情服务
      //初始化，以“服务”形式集成至服务总线
      Master.initialize(this);
      // 配置总线日志工厂
      //Master.get().setLoggerFactory(new AndroidLoggerFactory());
      ULog.setup("Logic", new AndroidLoggerFactory());
      ExpressServiceImpl.get();
      //应用第一次启动，必须刷新缓存
      //NotificationCenter.defaultCenter().publish(new RefreshCacheEvent());
      CrashReporter.init(this, "e8788155c8", BuildConfig.DEBUG, 10000,
          new CrashReporter.onCrashHandler() {
            @Override public String getRobotId() {
              return SysApi.get().readRobotSid();
            }

            @Override public Map<String, String> onCrashHappend() {
              return null;
            }
          });
    }
  }

  private void initTimber() {
    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
    } else {
      Timber.plant(new Timber.Tree() {
        @Override protected void log(int priority, String tag, String message, Throwable t) {
          if (priority >= Log.WARN) {
            LogUtils.w(message);
          }
        }
      });
    }
  }
}
