package me.xchro.neon.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Native display-pixel Inter text, independent of menu opacity and Minecraft GUI scale. */
public final class HudText {
    private HudText() {}
    public static void beginFrame() { UiFont.beginHudFrame(); }
    public static void flush() { UiFont.flush(); }
    public interface Batch extends AutoCloseable { @Override void close(); }
    /** Submit a glyph layer together while retaining each glyph's snapped display-pixel origin. */
    public static Batch batch(GuiGraphicsExtractor g){return UiFont.beginHudBatch(g);}
    public static Batch batch(GuiGraphicsExtractor g,int glyphs){return UiFont.beginHudBatch(g,glyphs);}
    public static int width(String text, int pixels) { return UiFont.hudWidth(text, pixels); }
    public static int width(String text,int pixels,boolean bold,boolean italic) { return UiFont.hudWidth(text,pixels,bold,italic); }
    public static float advance(String text,int pixels,boolean bold,boolean italic) { return UiFont.hudAdvance(text,pixels,bold,italic); }
    public static int lineHeight(int pixels){return (int)Math.ceil(pixels*1.2);}
    public static int baseline(String text,int pixels){return UiFont.hudBaseline(text,pixels);}
    public static void draw(GuiGraphicsExtractor g, String text, int x, int y, int color, int pixels, boolean shadow) {
        draw(g,text,x,y,color,pixels,shadow,false,false);
    }
    public static void draw(GuiGraphicsExtractor g,String text,int x,int y,int color,int pixels,boolean shadow,boolean bold,boolean italic) {
        draw(g,text,x,y,color,pixels,shadow,bold,italic,baseline(text,pixels));
    }
    public static void draw(GuiGraphicsExtractor g,String text,int x,int y,int color,int pixels,boolean shadow,boolean bold,boolean italic,int baseline) {
        if(shadow) {
            float physicalScale=Math.abs(g.pose().m11())*net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
            float offset=1/Math.max(.01f,physicalScale);
            g.pose().pushMatrix();g.pose().translate(offset,offset);
            UiFont.drawHud(g,text,x,y,((int)((color>>>24)*.7))<<24,pixels,bold,italic,baseline);g.pose().popMatrix();
            g.guiRenderState.up();
        }
        UiFont.drawHud(g,text,x,y,color,pixels,bold,italic,baseline);
    }
}
