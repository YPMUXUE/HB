package Server.handler;

import Client.handler.SimpleTransferHandler;
import Client.util.Connections;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class DestinationConnectHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private byte[] destinationCache;
    private Channel connectToServerChannel;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte[] destination=new byte[6];
        msg.readBytes(destination);
        if (this.destinationCache == null){
            this.destinationCache=destination;
            msg.retain();
            Connections.newConnectionToServer(ctx
                    ,new InetSocketAddress(InetAddress.getByAddress(new byte[]{destination[0],destination[1],destination[2],destination[3]}),((destination[4] & 0xFF)<<8)|(destination[5] & 0xFF))
                    ,(status, channelToServer)->{
                        if (status==1){
                            ctx.pipeline().addAfter(ctx.name(),"DestinationConnectHandler*Transfer",new SimpleTransferHandler(channelToServer,true));
                            ctx.channel().writeAndFlush(Unpooled.copiedBuffer("HTTP/1.1 200 Connection Established\r\n\r\n", Charset.forName("utf-8")));
                        }else{
                            ctx.channel().close();
                        }
            });
        }else if (isSameDestination(destinationCache,destination)){
            //do nothing
            ctx.fireChannelRead(msg.retain());
        }else{
            this.connectToServerChannel.close();
            this.connectToServerChannel=null;
            //this.connectToServerChannel=Connections.newConnectionToServer();
        }


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
}
