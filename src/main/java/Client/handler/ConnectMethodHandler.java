package Client.handler;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;

public class ConnectMethodHandler extends ChannelDuplexHandler {
    private final Channel clientChannel;
    public ConnectMethodHandler(Channel channel) {
        this.clientChannel =channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        clientChannel.writeAndFlush(msg);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        clientChannel.close();
        super.close(ctx, promise);
    }
}
