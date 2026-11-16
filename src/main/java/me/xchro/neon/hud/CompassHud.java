package me.xchro.neon.hud;

import me.xchro.neon.Neon;
import me.xchro.neon.config.ThemeColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class CompassHud extends HudCard {
    private static final String[] DIRECTIONS={"N","NE","E","SE","S","SW","W","NW"};
    CompassHud() { super(HudKind.COMPASS); }
    @Override protected void update(Minecraft mc,boolean preview) {
        double yaw=mc.player==null?0:mc.player.getYRot();
        lines=new String[]{HudNumbers.heading(yaw)+(Neon.config.compassDegrees?"  "+(int)HudNumbers.bearing(yaw)+"°":"")};
    }
    @Override protected int contentWidth() { return 120; }
    @Override protected int contentHeight() { return 21+text.height(); }
    @Override protected void renderContent(GuiGraphicsExtractor g,boolean preview) {
        super.renderContent(g,preview);
        var mc=Minecraft.getInstance();var theme=Neon.config.theme;
        double bearing=HudNumbers.bearing(mc.player==null?0:mc.player.getYRot());
        int muted=theme.color(ThemeColor.MUTED),accent=theme.color(ThemeColor.ACCENT);
        g.fill(64,14,66,17,accent);
        for(int degree=0;degree<360;degree+=15) {
            double offset=HudNumbers.angleDifference(degree,bearing);
            if(Math.abs(offset)>54)continue;
            int x=65+(int)Math.round(offset);
            g.fill(x,18,x+1,degree%45==0?22:20,muted);
            if(degree%45==0) {
                String label=DIRECTIONS[degree/45];
                text.draw(g,label,x-text.width(label)/2,24,degree==0?accent:theme.color(ThemeColor.TEXT),Neon.config.hud(kind).shadow);
            }
        }
    }
}
