package org.opennms.plugins.zabbix.agent;

import java.net.UnknownHostException;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class ZabbixAgentWin extends AbstractAgent {
    @Override
    public synchronized void start() throws InterruptedException, UnknownHostException {
        super.init();
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new RequestDecoder(), new ResponseEncoder(), new WinProcessingHandler());
            }
        });
        super.startServer();
    }
}
