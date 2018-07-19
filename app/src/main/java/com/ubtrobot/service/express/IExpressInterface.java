package com.ubtrobot.service.express;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.ubtrobot.express.protos.Express;
import java.util.Collection;

/**
 * IExpressInterface
 *
 * @author logic
 */
public interface IExpressInterface {

  int doExpress(Express.DoExpressReq req);

  int doExpress(Express.DoExpressReq req, @NonNull AnimationListener listener);

  Collection<Express.ExpressInfo> getExpressList();

  int setFrame(Express.SetFrameReq frameReq);

  void doTween(AnimationListener listener);

  int doProgressExpress(String name, int status, int progress, boolean animated,
      final @Nullable AnimationListener listener);
}
