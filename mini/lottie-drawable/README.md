# 控制版jni构建

## 发布命令

* 发布release 版:

```shell
 ./gradlew :mini:lottie-drawable:uploadArchives -Pshapshot=false
 ```

* 发布SNAPSHOT版：

```
  ./gradlew :mini:lottie-drawable:uploadArchives -Pshapshot=true
```

或者

```
  ./gradlew :mini:lottie-drawable:uploadArchives
```
