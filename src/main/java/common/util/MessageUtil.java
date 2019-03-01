package common.util;

import common.Message;
import common.resource.ConnectionEvents;
import common.resource.SystemConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import java.nio.charset.StandardCharsets;

public class MessageUtil {
    public static ByteBuf MessageToByteBuf(Message m, ChannelHandlerContext ctx) {
        int bufferSize = SystemConfig.HEADER_LENGTH + SystemConfig.LENGTH_HEADER_LENGTH + m.getContent().readableBytes();
        if (m.getDestination() != null && m.getDestination().length > 0 && m.getOperationCode() == ConnectionEvents.BIND.getCode()) {
            bufferSize = bufferSize + m.getDestination().length;
            ByteBuf out = ctx.alloc().buffer(bufferSize);
            //write operationCode
            out.writeShort(m.getOperationCode());
            //write length Header
            out.writeInt(m.getContent().readableBytes() + SystemConfig.IP_ADDRESS_LENGTH + SystemConfig.IP_PORT_LENGTH);
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

    public static Message ByteBufToMessage(ByteBuf buf){
        short operationCode=buf.readShort();
        int length=buf.readInt();
        if (operationCode == ConnectionEvents.BIND.getCode()){
            byte[] des=new byte[SystemConfig.DESTINATION_LENGTH];
            buf.readBytes(des);
            return new Message(operationCode,des,buf);
        }else{
            return new Message(operationCode,(byte[])null,buf);
        }
    }

    public static ByteBuf HttpRequestToByteBuf(ByteBuf buf, FullHttpRequest req){
        ByteBufUtil.copy(req.method().asciiName(), buf);
        String uri = req.uri();

        if (uri.isEmpty()) {
            buf.writeBytes(" / ".getBytes(StandardCharsets.US_ASCII));
        } else {
            buf.writeBytes(uri.getBytes(StandardCharsets.US_ASCII));
        }

        buf.writeBytes(req.protocolVersion().toString().getBytes(StandardCharsets.US_ASCII));
        buf.writeBytes("\r\n".getBytes(StandardCharsets.US_ASCII));

        req.headers().forEach((entry)->{
            ByteBufUtil.writeAscii(buf,entry.getKey().trim());
            ByteBufUtil.writeAscii(buf," : ");
            ByteBufUtil.writeAscii(buf,entry.getValue().trim());
            ByteBufUtil.writeAscii(buf,"\r\n");
        });
        ByteBufUtil.writeAscii(buf,"\r\n");
        if (req.content()!=null && req.content().readableBytes()>0) {
            buf.writeBytes(req.content());
        }
        return buf;
    }
    public static Message HttpRequestToMessage(ByteBuf buf,FullHttpRequest req) {
        ByteBuf content=HttpRequestToByteBuf(buf,req);
        return new Message(ConnectionEvents.CONNECT.getCode(),(byte[])null,content);
    }
}
