package Client.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

public class ConnectMethodHandler extends SimpleChannelInboundHandler {
    private final Channel clientChannel;
    public ConnectMethodHandler(Channel channel) {
        this.clientChannel =channel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        clientChannel.writeAndFlush(ReferenceCountUtil.retain(msg));
    }
}
