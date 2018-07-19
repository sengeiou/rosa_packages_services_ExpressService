# Mini表情服务

## 修改历史

|Version |Contributor| Date| Change Log|
|----|:---|:---|:---|
|v1.0|彭钉|17/12/26|新增Mini表情服务接口说明
|v1.1|彭钉|18/02/02|1、表情过渡功能支持可配;<br>2、新增PC表情仿真接口
|||||

## 概述

Mini服务是ROSE中一个服务应用，安装表情服务相应要安装ROSE中Master应用，master应用的安装参看[《master用户指导》](https://10.10.1.34/Rose/rosa_packages_Master/blob/master/doc/user-guide.md)。目前，Mini的眼部是两个LCD屏，通过在屏上渲染眼睛动画，Mini可显示设计给出的预定义表情，本文档简要说明这些接口及sdk集成。

## SDK

* protobuf 结构

```javascript
enum ExpressFormat {
    GIF = 0;
    PNG = 1;
}

//表情信息
message ExpressInfo {
    int32 id = 1;//id
    string name = 2;//名称
    int32 duration = 3;//时长
    ExpressFormat format = 4;
}

//请求参数
message DoExpressReq {
    string name = 1;
    float speed = 2;
    int32 repeat = 3;
    bool tweenable = 4;//表情过渡开关
}

```

* java 接口

```java
/**
* 获取机器人上支持的表情
*
* @return list of Express.ExpressInfo
*/
public @Nullable List<Express.ExpressInfo> getExpressList();

/**
* 做一个表情动作, 不关心动画状态
*
* @param name      表情名称
* @param loopCount 重复次数,,0表示循环播放
*/
public void doExpress(String name, @IntRange(from = 0, to = Character.MAX_VALUE) int loopCount);

/**
* 做表情动画，并监听表情动画状态
*
* @param name      表情名称
* @param loopCount 循环次数,0表示循环播放
* @param tweenable  是否使能表情过渡
* @param listener  动画监听
*/
public void doExpress(String name, @IntRange(from = 0, to = Character.MAX_VALUE) final int loopCount, boolean tweenable, @Nullable final AnimationListener listener);

/**
* PC端仿真调用，显示某个表情的指定帧
*
* @param name 表情名
* @param frame 帧
*/
public void setFrame(String name, @IntRange(from = 0) int frame);

/**
 * 主动调用tween动画, 使当前表情复位到正常
 * 
 * @param listener listener
 */
public void doExpressTween(@Nullable final AnimationListener listener)


public interface AnimationListener {
/**
* Notifies the start of animation.
*/
void onAnimationStart();

/**
* Notifies the end of animation.
*/
void onAnimationEnd();

/**
* @param loopNumber Number of loop
*/
void onAnimationRepeat(int loopNumber);
}

```

## 导入sdk

```groovy
   compile 'com.ubtrobot.lib.packets:express-sdk:x.y.z'
```