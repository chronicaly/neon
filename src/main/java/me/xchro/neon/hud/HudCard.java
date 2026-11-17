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
    private String firstLine="",firstLabel="",firstValue="",firstPrefix="";

    protected HudCard(HudKind kind) { this.kind=kind; }
    protected abstract void update(Minecraft mc, boolean preview);
    protected long revision(Minecraft mc) { return mc.player == null ? 0 : mc.player.tickCount; }
    protected int contentWidth() {
        int width = 24;
        for (int i=0;i<lines.length;i++) width = Math.max(width, text.width(i==0?firstLine:lines[i]));
        return width;
    }
    protected int contentHeight() { return Math.max(1,lines.length)*text.height()+Math.max(0,lines.length-1); }
    private void prepare(boolean preview) {
        var mc = Minecraft.getInstance();
        long now = revision(mc);
        // Text changes at game-tick speed; no per-render inventory or string rebuilding.
        if (player != mc.player || tick != now || previewState != preview) {
            player=mc.player; tick=now; previewState=preview; update(mc,preview);
            firstLine=lines.length==0?"":lines[0];int split=firstLine.indexOf("  ");
            firstLabel=split>0?firstLine.substring(0,split):"";
            firstValue=split>0?firstLine.substring(split+2):"";firstPrefix=firstLabel+" ";
            if(split>0)firstLine=firstPrefix+firstValue;
        }
    }
    public boolean hasContent(boolean preview) { prepare(preview); return lines.length > 0; }
    public HudBounds bounds(int width, int height, boolean preview) {
        text.scale(Neon.config.hud(kind).placement.scale*NeonHud.density());
        prepare(preview);
        var placement=Neon.config.hud(kind).placement;double scale=placement.scale*NeonHud.density();
        text.scale(scale);
        for(int i=0;i<2;i++) {
            scale=Math.min(scale,Math.min((double)width/(contentWidth()+4),(double)height/(contentHeight()+2)));text.scale(scale);
        }
        renderScale=(float)scale;
        int w=Math.min(width,(int)Math.ceil((contentWidth()+4)*scale)),h=Math.min(height,(int)Math.ceil((contentHeight()+2)*scale));
        return new HudBounds((int)Math.round(placement.x*(width-w)),(int)Math.round(placement.y*(height-h)),w,h);
    }
    public void draw(GuiGraphicsExtractor g,int width,int height,boolean preview) {
        var options = Neon.config.hud(kind);
        if (!options.placement.visible || !hasContent(preview)) return;
        var b=renderBounds(width,height,preview);
        chrome.panel(g,b,options.background,options.border);
        g.pose().pushMatrix(); g.pose().translate(b.x(),b.y()); g.pose().scale(renderScale);
        g.pose().translate(-3,-2);
        renderContent(g,preview);
        g.pose().popMatrix();
    }
    public void surface(GuiGraphicsExtractor g,int width,int height,boolean preview) {
        var options=Neon.config.hud(kind);
        if(options.placement.visible&&hasContent(preview))chrome.panel(g,renderBounds(width,height,preview),options.background,options.border);
    }
    private HudBounds renderBounds(int width,int height,boolean preview){
        var b=bounds(width,height,preview);
        return !preview&&kind==HudKind.COORDINATES&&HudChatDock.active()?HudChatDock.place(b):HudChatLift.place(b);
    }
    protected void renderContent(GuiGraphicsExtractor g,boolean preview) {
        var theme=Neon.config.theme;
        boolean shadow=Neon.config.hud(kind).shadow;
        for(int i=0;i<lines.length;i++) {
            int y=3+i*(text.height()+1);
            if(i==0&&!firstLabel.isEmpty()) {
                text.draw(g,firstLabel,5,y,theme.color(ThemeColor.ACCENT),shadow,firstLine);
                text.draw(g,firstValue,5+text.width(firstPrefix),y,theme.color(ThemeColor.TEXT),shadow,firstLine);
            } else text.draw(g,lines[i],5,y,theme.color(i==0?ThemeColor.ACCENT:ThemeColor.TEXT),shadow);
        }
    }
    public void invalidate() { tick=Long.MIN_VALUE; }
}
