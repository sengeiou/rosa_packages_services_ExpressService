package com.ubtrobot.service;

import com.google.protobuf.Int32Value;
import com.ubtrobot.express.protos.Express;
import com.ubtrobot.express.protos.ProgressExpress;
import com.ubtrobot.master.annotation.Call;
import com.ubtrobot.master.competition.CompetingItemDetail;
import com.ubtrobot.master.param.ProtoParam;
import com.ubtrobot.master.service.MasterSystemService;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.service.express.AnimationListener;
import com.ubtrobot.service.express.ExpressServiceImpl;
import com.ubtrobot.service.express.IExpressInterface;
import com.ubtrobot.service.utils.ProcessProtectUtils;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Responder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 表情服务
 *
 * @author Logic
 */
public final class ExpressService extends MasterSystemService {
  private IExpressInterface impl;

  @Override protected void onServiceCreate() {
    super.onServiceCreate();
    impl = ExpressServiceImpl.get();
    ProcessProtectUtils.sendProcessRegisterBroadcast(this);
  }

  @Override protected void onCall(Request request, Responder responder) {
    responder.respondFailure(CallGlobalCode.INTERNAL_ERROR,
        "unsupported call: " + request.getPath());
  }

  @Call(path = "/getExpressList")
  public void onGetExpressList(Request request, Responder responder) {
    try {
      Collection<Express.ExpressInfo> expressInfos = impl.getExpressList();
      if (expressInfos == null) {
        responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, "express scan exception");
      } else {
        responder.respondSuccess(ProtoParam.create(
            Express.ExpressListRes.newBuilder().addAllExpress(expressInfos).build()));
      }
    } catch (Exception e) {
      e.printStackTrace();
      responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, "call failed : " + e.getMessage());
    }
  }

  @Call(path = "/doExpress") public void onDoExpress(Request request, Responder responder) {
    try {
      Express.DoExpressReq value =
          ProtoParam.from(request.getParam(), Express.DoExpressReq.class).getProtoMessage();
      int code = impl.doExpress(value);
      if (code == 0) {
        responder.respondSuccess();
      } else {
        responder.respondFailure(CallGlobalCode.BAD_REQUEST,
            "unsupported express: " + value.getName());
      }
    } catch (ProtoParam.InvalidProtoParamException e) {
      e.printStackTrace();
      responder.respondFailure(CallGlobalCode.BAD_REQUEST, "call failed : " + e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, "call failed : " + e.getMessage());
    }
  }

  @Call(path = "/doExpressStickily")
  public void onDoExpressStickily(final Request request, final Responder responder) {
    try {
      Express.DoExpressReq value =
          ProtoParam.from(request.getParam(), Express.DoExpressReq.class).getProtoMessage();
      int ret = impl.doExpress(value, new AnimationListener() {
        @Override public void onAnimationStart() {
          responder.respondStickily(ProtoParam.create(Int32Value.newBuilder().setValue(0).build()));
        }

        @Override public void onAnimationEnd() {
          responder.respondSuccess();
        }

        @Override public void onAnimationRepeat(int loopNumber) {
          responder.respondStickily(
              ProtoParam.create(Int32Value.newBuilder().setValue(loopNumber).build()));
        }
      });
      if (ret != 0) {
        responder.respondFailure(CallGlobalCode.INTERNAL_ERROR,
            "unsupported express:" + value.getName());
      }
    } catch (ProtoParam.InvalidProtoParamException e) {
      e.printStackTrace();
      responder.respondFailure(CallGlobalCode.BAD_REQUEST, "call failed : " + e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, "call failed : " + e.getMessage());
    }
  }

  @Call(path = "/setExpressFrame")
  public void onSetExpressFrame(final Request request, final Responder responder) {
    try {
      Express.SetFrameReq value =
          ProtoParam.from(request.getParam(), Express.SetFrameReq.class).getProtoMessage();
      int ret = impl.setFrame(value);
      if (ret != 0) {
        responder.respondFailure(CallGlobalCode.INTERNAL_ERROR,
            "unsupported express:" + value.getName());
      }
    } catch (ProtoParam.InvalidProtoParamException e) {
      e.printStackTrace();
      responder.respondFailure(CallGlobalCode.BAD_REQUEST, "call failed : " + e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, "call failed : " + e.getMessage());
    }
  }

  @Call(path = "/doExpressTween")
  public void onDoExpressTween(final Request request, final Responder responder) {
    try {
      impl.doTween(new AnimationListener() {
        @Override public void onAnimationStart() {
          responder.respondStickily(ProtoParam.create(Int32Value.newBuilder().setValue(0).build()));
        }

        @Override public void onAnimationEnd() {
          responder.respondSuccess();
        }

        @Override public void onAnimationRepeat(int loopNumber) {
          responder.respondStickily(ProtoParam.create(Int32Value.newBuilder().setValue(1).build()));
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
      responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, "call failed : " + e.getMessage());
    }
  }

  @Call(path = "/doProgressExpress")
  public void onDoProgressExpress(final Request request, final Responder responder) {
    ProgressExpress.DoProgressExpressReq doProgressExpressReq;
    try {
      doProgressExpressReq =
          ProtoParam.from(request.getParam(), ProgressExpress.DoProgressExpressReq.class)
              .getProtoMessage();
    } catch (ProtoParam.InvalidProtoParamException e) {
      e.printStackTrace();
      responder.respondFailure(CallGlobalCode.BAD_REQUEST, "call failed : " + e.getMessage());
      return;
    }
    //
    if (doProgressExpressReq != null) {

      impl.doProgressExpress(doProgressExpressReq.getName(), doProgressExpressReq.getStatus(),
          doProgressExpressReq.getProgress(), doProgressExpressReq.getAnimated(),
          new AnimationListener() {
            @Override public void onAnimationStart() {
              responder.respondStickily(
                  ProtoParam.create(Int32Value.newBuilder().setValue(0).build()));
            }

            @Override public void onAnimationEnd() {
              responder.respondSuccess();
            }

            @Override public void onAnimationRepeat(int loopNumber) {
              responder.respondStickily(
                  ProtoParam.create(Int32Value.newBuilder().setValue(1).build()));
            }
          });
    } else {
      responder.respondFailure(CallGlobalCode.BAD_REQUEST,
          "call failed : " + "request params error !!!");
    }
  }

  @Override protected List<CompetingItemDetail> getCompetingItems() {
    return Collections.singletonList(
        new CompetingItemDetail.Builder("express", "eye-all").addCallPath("/doExpressTween")
            .addCallPath("/doExpressStickily")
            .addCallPath("/doExpress")
            .addCallPath("/doProgressExpress")
            .build());
  }
}

