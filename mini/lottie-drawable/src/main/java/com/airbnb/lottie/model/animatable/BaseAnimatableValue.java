package com.airbnb.lottie.model.animatable;

import android.util.Log;
import com.airbnb.lottie.model.layer.ComponentType;
import com.airbnb.lottie.value.Keyframe;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.airbnb.lottie.model.layer.CompomentUtil.TWEEN_DURATION;

abstract class BaseAnimatableValue<V, O> implements AnimatableValue<V, O> {
  final List<Keyframe<V>> keyframes;

  /**
   * Create a default static animatable path.
   */
  BaseAnimatableValue(V value) {
    this(Collections.singletonList(new Keyframe<>(value)));
  }

  BaseAnimatableValue(List<Keyframe<V>> keyframes) {
    this.keyframes = keyframes;
  }

  @Override public void tween(ComponentType componentType, float progress) {
    if (keyframes.get(0).isStatic()) return;
    Keyframe<V> originKeyframe = keyframes.get(0);
    float delta = Float.MAX_VALUE;
    Keyframe<V> fitKeyFrame = null;
    int size = keyframes.size();

    for (int i = 0; i < size; ++i) {
      Keyframe<V> keyframe = keyframes.get(i);
      final float startFrame = progress * keyframe.getDurationFrames();
      if (keyframe.containsProgress(progress)) {
        fitKeyFrame = new Keyframe<>(keyframe);
        fitKeyFrame.endValue = originKeyframe.startValue;
        fitKeyFrame.endFrame = startFrame + TWEEN_DURATION / fitKeyFrame.getFrameRate();
        fitKeyFrame.startFrame = startFrame;
        fitKeyFrame.updateProgress();
        //Log.w("Logic", "Base->fitKeyFrameComponentTye: "
        //    + componentType.name()
        //    + ", startProgress = "
        //    + fitKeyFrame.getStartProgress()
        //    + ", endProgress = "
        //    + fitKeyFrame.getEndProgress());
        break;
      }
    }

    if (fitKeyFrame != null) {
      keyframes.clear();
      keyframes.add(fitKeyFrame);
    } else if (originKeyframe.lessPrecomps()) {
      // Looking for the closest Keyframe based on startProgress
      for (int i = 1; i < size - 1; ++i) {
        Keyframe<V> keyframe = keyframes.get(i);
        float deltaProgress = keyframe.getStartProgress() - progress;
        if (Math.abs(deltaProgress) < delta && keyframe.endValue != originKeyframe.startValue) {
          delta = Math.abs(deltaProgress);
          fitKeyFrame = new Keyframe<>(keyframe);
        }
      }

      //found
      if (fitKeyFrame != null) {
        final float startFrame = progress * fitKeyFrame.getDurationFrames();
        //If the keyframe end value is less than the current progression,
        //you can conclude that the json data given by the design is abnormal
        if ((fitKeyFrame.getEndProgress() - progress) < 0) {
          fitKeyFrame.startValue = fitKeyFrame.endValue;
        } else {
          fitKeyFrame.startFrame = startFrame;
        }
        fitKeyFrame.endValue = originKeyframe.startValue;

        fitKeyFrame.endFrame = startFrame + TWEEN_DURATION / fitKeyFrame.getFrameRate();
        fitKeyFrame.updateProgress();
        //Log.w("Logic", "Base->xxxKeyFrameComponentTye: " + componentType.name());

        keyframes.clear();
        keyframes.add(fitKeyFrame);
      }
    }
  }

  @Override public String toString() {
    final StringBuilder sb = new StringBuilder();
    if (!keyframes.isEmpty()) {
      sb.append("values=").append(Arrays.toString(keyframes.toArray()));
    }
    return sb.toString();
  }
}
