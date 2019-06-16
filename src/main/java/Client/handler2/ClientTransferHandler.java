package Client.handler2;

import common.handler.SimpleTransferHandler;
import common.message.frame.Message;
import common.message.frame.close.CloseMessage;
import common.message.frame.connect.ConnectMessage;
import common.message.frame.establish.ConnectionEstablishFailedMessage;
import common.message.frame.establish.ConnectionEstablishMessage;
import common.resource.HttpResources;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.nio.charset.Charset;

public class ClientTransferHandler extends SimpleTransferHandler {
    public static final MessageProcessor HTTPS_PROCESSOR = (ctx, msg) -> {
        if (msg instanceof ConnectionEstablishMessage) {
            return Unpooled.buffer().writeBytes(HttpResources.HttpResponse.Connection_Established.getBytes(Charset.forName("utf-8")));
        } else if(msg instanceof ConnectionEstablishFailedMessage) {
            return Unpooled.buffer().writeBytes(HttpResources.HttpResponse.Connection_Failed.getBytes(Charset.forName("utf-8")));
        }else if (msg instanceof ConnectMessage){
            return ((ConnectMessage) msg).getContent();
        }else if (msg instanceof CloseMessage){
            ctx.channel().close();
            return null;
        }else{
            throw new UnsupportedOperationException(msg.getClass().toString());
        }
    };
    private final MessageProcessor messageProcessor;


    public ClientTransferHandler(Channel targetChannel, boolean closeTargetChannel) {
        super(targetChannel, closeTargetChannel);
        this.messageProcessor=HTTPS_PROCESSOR;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            Message m = new ConnectMessage((ByteBuf) msg);
            super.channelRead(ctx, m);
        } else if (msg instanceof Message) {
            super.channelRead(ctx, msg);
        } else {
            throw new RuntimeException("ClientTransferHandler#channelRead the msg is not a instance of Message or ByteBuf");
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            super.write(ctx, msg, promise);
        } else if (msg instanceof Message) {
            ByteBuf result = handleMessage(ctx, (Message) msg);
            if (result != null) {
                super.write(ctx, result, promise);
            }
        } else {
            throw new RuntimeException("ClientTransferHandler#write the msg is not a instance of Message or ByteBuf");
        }
    }

    private ByteBuf handleMessage(ChannelHandlerContext ctx, Message msg) {
        return messageProcessor.handleMessage(ctx, msg);
    }

    public interface MessageProcessor {
        ByteBuf handleMessage(ChannelHandlerContext ctx, Message msg);
    }
}
