package common.message;


import common.message.processor.MessageProcessor;
import common.resource.ConnectionEvents;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageProcessorFactory {
	private Map processorMap;
	public void register(List<MessageProcessor> processors){
		if (CollectionUtils.isEmpty(processors)){
			throw new IllegalArgumentException("没有注册任何处理器");
		}
		Map<ConnectionEvents,MessageProcessor> tempProcessorMap = new HashMap<>();
		for (MessageProcessor processor : processors) {
			tempProcessorMap.put(processor.getSupportConnectionEvent(),processor);
		}
		this.processorMap = Collections.unmodifiableMap(tempProcessorMap);
	}
}
