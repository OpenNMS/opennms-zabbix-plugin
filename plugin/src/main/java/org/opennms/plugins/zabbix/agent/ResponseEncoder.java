package org.opennms.plugins.zabbix.agent;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ResponseEncoder extends MessageToByteEncoder<AgentResponse> {

    @Override
    protected void encode(ChannelHandlerContext ctx,
                          AgentResponse msg, ByteBuf out) {
        final byte[] data;
        if (msg.getValue() != null) {
            data = msg.getValue().getBytes(StandardCharsets.UTF_8);
        } else {
            data = new byte[] {'Z', 'B', 'X', '_',
                    'N', 'O', 'T', 'S', 'U', 'P', 'P', 'O', 'R', 'T', 'E', 'D', '\0',
                    'O', 'O', 'P', 'S'};
        }

        // Response header
        byte[] header = new byte[] {
                'Z', 'B', 'X', 'D', '\1',
                (byte)(data.length & 0xFF),
                (byte)((data.length >> 8) & 0xFF),
                (byte)((data.length >> 16) & 0xFF),
                (byte)((data.length >> 24) & 0xFF),
                '\0', '\0', '\0', '\0'};
        out.writeBytes(header);
        // Response body
        out.writeBytes(data);
    }
}