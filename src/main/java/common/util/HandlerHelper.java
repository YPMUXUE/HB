package common.util;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class HandlerHelper {
    public static ChannelHandler newDefaultFrameDecoderInstance(){
        return  new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,2,4,0,0,true);
    }
}
