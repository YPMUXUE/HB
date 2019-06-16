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

@Deprecated
public class ClientTransferHandler extends SimpleTransferHandler {
    public static final MessageProcessor HTTPS_PROCESSOR = (ctx, targetChannel, msg) -> {
        if (msg.getOperationCode() == ConnectionEvents.CONNECTION_ESTABLISH.getCode()) {
            return Unpooled.buffer().writeBytes(HttpResources.HttpResponse.Connection_Established.getBytes(Charset.forName("utf-8")));
        } else {
            return msg.getContent().retain();
        }
    };
    private final MessageProcessor messageProcessor;

    public ClientTransferHandler(Channel targetChannel, boolean closeTargetChannel, MessageProcessor messageProcessor) {
        super(targetChannel, closeTargetChannel);
        this.messageProcessor=messageProcessor;
    }

    public ClientTransferHandler(Channel targetChannel, boolean closeTargetChannel) {
        super(targetChannel, closeTargetChannel);
        this.messageProcessor=HTTPS_PROCESSOR;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            super.channelRead(ctx, new Message(ConnectionEvents.CONNECT.getCode(), (HostAndPort) null, (ByteBuf) msg));
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
        try {
            return messageProcessor.handleMessage(ctx, super.targetChannel, msg);
        } finally {
            msg.release();
        }
    }

    public interface MessageProcessor {
        ByteBuf handleMessage(ChannelHandlerContext ctx, Channel targetChannel, Message msg);
    }
}
