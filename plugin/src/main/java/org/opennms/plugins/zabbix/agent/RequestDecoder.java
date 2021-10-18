package org.opennms.plugins.zabbix.agent;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class RequestDecoder extends ReplayingDecoder<AgentRequest> {

    @Override
    protected void decode(ChannelHandlerContext ctx,
                          ByteBuf in, List<Object> out) {
        AgentRequest request = new AgentRequest();
        // FIXME: Validate header
        // Header, ignore
        in.readBytes(5);
        int size = (in.readByte() & 0xFF)
                + ((in.readByte() & 0xFF) << 8)
                + ((in.readByte() & 0xFF) << 16)
                + ((in.readByte() & 0xFF) << 24);
        // FIXME: This part of the data len as well
        // Padding, ignore
        in.readBytes(4);
        // Key
        request.setKey(in.readCharSequence(size, StandardCharsets.UTF_8).toString());
        out.add(request);
    }
}
