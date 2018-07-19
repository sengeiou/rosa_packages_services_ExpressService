package com.airbnb.lottie.model.animatable;

import android.graphics.PointF;
import android.util.Log;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.PathKeyframe;
import com.airbnb.lottie.animation.keyframe.PathKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.PointKeyframeAnimation;
import com.airbnb.lottie.model.layer.ComponentType;
import com.airbnb.lottie.value.Keyframe;
import java.util.Collections;
import java.util.List;

import static com.airbnb.lottie.model.layer.CompomentUtil.TWEEN_DURATION;

public class AnimatablePathValue implements AnimatableValue<PointF, PointF> {
  private final List<Keyframe<PointF>> keyframes;

  /**
   * Create a default static animatable path.
   */
  public AnimatablePathValue() {
    keyframes = Collections.singletonList(new Keyframe<>(new PointF(0, 0)));
  }

  public AnimatablePathValue(List<Keyframe<PointF>> keyframes) {
    this.keyframes = keyframes;
  }

  @Override public BaseKeyframeAnimation<PointF, PointF> createAnimation() {
    if (keyframes.get(0).isStatic()) {
      return new PointKeyframeAnimation(keyframes);
    }
    return new PathKeyframeAnimation(keyframes);
  }

  @Override public void tween(ComponentType componentType, float progress) {
    if (keyframes.get(0).isStatic()) return;
    Keyframe<PointF> originKeyframe = keyframes.get(0);
    float delta = Float.MAX_VALUE;
    PathKeyframe fitKeyFrame = null;
    int size = keyframes.size();

    for (int i = 0; i < size; ++i) {
      PathKeyframe keyframe = (PathKeyframe) keyframes.get(i);
      final float startFrame = progress * keyframe.getDurationFrames();
      if (keyframe.containsProgress(progress)) {
        fitKeyFrame = new PathKeyframe(keyframe);
        fitKeyFrame.endValue = originKeyframe.startValue;
        fitKeyFrame.endFrame = startFrame + TWEEN_DURATION / fitKeyFrame.getFrameRate();
        fitKeyFrame.startFrame = startFrame;
        fitKeyFrame.updateProgress();
        fitKeyFrame.calculatePath(originKeyframe);
        //Log.w("Logic", "fitKeyFrameComponentTye: "
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
        PathKeyframe keyframe = (PathKeyframe) keyframes.get(i);
        float deltaProgress = keyframe.getStartProgress() - progress;
        if (Math.abs(deltaProgress) < delta && keyframe.endValue != originKeyframe.startValue) {
          delta = Math.abs(deltaProgress);
          fitKeyFrame = new PathKeyframe(keyframe);
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
        fitKeyFrame.calculatePath(originKeyframe);
        //Log.w("Logic", "xxxKeyFrameComponentTye: " + componentType.name());

        keyframes.clear();
        keyframes.add(fitKeyFrame);
      }
    }
  }
}
