package com.ubtrobot.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * boot receiver
 *
 * @author Logic
 */

public class ExpressScanReceiver extends BroadcastReceiver {

  @Override public void onReceive(Context context, Intent intent) {
    //if (Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())) {
    //  LogUtils.d("media mounted ---->scan express");
    //  NotificationCenter.defaultCenter().publish(new RefreshCacheEvent());
    //} else if ("com.ubtrobot.service.action.SCAN_EXPRESS".equals(intent.getAction())) {
    //  RefreshCacheEvent event = new RefreshCacheEvent();
    //  event.fileName = intent.getStringExtra("file");
    //  NotificationCenter.defaultCenter().publish(event);
    //}
  }
}
