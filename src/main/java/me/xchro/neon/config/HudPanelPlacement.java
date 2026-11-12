package me.xchro.neon.config;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.*;

/** Editor view preference, intentionally outside HUD configurations and profiles. */
public final class HudPanelPlacement {
    private double x, y;
    private boolean customized;
    public record Position(int x, int y) {}
    public static HudPanelPlacement load(Path file) throws IOException {
        return Files.exists(file) ? decode(Files.readString(file)) : new HudPanelPlacement();
    }
    public static HudPanelPlacement decode(String json) {
        try {
            var result = new Gson().fromJson(json, HudPanelPlacement.class);
            if (result == null || !Double.isFinite(result.x) || !Double.isFinite(result.y)) return new HudPanelPlacement();
            result.x = Math.clamp(result.x,0,1); result.y = Math.clamp(result.y,0,1); return result;
        } catch (RuntimeException invalid) { return new HudPanelPlacement(); }
    }
    public Position fit(int width, int height, int panelWidth, int minimumHeight) {
        return new Position(Math.clamp(customized ? (int)Math.round(x*width) : 12,8,Math.max(8,width-panelWidth-8)),
            Math.clamp(customized ? (int)Math.round(y*height) : 12,8,Math.max(8,height-minimumHeight-8)));
    }
    public void capture(int left, int top, int width, int height) {
        x=Math.clamp(left/(double)Math.max(1,width),0,1); y=Math.clamp(top/(double)Math.max(1,height),0,1); customized=true;
    }
    public void queue(Path file, QueuedSave writer) { writer.submit(file,new Gson().toJson(this)); }
}
