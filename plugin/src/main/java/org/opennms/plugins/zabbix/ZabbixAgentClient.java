package org.opennms.plugins.zabbix;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.opennms.plugins.zabbix.utils.ClientRequestEncoder;
import org.opennms.plugins.zabbix.utils.ClientResponseDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

/**
 * TODO: Better handling of unsupported keys i.e. ZBX_NOTSUPPORTEDUnsupported item key. or ZBX_NOTSUPPORTEDToo many parameters.
 * TODO: Full async communication and async API (could be exposed as something like CompletableFuture<Resuts> getKeys(String... keys))
 * TODO: Handle cases where the server rejects the connection - the connection will be closed without any response data - this should be made evident to the caller
 */
public class ZabbixAgentClient implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(ZabbixAgentClient.class);
    public static final String UNSUPPORTED_HEADER = "ZBX_NOTSUPPORTED";
    public static final int DEFAULT_PORT = 10050;
    private static AttributeKey<CompletableFuture<String>> FUTURE = AttributeKey.valueOf("zabbix_future");
    private ChannelPool channelPool;

    public ZabbixAgentClient(EventLoopGroup group, InetAddress address, int port, int maxConnection) {
       Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .channel(NioSocketChannel.class)
                .remoteAddress(address, port);

        channelPool = new FixedChannelPool(bootstrap, new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast("encoder", new ClientRequestEncoder())
                        .addLast("decoder", new ClientResponseDecoder())
                        .addLast("clientHandler", new PooledClientHandler(channelPool));
            }
        }, maxConnection);
    }

    public CompletableFuture<List<Map<String, Object>>> discoverData(String key) {
        return  retrieveData(key).thenApplyAsync(data -> {
            ObjectMapper mapper = new ObjectMapper();
            // FIXME: Not sure if all discovery rule keys follow the same format
            try {
                if(data.startsWith(UNSUPPORTED_HEADER)) {
                    LOG.error("{} <> {}", key, data);
                    return Collections.emptyList();
                }
                List<Map<String, Object>> entries = (List<Map<String, Object>>) mapper.readValue(data, List.class);
                LOG.trace("{} = {}", key, entries);
                return entries;
            } catch (JsonProcessingException e) {
                LOG.trace("{} = <invalid json>", key);
                return Collections.emptyList();
            }
        });

    }

    public CompletableFuture<String> retrieveData(String key) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Future<Channel> channelFuture = channelPool.acquire();
        channelFuture.addListener(new FutureListener<Channel>() {
            @Override
            public void operationComplete(Future<Channel> f) {
                if (f.isSuccess()) {
                    Channel channel = f.getNow();
                    channel.attr(FUTURE).set(future);
                    channel.writeAndFlush(key, channel.voidPromise());
                }
            }
        });
        return future;
    }

    @Override
    public void close() {
        if (channelPool != null) {
            channelPool.close();
        }
    }

    private static class PooledClientHandler extends ChannelInboundHandlerAdapter {
        private ChannelPool pool;
        private StringBuilder sb = new StringBuilder();

        public PooledClientHandler(ChannelPool pool) {
            this.pool = pool;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object in) {
            String msg = (String) in;
            sb.append(msg);
            pool.release(ctx.channel());
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            Attribute<CompletableFuture<String>> futureAttribute = ctx.channel().attr(FUTURE);
            CompletableFuture<String> future = futureAttribute.getAndSet(new CompletableFuture<>());
            future.complete(sb.toString());
            pool.release(ctx.channel());
            ctx.fireChannelReadComplete();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            Attribute<CompletableFuture<String>> futureAttribute = ctx.channel().attr(FUTURE);
            CompletableFuture<String> future = futureAttribute.getAndSet(new CompletableFuture<>());
            cause.printStackTrace();
            pool.release(ctx.channel());
            ctx.close();
            future.completeExceptionally(cause);
        }
    }

}
