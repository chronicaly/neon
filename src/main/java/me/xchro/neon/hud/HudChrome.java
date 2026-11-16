package me.xchro.neon.hud;

import me.xchro.neon.Neon;
import me.xchro.neon.config.ThemeColor;
import me.xchro.neon.core.OutlineTravel;
import me.xchro.neon.ui.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** One theme-linked surface and physical-pixel outline per widget, never per item. */
public final class HudChrome {
    private final OutlineTravel travel = new OutlineTravel();

    public void draw(GuiGraphicsExtractor g, HudBounds b) {
        var theme = Neon.config.theme;
        int background = theme.color(ThemeColor.BACKGROUND);
        g.fill(b.x(), b.y(), b.x()+b.width(), b.y()+b.height(),
            Ui.alpha(background, (int)Math.round((background >>> 24) * .74)));
        border(g,b,theme.color(ThemeColor.OUTLINE),theme.color(ThemeColor.SECONDARY));
    }

    public void border(GuiGraphicsExtractor g, HudBounds b, int primary, int secondary) {
        if (b.width() <= 0 || b.height() <= 0) return;
        if (Neon.config.hudAnimatedOutline) {
            Ui.hudOutline(g,b.x(),b.y(),b.width(),b.height(),travel,primary,secondary);
        } else {
            int x=b.x(), y=b.y(), r=x+b.width(), bottom=y+b.height();
            g.fill(x,y,r,y+1,primary); g.fill(x,bottom-1,r,bottom,primary);
            g.fill(x,y+1,x+1,bottom-1,primary); g.fill(r-1,y+1,r,bottom-1,primary);
        }
    }
}
