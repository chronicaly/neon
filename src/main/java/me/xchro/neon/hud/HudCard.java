package me.xchro.neon.hud;

import me.xchro.neon.Neon;
import me.xchro.neon.config.ThemeColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A reusable Neon surface; content and geometry use the same active font and display scale. */
public abstract class HudCard {
    public final HudKind kind;
    private final HudChrome chrome = new HudChrome();
    protected String[] lines = new String[0];
    protected final HudTypography text=new HudTypography();
    private float renderScale=2;
    private long tick = Long.MIN_VALUE;
    private boolean previewState;
    private Object player;

    protected HudCard(HudKind kind) { this.kind=kind; }
    protected abstract void update(Minecraft mc, boolean preview);
    protected long revision(Minecraft mc) { return mc.player == null ? 0 : mc.player.tickCount; }
    protected int contentWidth() {
        int width = 24;
        for (String line : lines) width = Math.max(width, text.width(line));
        return width;
    }
    protected int contentHeight() { return Math.max(1,lines.length)*text.height()+Math.max(0,lines.length-1)*3; }
    private void prepare(boolean preview) {
        var mc = Minecraft.getInstance();
        long now = revision(mc);
        // Text changes at game-tick speed; no per-render inventory or string rebuilding.
        if (player != mc.player || tick != now || previewState != preview) {
            player=mc.player; tick=now; previewState=preview; update(mc,preview);
        }
    }
    public boolean hasContent(boolean preview) { prepare(preview); return lines.length > 0; }
    public HudBounds bounds(int width, int height, boolean preview) {
        text.scale(Neon.config.hud(kind).placement.scale*NeonHud.density());
        prepare(preview);
        var placement=Neon.config.hud(kind).placement;double scale=placement.scale*NeonHud.density();
        text.scale(scale);
        for(int i=0;i<2;i++) {
            scale=Math.min(scale,Math.min((double)width/(contentWidth()+10),(double)height/(contentHeight()+6)));text.scale(scale);
        }
        renderScale=(float)scale;
        int w=Math.min(width,(int)Math.ceil((contentWidth()+10)*scale)),h=Math.min(height,(int)Math.ceil((contentHeight()+6)*scale));
        return new HudBounds((int)Math.round(placement.x*(width-w)),(int)Math.round(placement.y*(height-h)),w,h);
    }
    public final void draw(GuiGraphicsExtractor g,int width,int height,boolean preview) {
        var options = Neon.config.hud(kind);
        if (!options.placement.visible || !hasContent(preview)) return;
        var b=bounds(width,height,preview);
        var theme=Neon.config.theme;
        if(options.background) {
            int color=theme.color(ThemeColor.BACKGROUND);
            g.fill(b.x(),b.y(),b.x()+b.width(),b.y()+b.height(),(color&0xffffff)|((int)((color>>>24)*.74)<<24));
        }
        if(options.border) chrome.border(g,b,theme.color(ThemeColor.OUTLINE),theme.color(ThemeColor.SECONDARY));
        g.pose().pushMatrix(); g.pose().translate(b.x(),b.y()); g.pose().scale(renderScale);
        renderContent(g,preview);
        g.pose().popMatrix();
    }
    protected void renderContent(GuiGraphicsExtractor g,boolean preview) {
        var theme=Neon.config.theme;
        boolean shadow=Neon.config.hud(kind).shadow;
        for(int i=0;i<lines.length;i++) {
            int y=3+i*(text.height()+3),split=i==0?lines[i].indexOf("  "):-1;
            if(split>0) {
                String label=lines[i].substring(0,split),value=lines[i].substring(split+2);
                text.draw(g,label,5,y,theme.color(ThemeColor.MUTED),shadow);
                text.draw(g,value,5+text.width(label+"  "),y,theme.color(ThemeColor.ACCENT),shadow);
            } else text.draw(g,lines[i],5,y,theme.color(i==0?ThemeColor.ACCENT:ThemeColor.TEXT),shadow);
        }
    }
    public void invalidate() { tick=Long.MIN_VALUE; }
}
