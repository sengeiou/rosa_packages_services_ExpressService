

package com.ubtrobot.cpdrawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.ubtrobot.mini.spi.ConvertUtil;
import com.ubtrobot.mini.spi.IAnimationDrawable;
import com.ubtrobot.mini.spi.SpiRefresher;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * @author : kevin.liu@ubtrobot.com
 * @description :
 * @date : 2018/4/2
 * @modifier :
 * @modify time :
 * The arc is drawn clockwise. An angle of 0 degrees correspond to the geometric angle of 0 degrees (3 o'clock on a watch.)，
 * 也就是说，零度角是在时钟三点钟方向，沿着顺时针方向角度依次增大
 */
public class CircleProgressDrawable extends Drawable implements IAnimationDrawable{

    private int progressStartColor;
    private int progressEndColor;
    private int bgColor;
    private int bgMidColor;
    private int bgEndColor;
    private int textColor;
    private int flashColor;
    private int progress;
    private float progressWidth;
    private volatile int startAngle;
    private volatile int startTail1Angle;
    private volatile int startTail2Angle;
    private int sweepAngle;
    private boolean showAnim;

    private int mMeasureHeight;
    private int mMeasureWidth;

    private Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private Paint progressTail1Paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private Paint progressTail2Paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private Paint textPaint = new Paint();
    private Paint flashPaint = new Paint();

    private RectF pRectF;

    private float unitAngle;

    private int curProgress = 0;

    Bitmap buffer;
    Canvas rawCanvas;
    private final Bitmap mBgBitmapRaw;
    private final Bitmap mBgBitmapScale;
    private Paint unitPaint;
    private float mDensityScale;
    private AnimatorSet mAnimatorSet;

    public void setState(int state) {
        // state == 0: 非充电显示  state == 1:充电显示
        if (state == 0) {
            progressStartColor = getContext().getColor(R.color.ring_blue_start_color);
            progressEndColor = getContext().getColor(R.color.ring_blue_end_color);
            textColor = getContext().getColor(R.color.text_blue_color);
        } else if (state == 1) {
            progressStartColor = getContext().getColor(R.color.ring_green_start_color);
            progressEndColor = getContext().getColor(R.color.ring_green_end_color);
            textColor = getContext().getColor(R.color.text_green_color);
        }

    }

    private CircleProgressDrawable() {

        progressStartColor = getContext().getColor(R.color.ring_blue_start_color);
        progressEndColor = getContext().getColor(R.color.ring_blue_end_color);
        textColor = getContext().getColor(R.color.text_blue_color);

        bgColor = getContext().getColor(R.color.light_blue_color);
        bgMidColor = bgColor;
        bgEndColor = bgColor;
        flashColor = getContext().getColor(R.color.flash_blue_color);
        refreshAttrs();
        Resources res = getContext().getResources();
        mBgBitmapRaw = BitmapFactory.decodeResource(res, R.drawable.electricity_bg);
        mBgBitmapScale = getBitmap(mBgBitmapRaw, 240, 240);
    }


    public void refreshAttrs() {
        progress = 61;
        //TODO 设计师沟通从14 改为 10
        progressWidth = 8;
        startAngle = -90;
        resetTails();
        sweepAngle = 360;
        showAnim = true;

        unitAngle = (float) (sweepAngle / 100.0);

        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeCap(Paint.Cap.ROUND);
        bgPaint.setStrokeWidth(progressWidth - 1);
        bgPaint.setColor(bgColor);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setStrokeWidth(progressWidth);

        progressTail1Paint.setStyle(Paint.Style.STROKE);
        progressTail1Paint.setStrokeCap(Paint.Cap.ROUND);
        progressTail1Paint.setStrokeWidth(progressWidth);
        progressTail1Paint.setColor(getContext().getColor(R.color.ring_green_tail1_color));

        progressTail2Paint.setStyle(Paint.Style.STROKE);
        progressTail2Paint.setStrokeCap(Paint.Cap.ROUND);
        progressTail2Paint.setStrokeWidth(progressWidth);
        progressTail2Paint.setColor(getContext().getColor(R.color.ring_green_tail2_color));


        textPaint.setTextSize(44);
        textPaint.setTypeface(Typeface.MONOSPACE);

        unitPaint = new Paint(textPaint);
        unitPaint.setTextSize(20);

        flashPaint.setColor(flashColor);
        flashPaint.setStyle(Paint.Style.FILL);

        buffer = Bitmap.createBitmap(
                240, 240,
                Bitmap.Config.ARGB_8888);

        rawCanvas = new Canvas(buffer);
        pRectF = new RectF(55, 55, 185, 185);
    }

    private void resetTails() {
        startTail1Angle = -90;
        startTail2Angle = -90;
    }

    private static float getDensityScale(@NonNull Resources res, @DrawableRes @RawRes int id) {

        final TypedValue value = new TypedValue();
        res.getValue(id, value, true);
        final int resourceDensity = value.density;
        final int density;
        if (resourceDensity == TypedValue.DENSITY_DEFAULT) {
            density = DisplayMetrics.DENSITY_DEFAULT;
        } else if (resourceDensity != TypedValue.DENSITY_NONE) {
            density = resourceDensity;
        } else {
            density = 0;
        }
        final int targetDensity = res.getDisplayMetrics().densityDpi;

        if (density > 0 && targetDensity > 0) {
            return (float) targetDensity / density;
        }
        return 1f;
    }

    /**
     * 缩放Bitmap满屏
     *
     * @param bitmap
     * @param screenWidth
     * @param screenHight
     * @return
     */
    public static Bitmap getBitmap(Bitmap bitmap, int screenWidth,
                                   int screenHight) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scale = (float) screenWidth / w;
        float scale2 = (float) screenHight / h;
        // scale = scale < scale2 ? scale : scale2;
        matrix.postScale(scale, scale);
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
        if (bitmap != null && !bitmap.equals(bmp) && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        return bmp;// Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
    }

    private RotateAnimator mRa;

    public CircleProgressDrawable(RotateAnimator ra) {
        this();
        this.mRa = ra;
    }

    private static Context sAppContext;

    @Nullable
    private Context getContext() {
        if (sAppContext == null) {
            try {
                @SuppressLint("PrivateApi") final Class<?> activityThread = Class.forName("android.app.ActivityThread");
                final Method currentApplicationMethod =
                        activityThread.getDeclaredMethod("currentApplication");
                sAppContext = (Context) currentApplicationMethod.invoke(null);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "LibraryLoader not initialized. Call LibraryLoader.initialize() before using library classes.",
                        e);
            }
        }
        return sAppContext;
    }

    synchronized public void drawAndRefresh() {
        if (rawCanvas != null)
          draw(rawCanvas);
    }

    int max = 100;

    public int getGradient(float fraction, int startColor, int endColor) {
        if (fraction > 1) fraction = 1;
        int alphaStart = Color.alpha(startColor);
        int redStart = Color.red(startColor);
        int blueStart = Color.blue(startColor);
        int greenStart = Color.green(startColor);
        int alphaEnd = Color.alpha(endColor);
        int redEnd = Color.red(endColor);
        int blueEnd = Color.blue(endColor);
        int greenEnd = Color.green(endColor);
        int alphaDifference = alphaEnd - alphaStart;
        int redDifference = redEnd - redStart;
        int blueDifference = blueEnd - blueStart;
        int greenDifference = greenEnd - greenStart;
        int alphaCurrent = (int) (alphaStart + fraction * alphaDifference);
        int redCurrent = (int) (redStart + fraction * redDifference);
        int blueCurrent = (int) (blueStart + fraction * blueDifference);
        int greenCurrent = (int) (greenStart + fraction * greenDifference);
        return Color.argb(alphaCurrent, redCurrent, greenCurrent, blueCurrent);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();

        drawBg(canvas);
        drawProgress(canvas);

        canvas.restore();

        doExpressRefresh(ConvertUtil.convertBitmap(buffer));
    }


    // 只需要画进度之外的背景即可
    private void drawBg(Canvas canvas) {
        canvas.drawBitmap(mBgBitmapScale, 0, 0, new Paint());

//        float halfSweep = sweepAngle / 2;
//        for (int i = sweepAngle, st = (int) (curProgress * unitAngle); i > st; --i) {
//            if (i - halfSweep > 0) {
//                bgPaint.setColor(getGradient((i - halfSweep) / halfSweep, bgMidColor, bgEndColor));
//            } else {
//                bgPaint.setColor(getGradient((halfSweep - i) / halfSweep, bgMidColor, bgColor));
//            }
//
//        }
        canvas.drawArc(pRectF,
                0,
                360,
                false,
                bgPaint);

        // draw Flash mark!!! 闪电侠标志吼吼~~

        Path path = new Path();
        path.moveTo(132, 72);
        path.lineTo(117, 111);
        path.lineTo(158, 111);
        path.lineTo(100, 165);
        path.lineTo(111, 134);
        path.lineTo(72, 134);
        path.close();

        canvas.drawPath(path, flashPaint);
    }

    private void drawProgress(Canvas canvas) {

        if (mRa.isRunning()) {
            drawGradientProgress(canvas, mRa.animationProgressStart);

            if (!(startTail1Angle == -90 && startTail2Angle == -90)) {
                canvas.drawArc(pRectF,
                        startTail1Angle,
                        startAngle - startTail1Angle,
                        false,
                        progressTail1Paint);

                canvas.drawArc(pRectF,
                        startTail2Angle,
                        startTail1Angle - startTail2Angle,
                        false,
                        progressTail2Paint);
            }

        } else {
            drawGradientProgress(canvas, curProgress);
        }


        String text;
        if (!mRa.textChangedOnAnimation) {
            //draw progress text
            text = curProgress + "";

        } else {
            text = mRa.changedText;
        }
        textPaint.setColor(textColor);
        float textWidth = textPaint.measureText(text);
        canvas.drawText(text, (240 - textWidth) / 2,
                (240 - (textPaint.descent() - textPaint.ascent())) / 2 - textPaint.ascent(), textPaint);


        String unit = "%";
        unitPaint.setColor(textColor);
        float unit_offset_width = 0;
        canvas.drawText(unit, (240 + textWidth) / 2 + unit_offset_width, (240 - (textPaint.descent() - textPaint.ascent())) / 2 - textPaint.ascent(), unitPaint);

    }


    private void drawGradientProgress(Canvas canvas, int progress) {

        if (progress > 61) {
            for (int i = 0, end = 61; i <= end; i++) {
                progressPaint.setColor(getGradient(i / (float) end, progressStartColor, progressEndColor));
                canvas.drawArc(pRectF,
                        startAngle + i * unitAngle,
                        unitAngle,
                        false,
                        progressPaint);
            }

            for (int i = 62, end = progress; i <= end; i++) {
                progressPaint.setColor(getGradient((i - 61) / 39.0f, progressEndColor, progressStartColor));
                canvas.drawArc(pRectF,
                        startAngle + i * unitAngle,
                        unitAngle,
                        false,
                        progressPaint);
            }

        } else {
            for (int i = 0, end = progress; i <= end; i++) {
                progressPaint.setColor(getGradient(i / 100.0f, progressStartColor, progressEndColor));
                canvas.drawArc(pRectF,
                        startAngle + i * unitAngle,
                        unitAngle,
                        false,
                        progressPaint);
            }
        }


    }

    public void doExpressRefresh(Bitmap bitmap) {
        ByteBuffer byteBuffer = ConvertUtil.convertBmpToBuffer(2, bitmap);
        SpiRefresher.get().refreshSync(byteBuffer);
        bitmap.recycle();
    }

    public void setProgress(int progress) {
        this.curProgress = progress;
    }


    public static final int ANIMATION_STATE_NON_STARTED = -1;
    public static final int ANIMATION_STATE_STARTED = 1;
    public static final int ANIMATION_STATE_END = 2;

    public static int MAIN_ROTATE_ANIMATION_STATE = ANIMATION_STATE_NON_STARTED;
    public static int TAIL1_ROTATE_ANIMATION_STATE = ANIMATION_STATE_NON_STARTED;
    public static int TAIL2_ROTATE_ANIMATION_STATE = ANIMATION_STATE_NON_STARTED;


    public void startAnimation(@Nullable AnimatorListenerAdapter globalListener) {
        prepare();
        playAnimatorSet(globalListener);
    }

    private void prepare() {
        mRa.setRunning();
        mRa.updateAnimationProgressStart(RotateAnimator.DEFAULT_ANIMATION_PROGRESSSTART);
//        drawAndRefresh();
    }

    @Override public void stopAnimation() {
        cancelAnimation();
    }

    @Override
    public boolean isAnimationRunning() {
        return mRa.isRunning();
    }

    public void cancelAnimation() {
        if (mAnimatorSet != null) {
            mRa.stopRunning();
            mAnimatorSet.cancel();
//            drawAndRefresh();
        }
    }

    public synchronized void clearAnimation() {
        if (buffer != null) {
            buffer.recycle();
            buffer =null;
            rawCanvas = null;
        }
    }

    private void playAnimatorSet(final @Nullable AnimatorListenerAdapter globalListener) {
        final int commonRotateAngle = 720;
        final int FACTOR = 4; //I don't know why ~ ~ pleasee tell me !!
        final boolean extended = curProgress > mRa.animationProgressStart;
        final int animationProgressStart = mRa.animationProgressStart;
        int mainRotateEndAngle = extended ? -90 + commonRotateAngle : -90 + commonRotateAngle - (animationProgressStart - curProgress) * FACTOR;
        MAIN_ROTATE_ANIMATION_STATE = TAIL1_ROTATE_ANIMATION_STATE = TAIL2_ROTATE_ANIMATION_STATE = ANIMATION_STATE_NON_STARTED;

        final ValueAnimator mainRotateValueAnimator = setupMainRotateAnimation(mainRotateEndAngle);

        ValueAnimator tail1RotateValueAnimator = setupTail1Animation(mainRotateEndAngle);

        ValueAnimator tail2RotateValueAnimator = setupTail2Animation(mainRotateEndAngle);

        final ValueAnimator extendValueAnimator = setupExtendAnimation(FACTOR, extended, animationProgressStart);

        mAnimatorSet = new AnimatorSet();
        AnimatorSet as = new AnimatorSet();

//        mainRotateValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator valueAnimator) {
//                long currentPlayTime = valueAnimator.getCurrentPlayTime();
//                if (currentPlayTime > (mMainDuration - 0)&&!extendValueAnimator.isStarted()) {
//                    mainRotateValueAnimator.cancel();
//                    Log.e("extendValueAnimator","extendValueAnimator start");
//                    extendValueAnimator.start();
//                }
//            }
//        });
        long delay = 120;

        extendValueAnimator.setStartDelay(mMainDuration - delay);
        as.playTogether(mainRotateValueAnimator, extendValueAnimator);
//        as.playSequentially(mainRotateValueAnimator, extendValueAnimator);

        mAnimatorSet.playTogether(as, tail1RotateValueAnimator, tail2RotateValueAnimator);

        mAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (globalListener != null)
                    globalListener.onAnimationStart(animation);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                if (globalListener != null)
                    globalListener.onAnimationCancel(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mRa.stopRunning();
                mRa.updateAnimationProgressStart(animationProgressStart);
//                drawAndRefresh();
                if (globalListener != null)
                    globalListener.onAnimationEnd(animation);
            }
        });
        mAnimatorSet.start();
    }

    @NonNull
    private ValueAnimator setupExtendAnimation(final int FACTOR, final boolean extended, int animationProgressStart) {
        ValueAnimator extendValueAnimator = ValueAnimator.ofInt(animationProgressStart, curProgress);
        extendValueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        extendValueAnimator.setDuration(180l);
        extendValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private int startAngle = -90;
            private int diffAngle = 0;

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (extended) {
                    mRa.updateAnimationProgressStart((Integer) valueAnimator.getAnimatedValue() % 360);
                    if (Math.abs((Integer) valueAnimator.getAnimatedValue() - curProgress) < 6) {
                        resetTails();
                    }
                } else {
                    diffAngle = ((Integer) valueAnimator.getAnimatedValue() - curProgress);
                    startAngle = -90 - diffAngle * FACTOR;
                    mRa.updateAnimationProgressStart((curProgress + diffAngle) % 360);
                    setStartAngle(startAngle);
                    resetTails();
                }

                drawAndRefresh();
            }
        });
        return extendValueAnimator;
    }

    @NonNull
    private ValueAnimator setupTail2Animation(int mainRotateEndAngle) {
        ValueAnimator tail2RotateValueAnimator = ValueAnimator.ofInt(-90, mainRotateEndAngle);
        tail2RotateValueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        tail2RotateValueAnimator.setDuration(mMainDuration);
        tail2RotateValueAnimator.setStartDelay(80l);
        tail2RotateValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                // fix
                setTail2StartAngle((Integer) valueAnimator.getAnimatedValue());
                if (MAIN_ROTATE_ANIMATION_STATE == ANIMATION_STATE_END && TAIL1_ROTATE_ANIMATION_STATE == ANIMATION_STATE_END)
                    drawAndRefresh();
            }
        });
        tail2RotateValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation, boolean isReverse) {
                TAIL2_ROTATE_ANIMATION_STATE = ANIMATION_STATE_STARTED;
            }

            @Override
            public void onAnimationEnd(Animator animation, boolean isReverse) {
                TAIL2_ROTATE_ANIMATION_STATE = ANIMATION_STATE_END;

            }
        });
        return tail2RotateValueAnimator;
    }

    long mMainDuration = 1300l;

    @NonNull
    private ValueAnimator setupTail1Animation(int mainRotateEndAngle) {
        ValueAnimator tail1RotateValueAnimator = ValueAnimator.ofInt(-90, mainRotateEndAngle);
        tail1RotateValueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        tail1RotateValueAnimator.setDuration(mMainDuration);
        tail1RotateValueAnimator.setStartDelay(60l);
        tail1RotateValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                // fix
                setTail1StartAngle((Integer) valueAnimator.getAnimatedValue());
                if (MAIN_ROTATE_ANIMATION_STATE == ANIMATION_STATE_END)
                    drawAndRefresh();
            }
        });
        tail1RotateValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation, boolean isReverse) {
                TAIL1_ROTATE_ANIMATION_STATE = ANIMATION_STATE_STARTED;
            }

            @Override
            public void onAnimationEnd(Animator animation, boolean isReverse) {
                TAIL1_ROTATE_ANIMATION_STATE = ANIMATION_STATE_END;
            }
        });
        return tail1RotateValueAnimator;
    }

    @NonNull
    private ValueAnimator setupMainRotateAnimation(int mainRotateEndAngle) {
        ValueAnimator mainRotateValueAnimator = ValueAnimator.ofInt(-90, mainRotateEndAngle);
        mainRotateValueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mainRotateValueAnimator.setDuration(mMainDuration);
        mainRotateValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                // fix
                setStartAngle((Integer) valueAnimator.getAnimatedValue());
                drawAndRefresh();
            }
        });


        mainRotateValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation, boolean isReverse) {
                MAIN_ROTATE_ANIMATION_STATE = ANIMATION_STATE_STARTED;
            }

            @Override
            public void onAnimationEnd(Animator animation, boolean isReverse) {
                MAIN_ROTATE_ANIMATION_STATE = ANIMATION_STATE_END;
            }
        });
        return mainRotateValueAnimator;
    }


    public synchronized void setStartAngle(int angle) {
        this.startAngle = angle;
    }

    public synchronized void setTail1StartAngle(int angle) {
        this.startTail1Angle = angle;
    }

    public synchronized void setTail2StartAngle(int angle) {
        this.startTail2Angle = angle;
    }


    @Override
    public void setAlpha(int alpha) {
        progressPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        progressPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return 1 - progressPaint.getAlpha();
    }

    @Override
    public int getIntrinsicHeight() {
        return mBgBitmapScale.getHeight() == 0?240:mBgBitmapScale.getHeight();
    }

    @Override
    public int getIntrinsicWidth() {
        return mBgBitmapScale.getWidth()==0?240:mBgBitmapScale.getWidth();
    }

}

