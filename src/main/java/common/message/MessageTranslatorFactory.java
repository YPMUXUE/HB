package common.message;


import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageTranslatorFactory {
	private final Map<Short, MessageTranslator> translatorMap;

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
