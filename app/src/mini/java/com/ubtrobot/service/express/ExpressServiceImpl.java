package com.ubtrobot.service.express;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import com.airbnb.lottie.ExpressImageAssetDelegate;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieDrawable;
import com.ubtech.utilcode.utils.LogUtils;
import com.ubtrobot.cpdrawable.CircleProgressDrawable;
import com.ubtrobot.cpdrawable.RotateAnimator;
import com.ubtrobot.express.protos.Express;
import com.ubtrobot.giflib.GifDecoder;
import com.ubtrobot.giflib.GifDrawable;
import com.ubtrobot.giflib.GifInfoHandle;
import com.ubtrobot.giflib.InputSource;
import com.ubtrobot.mini.properties.sdk.PropertiesApi;
import com.ubtrobot.mini.spi.IAnimationDrawable;
import com.ubtrobot.mini.spi.SpiRefresher;
import com.ubtrobot.service.R;
import com.ubtrobot.service.app.DrawableCache;
import com.ubtrobot.service.app.ExpressApplication;
import com.ubtrobot.service.app.GifCache;
import com.ubtrobot.service.app.LottieCache;
import com.ubtrobot.service.express.utils.PngUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static com.airbnb.lottie.model.layer.CompomentUtil.TWEEN_DURATION;

/**
 * ExpressServiceImpl
 *
 * @author logic.peng
 */

public final class ExpressServiceImpl implements IExpressInterface {
  private static final String TAG = "Logic";
  private static final Express.ExpressInfo TWEEN = Express.ExpressInfo.newBuilder()
      .setName("TWEEN")
      .setFormat(Express.ExpressFormat.JSON)
      .setDuration(400)
      .setIntrinsicWidth(240)
      .setFrames(10)
      .setCustomize(false)
      .build();
  private final File express_dir = new File(PropertiesApi.getRootPath(), "expresss");
  private final File customize_express_dir =
      new File(Environment.getExternalStorageDirectory().getPath() + "/customize", "expresss");
  private final byte[] mSyncLock = new byte[0];
  private final DrawableCache cache;
  private final Pattern pattern = Pattern.compile(".*(gif|png|json)$");
  //<expressName, ExpressInfo>
  private final HashMap<String, Express.ExpressInfo> expressMap = new HashMap<>();
  private final HashMap<String, Express.ExpressInfo> customizeExpressMap = new HashMap<>();
  private final ConcurrentLinkedQueue<AnimationListener> listeners = new ConcurrentLinkedQueue<>();
  //<expressName, GifDecoder>
  private final GifCache gifMap = new GifCache();
  private final GifCache customizeGifMap = new GifCache();
  //<expressName, LottieComposition>
  private final LottieCache lottieMap = new LottieCache();
  private final LottieCache customizeLottieMap = new LottieCache();
  //private final AtomicBoolean rebuilding = new AtomicBoolean(false);
  //private final AtomicBoolean inited = new AtomicBoolean(false);
  private final float scale = Resources.getSystem().getDisplayMetrics().density;
  private final AtomicBoolean tweening = new AtomicBoolean(false);
  private final List<AnimationListener> doTweenListeners = new ArrayList<>();
  private volatile Express.ExpressInfo runningExpress;
  //当tween动画时，有可能有新的pending表情，默认替换pendingExpress
  private volatile Pair<Express.ExpressInfo, HolderInfo> pendingExpressPair;
  private Handler drawTweenHandler;

  private ExpressServiceImpl() {
    Context context = ExpressApplication.getContext();
    cache = new DrawableCache(context);
    try {
      SpiRefresher.get().init();
    } catch (IOException e) {
      e.printStackTrace();
    }
    //Subscriber<RefreshCacheEvent> subscriber = new Subscriber<RefreshCacheEvent>() {
    //  @Override public void onEvent(final RefreshCacheEvent e) {
    //    ThreadPool.runOnNonUIThread(new Runnable() {
    //      @Override public void run() {
    //        if (e.fileName == null) {
    //          if (!rebuilding.getAndSet(true)) rebuildExpressList();
    //        } else {
    //          try {
    //            synchronized (mSyncLock) {
    //              File file = new File(e.fileName);
    //              Express.ExpressInfo info = scanResult(file, file.getName(), false);
    //              expressMap.put(info.getName(), info);
    //            }
    //          } catch (IOException e1) {
    //            Log.e("express", "scan fail error: " + e1.getMessage());
    //          }
    //        }
    //      }
    //    });
    //  }
    //};
    //NotificationCenter.defaultCenter().subscriber(RefreshCacheEvent.class, subscriber);
    Runtime.getRuntime().gc();
    HandlerThread tweenThread = new HandlerThread("DrawTweenThread");
    tweenThread.start();
    drawTweenHandler = new Handler(tweenThread.getLooper());
  }

