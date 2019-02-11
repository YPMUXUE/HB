package common.handler.coder;

import common.Message;
import common.util.MessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class MessageToByteBufOutboundHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Message){
            Message m=(Message)msg;
            ByteBuf buf= MessageUtil.MessageToByteBuf(m,ctx);
            ((Message) msg).release();
            ctx.writeAndFlush(buf);
        }else {
            super.write(ctx, msg, promise);
        }
    }
}
