package me.xchro.neon.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Native display-pixel Inter text, independent of menu opacity and Minecraft GUI scale. */
public final class HudText {
    private HudText() {}
    public static void beginFrame() { UiFont.beginHudFrame(); }
    public static void flush() { UiFont.flush(); }
    public static int width(String text, int pixels) { return UiFont.hudWidth(text, pixels); }
    public static int width(String text,int pixels,boolean bold,boolean italic) { return UiFont.hudWidth(text,pixels,bold,italic); }
    public static void draw(GuiGraphicsExtractor g, String text, int x, int y, int color, int pixels, boolean shadow) {
        draw(g,text,x,y,color,pixels,shadow,false,false);
    }
    public static void draw(GuiGraphicsExtractor g,String text,int x,int y,int color,int pixels,boolean shadow,boolean bold,boolean italic) {
        if(shadow) {
            float physicalScale=Math.abs(g.pose().m11())*net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
            float offset=1/Math.max(.01f,physicalScale);
            g.pose().pushMatrix();g.pose().translate(offset,offset);
            UiFont.drawHud(g,text,x,y,((int)((color>>>24)*.7))<<24,pixels,bold,italic);g.pose().popMatrix();
            g.guiRenderState.up();
        }
        UiFont.drawHud(g,text,x,y,color,pixels,bold,italic);
    }
}
