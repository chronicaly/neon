package me.xchro.neon.ui;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import me.xchro.neon.Neon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

/** Whole text runs, shaped once and composited at 1:1 framebuffer resolution. */
final class UiFont {
    private static final int PAGE_SIZE = 1024;
    private static final Font MEDIUM = load("inter-medium.ttf"), SEMIBOLD = load("inter-semibold.ttf");
    private static Font bodyFont=MEDIUM,headingFont=SEMIBOLD;
    private static String family="Inter";
    private static boolean custom;
    private static final Map<Integer, Face> FACES = new HashMap<>();
    private static final Map<Integer, Face> HUD_FACES = new HashMap<>();
    private static float density;
    private static long frame;
    private static boolean warmed;
    private static boolean prewarming;
    private static java.nio.ByteBuffer uploadBuffer;
    private static HudBatch activeBatch;
    private UiFont() {}
    static void warmNative() {
        prewarming = true;
        try { beginFrame(); flush(); } finally { prewarming = false; }
    }
    private static Font load(String name) {
        try (var in = UiFont.class.getResourceAsStream("/assets/neon/font/" + name)) {
            return Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(in))
                .deriveFont(Map.of(TextAttribute.KERNING, TextAttribute.KERNING_ON, TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON));
        } catch (Exception failure) {
            Neon.LOG.error("Could not load Neon UI font {}", name, failure);
            return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
    }
    static void beginFrame() {
        // Never evict an atlas while the current frame still references it.
        ensureDensity();
        frame++;
        for (Face face : FACES.values()) while (face.pages.size() > 3 && !MenuPresentation.retainsTextures()) {
            Page victim = face.pages.stream().min(Comparator.comparingLong(p -> p.lastUsed)).orElseThrow();
            face.runs.values().removeIf(run -> run.page == victim);
            face.pages.remove(victim); victim.texture.close();
        }
        if (!warmed) {
            warmed = true;
            for (String label : new String[]{"Combat", "Visuals", "HUD", "Settings", "Autoclicker", "Freelook", "Zoom", "Motion blur", "Appearance", "Profiles", "Enabled", "Bind", "Advanced", "Hold", "Toggle", "Left", "Right", "Both", "Background", "Outline", "Accent", "Text"}) face(11).run(label);
            for (String label : new String[]{"Armor", "Totems", "FPS", "General", "Scale", "Guides", "Snap"}) face(10).run(label);
            for (String label : new String[]{"N", "E", "O", "Combat", "Visuals", "HUD", "Settings"}) face(13).run(label);
        }
    }
    private static void ensureDensity() {
        ensureFamily();
        float next = prewarming || Minecraft.getInstance().gui.screen() instanceof PixelScreen ? PixelScreen.UI_SCALE
            : Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        if (density != next) { closeFaces(FACES); warmed = false; density = next; }
    }
    private static void ensureFamily(){
        String next=Neon.config.clientFont;
        if(Objects.equals(family,next))return;
        family=next;Font body=FontCatalog.system(next,false),heading=FontCatalog.system(next,true);
        bodyFont=body==null?MEDIUM:body;headingFont=heading==null?SEMIBOLD:heading;custom=body!=null;
        MenuPresentation.invalidateAll();closeFaces(FACES);closeFaces(HUD_FACES);warmed=false;
    }
    private static Face face(int size) {
        ensureDensity();
        // A one-unit optical increase gives 16px body / 15px secondary text at 132.25%.
        return FACES.computeIfAbsent(size, key -> new Face((key == 13 ? headingFont : bodyFont)
            .deriveFont((float) Math.round((key + 1) * density))));
    }
    static int width(String value) { return width(value, 11); }
    static int width(String value, int size) { return (int) Math.ceil(face(size).advance(value) / density); }
    static void draw(GuiGraphicsExtractor g, String value, int x, int y, int color, boolean title) {
        draw(g, value, x, y, color, title ? 13 : 11);
    }
    static void draw(GuiGraphicsExtractor g, String value, int x, int y, int color, int size) {
        draw(g, face(size), value, x, y, Ui.faded(color));
    }
    static void beginHudFrame() {
        ensureFamily();
        frame++;
        // Scale dragging may visit many font sizes. Retire old faces only before extraction.
        while (HUD_FACES.size() > 24) {
            var oldest = HUD_FACES.entrySet().stream().min(Comparator.comparingLong(e -> e.getValue().lastUsed)).orElseThrow();
            oldest.getValue().pages.forEach(page -> page.texture.close()); HUD_FACES.remove(oldest.getKey());
        }
        for (Face face : HUD_FACES.values()) while (face.pages.size() > 3) {
            Page victim = face.pages.stream().min(Comparator.comparingLong(p -> p.lastUsed)).orElseThrow();
            face.runs.values().removeIf(run -> run.page == victim);
            face.pages.remove(victim); victim.texture.close();
        }
    }
    private static Face hudFace(int pixels) {
        return hudFace(pixels,false,false);
    }
    private static Face hudFace(int pixels,boolean bold,boolean italic) {
        ensureFamily();
        int size=Math.clamp(pixels,1,96),style=(bold?Font.BOLD:0)|(italic?Font.ITALIC:0);
        Face face = HUD_FACES.computeIfAbsent(size*4+style, ignored -> new Face(bodyFont.deriveFont(style,(float)size)));
        face.lastUsed = frame; return face;
    }
    static int hudWidth(String text, int pixels) { return (int) Math.ceil(hudFace(pixels).advance(text)); }
    static float hudAdvance(String text,int pixels,boolean bold,boolean italic) { return hudFace(pixels,bold,italic).advance(text); }
    static int hudWidth(String text,int pixels,boolean bold,boolean italic) { return (int)Math.ceil(hudFace(pixels,bold,italic).advance(text)); }
    static int hudBaseline(String text,int pixels) { return hudFace(pixels).baseline(text); }
    static void drawHud(GuiGraphicsExtractor g, String text, int x, int y, int color, int pixels) {
        drawHud(g,text,x,y,color,pixels,false,false);
    }
    static void drawHud(GuiGraphicsExtractor g,String text,int x,int y,int color,int pixels,boolean bold,boolean italic) {
        drawHud(g,text,x,y,color,pixels,bold,italic,hudFace(pixels,bold,italic).baseline(text));
    }
    static void drawHud(GuiGraphicsExtractor g,String text,int x,int y,int color,int pixels,boolean bold,boolean italic,int baseline) {
        draw(g,hudFace(pixels,bold,italic),text,x,y,color,baseline);
    }
    private static void draw(GuiGraphicsExtractor g, Face face, String value, int x, int y, int color) {
        draw(g,face,value,x,y,color,custom?face.menuBaseline:Math.round(face.font.getSize2D()*.82f));
    }
    private static void draw(GuiGraphicsExtractor g, Face face, String value, int x, int y, int color,int baseline) {
        if (value.isBlank()) return;
        Run run = face.run(value);
        if (run == null) return;
        Page page = run.page;
        int guiScale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        if(activeBatch!=null&&activeBatch.graphics==g){
            var matrix=g.pose();
            int px=(int)Math.floor((matrix.m00()*x+matrix.m10()*y+matrix.m20())*guiScale+.5001);
            int py=(int)Math.floor((matrix.m01()*x+matrix.m11()*y+matrix.m21())*guiScale+.5001);
            activeBatch.add(page,run,px,py+baseline,color);return;
        }
        var point = g.pose().transformPosition(x, y, new Vector2f());
        int px = (int) Math.floor(point.x * guiScale + .5001), py = (int) Math.floor(point.y * guiScale + .5001);
        var pose = new Matrix3x2f().translate(px / (float) guiScale, py / (float) guiScale).scale(1f / guiScale);
        int top = baseline + run.top;
        // Resolved antialiasing: one texel maps to one display pixel, with no second resampling.
        var texture = TextureSetup.singleTexture(page.texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        g.guiRenderState.addGuiElement(new BlitRenderState(RenderPipelines.GUI_TEXTURED, texture, pose,
            run.left, top, run.left + run.w, top + run.h,
            run.u / (float) PAGE_SIZE, (run.u + run.w) / (float) PAGE_SIZE,
            run.v / (float) PAGE_SIZE, (run.v + run.h) / (float) PAGE_SIZE, color, g.scissorStack.peek()));
    }
    static HudText.Batch beginHudBatch(GuiGraphicsExtractor graphics){
        return beginHudBatch(graphics,16);
    }
    static HudText.Batch beginHudBatch(GuiGraphicsExtractor graphics,int glyphs){
        if(activeBatch!=null)throw new IllegalStateException("Nested HUD glyph batch");
        return activeBatch=new HudBatch(graphics,glyphs);
    }
    private static final class HudBatch implements HudText.Batch {
        final GuiGraphicsExtractor graphics;
        final int capacity;
        Page page;
        Run[] runs;
        int[] vertices;
        int count,start;
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE;
        HudBatch(GuiGraphicsExtractor graphics,int capacity){this.graphics=graphics;this.capacity=Math.max(1,capacity);}
        void add(Page next,Run run,int x,int y,int color){
            // Preserve style order even when adjacent fonts live in different atlas pages.
            if(page!=null&&page!=next)submit();page=next;
            if(runs==null){runs=new Run[capacity];vertices=new int[capacity*3];}
            else if(count==runs.length){runs=Arrays.copyOf(runs,count*2);vertices=Arrays.copyOf(vertices,count*6);}
            runs[count]=run;vertices[count*3]=x+run.left;vertices[count*3+1]=y+run.top;vertices[count*3+2]=color;count++;
            minX=Math.min(minX,x+run.left);minY=Math.min(minY,y+run.top);
            maxX=Math.max(maxX,x+run.left+run.w);maxY=Math.max(maxY,y+run.top+run.h);
        }
        void submit(){
            if(count==start)return;
            var pose=new Matrix3x2f().scale(1f/Math.max(1,Minecraft.getInstance().getWindow().getGuiScale()));
            var clip=graphics.scissorStack.peek();var bounds=new ScreenRectangle(minX,minY,maxX-minX,maxY-minY).transformMaxBounds(pose);
            if(clip!=null)bounds=bounds.intersection(clip);
            var texture=TextureSetup.singleTexture(page.texture.getTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            if(bounds!=null)graphics.guiRenderState.addGuiElement(new GlyphBatchState(pose,texture,runs,vertices,start,count,clip,bounds));
            // Later spans append beyond this immutable range. Growth copies existing values;
            // submitted states retain their arrays, so atlas switches need no fresh buffers.
            start=count;minX=minY=Integer.MAX_VALUE;maxX=maxY=Integer.MIN_VALUE;
        }
        @Override public void close(){try{submit();}finally{activeBatch=null;}}
    }
    private record GlyphBatchState(Matrix3x2f pose,TextureSetup textureSetup,Run[] runs,int[] verticesData,int start,int count,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline(){return RenderPipelines.GUI_TEXTURED;}
        public void buildVertices(VertexConsumer vertices){
            for(int i=start;i<count;i++){var run=runs[i];int x=verticesData[i*3],y=verticesData[i*3+1],color=verticesData[i*3+2];float left=run.u/(float)PAGE_SIZE,right=(run.u+run.w)/(float)PAGE_SIZE,top=run.v/(float)PAGE_SIZE,bottom=(run.v+run.h)/(float)PAGE_SIZE;
                vertices.addVertexWith2DPose(pose,x,y).setUv(left,top).setColor(color);
                vertices.addVertexWith2DPose(pose,x,y+run.h).setUv(left,bottom).setColor(color);
                vertices.addVertexWith2DPose(pose,x+run.w,y+run.h).setUv(right,bottom).setColor(color);
                vertices.addVertexWith2DPose(pose,x+run.w,y).setUv(right,top).setColor(color);
            }
        }
    }
    private static void closeFaces(Map<Integer, Face> faces) {
        faces.values().forEach(face -> face.pages.forEach(page -> page.texture.close())); faces.clear();
    }
    static void close() {
        closeFaces(FACES); closeFaces(HUD_FACES); warmed = false;
        TextRaster.clearScratch();
        if (uploadBuffer != null) { org.lwjgl.system.MemoryUtil.memFree(uploadBuffer); uploadBuffer = null; }
    }
    static void flush() {
        flush(FACES); flush(HUD_FACES);
    }
    private static void flush(Map<Integer, Face> faces) {
        for (Face face : faces.values()) for (Page page : face.pages) if (page.dirtyX1 > page.dirtyX0) {
            int w = page.dirtyX1 - page.dirtyX0, h = page.dirtyY1 - page.dirtyY0;
            if (uploadBuffer == null) uploadBuffer = org.lwjgl.system.MemoryUtil.memAlloc(PAGE_SIZE * PAGE_SIZE * 4);
            uploadBuffer.clear();
            var source = page.image.getPixelBytes();
            for (int y = page.dirtyY0; y < page.dirtyY1; y++) {
                int offset = (y * PAGE_SIZE + page.dirtyX0) * 4;
                uploadBuffer.put(source.slice(offset, w * 4));
            }
            uploadBuffer.flip();
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(page.texture.getTexture(), uploadBuffer, 0, 0, page.dirtyX0, page.dirtyY0, w, h);
            page.dirtyX0 = page.dirtyY0 = PAGE_SIZE; page.dirtyX1 = page.dirtyY1 = 0;
        }
    }
    private record Run(Page page, int u, int v, int w, int h, int left, int top) {}
    private static final class Page {
        final NativeImage image = new NativeImage(PAGE_SIZE, PAGE_SIZE, true);
        final DynamicTexture texture = new DynamicTexture(() -> "Neon native-resolution client text", image);
        int x = 1, y = 1, row, dirtyX0 = PAGE_SIZE, dirtyY0 = PAGE_SIZE, dirtyX1, dirtyY1;
        long lastUsed;
        boolean fits(int w, int h) {
            if (x + w + 1 >= PAGE_SIZE) { x = 1; y += row + 2; row = 0; }
            return y + h + 1 < PAGE_SIZE;
        }
    }
    private static final class Face {
        final Font font;
        final int menuBaseline;
        long lastUsed;
        final Map<String, Run> runs = new HashMap<>();
        final Map<String, Float> advances = new LinkedHashMap<>(128, .75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, Float> entry) { return size() > 512; }
        };
        final Map<String,Integer> baselines=new LinkedHashMap<>(128,.75f,true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String,Integer> entry){return size()>512;}
        };
        int baseline(String text){return baselines.computeIfAbsent(text,value->TextRaster.centeredBaseline(font,value,(int)Math.ceil(font.getSize2D()*1.2)));}
        final List<Page> pages = new ArrayList<>();
        Face(Font font) { this.font = font;menuBaseline=TextRaster.centeredBaseline(font,"Ag",font.getSize()); }
        float advance(String value) { return advances.computeIfAbsent(value, text -> TextRaster.advance(font, text)); }
        Run run(String value) {
            Run cached = runs.get(value); if (cached != null) { cached.page.lastUsed = frame; return cached; }
            String displayed = value;
            while (advance(displayed) > PAGE_SIZE - 16 && !displayed.isEmpty())
                displayed = displayed.substring(0, displayed.offsetByCodePoints(displayed.length(), -1));
            if (displayed.isBlank()) return null;
            var raster = TextRaster.render(font, displayed); int w = raster.width(), h = raster.height();
            if (w >= PAGE_SIZE - 2 || h >= PAGE_SIZE - 2) return null;
            Page page = pages.isEmpty() ? null : pages.getLast();
            if (page == null || !page.fits(w, h)) {
                page = new Page(); pages.add(page);
            }
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
                page.image.setPixel(page.x + x, page.y + y, raster.pixels()[y * w + x]);
            Run run = new Run(page, page.x, page.y, w, h, raster.left(), raster.top());
            page.dirtyX0 = Math.min(page.dirtyX0, page.x); page.dirtyY0 = Math.min(page.dirtyY0, page.y);
            page.dirtyX1 = Math.max(page.dirtyX1, page.x + w); page.dirtyY1 = Math.max(page.dirtyY1, page.y + h);
            page.lastUsed = frame;
            page.x += w + 2; page.row = Math.max(page.row, h); runs.put(value, run); return run;
        }
    }
}
