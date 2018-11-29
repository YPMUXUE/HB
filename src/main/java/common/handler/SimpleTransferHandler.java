package common.handler;

import common.log.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;

public class SimpleTransferHandler extends ChannelInboundHandlerAdapter {
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

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (closeTargetChannel) {
            //recycle
//            targetChannel.deregister();
            targetChannel.close();
        }
        super.channelInactive(ctx);
    }
}
