package me.lokspel.spawnauth.storage;

import me.lokspel.spawnauth.config.SpawnAuthConfig;
import me.lokspel.spawnauth.storage.backend.MySqlStorageBackend;
import me.lokspel.spawnauth.storage.backend.SqliteStorageBackend;
import me.lokspel.spawnauth.storage.backend.StorageBackend;
import me.lokspel.spawnauth.storage.model.SavedLocation;

import java.io.File;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageService implements AutoCloseable {

    private final Logger logger;
    private final boolean cacheEnabled;
    private final Map<String, SavedLocation> memoryCache = new ConcurrentHashMap<>();
    private final StorageBackend backend;
    private final ExecutorService executor;

    public StorageService(File dataFolder, SpawnAuthConfig config, Logger logger) {
        this.logger = logger;
        this.cacheEnabled = config.isDatabaseCache();

        DatabaseType type = config.getDatabaseType();
        if (type == DatabaseType.NONE && !this.cacheEnabled) {
            throw new IllegalStateException("Database type 'none' requires the in-memory cache to be enabled.");
        }

        StorageBackend createdBackend = null;
        if (type == DatabaseType.SQLITE) {
            createdBackend = new SqliteStorageBackend(dataFolder, config.getTablePrefix(), logger);
        } else if (type == DatabaseType.MYSQL) {
            createdBackend = new MySqlStorageBackend(config, logger);
        }
        this.backend = createdBackend;

        if (this.backend != null) {
            int poolThreads = type == DatabaseType.SQLITE ? 1 : Math.min(config.getPoolMaxSize(), 4);
            this.executor = Executors.newFixedThreadPool(poolThreads, new NamedThreadFactory("SpawnAuth-StoragePool"));
        } else {
            this.executor = null;
        }
    }

    public void init() {
        if (backend != null) {
            try {
                backend.init();
                logger.info("Storage backend initialized successfully.");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize storage backend!", e);
                if (!cacheEnabled) {
                    throw new RuntimeException("Cannot continue without storage backend or cache", e);
                }
                logger.warning("Falling back to pure in-memory cache storage.");
            }
        }
    }

    private String normalize(String playerName) {
        return playerName == null ? "" : playerName.toLowerCase(Locale.ROOT);
    }

    public void saveLocation(SavedLocation location) {
        if (location == null) {
            return;
        }

        String key = normalize(location.playerName());
        if (cacheEnabled) {
            memoryCache.put(key, location);
        }

        if (backend != null && executor != null && !executor.isShutdown()) {
            submitAsync(() -> {
                backend.upsert(location);
                return null;
            });
        }
    }

    public CompletableFuture<SavedLocation> getLocation(String playerName) {
        String key = normalize(playerName);

        if (cacheEnabled) {
            SavedLocation cached = memoryCache.get(key);
            if (cached != null) {
                return CompletableFuture.completedFuture(cached);
            }
        }

        if (backend != null && executor != null && !executor.isShutdown()) {
            return submitAsync(() -> {
                SavedLocation fromDb = backend.get(playerName);
                if (fromDb != null && cacheEnabled) {
                    memoryCache.put(key, fromDb);
                }
                return fromDb;
            });
        }

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<SavedLocation> takeLocation(String playerName) {
        String key = normalize(playerName);

        if (cacheEnabled) {
            SavedLocation cached = memoryCache.remove(key);
            if (cached != null) {
                if (backend != null && executor != null && !executor.isShutdown()) {
                    submitAsync(() -> {
                        backend.delete(playerName);
                        return null;
                    });
                }
                return CompletableFuture.completedFuture(cached);
            }
        }

        if (backend != null && executor != null && !executor.isShutdown()) {
            return submitAsync(() -> {
                SavedLocation fromDb = backend.get(playerName);
                if (fromDb != null) {
                    backend.delete(playerName);
                }
                return fromDb;
            });
        }

        return CompletableFuture.completedFuture(null);
    }

    public void removeLocation(String playerName) {
        String key = normalize(playerName);
        if (cacheEnabled) {
            memoryCache.remove(key);
        }

        if (backend != null && executor != null && !executor.isShutdown()) {
            submitAsync(() -> {
                backend.delete(playerName);
                return null;
            });
        }
    }

    public SavedLocation getDirectFromCache(String playerName) {
        return memoryCache.get(normalize(playerName));
    }

    public Collection<SavedLocation> getCachedLocations() {
        return memoryCache.values();
    }

    private <T> CompletableFuture<T> submitAsync(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            executor.submit(() -> {
                try {
                    future.complete(task.call());
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Error executing database operation", t);
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    @Override
    public void close() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (backend != null) {
            backend.close();
        }

        memoryCache.clear();
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
