# 表情服务sdk构建

## 构建

* 打包jar
```shell
  ./gradlew jarSdk
```


## 发布命令

* 发布release 版:

```shell
 ./gradlew :express-sdk:jarSdk :express-sdk:artifactoryPublish -Pshapshot=false
 ```

* 发布SNAPSHOT版：

```
  ./gradlew :express-sdk:jarSdk :express-sdk:artifactoryPublish -Pshapshot=true
```

或者

```
  ./gradlew :express-sdk:jarSdk :express-sdk:artifactoryPublish
```
