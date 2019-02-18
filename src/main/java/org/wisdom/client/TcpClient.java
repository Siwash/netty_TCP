package org.wisdom.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.wisdom.client.echo.EchoHandler;
import org.wisdom.client.echo.EchoHandler_2_0;
import org.wisdom.client.echo.EchoHandler_3_0;
import org.wisdom.server.encoder.EncoderHandler_2_0;
import org.wisdom.server.encoder.EncoderHandler_3_0;

public class TcpClient {

    private String ip;
    private int port;
    public  void init() throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE,true);
        bootstrap.handler(new ChannelInitializer() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast("logging",new LoggingHandler("DEBUG"));
                ch.pipeline().addLast(new EncoderHandler_3_0());
                ch.pipeline().addLast(new EchoHandler_3_0());
            }
        });
        bootstrap.remoteAddress(ip,port);
        ChannelFuture future = bootstrap.connect().sync();

            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            group.shutdownGracefully().sync();
        }
    }

    public TcpClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public static void main(String[] args) throws InterruptedException {
        new TcpClient("127.0.0.1",8777).init();
    }
}
