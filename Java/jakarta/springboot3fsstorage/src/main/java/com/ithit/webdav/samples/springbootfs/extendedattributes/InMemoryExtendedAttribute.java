package com.ithit.webdav.samples.springbootfs.extendedattributes;

import com.ithit.webdav.integration.extendedattributes.ExtendedAttribute;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.SneakyThrows;

public class InMemoryExtendedAttribute implements ExtendedAttribute {

    private static final String ATTRIBUTES_FILE = "extended_attributes.json";
    private static final long SAVE_INTERVAL_SECONDS = 30;

    private final Map<String, Map<String, String>> attributes = new ConcurrentHashMap<>();
    private final Path filePath;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public InMemoryExtendedAttribute() {
        String userHome = System.getProperty("user.home");
        this.filePath = Paths.get(userHome, ATTRIBUTES_FILE);
        load();

        scheduler.scheduleWithFixedDelay(() -> {
            if (dirty.compareAndSet(true, false)) {
                synchronized (this) {
                    save();
                }
            }
        }, SAVE_INTERVAL_SECONDS, SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            synchronized (this) {
                if (dirty.get()) {
                    save();
                }
            }
        }));
    }

    @Override
    public synchronized void setExtendedAttribute(String path, String attribName, String attribValue) {
        attributes.computeIfAbsent(path, k -> new ConcurrentHashMap<>()).put(attribName, attribValue);
        dirty.set(true);
    }

    @Override
    public synchronized String getExtendedAttribute(String path, String attribName) {
        Map<String, String> pathAttrs = attributes.get(path);
        return pathAttrs != null ? pathAttrs.get(attribName) : null;
    }

    @Override
    public synchronized void deleteExtendedAttribute(String path, String attribName) {
        Map<String, String> pathAttrs = attributes.get(path);
        if (pathAttrs != null) {
            pathAttrs.remove(attribName);
            dirty.set(true);
        }
    }

    @Override
    public int getPriority() {
        // Set it to more than 0 to ensure it's used
        return -1;
    }

    /**
     * Forces an immediate save of extended attributes to disk.
     * This method is useful when you need to guarantee that changes are persisted
     * immediately, such as before critical operations or in tests.
     */
    public synchronized void flush() {
        if (dirty.compareAndSet(true, false)) {
            save();
        }
    }

    @SneakyThrows
    private synchronized void load() {
        if (Files.exists(filePath)) {
            String json = Files.readString(filePath);
            Map<String, Map<String, String>> loaded = new Gson().fromJson(json, new TypeToken<Map<String, Map<String, String>>>() {}.getType());
            if (loaded != null) {
                attributes.putAll(loaded);
            }
        }
    }

    @SneakyThrows
    private synchronized void save() {
        String json = new Gson().toJson(attributes);
        Files.writeString(filePath, json);
    }
}
