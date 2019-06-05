package Server.v2;

import common.message.MessageTranslator;
import common.message.MessageTranslatorFactory;
import common.message.frame.Message;
import common.message.frame.bind.translator.BindV2Translator;
import common.message.frame.connect.translator.ConnectMessageTranslator;
import common.message.frame.establish.translator.ConnectionEstablishFailedTranslator;
import common.message.frame.establish.translator.ConnectionEstablishTranslator;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCounted;

import java.util.ArrayList;
import java.util.List;


public class ServerMessageTransferHander extends ChannelDuplexHandler {

	private final MessageTranslatorFactory messageTranslatorFactory;

	public ServerMessageTransferHander() {
		List<MessageTranslator> list = new ArrayList<>();

		list.add(new BindV2Translator());
		list.add(new ConnectMessageTranslator());
		list.add(new ConnectionEstablishFailedTranslator());
		list.add(new ConnectionEstablishTranslator());

		messageTranslatorFactory = new MessageTranslatorFactory(list);
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

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof ByteBuf){
			short code = ((ByteBuf) msg).getShort(0);
			MessageTranslator messageTranslator = messageTranslatorFactory.find(code);
			Message message = messageTranslator.translate((ByteBuf) msg);
			((ByteBuf) msg).release();
			ctx.fireChannelRead(message);
		}else{
			ctx.fireChannelRead(msg);
		}
	}
}
