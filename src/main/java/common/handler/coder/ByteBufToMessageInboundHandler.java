package common.handler.coder;

import common.Message;
import common.resource.ConnectionEvents;
import common.util.MessageUtil;
import config.StaticConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

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
