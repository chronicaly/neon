package me.xchro.neon.render;

import me.xchro.neon.Neon;
import me.xchro.neon.core.CrosshairRaster;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Replaces only vanilla's reticle sprite; visibility rules and attack indicators remain native. */
public final class CrosshairRenderer {
    private static CrosshairRaster.Key cached;
    private static List<CrosshairRaster.Run> runs=List.of();
    private CrosshairRenderer() {}
    public static void draw(GuiGraphicsExtractor g) {
        var window=Minecraft.getInstance().getWindow();var c=Neon.config.crosshair;
        int width=window.getWidth(),height=window.getHeight();
        var key=new CrosshairRaster.Key(c.shape,(float)c.length,(float)c.gap,(float)c.thickness,c.dot,c.outline?(float)c.outlineWidth:0,(width&1)!=0,(height&1)!=0);
        if(!key.equals(cached)) { cached=key;runs=CrosshairRaster.rasterize(key); }
        g.pose().pushMatrix();g.pose().scale(1f/window.getGuiScale());
        try {
            g.pose().translate(width/2,height/2);
            int color=c.color(Neon.config.theme);
            for(var run:runs)g.fill(run.x(),run.y(),run.x()+run.length(),run.y()+1,run.color(color));
        } finally { g.pose().popMatrix(); }
    }
}
