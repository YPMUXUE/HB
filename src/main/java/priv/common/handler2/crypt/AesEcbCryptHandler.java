package priv.common.handler2.crypt;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import priv.common.resource.SystemConfig;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AesEcbCryptHandler extends ChannelDuplexHandler {
	public static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
	public static final String KEY_ALGORITHM = "AES";
	private final SecretKey secretKey;
	private static final String LENGTH_FIELD_BASED_FRAME_DECODER_FOR_AES = "LENGTH_FIELD_BASED_FRAME_DECODER_FOR_AES";

	public AesEcbCryptHandler(byte[] key) {
		this.secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		ctx.pipeline().addBefore(ctx.name(),LENGTH_FIELD_BASED_FRAME_DECODER_FOR_AES,new LengthFieldBasedFrameDecoder(SystemConfig.PACKAGE_MAX_LENGTH,0,4));
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		super.write(ctx, msg, promise);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		super.channelRead(ctx, msg);
	}
}
