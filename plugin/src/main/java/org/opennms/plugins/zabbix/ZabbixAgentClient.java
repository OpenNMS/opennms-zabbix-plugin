package org.opennms.plugins.zabbix;

import java.io.Closeable;
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
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.SimpleChannelPool;
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
    private static long REUSE_MAX_IDLE = 45_000L;
    private static int REUSE_MAX_REQUEST = 50;
    private static AttributeKey<CompletableFuture<String>> FUTURE = AttributeKey.newInstance("zabbix_future");
    private static AttributeKey<Long> POOL_RELEASED_TIME = AttributeKey.newInstance("releaseTime");
    private static AttributeKey<Integer> POOL_REQUEST_COUNT = AttributeKey.newInstance("requestCount");

    private ChannelPool channelPool;

    public ZabbixAgentClient(EventLoopGroup group, InetAddress address, int port) {
       Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .channel(NioSocketChannel.class)
                .remoteAddress(address, port);

        channelPool = new SimpleChannelPool(bootstrap, new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel channel) {
                channel.attr(POOL_RELEASED_TIME).set(System.currentTimeMillis());
                channel.attr(POOL_REQUEST_COUNT).set(0);
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast("encoder", new ClientRequestEncoder())
                        .addLast("decoder", new ClientResponseDecoder())
                        .addLast("clientHandler", new PooledClientHandler(channelPool));
            }

            @Override
            public void channelAcquired(Channel ch) {
                ch.attr(POOL_REQUEST_COUNT).set(ch.attr(POOL_REQUEST_COUNT).get() + 1);
            }

            @Override
            public void channelReleased(Channel ch) {
                ch.attr(POOL_RELEASED_TIME).set(System.currentTimeMillis());
            }
        }, new AsyncChannelHealthChecker());
    }

    public CompletableFuture<List<Map<String, Object>>> discoverData(String key) {
        return  retrieveData(key).thenApplyAsync(data -> {
            ObjectMapper mapper = new ObjectMapper();
            // FIXME: Not sure if all discovery rule keys follow the same format
            try {
                if(data.startsWith(UNSUPPORTED_HEADER)) {
                    return Collections.emptyList();
                }
                return (List<Map<String, Object>>) mapper.readValue(data, List.class);
            } catch (JsonProcessingException e) {
                LOG.trace("{} = <invalid json>", key);
                return Collections.emptyList();
            }
        });

    }

    public CompletableFuture<String> retrieveData(String key) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Future<Channel> channelFuture = channelPool.acquire();
        channelFuture.addListener((FutureListener<Channel>) f -> {
            if (f.isSuccess()) {
                Channel channel = f.getNow();
                channel.attr(FUTURE).set(future);
                channel.writeAndFlush(key);
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
            pool.release(ctx.channel()); //still need this otherwise it will hang forever when testing with real Windows agent
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            Attribute<CompletableFuture<String>> futureAttribute = ctx.channel().attr(FUTURE);
            CompletableFuture<String> future = futureAttribute.get();
            if(future!=null) {
                future.complete(sb.toString());
            }
            pool.release(ctx.channel());
            ctx.fireChannelReadComplete();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            Attribute<CompletableFuture<String>> futureAttribute = ctx.channel().attr(FUTURE);
            CompletableFuture<String> future = futureAttribute.get();
            pool.release(ctx.channel());
            ctx.close();
            if(future != null) {
                future.completeExceptionally(cause);
            }
        }
    }



    //This doesn't see any difference in local test
    private static class AsyncChannelHealthChecker implements ChannelHealthChecker {

        @Override
        public Future<Boolean> isHealthy(Channel channel) {
            final long timeSinceUsed = System.currentTimeMillis()-channel.attr(POOL_RELEASED_TIME).get();
            final int requestCount = channel.attr(POOL_REQUEST_COUNT).get();
            LOG.info("Channel used since last {} second(s) and required by {} times", timeSinceUsed/1000, requestCount);
            if(timeSinceUsed > REUSE_MAX_IDLE || requestCount > REUSE_MAX_REQUEST) {
                return channel.eventLoop().newSucceededFuture(Boolean.FALSE);
            }
            return ChannelHealthChecker.ACTIVE.isHealthy(channel);
        }
    }

}
