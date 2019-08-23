package priv.common.handler2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutor;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *  * @author  pyuan
 *  * @date    2019/8/23 0023
 *  * @Description 类的说明（必须）
 *  *
 *  
 */
public class InboundCallBackHandler extends ChannelInboundHandlerAdapter {
	private Consumer<Channel> channelRegisteredListener;
	private Consumer<Channel> channelUnregisteredListener;
	private Consumer<Channel> channelActiveListener;
	private Consumer<Channel> channelInactiveListener;
	private BiConsumer<Channel,Object> channelReadListener;
	private Consumer<Channel>  channelReadCompleteListener;
	private BiConsumer<Channel,Object> userEventTriggeredListener;
	private Consumer<Channel> channelWritabilityChangedListener;
	private BiConsumer<Channel,Throwable> exceptionCaughtListener;

	public InboundCallBackHandler() {
		super();
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelRegisteredListener != null) {
			channelRegisteredListener.accept(channel);
		}else {
			super.channelRegistered(ctx);
		}
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelUnregisteredListener != null) {
			channelUnregisteredListener.accept(channel);
		}else {
			super.channelUnregistered(ctx);
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelActiveListener != null) {
			channelActiveListener.accept(channel);
		}else {
			super.channelActive(ctx);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelInactiveListener != null) {
			channelInactiveListener.accept(channel);
		}else {
			super.channelInactive(ctx);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelReadListener != null) {
			channelReadListener.accept(channel,msg);
		}else {
			super.channelRead(ctx, msg);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelReadCompleteListener != null) {
			channelReadCompleteListener.accept(channel);
		}else {
			super.channelReadComplete(ctx);
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		Channel channel = ctx.channel();
		if (this.userEventTriggeredListener != null) {
			userEventTriggeredListener.accept(channel,evt);
		}else {
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelWritabilityChangedListener != null) {
			channelWritabilityChangedListener.accept(channel);
		}else {
			super.channelWritabilityChanged(ctx);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Channel channel = ctx.channel();
		if (this.exceptionCaughtListener != null) {
			exceptionCaughtListener.accept(channel,cause);
		}else {
			super.exceptionCaught(ctx, cause);
		}
	}

	public void setChannelRegisteredListener(Consumer<Channel> channelRegisteredListener) {
		this.channelRegisteredListener = channelRegisteredListener;
	}

	public void setChannelUnregisteredListener(Consumer<Channel> channelUnregisteredListener) {
		this.channelUnregisteredListener = channelUnregisteredListener;
	}

	public void setChannelActiveListener(Consumer<Channel> channelActiveListener) {
		this.channelActiveListener = channelActiveListener;
	}

	public void setChannelInactiveListener(Consumer<Channel> channelInactiveListener) {
		this.channelInactiveListener = channelInactiveListener;
	}

	public void setChannelReadListener(BiConsumer<Channel, Object> channelReadListener) {
		this.channelReadListener = channelReadListener;
	}

	public void setChannelReadCompleteListener(Consumer<Channel> channelReadCompleteListener) {
		this.channelReadCompleteListener = channelReadCompleteListener;
	}

	public void setUserEventTriggeredListener(BiConsumer<Channel, Object> userEventTriggeredListener) {
		this.userEventTriggeredListener = userEventTriggeredListener;
	}

	public void setChannelWritabilityChangedListener(Consumer<Channel> channelWritabilityChangedListener) {
		this.channelWritabilityChangedListener = channelWritabilityChangedListener;
	}

	public void setExceptionCaughtListener(BiConsumer<Channel, Throwable> exceptionCaughtListener) {
		this.exceptionCaughtListener = exceptionCaughtListener;
	}
}
