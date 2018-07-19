package com.ubtrobot.giflib;

/**
 * Builder for {@link com.ubtrobot.giflib.GifDrawable} which can be used to construct new drawables
 * by reusing old ones.
 */
public class GifDrawableBuilder extends GifDrawableInit<GifDrawableBuilder> {

  @Override protected GifDrawableBuilder self() {
    return this;
  }
}
