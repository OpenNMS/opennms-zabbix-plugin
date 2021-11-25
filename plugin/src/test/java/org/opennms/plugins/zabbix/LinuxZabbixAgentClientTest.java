package org.opennms.plugins.zabbix;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class LinuxZabbixAgentClientTest {

    @Rule
    public LinuxZabbixAgentResource zabbixAgent = new LinuxZabbixAgentResource();
    public static ZabbixAgentClientFactory clientFactory;

    @BeforeClass
    public static void setup() {
        clientFactory = new ZabbixAgentClientFactory();
    }

    @Test
    public void canQueryLocalAgent() throws ExecutionException, InterruptedException {
        try (ZabbixAgentClient client = clientFactory.createClient(zabbixAgent.getAddress(), zabbixAgent.getPort())) {
            List<Map<String, Object>> data = client.discoverData("vfs.fs.discovery").get();
            assertThat(data, not(empty()));
        }
    }

    @Test
    public void testRetrieveDataLocalAgent() throws ExecutionException, InterruptedException {
        try (ZabbixAgentClient client = clientFactory.createClient(zabbixAgent.getAddress(), zabbixAgent.getPort())) {
            String result = client.retrieveData("vfs.fs.discovery").get();
            assertThat(result, notNullValue());
        }
    }

    @Test
    public void testRetrieveUnSupport() throws ExecutionException, InterruptedException {
        try (ZabbixAgentClient client = clientFactory.createClient(zabbixAgent.getAddress(), zabbixAgent.getPort())) {
            String result = client.retrieveData("some-bad-key").get();
            assertThat(result, startsWith(ZabbixAgentClient.UNSUPPORTED_HEADER));
        }
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
}
