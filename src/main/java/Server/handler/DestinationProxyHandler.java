package Server.handler;

import common.Message;
import common.resource.SystemConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class DestinationProxyHandler extends ChannelInboundHandlerAdapter {
    private byte[] destinationCache;
    private volatile Channel connectToServerChannel;
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message){
            Message m=(Message)msg;
            switch (m.getOperationCode()){
                case SystemConfig.OPEN_CONNECTION:
                    handleOpenConnection(ctx,m);break;
                case SystemConfig.ONE_OFF_CONNECTION:
                    handleOneOffConnection(ctx,m);break;
                case SystemConfig.NEW_KEEP_CONNECTION:
                    handleNewKeepConnection(ctx,m);break;
                case SystemConfig.SIMPLE_MESSAGE:

                default:
                    ctx.channel().close();
            }
//            byte[] destination=m.getDestination();
//            if (this.destinationCache == null) {
//                this.destinationCache = destination;
//            }
        }else {
            ctx.fireChannelRead(msg);
        }
    }

    private void handleNewKeepConnection(ChannelHandlerContext ctx, Message m) {
    }

    private void handleOneOffConnection(ChannelHandlerContext ctx, Message m) {
        
    }

    private void handleOpenConnection(ChannelHandlerContext ctx, Message m) {
        
    }
}
