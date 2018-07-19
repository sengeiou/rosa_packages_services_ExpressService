# 表情服务集成指南

## 表情服务安装

* 确保动作服务apk已安装到系统中

* 构建alpha2表情服务apk

```javascript
./gradlew alphaRelease
```

* 安装到alpha2或lynx机器

```javascript
adb install -r ./app/build/outputs/apk/alpha_express_service__*_release.apk
```

## Gradle 依赖配置

```groovy
 compile 'com.ubtrobot.lib.packets:express-sdk:x.y.z'
```

## 权限配置

* 在AndroidManifest.xml中配置集成至服务总线的权限

```xml
<uses-permission android:name="com.ubtrobot.master.permission.SERVICE_BUS" />
```

## API 简要说明

* ExpressApi -- 表情控制相关接口

```java
/**
* 获取机器人上支持的表情
*
* @return list of Express.ExpressInfo
*/
public @Nullable List<Express.ExpressInfo> getExpressList();

/**
* 做一个表情动作, 不关系动画状态
*
* @param name      表情名称
* @param speed     默认1.0f
* @param loopCount 重复次数,,0表示循环播放
*/
public void doExpress(String name, float speed, @IntRange(from = 0, to = Character.MAX_VALUE) int loopCount);

/**
* 做表情动画，并监听表情动画状态
*
* @param name      表情名称
* @param speed     默认1.0f
* @param loopCount 循环次数,0表示循环播放
* @param listener  动画监听
*/
public void doExpress(String name, float speed, @IntRange(from = 0, to = Character.MAX_VALUE) final int loopCount, @Nullable final AnimationListener listener);
```