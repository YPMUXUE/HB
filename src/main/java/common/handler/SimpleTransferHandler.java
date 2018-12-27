package common.handler;

import common.log.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import java.nio.charset.Charset;

public class SimpleTransferHandler extends ChannelDuplexHandler {
    protected final Channel targetChannel;
    private final boolean closeTargetChannel;

    public SimpleTransferHandler(Channel channel) {
        targetChannel = channel;
        this.closeTargetChannel = false;
    }

    public SimpleTransferHandler(Channel targetChannel, boolean closeTargetChannel) {
        this.targetChannel = targetChannel;
        this.closeTargetChannel = closeTargetChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LogUtil.debug(()->(((ByteBuf)msg).toString(Charset.forName("utf-8"))));
        targetChannel.writeAndFlush(msg);
    }

//    @Override
//    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//        if (closeTargetChannel) {
//            //recycle
////            targetChannel.deregister();
//            targetChannel.close();
//        }
//        super.channelInactive(ctx);
//    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (closeTargetChannel) {
            targetChannel.close();
        }
        super.close(ctx,promise);
    }
}
