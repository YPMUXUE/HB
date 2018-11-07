package Server;

import Server.handler.HttpProxyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;

import java.net.InetAddress;
import java.nio.charset.Charset;

public class ChannelServer {
    public static final int COUNT_OF_PROCESSORS = Runtime.getRuntime().availableProcessors();
    public static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    protected Channel serverChannel;

    public ChannelServer(String host, int port, ChannelInitializer channelInitializer) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(channelInitializer);
        ChannelFuture future = bootstrap.bind(InetAddress.getByName(host), port);
        future.addListener(f->{
            if (f.isSuccess()){
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    serverChannel.close().syncUninterruptibly();
                }));
            }
        });
        future.syncUninterruptibly();
        this.serverChannel = future.channel();
    }

    public static void main(String[] args) throws Exception {
        ChannelServer channelServer=new ChannelServer("localhost", 9001, new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new HttpRequestDecoder());
                pipeline.addLast(new HttpObjectAggregator(64*1024));
                pipeline.addLast(new HttpProxyHandler());
            }
        });
        channelServer.serverChannel.closeFuture().addListener((f) -> {
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
            }else if(m instanceof FullHttpRequest){
                System.out.println(m.toString());
                String content="Hello";
                String msgHeader="HTTP/1.1 200 OK\r\n" +
                        "Content-Length: "+content.getBytes("UTF-8").length+"\r\n" +
                        "Content-Type: text/html;charset=UTF-8\r\n" +
                        "\r\n";
                if (((FullHttpRequest) m).method().equals(HttpMethod.CONNECT)) {
                    ctx.writeAndFlush(Unpooled.copiedBuffer("HTTP/1.1 200 Connection Established\r\n\r\n", Charset.forName("utf-8")));
                }else{
                    ctx.writeAndFlush(Unpooled.copiedBuffer(msgHeader+content, Charset.forName("utf-8")));

                }
            }
        }
    }
}
