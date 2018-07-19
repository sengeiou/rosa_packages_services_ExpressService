package com.airbnb.lottie.model.animatable;

import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.model.layer.ComponentType;

public interface AnimatableValue<K, A> {
  BaseKeyframeAnimation<K, A> createAnimation();
  void tween(ComponentType componentType, float progress);
}
