package common.handler;

import io.netty.channel.*;

public class SimpleTransferHandler extends ChannelDuplexHandler {
    protected final Channel targetChannel;
    protected final boolean closeTargetChannel;

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
//        LogUtil.debug(()->(((ByteBuf)msg).toString(Charset.forName("utf-8"))));

        targetChannel.writeAndFlush(msg);
    }


    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (targetChannel!=null && targetChannel.isActive() && closeTargetChannel) {
            targetChannel.eventLoop().submit(()->{
                targetChannel.close();
            });
        }
        super.close(ctx,promise);
    }
}
