package Server.handler;

import common.Message;
import common.log.LogUtil;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

public class HeaderIdentifyHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message) {
            Message m = (Message) msg;
            int operationCode = m.getOperationCode();
            if (!operationCodeRegistered(operationCode)) {
                String remoteAddress = ctx.channel().remoteAddress().toString();
                LogUtil.error(() -> "unregistered connection : remote IP:" + remoteAddress);
                ctx.channel().close();
                return;
            }
            ctx.fireChannelRead(msg);
        } else {
            throw new RuntimeException("HeaderIdentifyHandler#channelRead: msg is not a instance of Message");
        }
    }

    private boolean operationCodeRegistered(int operationCode) {
        ConnectionEvents[] connectionEvents = ConnectionEvents.values();
        for (ConnectionEvents connectionEvent : connectionEvents) {
            if (connectionEvent.getCode() == operationCode) {
                return true;
            }
        }
        return false;
    }
}
