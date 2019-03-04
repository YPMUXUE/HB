package Server.handler;

import common.Message;
import common.handler.SimpleTransferHandler;
import common.log.LogUtil;
import common.resource.ConnectionEvents;
import common.resource.SystemConfig;
import common.util.Connections;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class DestinationProxyHandler extends ChannelDuplexHandler {

    private Channel targetChannel;
    private final boolean closeTargetChannel;
//    private static final String Proxy_Transfer_Name="DestinationProxyHandler*Transfer";

    public DestinationProxyHandler() {
        this.closeTargetChannel=true;
        this.targetChannel = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message){
            Message m=(Message)msg;
            int operationCode=m.getOperationCode();
            if (operationCode == ConnectionEvents.BIND.getCode()){
                handleBind(ctx,m);
                m.release();
            }else if(operationCode == ConnectionEvents.CONNECT.getCode()) {
                handleConnect(ctx,m);
            }else{
                throw new Exception("a unsupported header :"+operationCode);
            }
        }else {
            throw new RuntimeException("DestinationProxyHandler#channelRead: the msg is not a instance of Message");
        }
    }

    private void handleConnect(ChannelHandlerContext ctx, Message m) {
        if (targetChannel == null || !targetChannel.isActive()){
            ctx.channel().close().addListener(LogUtil.LOGGER_ON_FAILED_CLOSE);
            m.release();
        }else {
            targetChannel.writeAndFlush(m.getContent()).addListener(LogUtil.LOGGER_ON_FAILED_CLOSE);
        }
    }

    private void handleBind(ChannelHandlerContext ctx, Message m) throws Exception {
        removeOldConnection(ctx,m);
        byte[] des=m.getDestination();
        ChannelFuture channelFuture= Connections.newConnectionToServer(ctx.channel().eventLoop()
                ,new InetSocketAddress(InetAddress.getByAddress(new byte[]{des[0],des[1],des[2],des[3]}),((des[4] & 0xFF)<<8)|(des[5] & 0xFF))
                ,(status, channelToServer)->{
                    if (status==SystemConfig.SUCCESS){
                        channelToServer.pipeline().addLast("ConnectionToServer*transfer",new SimpleTransferHandler(ctx.channel()));

                        LogUtil.info(()->channelToServer+" connect success");
//                        ctx.pipeline().addAfter(ctx.name(),Proxy_Transfer_Name,new SimpleTransferHandler(channelToServer,true));
                        ctx.writeAndFlush(new Message(ConnectionEvents.CONNECTION_ESTABLISH.getCode(), (byte[]) null, Unpooled.EMPTY_BUFFER))
                                .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                    }else{
                        LogUtil.info(()->channelToServer+" connect failed");
                        ctx.writeAndFlush(new Message(ConnectionEvents.CONNECTION_ESTABLISH_FAILED.getCode(),(byte[]) null,Unpooled.EMPTY_BUFFER))
                                .addListener(ChannelFutureListener.CLOSE);
                    }
                });
        this.targetChannel =channelFuture.channel();
    }

    private void removeOldConnection(ChannelHandlerContext ctx, Message m) {
        if (this.targetChannel !=null){
            this.targetChannel.close();
            this.targetChannel =null;
        }
//        boolean flag=false;
//        for (Map.Entry<String, ChannelHandler> stringChannelHandlerEntry : ctx.pipeline()) {
//            if (stringChannelHandlerEntry.getKey().equals(Proxy_Transfer_Name)){
//                flag=true;
//                break;
//            }
//        }
//        if (flag){
//            ctx.pipeline().remove(Proxy_Transfer_Name);
//        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf){
            msg=new Message(ConnectionEvents.CONNECT.getCode(),(byte[])null,(ByteBuf) msg);
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (closeTargetChannel && targetChannel != null && targetChannel.isActive()) {
            targetChannel.eventLoop().submit(()->{targetChannel.close();});
        }
        super.close(ctx,promise);
    }
}
