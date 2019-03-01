package Client.handler;

import Client.bean.HostAndPort;
import common.Message;
import common.handler.SimpleTransferHandler;
import common.log.LogUtil;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ProxyTransferHandler extends SimpleTransferHandler {
    private BlockingQueue<PendingWriteItem> pendingWrite = null;
    private boolean connectFinished = false;
    private boolean isInitialized = false;
    private final HostAndPort hostAndPort;

    public ProxyTransferHandler(Channel targetChannel, boolean closeTargetChannel, HostAndPort hostAndPort) {
        super(targetChannel, closeTargetChannel);
        this.hostAndPort = hostAndPort;
    }
    public ProxyTransferHandler(ProxyTransferHandler oldHandler,HostAndPort hostAndPort){
        super(oldHandler.targetChannel,oldHandler.closeTargetChannel);
        this.hostAndPort=hostAndPort;
    }

    private void init() {
        this.isInitialized = true;
        this.connectFinished = false;
        if (pendingWrite == null) {
            pendingWrite = new LinkedBlockingQueue<>();
        } else {
            pendingWrite.clear();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (connectFinished) {
            super.channelRead(ctx, msg);
        } else {
            try {
                if (msg instanceof ByteBuf) {
                    handleResponse(ctx, (ByteBuf) msg);
                } else if (msg instanceof Message) {
                    handleResponse(ctx, (Message) msg);
                }
            }catch (Exception e){
                LogUtil.info(()->"connect to proxy server error result:"+e.toString());
                ReferenceCountUtil.release(msg);
                ctx.channel().close();
                targetChannel.close();
                return;
            }
            writePendingDataLater(ctx);
            notifyTargetChannel(ctx,msg);
        }
    }

    private void notifyTargetChannel(ChannelHandlerContext ctx, Object msg) {
            targetChannel.writeAndFlush(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (!isInitialized){
            init();
            Message message = new Message(ConnectionEvents.BIND.getCode()
                    , this.hostAndPort
                    , Unpooled.EMPTY_BUFFER);
            ctx.writeAndFlush(message);
        }
        super.channelActive(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (isInitialized){
            if (pendingWrite == null) {
                pendingWrite = new LinkedBlockingQueue<>();
            } else {
                pendingWrite.clear();
            }
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (!ctx.channel().isActive()){
            LogUtil.info(()->"the channel ["+ctx.channel()+"] is not active");
            return;
        }
        if (!isInitialized) {
            init();
            Message message = new Message(ConnectionEvents.BIND.getCode()
                    , this.hostAndPort
                    , Unpooled.EMPTY_BUFFER);
            ctx.writeAndFlush(message);
        }
        super.handlerAdded(ctx);
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (connectFinished) {
            writePendingData(ctx);
            this.write0(ctx, msg, promise);
        } else {
            pendingWrite.put(new PendingWriteItem(msg, promise));
        }
    }

    private void writePendingData(ChannelHandlerContext ctx){
        if (pendingWrite != null && pendingWrite.size() > 0) {
            for (int i = pendingWrite.size() - 1; i >= 0; i--) {
                PendingWriteItem item = pendingWrite.poll();
                if (item != null) {
                    ctx.write(item.data, item.promise);
                }
            }
            pendingWrite = null;
        }
    }

    private void writePendingDataLater(ChannelHandlerContext ctx) {
        ctx.executor().execute(()-> writePendingData(ctx));
    }

    private void write0(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Message){
            super.write(ctx,msg,promise);
        }else if(msg instanceof ByteBuf){
            super.write(ctx,new Message(ConnectionEvents.CONNECT.getCode(),(HostAndPort) null,(ByteBuf) msg),promise);
        }else {
            throw new RuntimeException("ProxyTransferHandler#write0 the msg is not a instance of Message or ByteBuf");
        }
    }

    private void handleResponse(ChannelHandlerContext ctx, ByteBuf msg) {
        byte result = msg.readByte();
        if (result == ConnectionEvents.CONNECTION_ESTABLISH.getCode()) {
            connectFinished = true;
        } else {
            throw new RuntimeException("the response code of initializeRQ is not CONNECTION_ESTABLISH : " + result);
        }
    }

    private void handleResponse(ChannelHandlerContext ctx, Message msg) {
        int result = msg.getOperationCode();
        if (result == ConnectionEvents.CONNECTION_ESTABLISH.getCode()) {
            connectFinished = true;
        } else {
            //todo 可以添加一个连接权限认证逻辑
            throw new RuntimeException("the response code of initializeRQ is not CONNECTION_ESTABLISH : " + result);
        }
    }

    private class PendingWriteItem {
        private Object data;
        private ChannelPromise promise;

        public PendingWriteItem(Object data, ChannelPromise promise) {
            this.data = data;
            this.promise = promise;
        }
    }
}
