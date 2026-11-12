package me.xchro.neon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Named settings snapshots. Key bindings and active runtime states are deliberately global. */
public final class ProfileStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final QueuedSave writer;
    private Data data = new Data();
    private static final class Data {
        int version = 1;
        LinkedHashMap<String, NeonConfig> profiles = new LinkedHashMap<>();
    }
    public ProfileStore(Path file) { this(file, null); }
    public ProfileStore(Path file, QueuedSave writer) { this.file = file; this.writer = writer; }
    public void initialize(NeonConfig baseline) throws IOException {
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file);
                Data loaded = GSON.fromJson(json, Data.class);
                if (loaded == null || loaded.profiles == null) throw new IllegalArgumentException("Empty profiles");
                var saved = com.google.gson.JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("profiles");
                for (var entry : loaded.profiles.entrySet()) {
                    if (!entry.getKey().equals(validName(entry.getKey())) || entry.getValue() == null) throw new IllegalArgumentException("Invalid profile");
                    entry.setValue(ConfigStore.decode(saved.get(entry.getKey()).toString()));
                }
                data = loaded; return;
            } catch (RuntimeException invalid) {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".invalid-" + System.currentTimeMillis()));
            }
        }
        Data defaults = new Data();
        for (String name : List.of("Combat", "Mining", "Building")) {
            NeonConfig c = ConfigStore.copy(baseline); c.resetClicker();
            c.clickerEnabled = c.toggleClicker = true;
            if (name.equals("Combat")) c.clickerEntitiesOnly = true;
            if (name.equals("Mining")) c.leftAction = NeonConfig.LeftAction.HOLD;
            if (name.equals("Building")) { c.leftClick = false; c.rightClick = true; }
            defaults.profiles.put(name, c);
        }
        persist(defaults);
    }
    public List<String> names() { return List.copyOf(data.profiles.keySet()); }
    public NeonConfig get(String name) {
        NeonConfig found = data.profiles.get(name);
        if (found == null) throw new IllegalArgumentException("Profile not found");
        return ConfigStore.copy(found);
    }
    public void put(String name, NeonConfig config) throws IOException {
        putAsync(name,config);
    }
    public java.util.concurrent.CompletableFuture<Void> putAsync(String name, NeonConfig config) throws IOException {
        name = validName(name);
        Data next = new Data(); next.profiles.putAll(data.profiles);
        next.profiles.put(name, ConfigStore.copy(config)); return persist(next);
    }
    public void delete(String name) throws IOException {
        Data next = new Data(); next.profiles.putAll(data.profiles); next.profiles.remove(name); persist(next);
    }
    private java.util.concurrent.CompletableFuture<Void> persist(Data next) throws IOException {
        String snapshot = GSON.toJson(next);
        java.util.concurrent.CompletableFuture<Void> completion;
        if (writer == null) { ConfigStore.writeAtomically(file, snapshot); completion=java.util.concurrent.CompletableFuture.completedFuture(null); }
        else completion=writer.submit(file, snapshot);
        data = next;
        return completion;
    }
    public static String validName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 32 || trimmed.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Use a name with 1-32 characters");
        return trimmed;
    }
}
