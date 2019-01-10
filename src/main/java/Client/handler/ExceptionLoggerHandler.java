package Client.handler;

import common.log.LogUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionLoggerHandler extends ChannelInboundHandlerAdapter {
    private final String msg;

    public ExceptionLoggerHandler(String msg) {
        this.msg = msg+":";
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogUtil.info(()->(msg+cause.toString()+stackTraceToString(cause)));
    }
    private String stackTraceToString(Throwable e){
        StringWriter errorsWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(errorsWriter));
        return errorsWriter.toString();
    }
}
