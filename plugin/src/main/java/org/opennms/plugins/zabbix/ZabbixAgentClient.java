package org.opennms.plugins.zabbix;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * TODO: Better handling of unsupported keys i.e. ZBX_NOTSUPPORTEDUnsupported item key. or ZBX_NOTSUPPORTEDToo many parameters.
 * TODO: Support many keys in one session
 * TODO: Full async communication and async API (could be exposed as something like CompletableFuture<Resuts> getKeys(String... keys))
 */
public class ZabbixAgentClient implements Closeable {
    public static final int DEFAULT_PORT = 10050;

    private static String ZBX_HEADER="ZBXD";
    private static int HEADER_LENGTH = 13;

    private final InetAddress address;
    private final int port;

    private SocketChannel channel;

    public ZabbixAgentClient(InetAddress address, int port) {
        this.address = Objects.requireNonNull(address);
        this.port = port;
    }

    private SocketChannel openConnection(InetAddress address, int port) throws IOException {
        return SocketChannel.open(new InetSocketAddress(address, port));
    }

    public String retrieveData(String key) throws IOException {
        try(final SocketChannel channel = openConnection(address, port)) {
            ByteBuffer requestBuffer = prepareByteBufferToSend(key);
            channel.write(requestBuffer);        ByteBuffer buffer = ByteBuffer.allocate(512);
            StringBuilder sb = new StringBuilder();
            while (channel.read(buffer) > 0) {
                buffer.flip();
                while(buffer.hasRemaining()) {
                    sb.append((char) buffer.get());
                }
                buffer.clear();
            }
            return sb.length() > HEADER_LENGTH ? sb.substring(HEADER_LENGTH) : sb.toString();
        }
    }

    private static byte[] prepareBytes(String key) {
        byte[] data = key.getBytes(StandardCharsets.UTF_8);
        byte[] header = new byte[] {
                'Z', 'B', 'X', 'D', '\1',
                (byte)(data.length & 0xFF),
                (byte)((data.length >> 8) & 0xFF),
                (byte)((data.length >> 16) & 0xFF),
                (byte)((data.length >> 24) & 0xFF),
                '\0', '\0', '\0', '\0'};        byte[] packet = new byte[header.length + data.length];
        System.arraycopy(header, 0, packet, 0, header.length);
        System.arraycopy(data, 0, packet, header.length, data.length);
        return packet;
    }

    private static ByteBuffer prepareByteBufferToSend(String key) {
        return ByteBuffer.wrap(prepareBytes(key));
    }

    @Override
    public void close() throws IOException {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }
}
