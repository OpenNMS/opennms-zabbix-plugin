/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.plugins.zabbix.utils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.opennms.plugins.zabbix.ZabbixNotSupportedException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class ClientResponseDecoder extends ByteToMessageDecoder {
    private static final int HEADER_LENGTH = 13;
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf data, List<Object> list) throws Exception {
        validateData(data);
        data.readBytes(HEADER_LENGTH);
        int size = data.readableBytes();
        String msg = data.readCharSequence(size, StandardCharsets.UTF_8).toString();
        list.add(msg);
    }

    private void validateData(ByteBuf data) {
        if(data.readableBytes() <= HEADER_LENGTH) {
            throw new ZabbixNotSupportedException("Invalid message from Zabbix agent.");
        }
    }
}
