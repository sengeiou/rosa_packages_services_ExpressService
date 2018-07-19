package com.ubtrobot.service;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import com.ubtrobot.service.express.utils.PngUtils;

public class MainActivity extends Activity {
  private static final String TAG = "MainActivity";

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.change).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        try {
          PngUtils.main2(null);
          PngUtils.main3("/sdcard/signal_res/booting_0.jpg");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
