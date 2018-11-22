package Client.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

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
        targetChannel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (closeTargetChannel) {
            targetChannel.close();
        }
        super.channelInactive(ctx);
    }
}
