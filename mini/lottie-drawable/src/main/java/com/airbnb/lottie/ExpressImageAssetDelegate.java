package com.airbnb.lottie;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.ubtrobot.mini.properties.sdk.PropertiesApi;

/**
 * Created by logic on 18-1-9.
 */

public final class ExpressImageAssetDelegate implements ImageAssetDelegate {
    private final String expressDir =
            PropertiesApi.getRootPath() + "/expresss/";
    private BitmapFactory.Options options = new BitmapFactory.Options();

    public ExpressImageAssetDelegate() {
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    @Override
    public Bitmap fetchBitmap(LottieImageAsset asset) {
        return BitmapFactory.decodeFile(expressDir + asset.getDirName() + asset.getFileName(), options);
    }
}
