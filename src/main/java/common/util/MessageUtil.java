package common.util;

import common.Message;
import common.resource.ConnectionEvents;
import config.StaticConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class MessageUtil {
    public static ByteBuf MessageToByteBuf(Message m, ChannelHandlerContext ctx) {
        int bufferSize = StaticConfig.HEADER_LENGTH + StaticConfig.LENGTH_HEADER_LENGTH + m.getContent().readableBytes();
        if (m.getDestination() != null && m.getDestination().length > 0 && m.getOperationCode() == ConnectionEvents.BIND.getCode()) {
            bufferSize = bufferSize + m.getDestination().length;
            ByteBuf out = ctx.alloc().buffer(bufferSize);
            //write operationCode
            out.writeShort(m.getOperationCode());
            //write length Header
            out.writeInt(m.getContent().readableBytes() + StaticConfig.IP_ADDRESS_LENGTH + StaticConfig.IP_PORT_LENGTH);
            //write Destination
            out.writeBytes(m.getDestination());
            //write Content
            out.writeBytes(m.getContent());
            return out;
        } else {
            ByteBuf out = ctx.alloc().buffer(bufferSize);
            //write operationCode
            out.writeShort(m.getOperationCode());
            //write length Header
            out.writeInt(m.getContent().readableBytes());
            //write Content
            out.writeBytes(m.getContent());
            return out;
        }
    }
}
