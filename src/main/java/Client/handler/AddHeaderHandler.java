package Client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class AddHeaderHandler extends MessageToMessageEncoder<ByteBuf> {
    private static final byte[] header=new byte[]{(byte)0xCA,(byte)0xFE};

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        out.add(ctx.alloc().buffer(header.length).writeBytes(header));
        out.add(msg.retain());
    }

}
