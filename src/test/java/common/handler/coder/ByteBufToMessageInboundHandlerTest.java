package common.handler.coder;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

class ByteBufToMessageInboundHandlerTest {
    public static void main(String[] args) {
        EmbeddedChannel testinbound=new EmbeddedChannel(new ByteBufToMessageInboundHandler());
        ByteBuf msg=Unpooled.buffer().writeBytes(new byte[]{(byte)0x03,
                (byte)0x20,
                (byte)0x00,
                (byte)0x00,
                (byte)0x00,
                (byte)0x0C,
                (byte)0x03,
                (byte)0x20,
                (byte)0x00,
                (byte)0x00,
                (byte)0x00,
                (byte)0x0C,
                (byte)0x03,
                (byte)0x20,
                (byte)0x00,
                (byte)0x00,
                (byte)0x00,
                (byte)0x0C});
        testinbound.writeInbound(msg);
       Object o= testinbound.readInbound();
        EmbeddedChannel testoutbound=new EmbeddedChannel(new MessageToByteBufOutboundHandler());
        testoutbound.writeOutbound(o);
        testoutbound.finish();
        testoutbound.readOutbound();

    }

}