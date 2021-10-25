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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MessageCodec {
    public static String decode(ByteBuf data) {
        //TODO: validate header and data length
        /*byte [] bytes = new byte[data.readableBytes()];
        data.readBytes(bytes);*/
        //data.readBytes(5);
        /*int size = (data.readByte() & 0xFF)
                + ((data.readByte() & 0xFF) << 8)
                + ((data.readByte() & 0xFF) << 16)
                + ((data.readByte() & 0xFF) << 24);*/
        // FIXME: This part of the data len as well
        // Padding, ignore
        data.readBytes(13);

        int size = data.readableBytes();


        String msg =data.readCharSequence(size, StandardCharsets.UTF_8).toString();
        System.out.println(msg);
        return msg;
        /*String msg = new String(bytes);
        return msg.substring(13);*/
    }

    public static ByteBuf encode(String msg) {

        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        byte[] header = new byte[] {
                'Z', 'B', 'X', 'D', '\1',
                (byte)(data.length & 0xFF),
                (byte)((data.length >> 8) & 0xFF),
                (byte)((data.length >> 16) & 0xFF),
                (byte)((data.length >> 24) & 0xFF),
                '\0', '\0', '\0', '\0'};        byte[] packet = new byte[header.length + data.length];
        System.arraycopy(header, 0, packet, 0, header.length);
        System.arraycopy(data, 0, packet, header.length, data.length);

        return Unpooled.copiedBuffer(packet);
    }
}
