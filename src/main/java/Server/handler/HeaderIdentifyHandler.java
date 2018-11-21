package Server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class HeaderIdentifyHandler extends ChannelInboundHandlerAdapter {
    private static final int heartBeat=0xCAFE;
    private static final int header=0x0320;
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf m=(ByteBuf)msg;
            int readIndex=m.readerIndex();
            int msgHeader=m.getChar(readIndex);
            switch (msgHeader){
                case header:
                    //跳过header和length
                    m.readerIndex(readIndex+6);
                    ctx.fireChannelRead(m);
                    break;
                case heartBeat:
                    //nothing
                default:
                    ctx.channel().close();
            }
    }
}
