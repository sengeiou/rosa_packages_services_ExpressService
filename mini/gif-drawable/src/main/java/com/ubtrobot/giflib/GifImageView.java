package com.ubtrobot.giflib;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * An {@link ImageView} which tries treating background and src as {@link GifDrawable}
 *
 * @author koral--
 */
public class GifImageView extends ImageView {

  private boolean mFreezesAnimation;

  /**
   * A corresponding superclass constructor wrapper.
   *
   * @param context context
   * @see ImageView#ImageView(Context)
   */
  public GifImageView(Context context) {
    super(context);
  }

  /**
   * Like equivalent from superclass but also try to interpret src and background
   * attributes as {@link GifDrawable}.
   *
   * @param context context
   * @param attrs attrs
   * @see ImageView#ImageView(Context, AttributeSet)
   */
  public GifImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    postInit(GifViewUtils.initImageView(this, attrs, 0, 0));
  }

  /**
   * Like equivalent from superclass but also try to interpret src and background
   * attributes as GIFs.
   *
   * @param context context
   * @param attrs attrs
   * @param defStyle style
   * @see ImageView#ImageView(Context, AttributeSet, int)
   */
  public GifImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    postInit(GifViewUtils.initImageView(this, attrs, defStyle, 0));
  }

  /**
   * Like equivalent from superclass but also try to interpret src and background
   * attributes as GIFs.
   *
   * @param context context
   * @param attrs attrs
   * @param defStyle style
   * @param defStyleRes styleres
   * @see ImageView#ImageView(Context, AttributeSet, int, int)
   */
  @RequiresApi(Build.VERSION_CODES.LOLLIPOP) public GifImageView(Context context,
      AttributeSet attrs, int defStyle, int defStyleRes) {
    super(context, attrs, defStyle, defStyleRes);
    postInit(GifViewUtils.initImageView(this, attrs, defStyle, defStyleRes));
  }

  private void postInit(GifViewUtils.GifImageViewAttributes result) {
    mFreezesAnimation = result.freezesAnimation;
    if (result.mSourceResId > 0) {
      super.setImageResource(result.mSourceResId);
    }
    if (result.mBackgroundResId > 0) {
      super.setBackgroundResource(result.mBackgroundResId);
    }
  }

  /**
   * Sets the content of this GifImageView to the specified Uri.
   * If uri destination is not a GIF then {@link ImageView#setImageURI(Uri)}
   * is called as fallback.
   * For supported URI schemes see: {@link android.content.ContentResolver#openAssetFileDescriptor(Uri, String)}.
   *
   * @param uri The Uri of an image
   */
  @Override public void setImageURI(Uri uri) {
    if (!GifViewUtils.setGifImageUri(this, uri)) {
      super.setImageURI(uri);
    }
  }

  @Override public void setImageResource(int resId) {
    if (!GifViewUtils.setResource(this, true, resId)) {
      super.setImageResource(resId);
    }
  }

  @Override public void setBackgroundResource(int resId) {
    if (!GifViewUtils.setResource(this, false, resId)) {
      super.setBackgroundResource(resId);
    }
  }

  @Override public Parcelable onSaveInstanceState() {
    Drawable source = mFreezesAnimation ? getDrawable() : null;
    Drawable background = mFreezesAnimation ? getBackground() : null;
    return new GifViewSavedState(super.onSaveInstanceState(), source, background);
  }

  @Override public void onRestoreInstanceState(Parcelable state) {
    if (!(state instanceof GifViewSavedState)) {
      super.onRestoreInstanceState(state);
      return;
    }
    GifViewSavedState ss = (GifViewSavedState) state;
    super.onRestoreInstanceState(ss.getSuperState());
    ss.restoreState(getDrawable(), 0);
    ss.restoreState(getBackground(), 1);
  }

  /**
   * Sets whether animation position is saved in {@link #onSaveInstanceState()} and restored
   * in {@link #onRestoreInstanceState(Parcelable)}
   *
   * @param freezesAnimation whether animation position is saved
   */
  public void setFreezesAnimation(boolean freezesAnimation) {
    mFreezesAnimation = freezesAnimation;
  }
}
