package com.ubtrobot.eyetrack;

import android.animation.Animator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import com.ubtrobot.eyetrack.transforms.CornerRadiusTransform;
import com.ubtrobot.eyetrack.transforms.Transform;

/**
 * @author Logic
 */

public class EyeTrackDrawable extends Drawable {
  /**
   * Paint used to draw on a Canvas
   */
  private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
  private final Bitmap buffer;
  private final PointF mInitDot;
  private final Bitmap mPupilBmp;
  private final Bitmap mWhiteBmp;
  private final Rect mSrcRect;
  private final Rect mDstRect = new Rect();
  /**
   * 眼球圆心
   */
  private PointF mPupilDot;
  private PointF mEndDot;
  private @Nullable Interpolator mInterpolator;
  private volatile boolean mIsRunning = false;
  private ColorStateList mTint;
  private PorterDuffColorFilter mTintFilter;
  private PorterDuff.Mode mTintMode;
  private int mScaledWhiteWidth;
  private int mScaledWhiteHeight;
  private int mScalePupilWidth;
  private int mScalePupilHeight;
  private Transform mTransform;

  public EyeTrackDrawable(@NonNull Resources res, @Nullable Interpolator interpolator) {
    mInterpolator = interpolator;
    mPupilBmp = BitmapFactory.decodeResource(res, R.drawable.pupil);
    mWhiteBmp = BitmapFactory.decodeResource(res, R.drawable.white);
    buffer =
        Bitmap.createBitmap(mWhiteBmp.getWidth(), mWhiteBmp.getHeight(), Bitmap.Config.RGB_565);
    mSrcRect = new Rect(0, 0, mWhiteBmp.getWidth(), mWhiteBmp.getHeight());
    //scale可能不支持，所以计算可能有误
    mScaledWhiteWidth = (int) (mWhiteBmp.getWidth() * getDensityScale(res, R.drawable.white));
    mScaledWhiteHeight = (int) (mWhiteBmp.getHeight() * getDensityScale(res, R.drawable.white));
    mScalePupilWidth = (int) (mPupilBmp.getWidth() * getDensityScale(res, R.drawable.pupil));
    mScalePupilHeight = (int) (mPupilBmp.getHeight() * getDensityScale(res, R.drawable.pupil));
    mInitDot = new PointF(0, 0);
    mPupilDot = new PointF(mInitDot.x, mInitDot.y);
    mEndDot = new PointF(mInitDot.x, mInitDot.y);
    drawFrame();
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

  @Override public void draw(@NonNull Canvas canvas) {
    final boolean clearColorFilter;
    if (mTintFilter != null && mPaint.getColorFilter() == null) {
      mPaint.setColorFilter(mTintFilter);
      clearColorFilter = true;
    } else {
      clearColorFilter = false;
    }
    if (mTransform == null) {
      canvas.drawBitmap(buffer, mSrcRect, mDstRect, mPaint);
    } else {
      mTransform.onDraw(canvas, mPaint, buffer);
    }
    if (clearColorFilter) {
      mPaint.setColorFilter(null);
    }
  }

  @Override public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
    mPaint.setAlpha(alpha);
  }

  @Override public void setColorFilter(@Nullable ColorFilter colorFilter) {
    mPaint.setColorFilter(colorFilter);
  }

  @Override public int getOpacity() {
    if (mPaint.getAlpha() < 255) return PixelFormat.TRANSPARENT;
    return PixelFormat.OPAQUE;
  }

  public void setPointF(@NonNull PointF pointF) {
    float x = Math.max(Math.min(pointF.x, mScaledWhiteWidth / 2), -mScaledWhiteWidth / 2);
    float y = Math.max(Math.min(pointF.x, mScaledWhiteHeight / 2), -mScaledWhiteHeight / 2);
    mEndDot.set(x, y);
  }

  public void setInterpolator(@Nullable Interpolator inter) {
    mInterpolator = inter;
  }

  @FloatRange(from = 0) public float getCornerRadius() {
    if (mTransform instanceof CornerRadiusTransform) {
      return ((CornerRadiusTransform) mTransform).getCornerRadius();
    }
    return 0;
  }

