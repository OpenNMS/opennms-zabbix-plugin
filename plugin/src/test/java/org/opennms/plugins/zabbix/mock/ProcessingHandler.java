package org.opennms.plugins.zabbix.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ProcessingHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessingHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        AgentRequest request = (AgentRequest) msg;
        AgentResponse response = new AgentResponse();

        if ("agent.version".equals(request.getKey())) {
            response.setValue("5.4.4");
        } else if ("vfs.fs.inode[/,pfree]".equals(request.getKey())) {
            response.setValue("99.976526");
        } else if ("vfs.fs.discovery".equals(request.getKey())) {
            response.setValue("[{\"{#FSNAME}\":\"/\",\"{#FSTYPE}\":\"apfs\"},{\"{#FSNAME}\":\"/dev\",\"{#FSTYPE}\":\"devfs\"},{\"{#FSNAME}\":\"/System/Volumes/VM\",\"{#FSTYPE}\":\"apfs\"},{\"{#FSNAME}\":\"/System/Volumes/Preboot\",\"{#FSTYPE}\":\"apfs\"},{\"{#FSNAME}\":\"/System/Volumes/Update\",\"{#FSTYPE}\":\"apfs\"},{\"{#FSNAME}\":\"/System/Volumes/Data\",\"{#FSTYPE}\":\"apfs\"},{\"{#FSNAME}\":\"/System/Volumes/Data/home\",\"{#FSTYPE}\":\"autofs\"}]");
        } else if ("system.cpu.discovery".equals(request.getKey())) {
            response.setValue("[{\"{#CPU.NUMBER}\":0,\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER}\":1,\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER}\":2,\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER}\":3,\"{#CPU\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER}\":4,\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER}\":5,\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER}\":6,\"{#CPU.STATUS}\":\"onln,{\"#Pine\"},{\"{#CPU.NUMBER}\":7,\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER}\":8,\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER}\":9,\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER:CPUTU}\":10,\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER}\":11,\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER}\":12,\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER}\":13,\"{#CPU.STAT\"},{CNUS}\":\"online\"},{\"{#CPU.NUMBER}\":14,\"{#CPU.STATUS}\":\"online\"},{\"{#CPU.NUMBER}\":15,\"{#CPU.STATUS}\":\"online\"}]");
        }

        ctx.writeAndFlush(response);
        ctx.close();
        LOG.debug("Request: {}, Response: {}", request, response);
    }
}