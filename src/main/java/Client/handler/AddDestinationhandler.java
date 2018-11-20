package Client.handler;

import Client.bean.HostAndPort;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class AddDestinationhandler extends MessageToByteEncoder<ByteBuf> {
    private final byte[] des;

    public AddDestinationhandler(HostAndPort hostAndPort) throws Exception {
        byte[] host = hostAndPort.getHost().getAddress();
        int iport = hostAndPort.getPort();
        byte[] port = new byte[]{
//                (byte) ((iport >> 24) & 0xFF),
//                (byte) ((iport >> 16) & 0xFF),
                (byte) ((iport >> 8) & 0xFF),
                (byte) (iport & 0xFF)
        };
        this.des = new byte[]{
                host[0],
                host[1],
                host[2],
                host[3],
                port[0],
                port[1]
        };
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        out.writeBytes(ctx.alloc().buffer(6).writeBytes(des));
        out.writeBytes(msg);
    }
}
