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
	private Consumer<ChannelHandlerContext> channelRegisteredListener;
	private Consumer<ChannelHandlerContext> channelUnregisteredListener;
	private Consumer<ChannelHandlerContext> channelActiveListener;
	private Consumer<ChannelHandlerContext> channelInactiveListener;
	private BiConsumer<ChannelHandlerContext,Object> channelReadListener;
	private Consumer<ChannelHandlerContext>  channelReadCompleteListener;
	private BiConsumer<ChannelHandlerContext,Object> userEventTriggeredListener;
	private Consumer<ChannelHandlerContext> channelWritabilityChangedListener;
	private BiConsumer<ChannelHandlerContext,Throwable> exceptionCaughtListener;

	public InboundCallBackHandler() {
		super();
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		if (this.channelRegisteredListener != null) {
			channelRegisteredListener.accept(ctx);
		}else {
			super.channelRegistered(ctx);
		}
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelUnregisteredListener != null) {
			channelUnregisteredListener.accept(ctx);
		}else {
			super.channelUnregistered(ctx);
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelActiveListener != null) {
			channelActiveListener.accept(ctx);
		}else {
			super.channelActive(ctx);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelInactiveListener != null) {
			channelInactiveListener.accept(ctx);
		}else {
			super.channelInactive(ctx);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelReadListener != null) {
			channelReadListener.accept(ctx,msg);
		}else {
			super.channelRead(ctx, msg);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelReadCompleteListener != null) {
			channelReadCompleteListener.accept(ctx);
		}else {
			super.channelReadComplete(ctx);
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		Channel channel = ctx.channel();
		if (this.userEventTriggeredListener != null) {
			userEventTriggeredListener.accept(ctx,evt);
		}else {
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		if (this.channelWritabilityChangedListener != null) {
			channelWritabilityChangedListener.accept(ctx);
		}else {
			super.channelWritabilityChanged(ctx);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Channel channel = ctx.channel();
		if (this.exceptionCaughtListener != null) {
			exceptionCaughtListener.accept(ctx,cause);
		}else {
			super.exceptionCaught(ctx, cause);
		}
	}

	public void setChannelRegisteredListener(Consumer<ChannelHandlerContext> channelRegisteredListener) {
		this.channelRegisteredListener = channelRegisteredListener;
	}

	public void setChannelUnregisteredListener(Consumer<ChannelHandlerContext> channelUnregisteredListener) {
		this.channelUnregisteredListener = channelUnregisteredListener;
	}

	public void setChannelActiveListener(Consumer<ChannelHandlerContext> channelActiveListener) {
		this.channelActiveListener = channelActiveListener;
	}

	public void setChannelInactiveListener(Consumer<ChannelHandlerContext> channelInactiveListener) {
		this.channelInactiveListener = channelInactiveListener;
	}

	public void setChannelReadListener(BiConsumer<ChannelHandlerContext, Object> channelReadListener) {
		this.channelReadListener = channelReadListener;
	}

	public void setChannelReadCompleteListener(Consumer<ChannelHandlerContext> channelReadCompleteListener) {
		this.channelReadCompleteListener = channelReadCompleteListener;
	}

	public void setUserEventTriggeredListener(BiConsumer<ChannelHandlerContext, Object> userEventTriggeredListener) {
		this.userEventTriggeredListener = userEventTriggeredListener;
	}

	public void setChannelWritabilityChangedListener(Consumer<ChannelHandlerContext> channelWritabilityChangedListener) {
		this.channelWritabilityChangedListener = channelWritabilityChangedListener;
	}

	public void setExceptionCaughtListener(BiConsumer<ChannelHandlerContext, Throwable> exceptionCaughtListener) {
		this.exceptionCaughtListener = exceptionCaughtListener;
	}
}
