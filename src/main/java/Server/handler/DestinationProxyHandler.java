package Server.handler;

import common.Message;
import common.handler.SimpleTransferHandler;
import common.log.LogUtil;
import common.resource.ConnectionEvents;
import common.resource.HttpResources;
import common.resource.SystemConfig;
import common.util.Connections;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class DestinationProxyHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message){
            Message m=(Message)msg;
            int operationCode=m.getOperationCode();
            if (operationCode == ConnectionEvents.BIND.getCode()){
                handleBind(ctx,m);
            }else if(operationCode == ConnectionEvents.CONNECT.getCode()) {
                handleConnect(ctx,m);
            }else{
                throw new Exception("a unsupported header :"+operationCode);
            }
        }else {
            ctx.fireChannelRead(msg);
        }
    }

    private void handleConnect(ChannelHandlerContext ctx, Message m) {
    }

    private void handleBind(ChannelHandlerContext ctx, Message m) throws Exception {
        byte[] des=m.getDestination();
        ChannelFuture channelFuture= Connections.newConnectionToServer(ctx.channel().eventLoop()
                ,new InetSocketAddress(InetAddress.getByAddress(new byte[]{des[0],des[1],des[2],des[3]}),((des[4] & 0xFF)<<8)|(des[5] & 0xFF))
                ,(status, channelToServer)->{
                    if (status==SystemConfig.SUCCESS){
                        channelToServer.pipeline().addLast("ConnectionToServer*transfer",new SimpleTransferHandler(ctx.channel()));
                        ctx.pipeline().addAfter(ctx.name(),"DestinationConnectHandler*Transfer",new SimpleTransferHandler(channelToServer,true));





                    }else{
                        LogUtil.info(()->channelToServer.toString()+" connect failed");
                        ctx.channel().close();
                    }
                });
    }


}
