package priv.Server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
                        .addLast(new PacHandler("C:\\Users\\YP\\Desktop\\pac.txt"));
                    }
                });
        InetAddress host=InetAddress.getLocalHost();
        SocketAddress address=new InetSocketAddress(InetAddress.getLoopbackAddress(),7070);
        ChannelFuture future = serverBootstrap.bind(address);
        future.syncUninterruptibly();
    }

    private static class PacHandler extends ChannelDuplexHandler {
        private File pacFile;
        public PacHandler(String filePath) {
            pacFile=new File(filePath);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            assert msg instanceof FullHttpRequest;
            FullHttpRequest req= (FullHttpRequest) msg;
            System.out.println(req.uri());
            FileInputStream fins= new FileInputStream(pacFile);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] temp = new byte[1024];
            int i;
            while ((i=fins.read(temp))!=-1){
                out.write(temp,0,i);
            }
            byte[] result=out.toByteArray();
            ctx.write(Unpooled.buffer().writeBytes(("HTTP/1.1 200 OK\r\n" +
                    "Server: yp\r\n" +
                    "Content-Type: application/x-ns-proxy-autoconfig\r\n" +
                    "Content-Length: "+result.length+"\r\n" +
                    "Connection: Close\r\n" +
                    "\r\n").getBytes(Charset.forName("ascii"))));
            ctx.writeAndFlush(Unpooled.buffer().writeBytes(result));
            ((FullHttpRequest) msg).release();
            ctx.writeAndFlush("\r\n");
            ctx.close();
        }
    }
}
