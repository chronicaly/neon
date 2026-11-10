package me.xchro.neon.ui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/** Small line icons, area-resolved once and composited at native display resolution. */
final class UiIcons {
    private static final int SIZE = 18, COUNT = 7, SAMPLE = 4;
    private static DynamicTexture atlas;
    private UiIcons() {}
    static int category(String name) { return switch (name) { case "Combat" -> 0; case "Visuals" -> 1; case "HUD" -> 2; default -> 3; }; }
    private static void path(Graphics2D g, double... points) {
        var p = new Path2D.Double(); p.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) p.lineTo(points[i], points[i + 1]);
        g.draw(p);
    }
    private static void create() {
        var large = new BufferedImage(SIZE * COUNT * SAMPLE, SIZE * SAMPLE, BufferedImage.TYPE_INT_ARGB);
        var g = large.createGraphics(); g.scale(SAMPLE, SAMPLE);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.setStroke(new BasicStroke(1.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int icon = 0; icon < COUNT; icon++) {
            g.translate(icon * SIZE, 0);
            switch (icon) {
                case 0 -> { path(g,3,3,6,4,14,13,13,14,4,6,3,3); path(g,15,3,12,4,4,13); path(g,3,11,7,15); path(g,11,12,15,16); path(g,4,14,2,16); path(g,13,14,15,16); }
                case 1 -> { var p = new Path2D.Double(); p.moveTo(1.5,9); p.curveTo(6,2,12,2,16.5,9); p.curveTo(12,16,6,16,1.5,9); g.draw(p); g.draw(new Ellipse2D.Double(6.5,6.5,5,5)); }
                case 2 -> { g.draw(new Rectangle2D.Double(2,3,14,10)); path(g,9,13,9,16); path(g,5,16,13,16); }
                case 3 -> { var p = new Path2D.Double(); for(int i=0;i<32;i++){ double a=i*Math.PI/16, r=i%4==0||i%4==3?7.3:5.8; double x=9+Math.cos(a)*r,y=9+Math.sin(a)*r; if(i==0)p.moveTo(x,y);else p.lineTo(x,y); }p.closePath();g.draw(p);g.draw(new Ellipse2D.Double(6.5,6.5,5,5)); }
                case 4 -> path(g,5,7,9,11,13,7);
                case 5 -> path(g,5,11,9,7,13,11);
                case 6 -> path(g,7,5,11,9,7,13);
            }
            g.translate(-icon * SIZE, 0);
        }
        g.dispose(); var pixels = new NativeImage(SIZE * COUNT, SIZE, true);
        for(int y=0;y<SIZE;y++) for(int x=0;x<SIZE*COUNT;x++) {
            int alpha=0; for(int sy=0;sy<SAMPLE;sy++)for(int sx=0;sx<SAMPLE;sx++)alpha+=large.getRGB(x*SAMPLE+sx,y*SAMPLE+sy)>>>24;
            pixels.setPixel(x,y,((alpha+8)/16)<<24|0xffffff);
        }
        atlas = new DynamicTexture(() -> "Neon category icons", pixels);
    }
    static void draw(GuiGraphicsExtractor g, int icon, int x, int y, int color) {
        if(atlas==null)create();
        int scale=Math.max(1,Minecraft.getInstance().getWindow().getGuiScale());
        var point=g.pose().transformPosition(x,y,new Vector2f());
        int px=Math.round(point.x*scale),py=Math.round(point.y*scale);
        var pose=new Matrix3x2f().translate(px/(float)scale,py/(float)scale).scale(1f/scale);
        var texture=TextureSetup.singleTexture(atlas.getTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        g.guiRenderState.addGuiElement(new BlitRenderState(RenderPipelines.GUI_TEXTURED,texture,pose,0,0,SIZE,SIZE,
            icon/(float)COUNT,(icon+1)/(float)COUNT,0,1,Ui.faded(color),g.scissorStack.peek()));
    }
    static void close() { if(atlas!=null){atlas.close();atlas=null;} }
}
