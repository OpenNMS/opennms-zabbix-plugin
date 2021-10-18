package org.opennms.plugins.zabbix.lab;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LabContextManager {
    private static final Logger LOG = LoggerFactory.getLogger(LabContextManager.class);

    private final AtomicLong sessionIdGenerator = new AtomicLong(System.currentTimeMillis());
    protected static final String SESSION_ID_PARM_NAME = "sessionId";

    private final Map<Long, LabSession> sessionsById = new ConcurrentHashMap<>();

    public LabSession newSession() {
        final long sessionId = sessionIdGenerator.incrementAndGet();
        final LabSession session = new LabSession(sessionId);
        sessionsById.put(sessionId, session);
        return session;
    }

    public void trackGetRequisitionForSession(String sessionIdAsString) {
        final Long sessionId;
        try {
            sessionId = Long.parseLong(sessionIdAsString);
        } catch(NumberFormatException nfe) {
            LOG.warn("Invalid session id '{}'. Ignoring.", sessionIdAsString);
            return;
        }
        trackGetRequisitionForSession(sessionId);
    }

    public void trackGetRequisitionForSession(Long sessionId) {
        final LabSession session = sessionsById.get(sessionId);
        if (session == null) {
            return;
        }
        session.trackGetRequisition();
    }

    public class LabSession implements AutoCloseable {
        private final long sessionId;

        private CountDownLatch getLatch = new CountDownLatch(1);

        public LabSession(long sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public void close() {
            sessionsById.remove(sessionId);
        }

        private void trackGetRequisition() {
            getLatch.countDown();
        }

        public void waitForGet() throws InterruptedException {
            getLatch.await();
        }

        public String getSessionId() {
            return Long.toString(sessionId);
        }
    }
}