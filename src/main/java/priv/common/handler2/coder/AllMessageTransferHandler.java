package priv.common.handler2.coder;

import priv.common.message.MessageTranslatorFactory;
import io.netty.channel.CombinedChannelDuplexHandler;


public class AllMessageTransferHandler extends CombinedChannelDuplexHandler<MessageInboundTransferHandler,MessageOutboundTransferHandler> {

	public AllMessageTransferHandler() {
		super();
		MessageInboundTransferHandler messageInboundTransferHandler = new MessageInboundTransferHandler(MessageTranslatorFactory.ALL_TRANSLATORS);
		MessageOutboundTransferHandler messageOutboundTransferHandler = new MessageOutboundTransferHandler(MessageTranslatorFactory.ALL_TRANSLATORS);
		init(messageInboundTransferHandler, messageOutboundTransferHandler);
	}
}