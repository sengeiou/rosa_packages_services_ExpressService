package com.ubtrobot.mini.libs.express_test;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import com.ubtech.utilcode.utils.thread.HandlerUtils;
import com.ubtrobot.commons.Priority;
import com.ubtrobot.express.ExpressApi;
import com.ubtrobot.express.listeners.AnimationListener;
import com.ubtrobot.express.protos.Express;
import com.ubtrobot.master.Master;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

  ArrayAdapter<String> adapter;
  List<Express.ExpressInfo> infos;
  SeekBar mSeekBar;
  Button setFrameBtn;
  int progress;
  String expressName;
  int delta;
  TextView textView;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Master.initialize(this);
    setContentView(R.layout.activity_main);

    findViewById(R.id.onProgressExpress).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {

        startActivity(new Intent(MainActivity.this, ProgressTestActivity.class));
      }
    });

    //===========================================
    Spinner spinner = findViewById(R.id.spinner);
    adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
        new ArrayList<String>());
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    infos = ExpressApi.get().getExpressList();
    if (infos == null) return;
    List<String> names = new LinkedList<>();
    for (int i = 0; i < infos.size(); ++i) {
      names.add(infos.get(i).getName());
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      names.sort(new Comparator<String>() {
        @Override public int compare(String o1, String o2) {
          return o1.compareTo(o2);
        }
      });
    }
    names.add(0, "选择表情:");
    adapter.addAll(names);
    textView = findViewById(R.id.info_txt);

    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) return;
        expressName = adapter.getItem(position);
        Log.e("expressName", expressName);
        setExpressFrame();
        ExpressApi.get().doExpress(expressName, 1, Priority.MAXHIGH,new AnimationListener() {
          @Override public void onAnimationStart() {

          }

          @Override public void onAnimationEnd() {

          }

          @Override public void onAnimationRepeat(int loopNumber) {

          }
        });
        textView.setText(infos.get(position - 1).toString());
      }

      @Override public void onNothingSelected(AdapterView<?> parent) {

      }
    });

    findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        stopLoop = true;
      }
    });

    mSeekBar = findViewById(R.id.seekbar);
    setFrameBtn = findViewById(R.id.setExpressFrame);
    mSeekBar.setVisibility(View.GONE);
    setFrameBtn.setVisibility(View.GONE);

    findViewById(R.id.expressTween).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        triggerRandomExpress();
      }
    });

    findViewById(R.id.loopPlay).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        triggerLoopPlay();
      }
    });
  }

  private int loopIndex = 0;
  private volatile boolean stopLoop = false;

  private void triggerLoopPlay() {
    loopIndex = 0;
    if (infos == null || infos.size() == 0) return;
    findViewById(R.id.loopPlay).setClickable(false);
    loopPlay();
  }

  private void loopPlay() {
    textView.setText(infos.get(loopIndex).getName());
    ExpressApi.get().doExpress(infos.get(loopIndex).getName(), new AnimationListener() {
      @Override public void onAnimationStart() {

      }

      @Override public void onAnimationEnd() {
        if (++loopIndex < infos.size() && !stopLoop) {
          loopPlay();
        } else {
          findViewById(R.id.loopPlay).setClickable(true);
        }
      }

      @Override public void onAnimationRepeat(int loopNumber) {

      }
    });
  }

  private void triggerRandomExpress() {
    final Express.ExpressInfo info = getRandomExpress();
    final int loopCount = getRandomLoopCount();
    ExpressApi.get().doExpress(info.getName(), loopCount, Priority.NORMAL, new AnimationListener() {
      @Override public void onAnimationStart() {
        if (--triggerCount > 0) {
          HandlerUtils.runUITask(new Runnable() {
            @Override public void run() {
              triggerRandomExpress();
            }
          }, (info.getDuration() * loopCount) / 16);
        }
      }

      @Override public void onAnimationEnd() {

      }

      @Override public void onAnimationRepeat(int i) {

      }
    });
  }

  private Express.ExpressInfo getRandomExpress() {
    return infos.get(new Random(System.currentTimeMillis()).nextInt(infos.size()));
  }

  private int triggerCount = 1000;

  private int getRandomLoopCount() {
    return Math.max(1, new Random(System.currentTimeMillis()).nextInt(8));
  }

  private void setExpressFrame() {
    final List<Express.ExpressInfo> expresses = ExpressApi.get().getExpressList();
    if (expresses != null && expresses.size() > 0) {
      mSeekBar.setVisibility(View.VISIBLE);
      mSeekBar.setOnSeekBarChangeListener(null);
      setFrameBtn.setVisibility(View.VISIBLE);
      if (expressName != null) {
        setFrameBtn.setClickable(true);
        Express.ExpressInfo expressInfo = findExpress(expressName, expresses);
        if (expressInfo == null || expressInfo.getFormat() == Express.ExpressFormat.PNG) {
          mSeekBar.setVisibility(View.GONE);
          setFrameBtn.setVisibility(View.GONE);
          return;
        }
        final int frames = expressInfo.getFrames();
        setFrameBtn.setText("'" + expressInfo.getName() + "' 表情仿真");
        mSeekBar.setMax(frames);
        mSeekBar.setProgress(0);
        delta = expressInfo.getDuration() / expressInfo.getFrames();
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
          @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            ExpressApi.get().setFrame(expressName, progress);
          }

          @Override public void onStartTrackingTouch(SeekBar seekBar) {
          }

          @Override public void onStopTrackingTouch(SeekBar seekBar) {
          }
        });
        mSeekBar.setOnDragListener(null);
        mSeekBar.setClickable(false);

        setFrameBtn.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(final View v) {
            progress = 0;
            v.setClickable(false);
            mSeekBar.setProgress(progress++);
            setFrame();
          }
        });
      } else {
        setFrameBtn.setClickable(false);
      }
    } else {
      mSeekBar.setVisibility(View.GONE);
      setFrameBtn.setVisibility(View.GONE);
    }
  }

  private Express.ExpressInfo findExpress(String expressName, List<Express.ExpressInfo> expresses) {
    for (int i = 0; i < expresses.size(); ++i) {
      if (expresses.get(i).getName().equals(expressName)) return expresses.get(i);
    }
    return null;
  }

  private void setFrame() {
    HandlerUtils.runUITask(new Runnable() {
      @Override public void run() {
        mSeekBar.setProgress(progress++);
        if (progress == mSeekBar.getMax()) {
          setFrameBtn.setClickable(true);
        } else {
          setFrame();
        }
      }
    }, delta);
  }
}
