package me.xchro.neon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;

public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private String recoveryNotice = "";
    public ConfigStore(Path file) { this.file = file; }
    public String recoveryNotice() { return recoveryNotice; }
    public static String encode(NeonConfig config) { return GSON.toJson(config); }
    public static NeonConfig decode(String json) {
        NeonConfig config = GSON.fromJson(json, NeonConfig.class);
        if (config == null) throw new IllegalArgumentException("Empty config");
        var document = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        if (config.theme != null && document.has("theme") && document.get("theme").isJsonObject()
            && !document.getAsJsonObject("theme").has("paletteVersion")) config.theme.upgradeSurfaces();
        if (!document.has("armorAnchor") && document.has("armor") && document.get("armor").isJsonObject()) {
            var armor = document.getAsJsonObject("armor");
            if (armor.has("x") || armor.has("y")) config.armorAnchor = NeonConfig.ArmorAnchor.CUSTOM;
        }
        config.sanitize(); return config;
    }
    public static NeonConfig copy(NeonConfig config) { return decode(encode(config)); }
    public NeonConfig load() throws IOException {
        recoveryNotice="";
        boolean existed=Files.exists(file);
        if(existed) {
            try { return decode(Files.readString(file)); }
            catch(RuntimeException invalid) { archiveInvalid(file); }
        }
        Path backup=backupPath(file);
        if(Files.exists(backup)) {
            NeonConfig recovered=null;
            try { recovered=decode(Files.readString(backup)); }
            catch(RuntimeException invalid) { archiveInvalid(backup); }
            if(recovered!=null) {
                writeRecoverable(file,encode(recovered));
                recoveryNotice="Recovered settings from the last saved copy."+(existed?" The damaged file was preserved.":"");
                return recovered;
            }
        }
        if(existed)recoveryNotice="Settings could not be recovered; using defaults. The damaged files were preserved.";
        return new NeonConfig();
    }
    public void save(NeonConfig config) throws IOException {
        config.sanitize(); writeRecoverable(file, encode(config));
    }
    public java.util.concurrent.CompletableFuture<Void> queue(QueuedSave writer, NeonConfig config) {
        config.sanitize(); return writer.submitRecoverable(file, encode(config));
    }
    static Path backupPath(Path file) { return file.resolveSibling(file.getFileName()+".bak"); }
    static void archiveInvalid(Path file) throws IOException {
        Path archive=Files.createTempFile(file.toAbsolutePath().getParent(),file.getFileName()+".invalid-",".json");
        Files.copy(file,archive,StandardCopyOption.REPLACE_EXISTING);
    }
    static void writeRecoverable(Path file,String json) throws IOException {
        writeAtomically(file,json);
        writeAtomically(backupPath(file),json);
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
