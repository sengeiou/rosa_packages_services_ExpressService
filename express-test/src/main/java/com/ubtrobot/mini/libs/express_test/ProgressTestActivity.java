package com.ubtrobot.mini.libs.express_test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import com.ubtrobot.commons.Priority;
import com.ubtrobot.express.ExpressApi;

public class ProgressTestActivity extends Activity {

  private SeekBar sk;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_progress_test);

    sk = (SeekBar) findViewById(R.id.sk);
  }


  public void go(View view) {
    ExpressApi.get().doProgressExpress(0, sk.getProgress(), false, Priority.HIGH, null);
  }

  public void go2(View view) {
    ExpressApi.get().doProgressExpress(1, sk.getProgress(), true, Priority.HIGH, null);
  }

}
