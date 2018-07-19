package com.ubtrobot.service.express;

public interface AnimationListener {
  /**
   * Notifies the start of the animation.
   */
  void onAnimationStart();

  /**
   * Notifies the end of the animation.
   */
  void onAnimationEnd();

  /**
   * @param loopNumber the number of loop
   */
  void onAnimationRepeat(int loopNumber);
}