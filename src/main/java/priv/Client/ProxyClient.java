package priv.Client;


import priv.Client.handler2.HttpMethodHandler;
import priv.common.handler.EventLoggerHandler;
import priv.common.log.LogUtil;
import priv.common.resource.StaticConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class ProxyClient {
    public static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    protected Channel serverChannel;

    public ProxyClient(final SocketAddress address, ChannelInitializer channelInitializer) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(channelInitializer);
        ChannelFuture future = bootstrap.bind(address);
        future.addListener(f->{
            if (f.isSuccess()){
                LogUtil.info(()->"start success on" + address);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    serverChannel.close().syncUninterruptibly();
                }));
            }else{
                LogUtil.info(()->LogUtil.stackTraceToString(future.cause()));
            }
        });
        future.syncUninterruptibly();
        this.serverChannel = future.channel();
    }

    public static void main(String[] args) throws Exception {
        InetSocketAddress address=new InetSocketAddress(InetAddress.getByName(StaticConfig.LOCAL_HOST_ADDRESS),StaticConfig.LOCAL_HOST_PORT);
        ProxyClient proxyClient=new ProxyClient(address, new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("HttpRequestDecoder",new HttpRequestDecoder());
                pipeline.addLast("HttpObjectAggregator",new HttpObjectAggregator(64*1024));
                pipeline.addLast("ReadTimeoutHandler",new ReadTimeoutHandler(StaticConfig.timeout, TimeUnit.SECONDS));
                pipeline.addLast("HttpMethodHandler",new HttpMethodHandler());
                pipeline.addLast("ExceptionHandler",new EventLoggerHandler("ProxyClient",true));
            }
        });
        proxyClient.serverChannel.closeFuture().addListener((f) -> {
            System.out.println("server stop");
            eventLoopGroup.shutdownGracefully();
        });
    }

    public static class SoutChannelInboundHandler extends SimpleChannelInboundHandler {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object m) throws Exception {
            if (m instanceof ByteBuf) {
                ByteBuf msg=(ByteBuf)m;
                byte[] buffer = new byte[msg.readableBytes()];
                msg.readBytes(buffer);
                System.out.println(new String(buffer));
            }else{
                System.out.println(m.toString());
//                String content="Hello";
//                String msgHeader="HTTP/1.1 200 OK\r\n" +
//                        "Content-Length: "+content.getBytes("UTF-8").length+"\r\n" +
//                        "Content-Type: text/html;charset=UTF-8\r\n" +
//                        "\r\n";
//                if (((FullHttpRequest) m).method().equals(HttpMethod.CONNECT)) {
//                    ctx.writeAndFlush(Unpooled.copiedBuffer("HTTP/1.1 200 Connection Established\r\n\r\n", Charset.forName("utf-8")));
//                }else{
//                    ctx.writeAndFlush(Unpooled.copiedBuffer(msgHeader+content, Charset.forName("utf-8")));
//
//                }
            }
        }
    }
}
