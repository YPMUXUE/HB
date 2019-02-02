package common.handler.coder;

import common.Message;
import common.util.MessageUtill;
import config.StaticConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class MessageToByteBufOutboundHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Message){
            Message m=(Message)msg;
//            int bufferSize= StaticConfig.HEADER_LENGTH + StaticConfig.LENGTH_HEADER_LENGTH + m.getContent().readableBytes();
//            if (m.getDestination()!=null && m.getDestination().length > 0) {
//                bufferSize=bufferSize+m.getDestination().length;
//            }
//            ByteBuf out=ctx.alloc().buffer(bufferSize)
//                    .writeShort(m.getOperationCode())
//                    .writeInt(m.getContent().readableBytes());
//            if (m.getDestination()!=null && m.getDestination().length > 0){
//                out.writeBytes(m.getDestination());
//            }
//            out.writeBytes(m.getContent());
            ByteBuf buf=MessageUtill.MessageToByteBuf(m,ctx);
            ((Message) msg).release();
            ctx.writeAndFlush(buf);
        }else {
            super.write(ctx, msg, promise);
        }
    }
}
