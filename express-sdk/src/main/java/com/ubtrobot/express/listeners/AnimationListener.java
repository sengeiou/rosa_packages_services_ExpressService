package com.ubtrobot.express.listeners;

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
   * @param loopNumber loopNumber
   */
  void onAnimationRepeat(int loopNumber);
}