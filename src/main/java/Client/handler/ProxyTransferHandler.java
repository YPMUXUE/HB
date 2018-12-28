package Client.handler;

import Client.bean.HostAndPort;
import common.Message;
import common.handler.SimpleTransferHandler;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.concurrent.*;

public class ProxyTransferHandler extends SimpleTransferHandler {
    private BlockingQueue<PendingWriteItem> pendingWrite = new LinkedBlockingQueue<>();
    private boolean connectFinished=false;
    private boolean isSendinitializeRQ=false;
    private final HostAndPort hostAndPort;
    public ProxyTransferHandler(Channel targetChannel, boolean closeTargetChannel, HostAndPort hostAndPort) {
        super(targetChannel, closeTargetChannel);
        this.hostAndPort=hostAndPort;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (connectFinished) {
            super.channelRead(ctx, msg);
        } else {
            if (msg instanceof ByteBuf) {
                handleResponse(ctx, (ByteBuf) msg);
            }
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Message message=new Message();
        message.setOperationCode(ConnectionEvents.ONE_OFF_CONNECTION.getCode());
        message.setDestination(getDestinationBytes(this.hostAndPort));
        message.setContent(Unpooled.EMPTY_BUFFER);
        ctx.writeAndFlush(message);
        super.handlerAdded(ctx);
    }

    private byte[] getDestinationBytes(HostAndPort hostAndPort) throws Exception {
        byte[] host = hostAndPort.getHost().getAddress();
        int iport = hostAndPort.getPort();
        byte[] port = new byte[]{
//                (byte) ((iport >> 24) & 0xFF),
//                (byte) ((iport >> 16) & 0xFF),
                (byte) ((iport >> 8) & 0xFF),
                (byte) (iport & 0xFF)
        };
        return new byte[]{
                host[0],
                host[1],
                host[2],
                host[3],
                port[0],
                port[1]
        };
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (connectFinished){
            if (pendingWrite!=null && pendingWrite.size()>0){
                for (int i = pendingWrite.size() - 1; i >= 0; i--) {
                    PendingWriteItem item=pendingWrite.poll();
                    ctx.write(item.data,item.promise);
                }
                pendingWrite=null;
            }
            super.write(ctx, msg, promise);
        }else{
            pendingWrite.put(new PendingWriteItem(msg,promise));
        }
    }

    private void handleResponse(ChannelHandlerContext ctx, ByteBuf msg) {
        byte result=msg.readByte();
        if (result== ConnectionEvents.CONNECTION_ESTABLISH.getCode()){
            connectFinished=true;
        }
    }

    private class PendingWriteItem{
        private Object data;
        private ChannelPromise promise;

        public PendingWriteItem(Object data, ChannelPromise promise) {
            this.data = data;
            this.promise = promise;
        }
    }
}
