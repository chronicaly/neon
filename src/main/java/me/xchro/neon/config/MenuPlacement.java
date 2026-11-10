package me.xchro.neon.config;

import com.google.gson.Gson;
import java.nio.file.*;

/** View preferences are separate from module profiles and their undo history. */
public final class MenuPlacement {
    public int layoutVersion = 4;
    public double x = .25, y = .25, width = .5, height = .5;
    public boolean customized;
    public static MenuPlacement load(Path file) throws java.io.IOException {
        if (!Files.exists(file)) return new MenuPlacement();
        return decode(Files.readString(file));
    }
    public static MenuPlacement decode(String json) {
        try {
            var document = com.google.gson.JsonParser.parseString(json);
            if (!document.isJsonObject() || !document.getAsJsonObject().has("layoutVersion")) return new MenuPlacement();
            MenuPlacement p = new Gson().fromJson(json, MenuPlacement.class);
            if (p == null || !Double.isFinite(p.x + p.y + p.width + p.height) || p.width <= 0 || p.height <= 0) return new MenuPlacement();
            if (p.layoutVersion == 2) { p.width *= 1.15; p.height *= 1.15; p.layoutVersion = 3; }
            if (p.layoutVersion == 3) { p.width *= 1.15; p.height *= 1.15; p.layoutVersion = 4; }
            p.x = Math.clamp(p.x, 0, 1); p.y = Math.clamp(p.y, 0, 1);
            p.width = Math.clamp(p.width, .1, 1); p.height = Math.clamp(p.height, .1, 1); return p;
        } catch (RuntimeException invalid) { return new MenuPlacement(); }
    }
    public void save(Path file) throws java.io.IOException { ConfigStore.writeAtomically(file, new Gson().toJson(this)); }
    public void queue(Path file, QueuedSave writer) { writer.submit(file, new Gson().toJson(this)); }
}
