package priv.Client.handler2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.Client.bean.HostAndPort;
import priv.common.Message;
import priv.common.handler.SimpleTransferHandler;
import priv.common.resource.ConnectionEvents;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PendingWriteTransferHandler extends SimpleTransferHandler {
    private static final Logger logger = LoggerFactory.getLogger(PendingWriteTransferHandler.class);
    private BlockingQueue<PendingWriteItem> pendingWrite = null;
    private boolean connectEstablish = false;

    public PendingWriteTransferHandler(Channel targetChannel, boolean closeTargetChannel) {
        super(targetChannel, closeTargetChannel);
    }

    private void init() {
        this.connectEstablish = false;
        if (pendingWrite == null) {
            pendingWrite = new LinkedBlockingQueue<>();
        } else {
            clearPendingWrite();
        }
    }

    private void clearPendingWrite() {
        if (pendingWrite != null){
            for (PendingWriteItem pendingWriteItem : pendingWrite) {
                ReferenceCountUtil.release(pendingWriteItem);
            }
            pendingWrite.clear();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (connectEstablish) {
            super.channelRead(ctx, msg);
        } else {
            try {
                if (msg instanceof ByteBuf) {
                    handleResponse(ctx, (ByteBuf) msg);
                } else if (msg instanceof Message) {
                    handleResponse(ctx, (Message) msg);
                }
            }catch (Exception e){
                logger.error("connect to proxy server error result:"+e.toString());
                ReferenceCountUtil.release(msg);
                clearPendingWrite();
                ctx.channel().close();
                return;
            }
        }
    }

    private void notifyTargetChannel(ChannelHandlerContext ctx, Object msg) {
            targetChannel.writeAndFlush(msg);
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        clearPendingWrite();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        init();
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (connectEstablish) {
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
            pendingWrite.clear();
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
            throw new RuntimeException("PendingWriteTransferHandler#write0 the msg is not a instance of Message or ByteBuf");
        }
    }

    private void handleResponse(ChannelHandlerContext ctx, ByteBuf msg) {
        byte result = msg.readByte();
        if (result == ConnectionEvents.CONNECTION_ESTABLISH.getCode()) {
            connectEstablish = true;
            writePendingDataLater(ctx);
            notifyTargetChannel(ctx,msg);
        } else {
            throw new RuntimeException("the response code of initializeRQ is not CONNECTION_ESTABLISH : " + result);
        }
    }

    private void handleResponse(ChannelHandlerContext ctx, Message msg) {
        int result = msg.getOperationCode();
        if (result == ConnectionEvents.CONNECTION_ESTABLISH.getCode()) {
            connectEstablish = true;
            writePendingDataLater(ctx);
            notifyTargetChannel(ctx,msg);
        } else {
            throw new RuntimeException("the response code of initializeRQ is not CONNECTION_ESTABLISH : " + result);
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        clearPendingWrite();
        super.close(ctx, promise);
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
