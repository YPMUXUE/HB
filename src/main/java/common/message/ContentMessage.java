package common.message;


public interface ContentMessage<C> extends Message {
	C getContent();
}
