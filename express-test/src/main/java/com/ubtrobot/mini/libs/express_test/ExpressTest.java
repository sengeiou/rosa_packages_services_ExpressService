package com.ubtrobot.mini.libs.express_test;

import android.app.Application;

import com.ubtrobot.master.Master;

/**
 * @author : kevin.liu@ubtrobot.com
 * @description :
 * @date : 2018/3/30
 * @modifier :
 * @modify time :
 */

public class ExpressTest extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Master.initialize(this);
    }
}
