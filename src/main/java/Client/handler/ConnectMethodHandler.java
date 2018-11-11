package Client.handler;

import io.netty.channel.*;

public class ConnectMethodHandler extends ChannelDuplexHandler {
    private final Channel clientToServerChannel;
    public ConnectMethodHandler(Channel channel) {
        this.clientToServerChannel =channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        clientToServerChannel.writeAndFlush(msg);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        clientToServerChannel.close();
        super.close(ctx, promise);
    }
}
