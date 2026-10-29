package com.ayoubela.zerorenderafk.client.antiafk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

public final class AntiAfkStore {
    private final Path file;
    private AntiAfkConfig current = new AntiAfkConfig();
    public AntiAfkStore(Path file) { this.file = file; }
    public void load() throws IOException {
        if (!Files.exists(file)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { p.load(r); }
        try {
            AntiAfkConfig loaded = AntiAfkConfig.fromProperties(p);
            String error = loaded.validate();
            if (error != null) throw new IllegalArgumentException(error);
            current = loaded;
        } catch (IllegalArgumentException e) { throw new IOException("Invalid anti-AFK configuration", e); }
    }
    public AntiAfkConfig snapshot() { return current.copy(); }
    public void save(AntiAfkConfig next) throws IOException {
        String error = next.validate();
        if (error != null) throw new IllegalArgumentException(error);
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "zerorenderafk-", ".tmp");
        try {
            try (Writer w = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                next.toProperties().store(w, "Zero Render AFK - configure in game with F9");
            }
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            current = next.copy();
        } finally { Files.deleteIfExists(temporary); }
    }
}
