package Server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class DestinationConnectHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private byte[] destinationCache;
    private Channel connectToServerChannel;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte[] destination=new byte[6];
        msg.readBytes(destination);
        if (this.destinationCache == null){
            //this.connectToServerChannel= Connections.newConnectionToServer();
        }else if (isSameDestination(destinationCache,destination)){
            //do nothing
        }else{
            this.connectToServerChannel.close();
            this.connectToServerChannel=null;
            //this.connectToServerChannel=Connections.newConnectionToServer();
        }

    }
    private boolean isSameDestination(byte[] b1,byte[] b2){
        for (int i = 0; i < b1.length; i++) {
            if (b1[i]!=b2[i]){
                return false;
            }
        }
        return true;
    }
}
