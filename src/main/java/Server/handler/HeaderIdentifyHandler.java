package Server.handler;

import io.netty.handler.timeout.IdleStateHandler;

public class HeaderIdentifyHandler extends IdleStateHandler {
    public HeaderIdentifyHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }
}
