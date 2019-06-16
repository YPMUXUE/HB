package common.handler.coder;

import common.Message;
import common.util.MessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Deprecated
public class ByteBufToMessageInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            Message message=MessageUtil.ByteBufToMessage((ByteBuf)msg);
            super.channelRead(ctx, message);
        }else{
            super.channelRead(ctx,msg);
        }
    }
}
