package common.message;


import common.message.frame.bind.translator.BindV1Translator;
import common.message.frame.bind.translator.BindV2Translator;
import common.message.frame.connect.translator.ConnectMessageTranslator;
import common.message.frame.establish.translator.ConnectionEstablishFailedTranslator;
import common.message.frame.establish.translator.ConnectionEstablishTranslator;
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

		ALL_TRANSLATORS = new MessageTranslatorFactory(list);
	}



	public MessageTranslatorFactory(List<MessageTranslator> processors) {
		if (CollectionUtils.isEmpty(processors)) {
			throw new IllegalArgumentException("没有注册任何处理器");
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
