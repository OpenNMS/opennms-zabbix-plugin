package org.opennms.plugins.zabbix.agent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ZabbixAgent {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;
    private InetSocketAddress localAddress;

    public synchronized void start() throws InterruptedException, UnknownHostException {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        ch.pipeline().addLast(new RequestDecoder(),
                                new ResponseEncoder(),
                                new ProcessingHandler());
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        channelFuture = bootstrap.bind(InetAddress.getLocalHost(), 0).sync();
        localAddress = (InetSocketAddress) channelFuture.channel().localAddress();
    }

    public synchronized void stop() {
        channelFuture.channel().close();
        workerGroup.shutdownGracefully(2, 2, TimeUnit.SECONDS);
        bossGroup.shutdownGracefully(2, 2, TimeUnit.SECONDS);
    }

    public InetAddress getAddress() {
        return localAddress.getAddress();
    }

    public int getPort() {
        return localAddress.getPort();
    }
}
