package Client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;

public class ForwardToServerHandler extends ChannelInitializer {
    private final Channel forwardToServerChannel;

    public ForwardToServerHandler(Channel channel) {
        forwardToServerChannel = channel;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext c, ByteBuf msg) {
                forwardToServerChannel.writeAndFlush(msg.retain());
            }
        }).addLast("ExceptionHandler",new ExceptionLoggerHandler("ClientToServer"));
    }
}
