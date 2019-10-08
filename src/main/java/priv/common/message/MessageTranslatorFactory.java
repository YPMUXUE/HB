package priv.common.message;


import priv.common.message.frame.bind.translator.BindV1Translator;
import priv.common.message.frame.bind.translator.BindV2Translator;
import priv.common.message.frame.close.translator.CloseTranslator;
import priv.common.message.frame.connect.translator.ConnectMessageTranslator;
import priv.common.message.frame.establish.translator.ConnectionEstablishFailedTranslator;
import priv.common.message.frame.establish.translator.ConnectionEstablishTranslator;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

public class MessageTranslatorFactory {
	private final Map<Short, MessageTranslator> translatorMap;

	public static final MessageTranslatorFactory ALL_TRANSLATORS;
	static {
		ArrayList<MessageTranslator> list = new ArrayList<>();
		list.add(new BindV2Translator());
		list.add(new BindV1Translator());
		list.add(new ConnectMessageTranslator());
		list.add(new ConnectionEstablishFailedTranslator());
		list.add(new ConnectionEstablishTranslator());
		list.add(new CloseTranslator());

		ALL_TRANSLATORS = new MessageTranslatorFactory(list);
	}



	public MessageTranslatorFactory(List<MessageTranslator> processors) {
		if (CollectionUtils.isEmpty(processors)) {
			throw new IllegalArgumentException("processor is empty");
		}
		Map<Short, MessageTranslator> tempProcessorMap = new HashMap<>(processors.size());
		for (MessageTranslator processor : processors) {
			tempProcessorMap.put(processor.getSupportConnectionEvent().getCode(), processor);
		}
		this.translatorMap = Collections.unmodifiableMap(tempProcessorMap);
	}


	public MessageTranslator find(short operationCode) {
		return this.translatorMap.get(operationCode);
	}
}
