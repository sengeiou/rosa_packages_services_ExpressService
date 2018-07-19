package com.ubtrobot.express;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.protobuf.Int32Value;
import com.ubtrobot.commons.Priority;
import com.ubtrobot.commons.ResponseListener;
import com.ubtrobot.express.listeners.AnimationListener;
import com.ubtrobot.express.protos.Express;
import com.ubtrobot.express.protos.ProgressExpress;
import com.ubtrobot.master.Master;
import com.ubtrobot.master.call.CallConfiguration;
import com.ubtrobot.master.competition.ActivateCallback;
import com.ubtrobot.master.competition.ActivateException;
import com.ubtrobot.master.competition.ActivateOption;
import com.ubtrobot.master.competition.Competing;
import com.ubtrobot.master.competition.CompetingItem;
import com.ubtrobot.master.competition.CompetitionSession;
import com.ubtrobot.master.param.ProtoParam;
import com.ubtrobot.master.service.ServiceProxy;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.provider.ExpressStore;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;
import com.ubtrobot.transport.message.StickyResponseCallback;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ExpressApi
 * <p>email : logic.peng@ubtech.com</p>
 * <p>time : 2017/9/14</p>
 *
 * @author Logic
 */

public final class ExpressApi {
  private static final String TAG = ExpressApi.class.getSimpleName();
  private final Master master = Master.get();
  private final ServiceProxy express;

  private ExpressApi() {
    express = master.getGlobalContext().createSystemServiceProxy("express");
    CallConfiguration configuration =
        new CallConfiguration.Builder().suppressSyncCallOnMainThreadWarning(true).build();
    express.setConfiguration(configuration);
  }

  public static ExpressApi get() {
    return Holder._api;
  }

  /**
   * 获取机器人上支持的表情
   *
   * @return list of Express.ExpressInfo
   */
  public @Nullable List<Express.ExpressInfo> getExpressList() {
    try {
      Uri uri = ExpressStore.Express.getContentUri();
      Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
      if (cursor == null) {
        return Collections.emptyList();
      }
      List<Express.ExpressInfo> expresses = new ArrayList<>(cursor.getCount());
      Express.ExpressInfo info;
      while (cursor.moveToNext()) {
        info = Express.ExpressInfo.newBuilder()
            .setCustomize(false)
            .setDuration(
                cursor.getInt(cursor.getColumnIndex(ExpressStore.Express.Columns.DURATION)))
            .setFormat(Express.ExpressFormat.forNumber(
                cursor.getInt(cursor.getColumnIndex(ExpressStore.Express.Columns.FORMAT))))
            .setFrames(cursor.getInt(cursor.getColumnIndex(ExpressStore.Express.Columns.FRAMES)))
            .setName(
                cursor.getString(cursor.getColumnIndex(ExpressStore.Express.Columns.EXPRESS_ID)))
            .build();
        expresses.add(info);
      }
      cursor.close();
      return expresses;
    } catch (Exception e) {
      //ignore
    }
    return Collections.emptyList();
  }

  private Context mContext;

