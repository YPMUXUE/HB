package Client.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class TransferToProxyServerHandler extends SimpleTransferHandler {
    private String uri;

    public TransferToProxyServerHandler(String uri, Channel targetChannel) {
        super(targetChannel);
        this.uri = uri;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    }
}
