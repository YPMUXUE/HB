package Server.handler;

import common.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class MessageTransferHandler extends ChannelDuplexHandler {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            super.channelRead(ctx, Message.resloveRequest((ByteBuf) msg));
        }else{
            super.channelRead(ctx,msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Message){
            Message m=(Message)msg;
            ByteBuf out=ctx.alloc().buffer(6+m.getContent().readableBytes())
                    .writeShort(m.getOperationCode())
                    .writeInt(m.getContent().readableBytes())
                    .writeBytes(m.getContent());
            super.write(ctx,out,promise);
        }else {
            super.write(ctx, msg, promise);
        }
    }
}
