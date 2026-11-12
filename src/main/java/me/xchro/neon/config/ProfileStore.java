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
    private String recoveryNotice = "";
    private record Parsed(Data data,Set<String> damaged) {}
    private static final class Data {
        int version = 1;
        LinkedHashMap<String, NeonConfig> profiles = new LinkedHashMap<>();
    }
    public ProfileStore(Path file) { this(file, null); }
    public ProfileStore(Path file, QueuedSave writer) { this.file = file; this.writer = writer; }
    public String recoveryNotice() { return recoveryNotice; }
    public void initialize(NeonConfig baseline) throws IOException {
        recoveryNotice="";
        Parsed primary=null;
        boolean existed=Files.exists(file);
        if (existed) {
            try {
                primary=parse(Files.readString(file));
                if(primary.damaged.isEmpty()){data=primary.data;return;}
            } catch (RuntimeException invalid) { /* Try the validated recovery copy below. */ }
            ConfigStore.archiveInvalid(file);
        }
        Parsed backup=null;Path backupFile=ConfigStore.backupPath(file);
        if(Files.exists(backupFile)) {
            try {
                backup=parse(Files.readString(backupFile));
                if(!backup.damaged.isEmpty())ConfigStore.archiveInvalid(backupFile);
            } catch(RuntimeException invalid){ConfigStore.archiveInvalid(backupFile);}
        }
        if(primary!=null) {
            Data recovered=new Data();recovered.version=primary.data.version;
            int restored=0,skipped=0;
            for(var entry:primary.data.profiles.entrySet()) {
                NeonConfig value=entry.getValue();
                if(value==null&&backup!=null){value=backup.data.profiles.get(entry.getKey());if(value!=null)restored++;}
                if(value!=null)recovered.profiles.put(entry.getKey(),value);else skipped++;
            }
            // Only primary keys are considered: a backup must never resurrect deleted profiles.
            persist(recovered);
            recoveryNotice="Profiles recovered: "+restored+" restored, "+skipped+" skipped. Healthy profiles and the damaged file were preserved.";
            return;
        }
        if(backup!=null&&(backup.damaged.isEmpty()||backup.data.profiles.values().stream().anyMatch(Objects::nonNull))) {
            Data recovered=backup.data;recovered.profiles.values().removeIf(Objects::isNull);persist(recovered);
            recoveryNotice="Recovered profiles from the last saved copy."+(existed?" The damaged file was preserved.":"");
            return;
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
        if(existed)recoveryNotice="Profiles could not be recovered; using defaults. The damaged files were preserved.";
    }
    private static Parsed parse(String json) {
        var root=com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        var saved=root.getAsJsonObject("profiles");
        if(saved==null)throw new IllegalArgumentException("Missing profiles");
        Data parsed=new Data();Set<String> damaged=new LinkedHashSet<>();
        if(root.has("version"))parsed.version=root.get("version").getAsInt();
        for(var entry:saved.entrySet()) {
            try {
                if(!entry.getKey().equals(validName(entry.getKey())))throw new IllegalArgumentException("Invalid name");
                parsed.profiles.put(entry.getKey(),ConfigStore.decode(entry.getValue().toString()));
            }catch(RuntimeException invalid){parsed.profiles.put(entry.getKey(),null);damaged.add(entry.getKey());}
        }
        return new Parsed(parsed,damaged);
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
        if (writer == null) { ConfigStore.writeRecoverable(file, snapshot); completion=java.util.concurrent.CompletableFuture.completedFuture(null); }
        else completion=writer.submitRecoverable(file, snapshot);
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
