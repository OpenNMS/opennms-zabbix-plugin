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

package org.opennms.plugins.zabbix;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class ZabbixAgentClientFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ZabbixAgentClientFactory.class);

    private static int DEFAULT_THREAD_SIZE = 25;
    private static int DEFAULT_POOL_SIZE = 250;

    private int poolSize;
    private EventLoopGroup group;

    public ZabbixAgentClientFactory() {
        this(DEFAULT_THREAD_SIZE, DEFAULT_POOL_SIZE);
    }

    public ZabbixAgentClientFactory(int threadSize, int poolSize) {
        this.poolSize = poolSize;
        group = new NioEventLoopGroup(threadSize);
    }

    public ZabbixAgentClient createClient(InetAddress address, int port) {
        return new ZabbixAgentClient(group, address, port, poolSize);
    }
}
