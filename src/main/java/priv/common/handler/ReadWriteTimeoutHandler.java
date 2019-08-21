package priv.common.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ReadWriteTimeoutHandler extends IdleStateHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReadWriteTimeoutHandler.class);
    private boolean closed=false;
    public ReadWriteTimeoutHandler(long allIdleTime) {
        super(false, 0, 0, allIdleTime, TimeUnit.SECONDS);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
            logger.info(evt.state().name() + " triggered. start to close channel:" + ctx.channel().toString());
            ctx.channel().close();
    }
}
