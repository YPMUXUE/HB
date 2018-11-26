package Client.handler;

import Client.log.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;

public class SimpleTransferHandler extends ChannelInboundHandlerAdapter {
    protected final Channel targetChannel;
    private final boolean recycleTargetChannel;

    public SimpleTransferHandler(Channel channel) {
        targetChannel = channel;
        this.recycleTargetChannel = false;
    }

    public SimpleTransferHandler(Channel targetChannel, boolean recycleTargetChannel) {
        this.targetChannel = targetChannel;
        this.recycleTargetChannel = recycleTargetChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LogUtil.debug(()->(((ByteBuf)msg).toString(Charset.forName("utf-8"))));
        targetChannel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (recycleTargetChannel) {
            //recycle
//            targetChannel.deregister();
            targetChannel.close();
        }else{
            targetChannel.close();
        }
        super.channelInactive(ctx);
    }
}
