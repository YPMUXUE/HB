package priv.common.handler2.crypt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;
import priv.common.crypto.AesCrypto;

import java.net.SocketAddress;

public class AesEcbCryptHandler extends LengthFieldBasedFrameDecoder implements ChannelOutboundHandler {
	private static final int FRAME_HEADER_LENGTH = 4;
	private final AesCrypto aesCrypto;

	public AesEcbCryptHandler(AesCrypto aesCrypto) {
		super(Integer.MAX_VALUE, 0, FRAME_HEADER_LENGTH, 0, FRAME_HEADER_LENGTH);
		this.aesCrypto = aesCrypto;
	}


	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		ByteBuf frame = (ByteBuf) super.decode(ctx, in);
		if (frame == null) {
			return null;
		}
		int contentLength = frame.readableBytes();
		byte[] encryptedContent = new byte[contentLength];
		frame.readBytes(encryptedContent);
		frame.release();
		byte[] decryptedContent = aesCrypto.decrypt(encryptedContent);
		return Unpooled.buffer(decryptedContent.length).writeBytes(decryptedContent);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		try {
			if (!(msg instanceof ByteBuf)) {
				throw new IllegalArgumentException("msg is not a instance of ByteBuf");
			}
			ByteBuf buf = (ByteBuf) msg;
			int frameLength = buf.readableBytes();
			byte[] unencryptedContent = new byte[frameLength];
			buf.readBytes(unencryptedContent);
			byte[] encryptedContent = aesCrypto.encrypt(unencryptedContent);
			ByteBuf encryptedContentBuffer = ctx.alloc().buffer(FRAME_HEADER_LENGTH + encryptedContent.length);
			encryptedContentBuffer.writeInt(encryptedContent.length);
			encryptedContentBuffer.writeBytes(encryptedContent);
			ctx.write(encryptedContentBuffer, promise);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void flush(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
		ctx.bind(localAddress, promise);
	}

	@Override
	public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
		ctx.connect(remoteAddress, localAddress, promise);
	}

	@Override
	public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		ctx.disconnect(promise);
	}

	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		ctx.close(promise);
	}

	@Override
	public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		ctx.deregister(promise);
	}

	@Override
	public void read(ChannelHandlerContext ctx) throws Exception {
		ctx.read();
	}
}
