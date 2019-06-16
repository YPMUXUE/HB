package Server.handler;

import common.Message;
import common.resource.ConnectionEvents;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


@Deprecated
public class HeaderIdentifyHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message) {
            Message m = (Message) msg;
            int operationCode = m.getOperationCode();
            if (!operationCodeRegistered(operationCode)) {
                String remoteAddress = ctx.channel().remoteAddress().toString();
                throw new RuntimeException("unregistered connection : remote IP:" + remoteAddress);
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
