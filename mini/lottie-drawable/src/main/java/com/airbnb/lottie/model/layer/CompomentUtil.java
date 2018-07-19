package com.airbnb.lottie.model.layer;

/**
 * Created by logic on 18-1-30.
 */

public final class CompomentUtil {

  public static final int TWEEN_DURATION = 400;

  public static boolean isAnimatableCompoment(ComponentType type) {
    return type != ComponentType.White && type != null;
  }
}
