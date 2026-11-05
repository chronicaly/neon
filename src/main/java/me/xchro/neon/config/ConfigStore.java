package me.xchro.neon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;

public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    public ConfigStore(Path file) { this.file = file; }
    public NeonConfig load() throws IOException {
        if (!Files.exists(file)) return new NeonConfig();
        try {
            NeonConfig config = GSON.fromJson(Files.readString(file), NeonConfig.class);
            if (config == null) throw new IllegalArgumentException("Empty config");
            config.sanitize(); return config;
        } catch (RuntimeException invalid) {
            Files.copy(file, file.resolveSibling(file.getFileName() + ".invalid-" + System.currentTimeMillis()));
            return new NeonConfig();
        }
    }
    public void save(NeonConfig config) throws IOException {
        config.sanitize(); Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "neon-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(config));
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
