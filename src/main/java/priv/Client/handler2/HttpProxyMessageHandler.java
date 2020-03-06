package priv.Client.handler2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.collections4.CollectionUtils;
import priv.common.message.frame.bind.BindV1Message;
import priv.common.message.frame.bind.BindV2Message;
import priv.common.message.frame.close.CloseMessage;
import priv.common.message.frame.connect.ConnectMessage;
import priv.common.message.frame.establish.ConnectionEstablishFailedMessage;
import priv.common.message.frame.establish.ConnectionEstablishMessage;

import java.util.LinkedList;
import java.util.Queue;


public class HttpProxyMessageHandler extends ChannelDuplexHandler {
	private ChannelPromise bindPromise;
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof ConnectionEstablishMessage){
			bindSuccess(ctx, (ConnectionEstablishMessage)msg);
		}else if (msg instanceof ConnectionEstablishFailedMessage){
			bindFailed(ctx, (ConnectionEstablishFailedMessage)msg);
		}else{
			super.channelRead(ctx, msg);
		}
	}

	private void bindFailed(ChannelHandlerContext ctx, ConnectionEstablishFailedMessage msg) {
		bindPromise.tryFailure(new RuntimeException("bind failed:"+msg.getReason()));
	}

	private void bindSuccess(ChannelHandlerContext ctx, ConnectionEstablishMessage msg) {
		bindPromise.trySuccess();
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof BindV2Message){
//			this.bindPromise = ((BindV2Message) msg).getPromise();
		}else if (msg instanceof ByteBuf){
			msg = new ConnectMessage((ByteBuf) msg);
		}
		super.write(ctx, msg, promise);
	}
}
