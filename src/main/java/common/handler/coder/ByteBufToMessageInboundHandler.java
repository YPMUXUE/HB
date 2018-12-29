package common.handler.coder;

import common.Message;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ByteBufToMessageInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf m=(ByteBuf)msg;
            short operationCode=m.readShort();
            int contentLength=m.readInt();
            byte[] des=null;
            if (operationCode== ConnectionEvents.BIND.getCode()){
                des=new byte[6];
                m.readBytes(des);
                contentLength=contentLength-6;
            }
            Message message=new Message(operationCode,des,m);
            message.setContentLength(contentLength);
            super.channelRead(ctx, message);
        }else{
            super.channelRead(ctx,msg);
        }
    }
}
