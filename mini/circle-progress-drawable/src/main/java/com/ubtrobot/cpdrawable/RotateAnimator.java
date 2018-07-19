package com.ubtrobot.cpdrawable;

/**
 * @author : kevin.liu@ubtrobot.com
 * @description :
 * @date : 2018/4/2
 * @modifier :
 * @modify time :
 */

public class RotateAnimator {

    public static RotateAnimator getDefault(){
        return new RotateAnimator();
    }

    public static final int DEFAULT_ANIMATION_PROGRESSSTART = 30;

    boolean textChangedOnAnimation = false;

    String changedText = "";

    int animationProgressStart = DEFAULT_ANIMATION_PROGRESSSTART;

    int numOfTurns = 1;

    boolean running = false;

    public synchronized boolean isRunning(){
        return running;
    }

    public synchronized void setRunning(){
        running = true;
    }

    public synchronized void stopRunning(){
        running = false;
    }

    public synchronized void updateAnimationProgressStart(int progress){
        animationProgressStart = progress;
    }
    public synchronized void setNumOfTurns(int turns){
        this.numOfTurns = turns;
    }

}
