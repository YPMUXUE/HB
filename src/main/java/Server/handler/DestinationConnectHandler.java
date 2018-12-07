package Server.handler;

import common.handler.SimpleTransferHandler;
import common.log.LogUtil;
import common.resource.ConnectionEvents;
import common.util.Connections;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import common.resource.HttpResources;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;

public class DestinationConnectHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private byte[] destinationCache;
    private volatile Channel connectToServerChannel;
    private volatile boolean connectFinished=false;
    private boolean isHTTPS;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte[] destination=new byte[6];
        msg.readBytes(destination);
        if (this.destinationCache == null){
            this.destinationCache=destination;
            boolean isHTTPS=this.isHTTPS;
            if (!isHTTPS){
                msg.retain();
            }
            ChannelFuture channelFuture=Connections.newConnectionToServer(ctx.channel().eventLoop()
                    ,new InetSocketAddress(InetAddress.getByAddress(new byte[]{destination[0],destination[1],destination[2],destination[3]}),((destination[4] & 0xFF)<<8)|(destination[5] & 0xFF))
                    ,(status, channelToServer)->{
                        if (status==1){
                            channelToServer.pipeline().addLast("ConnectionToServer*transfer",new SimpleTransferHandler(ctx.channel()));
                            this.connectFinished=true;
                            this.connectToServerChannel=channelToServer;

                            ctx.pipeline().addAfter(ctx.name(),"DestinationConnectHandler*Transfer",new SimpleTransferHandler(channelToServer,true));
                            if (isHTTPS) {
                                ctx.channel().writeAndFlush(Unpooled.copiedBuffer(HttpResources.HttpResponse.Connection_Established, Charset.forName("utf-8")));
                            }else{
                                ctx.fireChannelRead(msg);
                            }
                        }else{
                            LogUtil.info(()->channelToServer.toString()+" connect failed");
                            ctx.channel().close();
                        }
            });
        }else if (isSameDestination(destinationCache,destination)){
            if (this.connectToServerChannel == null){
                LogUtil.info(()->Arrays.toString(destination)+" the connection is not connect finish yet,but there is message inbound");
                ctx.close();
            }
            if (this.connectToServerChannel.isOpen()) {
                ctx.fireChannelRead(msg.retain());
            } else{
                reConnect(ctx,destination,msg);
            }
        }else{
            if (this.connectFinished) {
                this.connectToServerChannel.close();
            }
            this.connectToServerChannel = null;
            reConnect(ctx,destination,msg);
        }


    }

    private void reConnect(ChannelHandlerContext ctx, byte[] destination, ByteBuf msg) throws Exception {
        boolean isHTTPS=this.isHTTPS;
        if (!isHTTPS){
            msg.retain();
        }
        ChannelFuture channelFuture=Connections.newConnectionToServer(ctx.channel().eventLoop()
                , new InetSocketAddress(InetAddress.getByAddress(new byte[]{destination[0], destination[1], destination[2], destination[3]}), ((destination[4] & 0xFF) << 8) | (destination[5] & 0xFF))
                , (status, channelToServer)->{
                    if (status==1){
                        channelToServer.pipeline().addLast("ConnectionToServer*transfer",new SimpleTransferHandler(ctx.channel(),true));
                        this.connectFinished=true;
                        this.connectToServerChannel=channelToServer;

                        ctx.pipeline().replace("DestinationConnectHandler*Transfer","DestinationConnectHandler*Transfer",new SimpleTransferHandler(channelToServer,true));
                        if (isHTTPS) {
                            ctx.channel().writeAndFlush(Unpooled.copiedBuffer(HttpResources.HttpResponse.Connection_Established, Charset.forName("utf-8")));
                        }else{
                            ctx.fireChannelRead(msg);
                        }
                    }else{
                        LogUtil.info(()->channelToServer.toString()+" connect failed");
                        ctx.channel().close();
                    }
                });
    }

    private boolean isSameDestination(byte[] b1,byte[] b2){
        if (b1==null || b2 == null){
            return false;
        }
        if (b1.length != b2.length){
            return false;
        }
        for (int i = 0; i < b1.length; i++) {
            if (b1[i]!=b2[i]){
                return false;
            }
        }
        return true;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof ConnectionEvents){
            switch ((ConnectionEvents)evt){
                case RECONNECT_IF_NECESSARY:
                    this.isHTTPS =false;
                    break;
                case USE_OLD_CONNECTION:
                    this.isHTTPS =true;
                    break;
            }
        }else{
            ctx.fireUserEventTriggered(evt);
        }
    }
}
