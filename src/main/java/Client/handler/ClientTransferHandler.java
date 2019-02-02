package Client.handler;

import Client.bean.HostAndPort;
import common.Message;
import common.handler.SimpleTransferHandler;
import common.resource.ConnectionEvents;
import common.resource.HttpResources;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.nio.charset.Charset;

public class ClientTransferHandler extends SimpleTransferHandler {
    public ClientTransferHandler(Channel targetChannel, boolean closeTargetChannel) {
        super(targetChannel, closeTargetChannel);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf){
            super.channelRead(ctx,new Message(ConnectionEvents.CONNECT.getCode(),(HostAndPort) null,(ByteBuf) msg));
        }else if (msg instanceof Message){
            super.channelRead(ctx,msg);
        }else{
            throw new RuntimeException("ClientTransferHandler#channelRead the msg is not a instance of Message or ByteBuf");
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            super.write(ctx, msg, promise);
        }else if (msg instanceof Message){
//            super.write(ctx,((Message)msg).getContent(),promise);
           super.write(ctx,handlerMessage(ctx,(Message) msg),promise);
        }else{
            throw new RuntimeException("ClientTransferHandler#write the msg is not a instance of Message or ByteBuf");
        }
    }

    private ByteBuf handlerMessage(ChannelHandlerContext ctx, Message msg) {
        if (msg.getOperationCode()==ConnectionEvents.CONNECTION_ESTABLISH.getCode()){
            msg.release();
            return Unpooled.buffer().writeBytes(HttpResources.HttpResponse.Connection_Established.getBytes(Charset.forName("utf-8")));
        }else{
            return msg.getContent();
        }
    }
}
