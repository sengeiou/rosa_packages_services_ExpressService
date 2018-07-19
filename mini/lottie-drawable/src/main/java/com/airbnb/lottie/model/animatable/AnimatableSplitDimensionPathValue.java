package com.airbnb.lottie.model.animatable;

import android.graphics.PointF;

import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.SplitDimensionPathKeyframeAnimation;
import com.airbnb.lottie.model.layer.ComponentType;

public class AnimatableSplitDimensionPathValue implements AnimatableValue<PointF, PointF> {
  private final AnimatableFloatValue animatableXDimension;
  private final AnimatableFloatValue animatableYDimension;

  public AnimatableSplitDimensionPathValue(
      AnimatableFloatValue animatableXDimension,
      AnimatableFloatValue animatableYDimension) {
    this.animatableXDimension = animatableXDimension;
    this.animatableYDimension = animatableYDimension;
  }

  @Override public BaseKeyframeAnimation<PointF, PointF> createAnimation() {
    return new SplitDimensionPathKeyframeAnimation(
        animatableXDimension.createAnimation(), animatableYDimension.createAnimation());
  }

  @Override public void tween(ComponentType componentType, float progress) {
    animatableXDimension.tween(componentType, progress);
    animatableYDimension.tween(componentType, progress);
  }
}
