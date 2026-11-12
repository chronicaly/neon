package me.xchro.neon.smoke;

import com.mojang.blaze3d.platform.InputConstants;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Inspect every detail page at default/minimum sizes, not only category cards. */
final class PageLayoutSmoke {
    private record View(String category, String module, boolean advanced, boolean bottom, String color) {}
    private final Minecraft mc;
    private final NeonConfig original;
    private final String binding;
    private final List<View> views = new ArrayList<>();
    private int index, age, checks;
    private boolean capturePending, captureDone;
    private int lastTextCoverage;
    private int pickerColors;
    PageLayoutSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks; original = ConfigStore.copy(Neon.config); binding = Neon.autoclicker.saveString();
        mc.options.guiScale().set(1); mc.getWindow().setWindowed(1280, 800); mc.resizeGui(); mc.setLastInputType(InputType.MOUSE);
        Neon.autoclicker.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_DELETE));
        Neon.config.clickerEnabled = true; Neon.config.leftClick = Neon.config.rightClick = true; Neon.config.motionBlurEnabled = true;
        Neon.config.motionBlurStrength = new NeonConfig().motionBlurStrength;
        for (String[] entry : new String[][]{{"Combat", "Autoclicker"}, {"Visuals", "Freelook"}, {"Visuals", "Zoom"}, {"Visuals", "Motion blur"}, {"Visuals", "Totem pops"}, {"Visuals", "Better chat"}, {"Visuals", "Better screenshot"}, {"Settings", "Colors"}, {"Settings", "Appearance"}, {"Settings", "Profiles"}}) {
            views.add(new View(entry[0], entry[1], false, false, ""));
            views.add(new View(entry[0], entry[1], true, true, ""));
        }
        for (String color : new String[]{"Theme", "Background", "Outline", "Accent", "Text", "Cards", "Secondary outline", "Muted text"}) {
            views.add(new View("Settings", "Colors", true, false, color));
            views.add(new View("Settings", "Colors", true, true, color));
        }
        index=Integer.getInteger("neon.smoke.pageIndex",0);setup();
    }
    int checks() { return checks; }
    private void check(boolean ok, String name) { if (!ok) throw new AssertionError("Page " + index + ": " + name); checks++; System.out.println("NEON_CHECK " + name); }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        List<AbstractWidget> result = new ArrayList<>();
        for (var child : parent.children()) { if (child instanceof AbstractWidget w) result.add(w); if (child instanceof ContainerEventHandler c) result.addAll(widgets(c)); }
        return result;
    }
    private Button button(String name) { return widgets(mc.gui.screen()).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(name)).map(w -> (Button) w).findFirst().orElseThrow(); }
    private AbstractScrollArea scroll() { return widgets(mc.gui.screen()).stream().filter(w -> w.getClass().getSimpleName().equals("UiScroll")).map(w -> (AbstractScrollArea) w).findFirst().orElseThrow(); }
    private AbstractScrollArea pageScroll() { return widgets(mc.gui.screen()).stream().filter(w -> w.getClass().getSimpleName().equals("UiScroll")).map(w -> (AbstractScrollArea) w).reduce((a,b)->b).orElseThrow(); }
    private MouseButtonEvent mouse(double x, double y) { return new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)); }
    private void setup() {
        mc.options.guiScale().set(index / (views.size() * 2) + 1); mc.resizeGui();
        try { new me.xchro.neon.config.MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join(); } catch (Exception e) { throw new AssertionError(e); }
        mc.gui.setScreen(Menus.fresh(null));
        var screen = (SettingsScreen) mc.gui.screen();
        if ((index / views.size()) % 2 == 1) {
            var b = screen.menuBounds();
            double x = screen.guiCoordinate(b.right() - 1), y = screen.guiCoordinate(b.bottom() - 1);
            screen.mouseClicked(mouse(x, y), false);
            screen.mouseDragged(mouse(x - screen.guiCoordinate(120), y - screen.guiCoordinate(70)), -120, -70);
            screen.mouseReleased(mouse(x - screen.guiCoordinate(120), y - screen.guiCoordinate(70)));
            check(screen.menuBounds().width() == 320 && screen.menuBounds().height() == 210, "page audit uses minimum resizable window");
        }
        View v = views.get(index % views.size());
        button(v.category).onPress(null); Menus.configure(screen,v.module);
        check(widgets(mc.gui.screen()).stream().noneMatch(w -> w.getMessage().getString().equals("Advanced")), "page exposes its controls without Advanced folds");
        if (!v.color.isEmpty()) button(v.color).onPress(null);
        else pageScroll().setScrollAmount(v.bottom ? pageScroll().maxScrollAmount() : 0);
        age = 0; capturePending = captureDone = false;
    }
    private void geometry() {
        var bounds=((SettingsScreen)mc.gui.screen()).menuBounds(); boolean narrow=(index/views.size())%2==1;
        boolean colorOpen = !views.get(index % views.size()).color.isEmpty();
        int expectedWidth = Math.min(narrow?320:440, ((SettingsScreen)mc.gui.screen()).canvasWidth()-28-300-(colorOpen?me.xchro.neon.core.ExpandingMenu.COLOR_WIDTH:0));
        check(bounds.width()==expectedWidth && bounds.height()==(narrow?210:280),"captured page fits its requested window and attached drawers: "+bounds);
        var scroll = scroll(); var all = widgets((ContainerEventHandler) scroll);
        var settings = button("Settings");
        var editor = button("HUD editor");
        check(editor.getBottom() == bounds.bottom() - 12 && settings.getBottom() + 2 == editor.getY(),
            "Settings and HUD editor stay stacked at the sidebar bottom");
        check(editor.getX() == settings.getX() && editor.getWidth() == settings.getWidth() && editor.getHeight() == settings.getHeight(),
            "bottom sidebar actions have matching geometry");
        check(editor.getRight() < scroll.getX() && editor.getX() >= bounds.left() && Ui.width("HUD editor") + 19 <= editor.getWidth(),
            "HUD icon and complete label fit beside every category viewport");
        check(widgets(mc.gui.screen()).stream().filter(w -> w.getMessage().getString().equals("HUD editor")).count() == 1,
            "every category has exactly one HUD editor action");
        check(scroll.getBottom() == bounds.bottom() - 12, "module grid uses the former footer space");
        check(widgets(mc.gui.screen()).stream().noneMatch(w -> w.getMessage().getString().equals("HUD")), "HUD editor is an action rather than a fourth category");
        boolean visuals = views.get(index % views.size()).category.equals("Visuals");
        var tiles=all.stream().filter(w->w.getClass().getSimpleName().equals("UiModuleTile")).toList();
        check(!tiles.isEmpty()&&tiles.stream().map(AbstractWidget::getWidth).distinct().count()==1,"all module buttons have equal compact widths");
        if(!narrow&&(visuals||views.get(index%views.size()).category.equals("Settings"))) {
            int columns = colorOpen ? Math.max(1,(scroll.getWidth()-14)/(tiles.getFirst().getWidth()+10)) : 2;
            check(tiles.stream().map(AbstractWidget::getX).distinct().count()==columns,"grid wraps to fit beside attached drawers");
        }
        for(var tile:tiles) {
            check(tile.getWidth()<200 && tile.getHeight()==26,"module button keeps compact dimensions");
            check(tile.getX()>=scroll.getX() && tile.getRight()<=scroll.getRight(),"grid button fits viewport");
            var nested=widgets((ContainerEventHandler)tile);
            for(var bind:nested.stream().filter(w->w.getClass().getSimpleName().equals("UiBindButton")).toList())
                check(bind.getX()>tile.getX() && bind.getRight()<tile.getRight(),"bind stays on its module button");
        }
        var detail=widgets(mc.gui.screen()).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).skip(1).findFirst().orElseThrow();
        var controls=widgets((ContainerEventHandler)detail);
        if(views.get(index%views.size()).bottom)check(controls.stream().mapToInt(AbstractWidget::getBottom).max().orElse(detail.getY())<=detail.getBottom(),"last drawer setting is reachable by scrolling");
        for(var w:controls)check(w.getX()>=detail.getX()&&w.getRight()<=detail.getRight(),"drawer control fits: "+w.getMessage().getString()+" ["+w.getX()+","+w.getRight()+"] within ["+detail.getX()+","+detail.getRight()+"]");
        check(controls.stream().noneMatch(w->w.getMessage().getString().startsWith("[")||Set.of("Activation","Toggle","Reset").contains(w.getMessage().getString())),"bind and activation mode remain exclusively on module buttons");
        check(((SettingsScreen)mc.gui.screen()).expandedBounds().right()<=((SettingsScreen)mc.gui.screen()).canvasWidth()-13,"combined outline fits display");
        View v = views.get(index % views.size());
        if (!v.color.isEmpty()) {
            var screen=(SettingsScreen)mc.gui.screen();
            var popup=widgets(screen).stream().filter(w->w.getClass().getSimpleName().equals("ColorPicker")).findFirst().orElseThrow();
            var shade = widgets(screen).stream().filter(w -> w.getMessage().getString().equals("Saturation and brightness")).findFirst().orElseThrow();
            check(shade.getWidth() == shade.getHeight(), "Unity picker uses a square saturation/brightness surface");
            check(popup.getX()>screen.moduleSettingsBounds().right()&&popup.getRight()<screen.expandedBounds().right()&&popup.getBottom()<screen.expandedBounds().bottom(),"color drawer fits inside the shared menu outline");
            check(!all.contains(popup),"picker is outside the settings scroll viewport");
            check(button("Paste").getBottom() <= popup.getBottom() && button("Paste").getY() >= popup.getY(), "picker actions are always visible without scrolling");
            check(shade.getX()>=popup.getX()&&shade.getRight()<=popup.getRight(),"gradient stays inside the color drawer");
            check(all.stream().noneMatch(w -> Set.of("Apply", "Cancel").contains(w.getMessage().getString())), "no color dialog buttons");
        }
    }
    private void capture() {
        capturePending = true;
        var b = ((SettingsScreen) mc.gui.screen()).expandedBounds();
        var popup=widgets(mc.gui.screen()).stream().filter(w->w.getClass().getSimpleName().equals("ColorPicker")).findFirst().orElse(null);
        if(popup!=null){int left=Math.min(b.left(),popup.getX()),top=Math.min(b.top(),popup.getY());b=new net.minecraft.client.gui.navigation.ScreenRectangle(left,top,Math.max(b.right(),popup.getRight())-left,Math.max(b.bottom(),popup.getBottom())-top);}
        final var captureBounds=b;
        View v = views.get(index % views.size());
        String name = String.format("neon-quality-page-%03d-scale%d-%s-%s%s.png", index, mc.getWindow().getGuiScale(), (index / views.size()) % 2 == 0 ? "default" : "narrow", v.module.replace(' ', '-'), v.color.isEmpty() ? v.advanced ? "-advanced" : "" : "-" + v.color.replace(' ', '-') + (v.bottom ? "-bottom" : ""));
        var lastAction = v.bottom ? widgets(mc.gui.screen()).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(v.color.isEmpty() ? "Reset" : "Paste") && w.getY() >= scroll().getY() && w.getBottom() <= scroll().getBottom()).findFirst().orElse(null) : null;
        lastTextCoverage = -1;
        pickerColors=-1;
        var shade=widgets(mc.gui.screen()).stream().filter(w->w.getMessage().getString().equals("Saturation and brightness")).findFirst().orElse(null);
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                int x = Math.round(captureBounds.left() * PixelScreen.UI_SCALE), y = Math.round(captureBounds.top() * PixelScreen.UI_SCALE);
                int width = Math.round(captureBounds.right() * PixelScreen.UI_SCALE) - x, height = Math.round(captureBounds.bottom() * PixelScreen.UI_SCALE) - y;
                BufferedImage crop = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                // The presented framebuffer is opaque; its unused alpha channel is not display coverage.
                for (int cy = 0; cy < height; cy++) for (int cx = 0; cx < width; cx++) crop.setRGB(cx, cy, image.getPixel(x + cx, y + cy) | 0xff000000);
                if (lastAction != null) {
                    lastTextCoverage = 0;
                    for (int py = Math.round(lastAction.getY() * PixelScreen.UI_SCALE); py < Math.round(lastAction.getBottom() * PixelScreen.UI_SCALE); py++)
                        for (int px = Math.round(lastAction.getX() * PixelScreen.UI_SCALE); px < Math.round(lastAction.getRight() * PixelScreen.UI_SCALE); px++) {
                            int pixel = image.getPixel(px, py);
                            if ((pixel >>> 16 & 255) > 70 && (pixel >>> 8 & 255) > 70 && (pixel & 255) > 70) lastTextCoverage++;
                        }
                }
                if(shade!=null){
                    var colors=new HashSet<Integer>();
                    for(int sy=1;sy<12;sy++)for(int sx=1;sx<12;sx++)colors.add(image.getPixel(Math.round((shade.getX()+shade.getWidth()*sx/12f)*PixelScreen.UI_SCALE),Math.round((shade.getY()+shade.getHeight()*sy/12f)*PixelScreen.UI_SCALE))&0xffffff);
                    pickerColors=colors.size();
                }
                ImageIO.write(crop, "png", mc.gameDirectory.toPath().resolve("screenshots").resolve(name).toFile()); captureDone = true;
            } catch (Exception e) { throw new AssertionError(e); }
        });
    }
    boolean run() {
        age++;
        if (age == 8) {
            View v = views.get(index % views.size());
            if (v.bottom && v.color.isEmpty()) pageScroll().setScrollAmount(pageScroll().maxScrollAmount());
            if (v.bottom && !v.color.isEmpty()) pageScroll().setScrollAmount(pageScroll().maxScrollAmount());
        }
        if (age < 10) return false;
        View settledView = views.get(index % views.size());
        if (settledView.bottom && settledView.color.isEmpty()) pageScroll().setScrollAmount(pageScroll().maxScrollAmount());
        try {
            var field=SettingsScreen.class.getDeclaredField("frame");field.setAccessible(true);
            var frame=(me.xchro.neon.core.ExpandingMenu.Frame)field.get(mc.gui.screen());
            if(frame.drawerWidth()<frame.fullDrawerWidth()-.01 || !settledView.color.isEmpty() && frame.colorWidth()<frame.fullColorWidth()-.01) {
                check(age<200,"drawer animation completes before page capture"); return false;
            }
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        if (!capturePending) { geometry(); capture(); return false; }
        if (!captureDone) return false;
        if(pickerColors>=0)check(pickerColors>30,"HSV square is visible in GPU readback ("+pickerColors+" colors)");
        if (lastTextCoverage >= 0) check(lastTextCoverage > 15, "last scrolled action text is visible in GPU readback (" + lastTextCoverage + " pixels)");
        if (++index < views.size() * 6 && System.getProperty("neon.smoke.pageIndex")==null) { setup(); return false; }
        Neon.autoclicker.setKey(InputConstants.getKey(binding)); KeyMapping.resetMapping(); Neon.applyConfig(original);
        mc.gui.setScreen(null);
        try { new me.xchro.neon.config.MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join(); } catch (Exception e) { throw new AssertionError(e); }
        return true;
    }
}
