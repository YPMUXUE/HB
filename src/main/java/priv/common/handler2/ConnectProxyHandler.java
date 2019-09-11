package priv.common.handler2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.common.events.ConnectFailedProxyEvent;
import priv.common.events.ConnectSuccessProxyEvent;
import priv.common.message.frame.Message;
import priv.common.message.frame.close.CloseMessage;
import priv.common.message.frame.connect.ConnectMessage;
import priv.common.message.frame.establish.ConnectionEstablishFailedMessage;
import priv.common.message.frame.establish.ConnectionEstablishMessage;
import priv.common.resource.HttpResources;

import java.nio.charset.StandardCharsets;

/**
 *  * @author  pyuan
 *  * @date    2019/8/26 0026
 *  * @Description
 *  *
 *  
 */
public class ConnectProxyHandler extends ChannelDuplexHandler {

	private static final Logger logger = LoggerFactory.getLogger(ConnectProxyHandler.class);
	private static final MessageProcessor HTTPS_PROCESSOR = (ctx, msg) -> {
		if (msg instanceof ConnectionEstablishMessage) {
			return Unpooled.buffer().writeBytes(HttpResources.HttpResponse.Connection_Established.getBytes(StandardCharsets.UTF_8));
		} else if(msg instanceof ConnectionEstablishFailedMessage) {
			logger.info(((ConnectionEstablishFailedMessage) msg).getReason());
			return Unpooled.buffer().writeBytes(HttpResources.HttpResponse.Connection_Failed.getBytes(StandardCharsets.UTF_8));
		}else if (msg instanceof ConnectMessage){
			return ((ConnectMessage) msg).getContent();
		}else if (msg instanceof CloseMessage){
			ctx.executor().execute(()->ctx.channel().close());
			return null;
		}else{
			throw new UnsupportedOperationException(msg.getClass().toString());
		}
	};

	private Channel targetChannel;
	private final MessageProcessor messageProcessor;

	public ConnectProxyHandler() {
		this.messageProcessor = HTTPS_PROCESSOR;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof ByteBuf) {
			Message m = new ConnectMessage((ByteBuf) msg);
			targetChannel.writeAndFlush(m);
		} else if (msg instanceof Message) {
			targetChannel.writeAndFlush(msg);
		} else {
			throw new RuntimeException("ClientTransferHandler#channelRead the msg is not a instance of Message or ByteBuf");
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof ConnectSuccessProxyEvent){
			Channel channel = ((ConnectSuccessProxyEvent) evt).getChannel();
			this.targetChannel = channel;
		}else if (evt instanceof ConnectFailedProxyEvent){
			ctx.channel().close();
		}else{
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof ByteBuf) {
			super.write(ctx, msg, promise);
		} else if (msg instanceof Message) {
			ByteBuf result = messageProcessor.handleMessage(ctx, (Message) msg);
			if (result != null) {
				super.write(ctx, result, promise);
			}
		} else {
			throw new RuntimeException("ConnectProxyHandler#write the msg is not a instance of Message or ByteBuf");
		}
	}
	public interface MessageProcessor {
		ByteBuf handleMessage(ChannelHandlerContext ctx, Message msg);
	}
}
