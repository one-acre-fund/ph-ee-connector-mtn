package org.mifos.connector.mtn.flowcomponents.state;

/**
 * Key value store for transaction ID and workflow ID.
 */
public interface WorkflowInstanceStore {

    /**
     * Store a workflow ID in the store.
     *
     * @param key
     *            transaction ID
     * @param value
     *            workflow ID
     */
    void put(String key, String value);

    /**
     * Retrieve a workflow ID from the store.
     *
     * @param key
     *            transaction ID
     * @return workflow ID
     */
    String get(String key);

    /**
     * Remove a workflow ID from the store.
     *
     * @param key
     *            transaction ID
     */
    void remove(String key);
}
