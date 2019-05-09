package common.util;

import common.resource.SystemConfig;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class HandlerHelper {
    public static ChannelHandler newDefaultFrameDecoderInstance(){
        return  new LengthFieldBasedFrameDecoder(SystemConfig.PACKAGE_MAX_LENGTH,2,4,0,0,true);
    }
}
