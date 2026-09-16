package com.cluster.worker.persistence;

public interface LocalStateStore {
    /**
     * Reads the state as a raw JSON string.
     * @return the JSON string, or null if it doesn't exist.
     */
    String readState();

    /**
     * Writes the state as a raw JSON string.
     * @param json the JSON string to write.
     */
    void writeState(String json);

    /**
     * Deletes the state.
     */
    void deleteState();
}
