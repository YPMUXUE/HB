package common.handler.coder;

import common.Message;
import common.log.LogUtil;
import common.util.MessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

@Deprecated
public class MessageToByteBufOutboundHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Message){
            Message m=(Message)msg;
            ByteBuf buf= MessageUtil.MessageToByteBuf(m,ctx);
            m.release();
            ctx.writeAndFlush(buf);
        }else {
            LogUtil.error(()->"msg is not a instance of Message");
            throw new RuntimeException("msg is not a instance of Message;"+msg);
        }
    }
}
