package me.lokspel.spawnauth.storage.backend;

import me.lokspel.spawnauth.storage.model.SavedLocation;

import java.util.Collection;

public interface StorageBackend extends AutoCloseable {

    void init() throws Exception;

    void upsert(SavedLocation location) throws Exception;

    SavedLocation get(String playerName) throws Exception;

    void delete(String playerName) throws Exception;

    Collection<SavedLocation> getAll() throws Exception;

    @Override
    void close();
}
