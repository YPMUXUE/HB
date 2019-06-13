package common.handler2.coder;

import common.message.MessageTranslator;
import common.message.MessageTranslatorFactory;
import common.message.frame.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.List;

/**
 *  * @author  pyuan
 *  * @date    2019/6/13 0013
 *  * @Description 类的说明（必须）
 *  *
 *  
 */
public class MessageInboundTransferHandler extends ChannelInboundHandlerAdapter {

	private final MessageTranslatorFactory messageTranslatorFactory;

	public MessageInboundTransferHandler(List<MessageTranslator> c){
		messageTranslatorFactory = new MessageTranslatorFactory(c);
	}
	public MessageInboundTransferHandler(MessageTranslatorFactory f){
		messageTranslatorFactory = f;
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
