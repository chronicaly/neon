package me.xchro.neon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;

public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    public ConfigStore(Path file) { this.file = file; }
    public static String encode(NeonConfig config) { return GSON.toJson(config); }
    public static NeonConfig decode(String json) {
        NeonConfig config = GSON.fromJson(json, NeonConfig.class);
        if (config == null) throw new IllegalArgumentException("Empty config");
        var document = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        if (!document.has("armorAnchor") && document.has("armor") && document.get("armor").isJsonObject()) {
            var armor = document.getAsJsonObject("armor");
            if (armor.has("x") || armor.has("y")) config.armorAnchor = NeonConfig.ArmorAnchor.CUSTOM;
        }
        config.sanitize(); return config;
    }
    public static NeonConfig copy(NeonConfig config) { return decode(encode(config)); }
    public NeonConfig load() throws IOException {
        if (!Files.exists(file)) return new NeonConfig();
        try {
            return decode(Files.readString(file));
        } catch (RuntimeException invalid) {
            Files.copy(file, file.resolveSibling(file.getFileName() + ".invalid-" + System.currentTimeMillis()));
            return new NeonConfig();
        }
    }
    public void save(NeonConfig config) throws IOException {
        config.sanitize(); writeAtomically(file, encode(config));
    }
    public java.util.concurrent.CompletableFuture<Void> queue(QueuedSave writer, NeonConfig config) {
        config.sanitize(); return writer.submit(file, encode(config));
    }
    static void writeAtomically(Path file, String json) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "neon-", ".tmp");
        try {
            Files.writeString(temporary, json);
            for (int attempt = 0; ; attempt++) {
                try {
                    try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                    catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
                    break;
                } catch (AccessDeniedException busy) {
                    // Windows scanners can briefly hold the replaced file after a successful save.
                    if (attempt == 3) throw busy;
                    try { Thread.sleep(10L << attempt); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("Interrupted while saving " + file, interrupted); }
                }
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
