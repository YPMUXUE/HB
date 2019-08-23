package priv.Server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import priv.common.ResourceLoader;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.charset.Charset;

public class PacServer {
    public static void main(String[] args) throws Exception {
        ServerBootstrap serverBootstrap=new ServerBootstrap();
        EventLoopGroup eventLoopGroup=new NioEventLoopGroup();

        serverBootstrap.group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new HttpRequestDecoder())
                        .addLast(new HttpObjectAggregator(8*8*1024))
                        .addLast(new PacHandler());
                    }
                });
        InetAddress host=InetAddress.getLocalHost();
        SocketAddress address=new InetSocketAddress(InetAddress.getLoopbackAddress(),7070);
        ChannelFuture future = serverBootstrap.bind(address);
        future.syncUninterruptibly();
    }

    private static class PacHandler extends ChannelDuplexHandler {
        private static final String DEFAULT_PAC = "function FindProxyForURL(url, host) {\n" +
                "\treturn \"PROXY 127.0.0.1:4449\";\n" +
                "}";
        private byte[] pacDate;

        public PacHandler() throws IOException {
            InputStream resource = ResourceLoader.getResource("pac.pac");
            if (resource != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] temp = new byte[1024];
                int i = 0;
                while ((i = resource.read(temp)) != -1) {
                    byteArrayOutputStream.write(temp, 0, i);
                }
                pacDate = byteArrayOutputStream.toByteArray();
            }else{
                pacDate = DEFAULT_PAC.getBytes();
            }

        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            assert msg instanceof FullHttpRequest;
            FullHttpRequest req= (FullHttpRequest) msg;
            System.out.println(req.uri());
            ctx.write(Unpooled.buffer().writeBytes(("HTTP/1.1 200 OK\r\n" +
                    "Server: yp\r\n" +
                    "Content-Type: application/x-ns-proxy-autoconfig\r\n" +
                    "Content-Length: "+pacDate.length+"\r\n" +
                    "Connection: Close\r\n" +
                    "\r\n").getBytes(Charset.forName("ascii"))));
            ctx.writeAndFlush(Unpooled.buffer().writeBytes(pacDate));
            ((FullHttpRequest) msg).release();
            ctx.writeAndFlush("\r\n");
            ctx.close();
        }
    }
}
