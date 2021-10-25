package org.opennms.plugins.zabbix;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.junit.Rule;
import org.junit.Test;
import org.opennms.plugins.zabbix.utils.MessageCodec;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

public class ZabbixAgentClientTest {

    @Rule
    public MockZabbixAgent zabbixAgent = new MockZabbixAgent();

    private static AttributeKey<CompletableFuture<String>> FUTURE = AttributeKey.valueOf("future");
    private ChannelPool channelPool;

    @Test
    public void canQueryLocalAgent() throws IOException, ExecutionException, InterruptedException {
        try (ZabbixAgentClient client = new ZabbixAgentClient(zabbixAgent.getAddress(), zabbixAgent.getPort())) {
            List<Map<String, Object>> data = client.discoverData("vfs.fs.discovery");
            assertThat(data, not(empty()));
        }
    }

    @Test
    public void testRetrieveDataLocalAgent() throws IOException, ExecutionException, InterruptedException {
        try (ZabbixAgentClient client = new ZabbixAgentClient(zabbixAgent.getAddress(), zabbixAgent.getPort())) {
            String result = client.retrieveData("vfs.fs.discovery").get();
            assertThat(result, notNullValue());
        }
    }

    //example client with promise
    @Test
    public void testNewClient() throws IOException, ExecutionException, InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        Promise<String> promise = group.next().newPromise();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.remoteAddress(new InetSocketAddress("localhost", 10050));
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast(new ClientHandler(promise, "vfs.fs.discovery"));
            }
        });
        ChannelFuture cf = bootstrap.connect().sync();
        cf.channel().closeFuture().sync();

        System.out.println(promise.get());

    }

    //example client with ChannelPool
    @Test
    public void testWithChannelPool() {


        EventLoopGroup group = new NioEventLoopGroup(10);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress("localhost", 10050);

        channelPool = new FixedChannelPool(bootstrap, new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                //pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                pipeline.addLast("clientHandler", new PooledClientHandler(channelPool));
            }
        }, 20);

        IntStream.range(0, 100).forEach(i ->{
            CompletableFuture<String> future = new CompletableFuture<>();
            ByteBuf buf = MessageCodec.encode("vfs.fs.discovery");
            Future<Channel> channelFuture = channelPool.acquire();
            channelFuture.addListener(new FutureListener<Channel>() {

                @Override
                public void operationComplete(Future<Channel> f) throws Exception {
                    if(f.isSuccess()) {
                        Channel channel = f.getNow();
                        channel.attr(FUTURE).set(future);
                        channel.writeAndFlush(buf, channel.voidPromise());
                    }
                }
            });


            try {
                System.out.println(i + " " + future.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }


        });

        System.out.println("---------------------another 100 ----------------------------------------------");
        IntStream.range(0, 100).forEach(i ->{
            CompletableFuture<String> future = new CompletableFuture<>();
            ByteBuf buf = MessageCodec.encode("vfs.fs.discovery");
            Future<Channel> channelFuture = channelPool.acquire();
            channelFuture.addListener(new FutureListener<Channel>() {

                @Override
                public void operationComplete(Future<Channel> f) throws Exception {
                    if(f.isSuccess()) {
                        Channel channel = f.getNow();
                        channel.attr(FUTURE).set(future);
                        channel.writeAndFlush(buf, channel.voidPromise());
                    }
                }
            });


            try {
                System.out.println(i + " " + future.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }


        });

        group.shutdownGracefully();
    }

    @Test
    public void byteManipulation() {
        assertThat(encodeDecodeLen(168), equalTo(168));
        assertThat(encodeDecodeLen(0), equalTo(0));
        assertThat(encodeDecodeLen(Integer.MAX_VALUE), equalTo(Integer.MAX_VALUE));
    }

    private static int encodeDecodeLen(int len) {
        byte[] sizeBytes = new byte[]{
                (byte)(len & 0xFF),
                (byte)((len >> 8) & 0xFF),
                (byte)((len >> 16) & 0xFF),
                (byte)((len >> 24) & 0xFF)};
        int decodedLen = sizeBytes[0] & 0xFF;
        decodedLen += ((sizeBytes[1] & 0xFF) << 8);
        decodedLen += ((sizeBytes[2] & 0xFF) << 16);
        decodedLen += ((sizeBytes[3] & 0xFF) << 24);
        return decodedLen;
    }


    private static class ClientHandler extends SimpleChannelInboundHandler {

        private Promise<String> promise;
        private String key;

        public ClientHandler(Promise<String> promise, String key) {
            this.promise = promise;
            this.key = key;
        }

        @Override
        public void channelActive(ChannelHandlerContext channelHandlerContext){
            ByteBuf buf = MessageCodec.encode(key);
            channelHandlerContext.writeAndFlush(buf);
        }

        @Override
        public void channelRead0(ChannelHandlerContext channelHandlerContext, Object in) throws Exception {
            ByteBuf data =(ByteBuf) in;
            String msg = MessageCodec.decode(data);
            promise.trySuccess(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable cause){
            cause.printStackTrace();
            channelHandlerContext.close();
        }
    }

    private static class PooledClientHandler extends SimpleChannelInboundHandler {

        private ChannelPool pool;

        public PooledClientHandler(ChannelPool pool) {
            this.pool = pool;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object in) throws Exception {
            Attribute<CompletableFuture<String>> futureAttribute = ctx.channel().attr(FUTURE);
            CompletableFuture<String> future = futureAttribute.getAndSet(new CompletableFuture<>());
            ByteBuf data =(ByteBuf) in;
            String msg = MessageCodec.decode(data);
            try {
                pool.release(ctx.channel(), ctx.channel().voidPromise());
            } catch (IllegalArgumentException e) {
                System.out.println("error");
            }
            future.complete(msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.writeAndFlush("OK");
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
