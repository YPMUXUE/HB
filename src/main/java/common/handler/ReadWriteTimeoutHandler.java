package common.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import common.log.LogUtil;

import java.util.concurrent.TimeUnit;

public class ReadWriteTimeoutHandler extends IdleStateHandler {
    private boolean closed=false;
    public ReadWriteTimeoutHandler(long allIdleTime) {
        super(false, 0, 0, allIdleTime, TimeUnit.SECONDS);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (!closed) {
            LogUtil.info(() -> evt.state().name() + " triggered. close channel:" + ctx.channel().toString());
            ctx.channel().close();
            closed=true;
        }
    }
}
