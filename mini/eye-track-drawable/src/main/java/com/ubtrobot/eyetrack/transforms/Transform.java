package com.ubtrobot.eyetrack.transforms;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public interface Transform {

  void onBoundsChange(Rect bounds);

  void onDraw(Canvas canvas, Paint paint, Bitmap buffer);
}
