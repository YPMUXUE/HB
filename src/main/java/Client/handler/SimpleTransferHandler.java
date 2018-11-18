package Client.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class SimpleTransferHandler extends ChannelInboundHandlerAdapter {
    private final Channel targetChannel;

    public SimpleTransferHandler(Channel channel) {
        targetChannel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        targetChannel.writeAndFlush(msg);
    }

}
