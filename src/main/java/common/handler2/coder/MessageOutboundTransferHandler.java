package common.handler2.coder;

import common.message.MessageTranslator;
import common.message.MessageTranslatorFactory;
import common.message.frame.Message;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCounted;

import java.util.List;

/**
 *  * @author  pyuan
 *  * @date    2019/6/13 0013
 *  * @Description 类的说明（必须）
 *  *
 *  
 */
public class MessageOutboundTransferHandler extends ChannelOutboundHandlerAdapter {

	private final MessageTranslatorFactory messageTranslatorFactory;

	public MessageOutboundTransferHandler(List<MessageTranslator> translators){
		messageTranslatorFactory = new MessageTranslatorFactory(translators);
	}
	public MessageOutboundTransferHandler(MessageTranslatorFactory f){
		messageTranslatorFactory = f;
	}
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof Message){
			Message m = (Message) msg;
			ConnectionEvents connectionEvents = m.supportConnectionEvent();
			MessageTranslator messageTranslator = messageTranslatorFactory.find(connectionEvents.getCode());
			ByteBufAllocator alloc = ctx.alloc();
			ByteBuf out = messageTranslator.writeByteBuf(alloc.buffer(), m);
			if (m instanceof ReferenceCounted){
				((ReferenceCounted) m).release();
			}

			ctx.write(out,promise);
		}else {
			ctx.write(msg,promise);
		}
	}
}
