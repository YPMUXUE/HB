package Client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class AddHeaderHandler extends ChannelDuplexHandler {
    private static final byte[] heartBeat=new byte[]{(byte)0xCA,(byte)0xFE};
    private static final byte[] header=new byte[]{(byte)0x03,(byte)0x20};
    private boolean runAsServer = false;

    public AddHeaderHandler(boolean runAsServer) {
        this.runAsServer=runAsServer;
    }

    public AddHeaderHandler() {
        this(false);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            msg = ctx.alloc().buffer(header.length).writeBytes(header).writeBytes((ByteBuf)msg);
        }
        super.write(ctx, msg, promise);
    }
}
