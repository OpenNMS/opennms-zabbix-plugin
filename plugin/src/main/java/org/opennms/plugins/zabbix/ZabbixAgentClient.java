package org.opennms.plugins.zabbix;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TODO: Better handling of unsupported keys i.e. ZBX_NOTSUPPORTEDUnsupported item key. or ZBX_NOTSUPPORTEDToo many parameters.
 * TODO: Full async communication and async API (could be exposed as something like CompletableFuture<Resuts> getKeys(String... keys))
 * TODO: Handle cases where the server rejects the connection - the connection will be closed without any response data - this should be made evident to the caller
 */
public class ZabbixAgentClient implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(ZabbixAgentClient.class);
    public static final int DEFAULT_PORT = 10050;

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

    public List<Map<String, Object>> discoverData(String key) throws IOException  {
        final String json = retrieveData(key);
        ObjectMapper mapper = new ObjectMapper();
        // FIXME: Not sure if all discovery rule keys follow the same format
        try {
            List<Map<String, Object>> entries = (List<Map<String, Object>>) mapper.readValue(json, List.class);
            LOG.trace("{} = {}", key, entries);
            return entries;
        } catch (JsonParseException e) {
            LOG.trace("{} = <invalid json>", key);
            // FIXME: Handle "ZBX_NOTSUPPORTED" better
            return Collections.emptyList();
        }
    }

    public String retrieveData(String key) throws IOException, ZabbixNotSupportedException {
        try(final SocketChannel channel = openConnection(address, port)) {
            ByteBuffer requestBuffer = prepareByteBufferToSend(key);
            channel.write(requestBuffer);
            ByteBuffer buffer = ByteBuffer.allocate(512);
            StringBuilder sb = new StringBuilder();
            while (channel.read(buffer) > 0) {
                buffer.flip();
                while(buffer.hasRemaining()) {
                    sb.append((char) buffer.get());
                }
                buffer.clear();
            }
            // FIXME: Validate header and data length when processing response too
            String response = sb.length() > HEADER_LENGTH ? sb.substring(HEADER_LENGTH) : sb.toString();
            if (response.startsWith("ZBX_NOTSUPPORTED")) {
                LOG.trace("{} <> {}", key, response);
                throw new ZabbixNotSupportedException(String.format("Host: %s, Port: %d, Key: '%s': %s",
                        address.getHostAddress(), port, key, response));
            }
            LOG.trace("{} = {}", key, response);
            return response;
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
