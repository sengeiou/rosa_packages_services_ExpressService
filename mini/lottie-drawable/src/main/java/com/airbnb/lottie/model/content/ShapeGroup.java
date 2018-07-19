package com.airbnb.lottie.model.content;

import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.animation.content.Content;
import com.airbnb.lottie.animation.content.ContentGroup;
import com.airbnb.lottie.model.layer.BaseLayer;

import com.airbnb.lottie.model.layer.ComponentType;
import java.util.Arrays;
import java.util.List;

public class ShapeGroup implements ContentModel {
  private final String name;
  private final List<ContentModel> items;

  public ShapeGroup(String name, List<ContentModel> items) {
    this.name = name;
    this.items = items;
  }

  public String getName() {
    return name;
  }

  public List<ContentModel> getItems() {
    return items;
  }

  @Override public Content toContent(LottieDrawable drawable, BaseLayer layer) {
    return new ContentGroup(drawable, layer, this);
  }

  @Override public void tween(ComponentType componentType, float progress) {
    for (int i = 0 ; i < items.size(); ++i) {
      items.get(i).tween(componentType, progress);
    }
  }

  @Override public String toString() {
    return "ShapeGroup{" + "name='" + name + "\' Shapes: " + Arrays.toString(items.toArray()) + '}';
  }
}
