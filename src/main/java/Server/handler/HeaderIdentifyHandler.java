package Server.handler;

import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

public class HeaderIdentifyHandler extends ChannelDuplexHandler {
    public static final int heartBeat=0xCAFE;
    public static final int HTTPS=0x0320;
    public static final int HTTP=0x1995;
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf m=(ByteBuf)msg;
            int readIndex=m.readerIndex();
            int msgHeader=m.getChar(readIndex);
            switch (msgHeader){
                case HTTPS:
                    //跳过header和length
                    ctx.fireUserEventTriggered(ConnectionEvents.HTTPS_REQ);
                    m.readerIndex(readIndex+6);
                    ctx.fireChannelRead(m);
                    break;
                case heartBeat:
                    //nothing
                    break;
                case HTTP:
                    ctx.fireUserEventTriggered(ConnectionEvents.HTTP_REQ);
                    m.readerIndex(readIndex+6);
                    ctx.fireChannelRead(m);
                    break;
                default:
                    ctx.channel().close();
            }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf m=(ByteBuf)msg;
        ByteBuf result=ctx.alloc().buffer(6).writeBytes(new byte[]{(byte)0x03,(byte)0x20}).writeInt(m.readableBytes()).writeBytes(m);
        ReferenceCountUtil.release(msg);
        super.write(ctx, result, promise);
    }
}
