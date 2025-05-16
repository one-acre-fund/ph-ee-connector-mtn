package org.mifos.connector.mtn.flowcomponents.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory implementation of the WorkflowInstanceStore interface. This class is used to store and retrieve workflow
 * IDs in memory.
 */
@Component
public class InMemoryWorkflowInstanceStore implements WorkflowInstanceStore {

    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    public void put(String key, String value) {
        store.put(key, value);
    }

    @Override
    public String get(String key) {
        return store.get(key);
    }

    @Override
    public void remove(String key) {
        store.remove(key);
    }
}
