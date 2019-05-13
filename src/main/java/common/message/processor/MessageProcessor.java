package common.message.processor;


import common.resource.ConnectionEvents;

public interface MessageProcessor<T> {
	ConnectionEvents getSupportConnectionEvent();

	void process(T buf);
}
