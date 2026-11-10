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
    PageLayoutSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks; original = ConfigStore.copy(Neon.config); binding = Neon.autoclicker.saveString();
        mc.options.guiScale().set(1); mc.getWindow().setWindowed(1280, 800); mc.resizeGui(); mc.setLastInputType(InputType.MOUSE);
        Neon.autoclicker.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_DELETE));
        Neon.config.clickerEnabled = true; Neon.config.leftClick = Neon.config.rightClick = true; Neon.config.motionBlurEnabled = true;
        for (String[] entry : new String[][]{{"Combat", "Autoclicker"}, {"Visuals", "Freelook"}, {"Visuals", "Zoom"}, {"Visuals", "Motion blur"}, {"Settings", "Appearance"}, {"Settings", "Profiles"}}) {
            views.add(new View(entry[0], entry[1], false, false, ""));
            views.add(new View(entry[0], entry[1], true, true, ""));
        }
        for (String color : new String[]{"Background", "Outline", "Accent", "Text", "Cards", "Secondary outline", "Muted text"}) {
            views.add(new View("Settings", "Appearance", true, false, color));
            views.add(new View("Settings", "Appearance", true, true, color));
        }
        setup();
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
    private MouseButtonEvent mouse(double x, double y) { return new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)); }
    private void setup() {
        mc.options.guiScale().set(index / (views.size() * 2) + 1); mc.resizeGui();
        try { new me.xchro.neon.config.MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join(); } catch (Exception e) { throw new AssertionError(e); }
        mc.gui.setScreen(new SettingsScreen(null));
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
        button(v.category).onPress(null); button(v.module).onPress(null);
        check(widgets(mc.gui.screen()).stream().noneMatch(w -> w.getMessage().getString().equals("Advanced")), "page exposes its controls without Advanced folds");
        if (!v.color.isEmpty()) button(v.color).onPress(null);
        else scroll().setScrollAmount(v.bottom ? scroll().maxScrollAmount() : 0);
        age = 0; capturePending = captureDone = false;
    }
    private void geometry() {
        var bounds=((SettingsScreen)mc.gui.screen()).menuBounds(); boolean narrow=(index/views.size())%2==1;
        check(bounds.width()==(narrow?320:440) && bounds.height()==(narrow?210:280),"captured page retains its requested window size: "+bounds);
        var scroll = scroll(); var all = widgets((ContainerEventHandler) scroll);
        for (var w : all) {
            check(w.getWidth() > 0 && w.getX() >= scroll.getX() && w.getRight() <= scroll.getRight() - 8,
                w.getClass().getSimpleName() + " " + w.getMessage().getString() + " fits inside control column");
            if (w instanceof StringWidget) check(Ui.width(w.getMessage().getString()) <= w.getWidth(), "setting label is complete: " + w.getMessage().getString());
            if (w instanceof UiButton) {
                try {
                    var kindField = UiButton.class.getDeclaredField("kind"); kindField.setAccessible(true);
                    var smallField = UiButton.class.getDeclaredField("small"); smallField.setAccessible(true);
                    var kind = (UiButton.Kind) kindField.get(w);
                    if (kind == UiButton.Kind.SWITCH || kind == UiButton.Kind.SWATCH || kind == UiButton.Kind.MODULE_TOGGLE) continue;
                    int size = kind == UiButton.Kind.NAV ? 11 : smallField.getBoolean(w) || kind == UiButton.Kind.SEGMENT || w.getHeight() <= 24 ? 10 : 11;
                    int inset = kind == UiButton.Kind.KEY ? 2 : kind == UiButton.Kind.SEGMENT || w.getHeight() <= 24 ? 6 : 8;
                    int available = w.getWidth() - (kind == UiButton.Kind.CARD ? 13 : inset * 2);
                    if(kind==UiButton.Kind.MODULE)available=w.getWidth()-2;
                    if(kind==UiButton.Kind.NAV)available=w.getWidth()-26;
                    if(kind==UiButton.Kind.DISCLOSURE){available=w.getWidth()-19;size=11;}
                    if(w.getMessage().getString().isEmpty())continue;
                    var measure = Class.forName("me.xchro.neon.ui.UiFont").getDeclaredMethod("width", String.class, int.class); measure.setAccessible(true);
                    var wrapped = UiButton.class.getDeclaredField("wrapped"); wrapped.setAccessible(true);
                    if(wrapped.getBoolean(w)) {
                        var textLines=UiButton.class.getDeclaredField("textLines"); textLines.setAccessible(true);
                        @SuppressWarnings("unchecked") var lines=(List<String>)textLines.get(w);
                        check(String.join("",lines).replace(" ","").equals(w.getMessage().getString().replace(" ","")),"wrapped profile preserves its complete name");
                        for(String line:lines)check((int)measure.invoke(null,line,11)<=w.getWidth()-18,"wrapped profile line fits its column");
                        continue;
                    }
                    check((int) measure.invoke(null, w.getMessage().getString(), size) <= available, "control text fits without ellipsis: " + w.getMessage().getString());
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            }
        }
        View v = views.get(index % views.size());
        if (v.bottom && v.color.isEmpty()) {
            int last = all.stream().mapToInt(AbstractWidget::getBottom).max().orElse(scroll.getY());
            check(last <= scroll.getBottom(), "bottom of " + v.module + " is reachable (last=" + last + ", viewport=" + scroll.getBottom() + ", scroll=" + scroll.scrollAmount() + "/" + scroll.maxScrollAmount() + ")");
        }
        if (!v.color.isEmpty()) {
            var shade = all.stream().filter(w -> w.getMessage().getString().equals("Saturation and brightness")).findFirst().orElseThrow();
            check(shade.getWidth() == shade.getHeight(), "Unity picker uses a square saturation/brightness surface");
            if (v.bottom) check(button("Paste").getBottom() <= scroll.getBottom() && button("Paste").getY() >= scroll.getY(), "bottom picker actions remain reachable without a scrollbar");
            else check(button(v.color).getY() >= scroll.getY() && shade.getY() >= scroll.getY(), "opening reveals the color name and gradient: " + v.color);
            check(all.stream().noneMatch(w -> Set.of("Apply", "Cancel").contains(w.getMessage().getString())), "no color dialog buttons");
        }
    }
    private void capture() {
        capturePending = true;
        var b = ((SettingsScreen) mc.gui.screen()).menuBounds();
        View v = views.get(index % views.size());
        String name = String.format("neon-quality-page-%03d-scale%d-%s-%s%s.png", index, mc.getWindow().getGuiScale(), (index / views.size()) % 2 == 0 ? "default" : "narrow", v.module.replace(' ', '-'), v.color.isEmpty() ? v.advanced ? "-advanced" : "" : "-" + v.color.replace(' ', '-') + (v.bottom ? "-bottom" : ""));
        var lastAction = v.bottom ? widgets(mc.gui.screen()).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(v.color.isEmpty() ? "Reset" : "Paste")).findFirst().orElse(null) : null;
        lastTextCoverage = -1;
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                int x = Math.round(b.left() * PixelScreen.UI_SCALE), y = Math.round(b.top() * PixelScreen.UI_SCALE);
                int width = Math.round(b.right() * PixelScreen.UI_SCALE) - x, height = Math.round(b.bottom() * PixelScreen.UI_SCALE) - y;
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
                ImageIO.write(crop, "png", mc.gameDirectory.toPath().resolve("screenshots").resolve(name).toFile()); captureDone = true;
            } catch (Exception e) { throw new AssertionError(e); }
        });
    }
    boolean run() {
        age++;
        if (age == 8) {
            View v = views.get(index % views.size());
            if (v.bottom && !v.color.isEmpty()) {
                var scroll = scroll();
                mc.gui.screen().mouseScrolled(((SettingsScreen)mc.gui.screen()).guiCoordinate(scroll.getX()+10),((SettingsScreen)mc.gui.screen()).guiCoordinate(scroll.getY()+10),0,0);
                scroll.setScrollAmount(scroll.scrollAmount()+button("Paste").getBottom()-scroll.getBottom());
            }
        }
        if (age < 10) return false;
        if (!capturePending) { geometry(); capture(); return false; }
        if (!captureDone) return false;
        if (lastTextCoverage >= 0) check(lastTextCoverage > 15, "last scrolled action text is visible in GPU readback (" + lastTextCoverage + " pixels)");
        if (++index < views.size() * 6) { setup(); return false; }
        Neon.autoclicker.setKey(InputConstants.getKey(binding)); KeyMapping.resetMapping(); Neon.applyConfig(original);
        mc.gui.setScreen(null);
        try { new me.xchro.neon.config.MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join(); } catch (Exception e) { throw new AssertionError(e); }
        return true;
    }
}
