package me.xchro.neon;

import java.nio.file.*;
import me.xchro.neon.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HudPanelPlacementTest {
    @TempDir Path directory;
    @Test void roundTripsIndependentlyOfModuleProfiles() throws Exception {
        var placement=new HudPanelPlacement(); placement.capture(600,300,1000,600);
        Path file=directory.resolve("neon-hud-window.json");
        try(var writer=new QueuedSave((p,e)->fail(e))) { placement.queue(file,writer); writer.flush().join(); }
        assertEquals(new HudPanelPlacement.Position(600,300),HudPanelPlacement.load(file).fit(1000,600,188,89));
        assertFalse(ConfigStore.encode(new NeonConfig()).contains("customized"));
    }
    @Test void shrinkingViewportKeepsEntireCollapsedColumnReachable() {
        var placement=new HudPanelPlacement(); placement.capture(900,500,1000,600);
        var fit=placement.fit(320,240,188,89);
        assertEquals(124,fit.x()); assertEquals(143,fit.y());
        assertEquals(new HudPanelPlacement.Position(12,12),new HudPanelPlacement().fit(1000,600,188,89));
    }
    @Test void corruptAndNonfinitePlacementFallBackWithoutAffectingSettings() {
        for(String json:new String[]{"null","oops","{\"customized\":true,\"x\":1e999}","{\"customized\":true,\"y\":1e999}"})
            assertEquals(new HudPanelPlacement.Position(12,12),HudPanelPlacement.decode(json).fit(1000,600,188,89));
    }
}
