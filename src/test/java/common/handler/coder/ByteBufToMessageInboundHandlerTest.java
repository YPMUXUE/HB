package common.handler.coder;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

import java.nio.charset.Charset;

class ByteBufToMessageInboundHandlerTest {
    public static void main(String[] args) {
        EmbeddedChannel testinbound=new EmbeddedChannel(new ByteBufToMessageInboundHandler());
        byte[] hello="yuanpan".getBytes(Charset.forName("utf-8"));

        ByteBuf msg=Unpooled.buffer().writeBytes(new byte[]{(byte)0x03,
                (byte)0x20}).writeInt(hello.length).writeBytes(new byte[6]).writeBytes(hello);
        testinbound.writeInbound(msg);
       Object o= testinbound.readInbound();
        EmbeddedChannel testoutbound=new EmbeddedChannel(new MessageToByteBufOutboundHandler());
        testoutbound.writeOutbound(o);
        testoutbound.finish();
        testoutbound.readOutbound();

    }

}