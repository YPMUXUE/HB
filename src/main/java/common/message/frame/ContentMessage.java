package common.message.frame;


public interface ContentMessage<C> extends Message {
	C getContent();
	void setContent(C content);
}