  private Context getContext() {
    if (mContext != null) return mContext;
    synchronized (this) {
      if (mContext == null) {
        try {
          @SuppressLint("PrivateApi") final Class<?> activityThread =
              Class.forName("android.app.ActivityThread");
          final Method currentApplicationMethod =
              activityThread.getDeclaredMethod("currentApplication");
          mContext = (Context) currentApplicationMethod.invoke(null);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      return mContext;
    }
  }

  /**
   * 做一个表情动作
   *
   * @param name 表情名称
   */
  public void doExpress(@NonNull String name) {
    doExpress(name, 1, Priority.NORMAL);
  }

  /**
   * 做一个表情动作
   *
   * @param name 表情名称
   * @param loopCount 运行次数
   */
  public void doExpress(String name, @IntRange(from = 0, to = Character.MAX_VALUE) int loopCount) {
    doExpress(name, loopCount, true, Priority.NORMAL);
  }

  /**
   * 做一个表情动作
   *
   * @param name 表情名称
   * @param priority 优先级
   * @param loopCount 运行次数
   */
  public void doExpress(String name, @IntRange(from = 0, to = Character.MAX_VALUE) int loopCount,
      Priority priority) {
    doExpress(name, loopCount, true, priority);
  }

  /**
   * 做一个表情动作
   *
   * @param name 表情名称
   * @param loopCount 运行次数
   * @param tweenable 是否使能表情过渡
   * @param priority 优先级 {@link Priority}
   */
  public void doExpress(String name, @IntRange(from = 0, to = Character.MAX_VALUE) int loopCount,
      boolean tweenable, Priority priority) {
    doExpress(name, loopCount, tweenable, priority, null);
  }

  /**
   * 做表情动画，并监听表情动画状态
   *
   * @param name 表情名称
   * @param loopCount 运行次数
   * @param priority 优先级 {@link Priority}
   * @param listener 动画监听
   */
  public void doExpress(String name,
      @IntRange(from = 0, to = Character.MAX_VALUE) final int loopCount, Priority priority,
      @Nullable final AnimationListener listener) {
    doExpress(name, loopCount, true, priority, listener);
  }

  /**
   * 做表情动画，并监听表情动画状态
   *
   * @param name 表情名称
   * @param loopCount 运行次数
   * @param tweenable 是否使能表情过渡
   * @param priority 优先级 {@link Priority}
   * @param listener 动画监听
   */
  public void doExpress(final String name,
      @IntRange(from = 0, to = Character.MAX_VALUE) final int loopCount, final boolean tweenable,
      final Priority priority, @Nullable final AnimationListener listener) {
    final CompetitionSession session = createCompetitionSession();
    session.setActivateOption(new ActivateOption.Builder().setPriority(priority.ordinal()).build());
    session.activate(new ActivateCallback() {
      @Override public void onSuccess(String s) {
        session.createSystemServiceProxy("express")
            .callStickily("/doExpressStickily", ProtoParam.create(Express.DoExpressReq.newBuilder()
                .setName(name)
                .setSpeed(1.0f)
                .setRepeat(loopCount)
                .setTweenable(tweenable)
                .build()), new StickyResponseCallback() {
              @Override public void onResponseStickily(Request request, Response response) {
                try {
                  if (listener == null) return;
                  int loopNumber = ProtoParam.from(response.getParam(), Int32Value.class)
                      .getProtoMessage()
                      .getValue();
                  if (loopNumber == 0) {
                    listener.onAnimationStart();
                  } else {
                    listener.onAnimationRepeat(loopNumber);
                  }
                } catch (ProtoParam.InvalidProtoParamException e) {
                  Log.w(TAG, "" + e.getMessage());
                }
              }

              @Override public void onResponseCompletely(Request request, Response response) {
                session.deactivate();
                if (listener != null) {
                  listener.onAnimationEnd();
                }
              }

              @Override public void onFailure(Request request, CallException e) {
                session.deactivate();
                Log.w(TAG, "" + e.getMessage());
                if (listener != null) {
                  listener.onAnimationEnd();
                }
              }
            });
      }

      @Override public void onFailure(ActivateException e) {
        Log.w(TAG, "ActivateException: " + e.getMessage());
        // TODO: 18-3-22
        if (listener != null) {
          listener.onAnimationEnd();
        }
      }
    });
  }

  /**
   * 做表情动画，并监听表情动画状态
   *
   * @param name 表情名称
   * @param listener 动画监听
   */
  public void doExpress(String name, @Nullable final AnimationListener listener) {
    doExpress(name, 1, true, Priority.NORMAL, listener);
  }

  /**
   * PC仿真调用，显示表情的指定帧
   *
   * @param name 表情名
   * @param frame 帧
   */
  public void setFrame(String name, @IntRange(from = 0) int frame) {
    express.call("/setExpressFrame",
        ProtoParam.create(Express.SetFrameReq.newBuilder().setFrame(frame).setName(name).build()),
        null);
  }

  /**
   * 调用tween动画,恢复到正常
   *
   * @param listener listener
   */
  public void doExpressTween(@Nullable final AnimationListener listener) {
    doExpressTween(Priority.NORMAL, listener);
  }

  /**
   * 主动调用tween动画
   *
   * @param priority 优先级, {@link Priority}
   * @param listener listener
   */
  public void doExpressTween(Priority priority, @Nullable final AnimationListener listener) {
    final CompetitionSession session = createCompetitionSession();
    session.setActivateOption(new ActivateOption.Builder().setPriority(priority.ordinal()).build());
    session.activate(new ActivateCallback() {
      @Override public void onSuccess(String s) {
        express.callStickily("/doExpressTween", new StickyResponseCallback() {

          @Override public void onResponseStickily(Request request, Response response) {
            try {
              if (listener == null) return;
              int loopNumber = ProtoParam.from(response.getParam(), Int32Value.class)
                  .getProtoMessage()
                  .getValue();
              if (loopNumber == 0) {
                listener.onAnimationStart();
              } else {
                listener.onAnimationRepeat(loopNumber);
              }
            } catch (ProtoParam.InvalidProtoParamException e) {
              Log.w(TAG, "" + e.getMessage());
            }
          }

          @Override public void onResponseCompletely(Request request, Response response) {
            session.deactivate();
            if (listener != null) {
              listener.onAnimationEnd();
            }
          }

          @Override public void onFailure(Request request, CallException e) {
            session.deactivate();
            if (listener != null) {
              listener.onAnimationEnd();
            }
          }
        });
      }

      @Override public void onFailure(ActivateException e) {
        Log.w(TAG, "ActivateException: " + e.getMessage());
        // TODO: 18-3-22
        if (listener != null) listener.onAnimationEnd();
      }
    });
  }

  private CompetitionSession createCompetitionSession() {
    return master.getGlobalContext().openCompetitionSession().addCompeting(new Competing() {
      @Override public List<CompetingItem> getCompetingItems() {
        return Collections.singletonList(new CompetingItem("express", "eye-all"));
      }
    });
  }

  private static final class Holder {
    private static ExpressApi _api = new ExpressApi();
  }

  /**
   * 圆形进度条动画表情
   *
   * @param status 0 非充电 ,1 充电
   * @param progress 当前进度
   * @param priority 优先级, {@link Priority}
   * @param animated 是否动画
   * @param listener listener
   */
  public void doProgressExpress(@IntRange(from = 0, to = 1) final int status,
      final @IntRange(from = 0, to = 100) int progress, final boolean animated, Priority priority,
      @Nullable final AnimationListener listener) {
    final CompetitionSession session = createCompetitionSession();
    session.setActivateOption(new ActivateOption.Builder().setPriority(priority.ordinal()).build());
    session.activate(new ActivateCallback() {
      @Override public void onFailure(ActivateException e) {
        if (listener != null) listener.onAnimationEnd();
      }

      @Override public void onSuccess(String s) {
        session.createSystemServiceProxy("express")
            .callStickily("/doProgressExpress", ProtoParam.create(
                ProgressExpress.DoProgressExpressReq.newBuilder()
                    .setName("show_battery_info")
                    .setStatus(status)
                    .setProgress(progress)
                    .setAnimated(animated)
                    .build()), new StickyResponseCallback() {

              @Override public void onResponseStickily(Request request, Response response) {
                try {
                  if (listener == null) return;
                  int loopNumber = ProtoParam.from(response.getParam(), Int32Value.class)
                      .getProtoMessage()
                      .getValue();
                  if (loopNumber == 0) {
                    listener.onAnimationStart();
                  } else {
                    listener.onAnimationRepeat(loopNumber);
                  }
                } catch (ProtoParam.InvalidProtoParamException e) {
                  Log.w(TAG, "" + e.getMessage());
                }
              }

              @Override public void onResponseCompletely(Request request, Response response) {
                session.deactivate();
                if (listener != null) {
                  listener.onAnimationEnd();
                }
              }

              @Override public void onFailure(Request request, CallException e) {
                session.deactivate();
                if (listener != null) {
                  listener.onAnimationEnd();
                }
              }
            });
      }
    });
  }
}