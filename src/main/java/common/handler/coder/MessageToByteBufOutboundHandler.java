package common.handler.coder;

import common.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class MessageToByteBufOutboundHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Message){
            Message m=(Message)msg;
            int bufferSize=2 + 4 + m.getContent().readableBytes();
            //暂时还不需要写入IP地址信息
            ByteBuf out=ctx.alloc().buffer(bufferSize)
                    .writeShort(m.getOperationCode())
                    .writeInt(m.getContent().readableBytes())
                    .writeBytes(m.getContent());
            ctx.writeAndFlush(out);
        }else {
            super.write(ctx, msg, promise);
        }
    }
}