  public void setCornerRadius(@FloatRange(from = 0) final float cornerRadius) {
    mTransform = new CornerRadiusTransform(cornerRadius);
    mTransform.onBoundsChange(mDstRect);
  }

  public void start(boolean animated) {
    synchronized (this) {
      if (mIsRunning) {
        return;
      }
      mIsRunning = true;
    }
    if (animated) {
      startAnimation();
    } else {
      drawFrame();
    }
  }

  private void startAnimation() {
    ValueAnimator valueAnimator = new ValueAnimator();
    valueAnimator.setDuration(1500);
    valueAnimator.setInterpolator(
        mInterpolator == null ? new AccelerateDecelerateInterpolator() : mInterpolator);
    valueAnimator.setObjectValues(mPupilDot, mEndDot);
    valueAnimator.setEvaluator(new TypeEvaluator<PointF>() {
      @Override public PointF evaluate(float fraction, PointF startValue, PointF endValue) {
        // x方向20 * t * 3  ，则y方向20 * t * 3
        Log.d("TAG", startValue.toString() + endValue.toString() + ", fraction =" + fraction);
        PointF point = new PointF();
        point.x = fraction * (endValue.x - startValue.x) + mPupilDot.x;
        point.y = fraction * (endValue.y - endValue.y) + mPupilDot.y;
        return point;
      }
    });
    valueAnimator.start();
    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override public void onAnimationUpdate(ValueAnimator animation) {
        PointF point = (PointF) animation.getAnimatedValue();
        mPupilDot.set(point);
        drawFrame();
      }
    });
    valueAnimator.addListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animation) {

      }

      @Override public void onAnimationEnd(Animator animation) {
        mIsRunning = false;
        mEndDot.set(mPupilDot);
        //reset(true);
      }

      @Override public void onAnimationCancel(Animator animation) {
        mIsRunning = false;
      }

      @Override public void onAnimationRepeat(Animator animation) {

      }
    });
  }

  public void reset(boolean animated) {
    mEndDot.set(mInitDot.x, mInitDot.y);
    if (animated) {
      // TODO: 2017/10/13 贝塞尔曲线回弹
      start(true);
    } else {
      mPupilDot.set(mInitDot.x, mInitDot.y);
      drawFrame();
    }
  }

  private void drawFrame() {
    Canvas canvas = new Canvas(buffer);
    canvas.drawBitmap(mWhiteBmp, 0, 0, mPaint);
    canvas.save();
    canvas.translate((mScaledWhiteWidth - mScalePupilWidth) / 2,
        (mScaledWhiteHeight - mScalePupilHeight) / 2);
    canvas.drawBitmap(mPupilBmp, mPupilDot.x, mPupilDot.y, mPaint);
    canvas.restore();
    invalidateSelf();
  }

  private PorterDuffColorFilter updateTintFilter(ColorStateList tint, PorterDuff.Mode tintMode) {
    if (tint == null || tintMode == null) {
      return null;
    }

    final int color = tint.getColorForState(getState(), Color.TRANSPARENT);
    return new PorterDuffColorFilter(color, tintMode);
  }

  @Override public void setTintList(ColorStateList tint) {
    mTint = tint;
    mTintFilter = updateTintFilter(tint, mTintMode);
    invalidateSelf();
  }

  @Override public void setTintMode(@NonNull PorterDuff.Mode tintMode) {
    mTintMode = tintMode;
    mTintFilter = updateTintFilter(mTint, tintMode);
    invalidateSelf();
  }

  @Override protected boolean onStateChange(int[] stateSet) {
    if (mTint != null && mTintMode != null) {
      mTintFilter = updateTintFilter(mTint, mTintMode);
      return true;
    }
    return false;
  }

  @Override public boolean isStateful() {
    return super.isStateful() || (mTint != null && mTint.isStateful());
  }

  @Override protected void onBoundsChange(Rect bounds) {
    mDstRect.set(bounds);
    if (mTransform != null) {
      mTransform.onBoundsChange(bounds);
    }
  }

  @Override public int getIntrinsicHeight() {
    return mScaledWhiteHeight;
  }

  @Override public int getIntrinsicWidth() {
    return mScaledWhiteWidth;
  }
}