  public static ExpressServiceImpl get() {
    return ExpressServiceImpl.Holder._instance;
  }

  @Override public int doExpress(final Express.DoExpressReq req) {
    Express.ExpressInfo info = findExpress(req);
    if (info == null) {
      Log.e(TAG, "**express:" + req.getName() + " not found!");
      return -1;//not found
    }
    Log.v(TAG, "receive express: " + req.getName() + ", customize: " + req.getCustomize());
    return refresh(info, Math.max(0, req.getRepeat()), Math.max(req.getSpeed(), 1.0f),
        req.getTweenable(), null);
  }

  private @Nullable Express.ExpressInfo findExpress(Express.DoExpressReq req) {
    final String expressName = req.getName();
    final boolean customize = req.getCustomize();
    Express.ExpressInfo info =
        customize ? customizeExpressMap.get(expressName) : expressMap.get(expressName);
    final File expressDir = customize ? customize_express_dir : express_dir;

    if (info == null) {
      File file = new File(expressDir, expressName + ".gif");
      if (!file.exists()) file = new File(expressDir, expressName + ".json");

      if (file.exists()) {
        try {
          synchronized (mSyncLock) {
            info = scanResult(file, file.getName(), customize);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        file = new File(expressDir, expressName + ".png");
        if (file.exists()) {
          info = Express.ExpressInfo.newBuilder()
              .setName(expressName)
              .setDuration(0)
              .setFrames(1)
              .setFormat(Express.ExpressFormat.PNG)
              .setIntrinsicWidth(240)
              .setCustomize(customize)
              .build();
        }
      }
    }
    return info;
  }

  @Override
  public int doExpress(final Express.DoExpressReq req, @NonNull final AnimationListener listener) {
    Express.ExpressInfo info = findExpress(req);
    if (info == null) {
      Log.e(TAG, "##express:" + req.getName() + " not found!");
      return -1;//not found
    }
    Log.v(TAG, "receive express: " + req.getName() + ", customize: " + req.getCustomize());

    return refresh(info, Math.max(0, req.getRepeat()), Math.max(req.getSpeed(), 1.0f),
        req.getTweenable(), listener);
  }

  private int refresh(final Express.ExpressInfo express, final int loopCount, final float speed,
      boolean tweenable, @Nullable final AnimationListener listener) {
    HolderInfo holder = new HolderInfo();
    holder.listener = listener;
    holder.loopCount = loopCount;
    holder.speed = speed;
    return tweenWithPendingTask(new Pair<>(express, holder),
        new FutureTask<>(new Callable<Integer>() {
          @Override public Integer call() throws Exception {
            if (tweening.compareAndSet(true, false)) {
              Log.v(TAG, "tween finished...");
              for (AnimationListener listener : doTweenListeners) {
                listener.onAnimationEnd();
              }
              doTweenListeners.clear();
            }

            synchronized (mSyncLock) {
              if (ExpressServiceImpl.this.pendingExpressPair != null) {
                final Express.ExpressInfo express = pendingExpressPair.first;
                final AnimationListener pendingListener =
                    ExpressServiceImpl.this.pendingExpressPair.second.listener;
                int loopCount = ExpressServiceImpl.this.pendingExpressPair.second.loopCount;
                float speed = ExpressServiceImpl.this.pendingExpressPair.second.speed;
                ExpressServiceImpl.this.pendingExpressPair = null;
                return handleOneExpress(express, loopCount, speed, pendingListener);
              } else {
                return handleOneExpress(express, loopCount, speed, listener);
              }
            }
          }
        }), tweenable);
  }

  private int tweenWithPendingTask(final Pair<Express.ExpressInfo, HolderInfo> pendingExpressPair,
      @NonNull final FutureTask<Integer> pendingTask, boolean tweenable) {
    synchronized (mSyncLock) {
      if (runningExpress != null) {
        IAnimationDrawable runningDrawable = cache.getItem(runningExpress);
        final boolean runningIsGif = runningExpress.getFormat() == Express.ExpressFormat.GIF;
        final boolean runningIsProgress =
            runningExpress.getFormat() == Express.ExpressFormat.CODE_DRAW;
        final boolean isRunning;
        final Express.ExpressInfo info = runningExpress;
        runningExpress = null;
        if (runningDrawable != null) {
          //停止前一个
          isRunning = runningDrawable.isAnimationRunning();
          runningDrawable.stopAnimation();

          if (tweenable && isRunning && tweening.compareAndSet(false, true)) {
            //需要转场情况且前一个非转场表情正在运行且没有进入转场
            this.pendingExpressPair = pendingExpressPair;
            if (runningIsGif) {
              Log.v(TAG, "tween animation happen...");
              return doTweenAnimation(pendingTask, pendingExpressPair.second.listener);
            } else if (runningIsProgress) {
              return doTweenAnimation(pendingTask, pendingExpressPair.second.listener);
            } else {
              Log.v(TAG, "interpolator animation happen...");
              LottieDrawable oldDrawable = (LottieDrawable) runningDrawable;
              return doInterpolatorAnimation(info, oldDrawable, pendingTask,
                  pendingExpressPair.second.listener);
            }
          }
        }
      }

      if (tweening.get()) {
        if (this.pendingExpressPair != null && this.pendingExpressPair.second.listener != null) {
          Log.w(TAG, "express: "
              + this.pendingExpressPair.first.getName()
              + " be interrupted and not running!");
          this.pendingExpressPair.second.listener.onAnimationEnd();
        }
        this.pendingExpressPair = pendingExpressPair;
        return 0;
      } else {
        Log.w(TAG, "express no need tween.");
        pendingTask.run();
        try {
          // 在Call 调用线程中处理错误码
          return pendingTask.get();
        } catch (InterruptedException | ExecutionException e) {
          //被异常中断, 不应该发生的
          LogUtils.e(TAG, "never come to here....111");
        }
        return -4;
      }
    }
  }

  private Integer doInterpolatorAnimation(Express.ExpressInfo express, LottieDrawable oldDrawable,
      @NonNull final FutureTask<Integer> pendingTask, final @Nullable AnimationListener listener) {
    float progress = oldDrawable.getProgress();

    LottieComposition interpolatorLottie = null;
    try {
      interpolatorLottie = LottieComposition.Factory.fromInputStreamSync(
          new FileInputStream(new File(express_dir, express.getName() + ".json")));
    } catch (FileNotFoundException e) {
      LogUtils.e("pending json express parse fail...");
    }

    if (interpolatorLottie == null) {
      pendingTask.run();
      try {
        return pendingTask.get();
      } catch (InterruptedException | ExecutionException e) {
        //被异常中断, 不应该发生的
        LogUtils.e(TAG, "never come to here....2222");
      }
      return -4;
    }

    float endFrame = interpolatorLottie.getDurationFrames() * progress
        + TWEEN_DURATION / interpolatorLottie.getFrameRate();
    interpolatorLottie.tween(progress);

    final LottieDrawable interpolatorDrawable = new LottieDrawable();
    //如果LottieImageAsset.getDirName().equals("Images/"),代表使用通用资源
    interpolatorDrawable.setImagesAssetsFolder("Images/");
    //否则,从SD卡"expresss/LottieImageAsset.getDirName()"中找资源
    interpolatorDrawable.setImageAssetDelegate(new ExpressImageAssetDelegate());
    interpolatorDrawable.setComposition(interpolatorLottie);
    interpolatorDrawable.setProgress(progress);
    //setProgress() tween()方法需要根据前一个endFrame更新数据，
    //setEndFrame 放在最后面
    //TODO: 18-3-22 需要优化
    interpolatorLottie.setEndFrame(endFrame);
    interpolatorDrawable.addAnimatorListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animation) {
      }

      @Override public void onAnimationEnd(Animator animation) {
        doPendingTaskWithListener(pendingTask, listener);
      }

      @Override public void onAnimationCancel(Animator animation) {
        doPendingTaskWithListener(pendingTask, listener);
      }

      @Override public void onAnimationRepeat(Animator animation) {
      }
    });
    Log.v(TAG, "interpolator animation start...");
    drawTweenHandler.post(new Runnable() {
      @Override public void run() {
        interpolatorDrawable.resumeAnimation();
      }
    });
    //interpolatorDrawable.resumeAnimation();
    return 0;
  }

  private Integer doTweenAnimation(@NonNull final FutureTask<Integer> pendingTask,
      @Nullable final AnimationListener listener) {
    final LottieDrawable tweenDrawable = lazyGetTweenDrawable();
    LottieComposition composition = tweenDrawable.getComposition();
    if (composition != null) {
      Log.w(TAG, "json-start : "
          + TWEEN
          + ".json"
          + "->[time: "
          + composition.getDuration()
          + ",frameCount: "
          + composition.getDurationFrames()
          + ",loopCount: 1"
          + "]");
      drawTweenHandler.post(new Runnable() {
        @Override public void run() {
          tweenDrawable.playAnimation();
        }
      });
      //tweenDrawable.playAnimation();
      setupJsonEyeDrawableCallback(TWEEN.getName(), new AnimationListener() {
        @Override public void onAnimationStart() {
        }

        @Override public void onAnimationEnd() {
          doPendingTaskWithListener(pendingTask, listener);
        }

        @Override public void onAnimationRepeat(int loopNumber) {
        }
      }, tweenDrawable);
      return 0;
    }
    //tween.json 没有放在apk内，或者解析失败
    return -1;
  }

  @NonNull private LottieDrawable lazyGetTweenDrawable() {
    LottieDrawable drawable = (LottieDrawable) cache.getItem(TWEEN);
    if (drawable == null) {
      LottieComposition composition = lottieMap.get(TWEEN.getName());
      if (composition == null) {
        composition = LottieComposition.Factory.fromInputStreamSync(
            ExpressApplication.getContext().getResources().openRawResource(R.raw.tween));
        lottieMap.put(TWEEN.getName(), composition);
      } else {
        Log.v(TAG, "hint tween decoder...");
      }
      drawable = new LottieDrawable();
      drawable.setImagesAssetsFolder("Images/");//资源放在APK asset
      drawable.setComposition(composition);
      cache.addItem(TWEEN, drawable);
    }
    return drawable;
  }

  private void doPendingTaskWithListener(@NonNull FutureTask<Integer> pendingTask,
      @Nullable AnimationListener listener) {
    pendingTask.run();
    int resultCode;
    try {
      resultCode = pendingTask.get();
    } catch (InterruptedException | ExecutionException e) {
      //被异常中断, 不应该发生的
      LogUtils.w(TAG, "!!!never come to here...!!!");
      resultCode = -4;
    }
    if (resultCode != 0 && listener != null) {
      //表情文件不存在，或者解析失败
      listener.onAnimationEnd();
    }
  }

  @NonNull private Integer handleOneExpress(Express.ExpressInfo express, int loopCount, float speed,
      @Nullable AnimationListener listener) {
    int resultCode;
    if (express.getFormat() == Express.ExpressFormat.GIF) {
      //gif
      resultCode = handleGifEye(express, loopCount, speed, listener) ? 0 : -1;
    } else if (express.getFormat() == Express.ExpressFormat.JSON) {
      //json
      resultCode = handleJSONEye(express, loopCount, speed, listener) ? 0 : -1;
    } else {
      if (listener != null) {
        listener.onAnimationStart();
      }
      handlePNGEye(express.getName() + ".png");

      if (listener != null) {
        listener.onAnimationEnd();
      }
      resultCode = 0;
    }
    return resultCode;
  }

  //=========================gif==============================
  private boolean handleGifEye(Express.ExpressInfo express, final int loopCount, float speed,
      @Nullable final AnimationListener listener) {
    final String expressName = express.getName();
    GifDrawable gifDrawable = (GifDrawable) cache.getItem(express);
    if (gifDrawable == null) {
      gifDrawable = createGifDrawable(expressName);
    } else if (isWrongDrawable(gifDrawable)) {
      gifMap.remove(expressName);
      cache.removeItem(express);
      gifDrawable = createGifDrawable(expressName);
    }

    if (gifDrawable == null) return false;
    cache.addItem(express, gifDrawable);
    gifDrawable.setLoopCount(loopCount);
    gifDrawable.setSpeed(speed);
    gifDrawable.setVisible(true, true);
    String fileName = expressName + ".gif";
    Log.w(TAG, "gif-start : "
        + fileName
        + "->[time: "
        + gifDrawable.getDuration()
        + ",frameCount: "
        + gifDrawable.getNumberOfFrames()
        + ",loopCount: "
        + gifDrawable.getLoopCount()
        + "]");

    setupGifEyeDrawableCallback(fileName, listener, gifDrawable);
    runningExpress = express;
    return true;
  }

  private boolean isWrongDrawable(GifDrawable gifDrawable) {
    return gifDrawable.getDuration() == 0 || gifDrawable.getNumberOfFrames() == 0;
  }

  private GifDrawable createGifDrawable(String expressName) {
    GifDrawable gifDrawable;
    try {
      File file = new File(express_dir, expressName + ".gif");
      if (file.exists()) {
        GifDecoder decoder = gifMap.get(expressName);
        GifInfoHandle handle;
        if (decoder == null) {
          decoder = new GifDecoder(new InputSource.FileSource(file));
          gifMap.put(expressName, decoder);
          handle = decoder.getHandle();
        } else {
          handle = decoder.getHandle();
          //TODO 优化
          if (handle.isRecycled()) {
            gifMap.remove(expressName);
            decoder = new GifDecoder(new InputSource.FileSource(file));
            gifMap.put(expressName, decoder);
            handle = decoder.getHandle();
          } else {
            Log.v(TAG, "hint gif decoder...");
          }
        }
        gifDrawable = new GifDrawable(handle, null, null, false);
        return gifDrawable;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void setupGifEyeDrawableCallback(final String fileName,
      @Nullable final AnimationListener listener, final GifDrawable gifDrawable) {
    if (listener != null) {
      listener.onAnimationStart();
      gifDrawable.addAnimationListener(new com.ubtrobot.giflib.AnimationListener() {
        @Override public void onAnimationCompleted(int loopNumber) {
          if (gifDrawable.getLoopCount() != loopNumber) {
            Log.d(TAG, "gif-loop: "
                + fileName
                + "->[loopCount:"
                + gifDrawable.getLoopCount()
                + " , loopNumber = "
                + loopNumber
                + "]");
            listener.onAnimationRepeat(loopNumber);
          } else {
            synchronized (mSyncLock) {
              if (listeners.contains(listener)) {
                Log.w(TAG, "gif-end: "
                    + fileName
                    + "->[loopCount:"
                    + gifDrawable.getLoopCount()
                    + " , loopNumber = "
                    + loopNumber
                    + "]");
                listeners.remove(listener);
                //Don't call gifDrawable.removeAllAnimatorListeners();
                //否则可能导致收不到回调
                gifDrawable.removeAnimationListener(this);
                listener.onAnimationEnd();
              }
            }
          }
        }
      });
      listeners.add(listener);
    }
  }

  //===============================json=====================================
  private boolean handleJSONEye(Express.ExpressInfo express, int loopCount, float speed,
      AnimationListener listener) {
    final String expressName = express.getName();
    LottieDrawable lottieDrawable = (LottieDrawable) cache.getItem(express);
    if (lottieDrawable == null) {
      lottieDrawable = createLottieDrawable(expressName);
    }
    if (lottieDrawable == null) return false;
    cache.addItem(express, lottieDrawable);
    LottieComposition composition = lottieDrawable.getComposition();
    if (composition != null) {
      Log.w(TAG, "json-start : "
          + expressName
          + ".json"
          + "->[time: "
          + composition.getDuration()
          + ",frameCount: "
          + composition.getDurationFrames()
          + ",loopCount: "
          + (lottieDrawable.getRepeatCount() + 1)
          + "]");
    } else {
      Log.e(TAG, "ExpressDrawable without Composition...");
    }
    lottieDrawable.setRepeatCount(Math.max(0, loopCount - 1));
    lottieDrawable.setSpeed(speed);
    lottieDrawable.playAnimation();
    setupJsonEyeDrawableCallback(express.getName(), listener, lottieDrawable);
    runningExpress = express;
    return true;
  }

  private LottieDrawable createLottieDrawable(String expressName) {
    LottieDrawable lottieDrawable;
    try {
      File file = new File(express_dir, expressName + ".json");
      if (file.exists()) {
        LottieComposition composition = lottieMap.get(expressName);
        if (composition == null) {
          composition = LottieComposition.Factory.fromInputStreamSync(new FileInputStream(file));
        } else {
          Log.v(TAG, "hint json decoder...");
        }
        lottieDrawable = new LottieDrawable();
        //如果LottieImageAsset.getDirName().equals("Images/"),代表使用通用资源
        lottieDrawable.setImagesAssetsFolder("Images/");
        //否则,从SD卡"expresss/LottieImageAsset.getDirName()"中找资源
        lottieDrawable.setImageAssetDelegate(new ExpressImageAssetDelegate());
        lottieDrawable.setComposition(composition);
        return lottieDrawable;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private void setupJsonEyeDrawableCallback(final String name,
      @Nullable final AnimationListener listener, final LottieDrawable lottieDrawable) {
    if (listener != null) {
      listener.onAnimationStart();//onStart
      lottieDrawable.addAnimatorListener(new Animator.AnimatorListener() {
        @Override public void onAnimationStart(Animator animation) {
          //ValueAnimator onStart只会调用一次，所以重用的Drawable onStart不回调
        }

        @Override public void onAnimationEnd(Animator animation) {
          synchronized (mSyncLock) {
            if (listeners.contains(listener)) {
              Log.w(TAG, "json-end: "
                  + name
                  + ".json->[loopCount:"
                  + (lottieDrawable.getRepeatCount() + 1)
                  + " , loopNumber = "
                  + (lottieDrawable.getRepeatCount() + 1)
                  + "]");
              listeners.remove(listener);
              //Don't call lottieDrawable.removeAllAnimatorListeners();
              lottieDrawable.removeAnimatorListener(this);
              listener.onAnimationEnd();
            }
          }
        }

        @Override public void onAnimationCancel(Animator animation) {
          synchronized (mSyncLock) {
            if (listeners.contains(listener)) {
              Log.w(TAG, "json-end: "
                  + name
                  + ".json->[loopCount:"
                  + (lottieDrawable.getRepeatCount() + 1)
                  + " , loopNumber = 1"
                  //todo
                  + "]");
              listeners.remove(listener);
              //Don't call lottieDrawable.removeAllAnimatorListeners();
              lottieDrawable.removeAnimatorListener(this);
              listener.onAnimationEnd();
            }
          }
        }

        @Override public void onAnimationRepeat(Animator animation) {
          synchronized (mSyncLock) {
            if (listeners.contains(listener)) {
              listener.onAnimationRepeat(lottieDrawable.getRepeatCount() - 1);
            }
          }
        }
      });
      listeners.add(listener);
    }
  }

  //===============================PNG=============================
  private void handlePNGEye(String fileName) {
    File file = new File(express_dir, fileName);
    if (!file.exists()) {
      Log.w(TAG, "bitmap：" + fileName + " not existed.");
      return;
    }
    PngUtils.refreshWidthPng(file);
  }

  @Override public Collection<Express.ExpressInfo> getExpressList() {
    return Collections.unmodifiableCollection(expressMap.values());
  }

  //private void rebuildExpressList() {
  //  List<File> files = FileUtils.listFilesInDir(express_dir, false);
  //  if (files != null) {
  //    synchronized (mSyncLock) {
  //      expressMap.clear();
  //    }
  //    for (File file : files) {
  //      String fileName = file.getName();
  //      if (pattern.matcher(fileName).matches()) {
  //        synchronized (mSyncLock) {
  //          try {
  //            Express.ExpressInfo info = scanResult(file, fileName, false);
  //            expressMap.put(info.getName(), info);
  //          } catch (Exception e) {
  //            LogUtils.e(TAG, "rebuildExpressList: " + e.getMessage());
  //          }
  //        }
  //      }
  //    }
  //  }
  //  synchronized (mSyncLock) {
  //    if (!inited.get()) {
  //      inited.set(true);
  //    }
  //    rebuilding.set(false);
  //  }
  //}

  @NonNull private Express.ExpressInfo scanResult(File file, String fileName, boolean customize)
      throws IOException {
    final String expressName = fileName.substring(0, file.getName().lastIndexOf("."));
    Express.ExpressInfo.Builder builder = Express.ExpressInfo.newBuilder().setName(expressName);
    if (fileName.endsWith(".gif")) {
      GifDecoder decoder = new GifDecoder(new InputSource.FileSource(file));
      builder.setName(expressName)
          .setFrames(decoder.getNumberOfFrames())
          .setDuration(decoder.getDuration())
          .setFormat(Express.ExpressFormat.GIF)
          .setIntrinsicWidth((int) (decoder.getWidth() * scale))
          .setCustomize(customize);
      if (runningExpress == null || !builder.build().equals(runningExpress)) {
        if (!removeOldDrawable(builder.build())) {
          //说明该表情没有被播放过，缓存的GifDecoder需要recycle
          GifDecoder cacheDecoder = gifMap.get(expressName);
          if (cacheDecoder != null) {
            cacheDecoder.recycle();
          }
        }
      }
      if (customize) {
        customizeGifMap.put(expressName, decoder);
      } else {
        gifMap.put(expressName, decoder);
      }
    } else if (fileName.endsWith(".json")) {
      LottieComposition composition =
          LottieComposition.Factory.fromInputStreamSync(new FileInputStream(file));
      if (composition != null) {
        builder.setName(expressName)
            .setDuration(Math.round(composition.getDuration()))
            .setFrames(Math.round(composition.getDurationFrames()))
            .setFormat(Express.ExpressFormat.JSON)
            .setIntrinsicWidth(composition.getBounds().width())
            .setCustomize(customize);
        if (runningExpress == null || !builder.build().equals(runningExpress)) {
          removeOldDrawable(builder.build());
        }
        if (customize) {
          customizeLottieMap.put(expressName, composition);
        } else {
          lottieMap.put(expressName, composition);
        }
      } else {
        LogUtils.e(TAG, "file[" + file.getPath() + "] parse fail!");
      }
    } else {
      builder.setName(expressName)
          .setDuration(0)
          .setFormat(Express.ExpressFormat.PNG)
          .setCustomize(customize);
    }
    return builder.build();
  }

  private boolean removeOldDrawable(Express.ExpressInfo express) {
    //cache entryRemoved 会让动画停止,并recycle
    return cache.removeItem(express) != null;
  }

  @Override public int setFrame(Express.SetFrameReq frameReq) {
    String expressName = frameReq.getName();
    Express.ExpressInfo info = expressMap.get(expressName);
    final File expressDir = express_dir;
    if (info == null) {
      File file = new File(expressDir, expressName + ".gif");
      if (!file.exists()) file = new File(expressDir, expressName + ".json");

      if (file.exists()) {
        try {
          synchronized (mSyncLock) {
            info = scanResult(file, file.getName(), false);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        file = new File(expressDir, expressName + ".png");
        if (file.exists()) {
          info = Express.ExpressInfo.newBuilder()
              .setName(expressName)
              .setDuration(0)
              .setFrames(1)
              .setFormat(Express.ExpressFormat.PNG)
              .setIntrinsicWidth(240)
              .setCustomize(false)
              .build();
        }
      }
    }
    if (info == null) {
      return -1;
    }
    Log.i(TAG, "set Frame : " + frameReq.getFrame());
    if (info.getFormat() == Express.ExpressFormat.GIF) {
      GifDrawable gifDrawable = (GifDrawable) cache.getItem(info);
      if (gifDrawable == null) {
        synchronized (mSyncLock) {
          gifDrawable = createGifDrawable(expressName);
        }
      }
      if (gifDrawable == null) return -1;
      synchronized (mSyncLock) {
        cache.addItem(info, gifDrawable);
      }
      gifDrawable.seekToFrame(frameReq.getFrame());
    } else if (info.getFormat() == Express.ExpressFormat.JSON) {
      LottieDrawable lottieDrawable = (LottieDrawable) cache.getItem(info);
      if (lottieDrawable == null) {
        synchronized (mSyncLock) {
          lottieDrawable = createLottieDrawable(expressName);
        }
      }
      if (lottieDrawable == null) return -1;
      synchronized (mSyncLock) {
        cache.addItem(info, lottieDrawable);
      }
      lottieDrawable.setFrame(frameReq.getFrame());
    } else {
      handlePNGEye(info.getName() + ".png");
    }
    return 0;
  }

  /**
   * 只做tween, 相当于将表情恢复正常
   */
  @Override public void doTween(final @NonNull AnimationListener tweenListener) {
    synchronized (mSyncLock) {
      Log.v("Logic", "doExpressTween...");
      if (runningExpress != null) {
        IAnimationDrawable runningDrawable = cache.getItem(runningExpress);
        final boolean runningIsGif = runningExpress.getFormat() == Express.ExpressFormat.GIF;
        final boolean runningIsProgress =
            runningExpress.getFormat() == Express.ExpressFormat.CODE_DRAW;
        final boolean isRunning;
        final Express.ExpressInfo info = runningExpress;
        runningExpress = null;
        if (runningDrawable != null) {
          //停止前一个
          isRunning = runningDrawable.isAnimationRunning();
          runningDrawable.stopAnimation();
          //前一个非转场表情正在运行且没有进入转场
          if (isRunning && tweening.compareAndSet(false, true)) {
            tweenListener.onAnimationStart();
            int resultCode;
            if (runningIsGif) {
              Log.v(TAG, "do Tween-> tween animation happen...");
              resultCode = doTweenAnimation(createTweenPendingTask(tweenListener), null);
            } else if (runningIsProgress) {
              PngUtils.refreshWidthPng(ExpressApplication.getContext().getResources(),
                  R.raw.normal);
              tweenListener.onAnimationEnd();
              return;
            } else {
              Log.v(TAG, "do Tween -> interpolator animation happen...");
              LottieDrawable oldDrawable = (LottieDrawable) runningDrawable;
              resultCode =
                  doInterpolatorAnimation(info, oldDrawable, createTweenPendingTask(tweenListener),
                      null);//createTweenPendingTask中处理了tweenListener
            }

            if (resultCode != 0) {
              //tween表情不存在或解析失败，tween内置在APK,如果这都失败。。。
              //直接回调, tween失败也会让PendingTask.run
              tweenListener.onAnimationEnd();
            }
            return;
          }
        }
      }

      if (tweening.get()) {
        Log.w(TAG, "it's in tweening");
        tweenListener.onAnimationStart();
        doTweenListeners.add(tweenListener);
      } else {
        tweenListener.onAnimationEnd();
      }
    }
  }

  @NonNull private FutureTask<Integer> createTweenPendingTask(
      @NonNull final AnimationListener tweenlistener) {
    return new FutureTask<>(new Callable<Integer>() {
      @Override public Integer call() throws Exception {
        if (tweening.compareAndSet(true, false)) {
          Log.v(TAG, "do Tween -> tween finished...");
          tweenlistener.onAnimationEnd();
          for (AnimationListener listener : doTweenListeners) {
            listener.onAnimationEnd();
          }
          doTweenListeners.clear();
          synchronized (mSyncLock) {
            // must show next express...
            if (ExpressServiceImpl.this.pendingExpressPair != null) {
              final Express.ExpressInfo express = pendingExpressPair.first;
              final AnimationListener pendingListener =
                  ExpressServiceImpl.this.pendingExpressPair.second.listener;
              int loopCount = ExpressServiceImpl.this.pendingExpressPair.second.loopCount;
              float speed = ExpressServiceImpl.this.pendingExpressPair.second.speed;
              ExpressServiceImpl.this.pendingExpressPair = null;
              return handleOneExpress(express, loopCount, speed, pendingListener);
            }
          }
        }
        return 0;
      }
    });
  }

  private CircleProgressDrawable lazyGetCircleProgressDrawable(Express.ExpressInfo express,
      int status, int progress) {
    CircleProgressDrawable circleProgressDrawable = (CircleProgressDrawable) cache.getItem(express);
    if (circleProgressDrawable == null) {
      circleProgressDrawable = new CircleProgressDrawable(RotateAnimator.getDefault());
      circleProgressDrawable.setState(status);
      circleProgressDrawable.setProgress(progress);
      cache.addItem(express, circleProgressDrawable);
    } else {
      circleProgressDrawable.refreshAttrs();
      circleProgressDrawable.setState(status);
      circleProgressDrawable.setProgress(progress);
    }
    return circleProgressDrawable;
  }

  @Override public int doProgressExpress(String name, int status, int progress, boolean animated,
      final @Nullable AnimationListener listener) {
    Log.d(TAG, name);
    final Express.ExpressInfo expressInfo = Express.ExpressInfo.newBuilder()
        .setName(name)
        .setFormat(Express.ExpressFormat.CODE_DRAW)
        .build();
    final HolderInfo holderInfo = new CircleProgressHolderInfo();
    holderInfo.listener = listener;
    ((CircleProgressHolderInfo) holderInfo).animated = animated;
    ((CircleProgressHolderInfo) holderInfo).status = status;
    ((CircleProgressHolderInfo) holderInfo).progress = progress;

    return tweenWithPendingTask(new Pair<>(expressInfo, holderInfo),
        new FutureTask<>(new Callable<Integer>() {
          @Override public Integer call() throws Exception {
            if (tweening.compareAndSet(true, false)) {
              Log.v(TAG, "tween finished...");
              for (AnimationListener listener : doTweenListeners) {
                listener.onAnimationEnd();
              }
              doTweenListeners.clear();
            }

            synchronized (mSyncLock) {
              if (ExpressServiceImpl.this.pendingExpressPair != null) {
                final Express.ExpressInfo express = pendingExpressPair.first;
                CircleProgressHolderInfo holderInfo =
                    (CircleProgressHolderInfo) ExpressServiceImpl.this.pendingExpressPair.second;
                final AnimationListener pendingListener = holderInfo.listener;
                boolean animated = holderInfo.animated;
                int status = holderInfo.status;
                int progress = holderInfo.progress;

                ExpressServiceImpl.this.pendingExpressPair = null;
                return handleProgressExpress(express, status, progress, animated, pendingListener);
              } else {
                return handleProgressExpress(expressInfo,
                    ((CircleProgressHolderInfo) holderInfo).status,
                    ((CircleProgressHolderInfo) holderInfo).progress,
                    ((CircleProgressHolderInfo) holderInfo).animated, holderInfo.listener);
              }
            }
          }
        }), false);
  }

  private int handleProgressExpress(final Express.ExpressInfo expressInfo, final int status,
      final int progress, boolean animated, @Nullable final AnimationListener listener) {
    CircleProgressDrawable circleProgressDrawable =
        lazyGetCircleProgressDrawable(expressInfo, status, progress);

    if (animated) {
      circleProgressDrawable.startAnimation(new AnimatorListenerAdapter() {
        @Override public void onAnimationStart(Animator animation) {
          Log.d("circleProgressDrawable", "onAnimationStart");
          if (listener != null) listener.onAnimationStart();
        }

        @Override public void onAnimationCancel(Animator animation) {
          if (listener != null) listener.onAnimationEnd();
        }

        @Override public void onAnimationEnd(Animator animation) {
          Log.d("circleProgressDrawable", "onAnimationEnd");
          if (listener != null) listener.onAnimationEnd();
        }
      });
    } else {
      circleProgressDrawable.drawAndRefresh();
    }
    runningExpress = expressInfo;
    return 0;
  }

  private static class Holder {
    private static final ExpressServiceImpl _instance = new ExpressServiceImpl();
  }

  private static class HolderInfo {
    AnimationListener listener;
    int loopCount = 1;
    float speed = 1.0f;
  }

  private static class CircleProgressHolderInfo extends HolderInfo {
    boolean animated = false;
    int status = 0;
    int progress = 0;
  }
}