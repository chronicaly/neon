package me.xchro.neon.smoke;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.xchro.neon.Neon;
import me.xchro.neon.core.FrameHistory;
import me.xchro.neon.render.MotionBlurRenderer;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.*;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

/** Real GPU readback plus lifecycle/UI checks against the packaged mod. Never shipped. */
public final class MotionBlurSmoke {
    public static MotionBlurSmoke active;
    private final Minecraft mc;
    private int checks, stage, age, frame, verified, beforePasses, afterPasses, waitForWidth;
    private long previousFrame;
    private String fixture;
    private Throwable failure;
    private PixelCheck current, previous;
    private GpuTexture oldTexture;
    private CompletableFuture<Void> reload;

    MotionBlurSmoke(Minecraft mc, int checks) { this.mc = mc; this.checks = checks; active = this; }
    int checks() { return checks; }
    private static Object field(Object target, Class<?> owner, String name) {
        try { Field f = owner.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Object renderer(String name) { return field(null, MotionBlurRenderer.class, name); }
    private static long frameTime() { return (long) field(renderer("TIMING"), FrameHistory.class, "lastFrame"); }
    private static boolean ready() { return (boolean) field(renderer("TIMING"), FrameHistory.class, "ready"); }
    private void check(boolean ok, String message) {
        if (!ok) throw new AssertionError("Motion blur stage " + stage + ": " + message);
        checks++; System.out.println("NEON_CHECK " + message);
    }
    private void next() { stage++; age = 0; }
    private void undo() { mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL)); }
    private void begin(String name) {
        fixture = name; frame = verified = beforePasses = afterPasses = 0;
        previous = current = null; waitForWidth = 0;
    }
    private boolean finished() {
        if (verified < 2) return false;
        check(Neon.config.motionBlurIncludeUi ? afterPasses == 2 && beforePasses == 0 : beforePasses == 2 && afterPasses == 0,
            fixture + ": exactly one compositor pass at the selected UI boundary");
        fixture = null; return true;
    }
    public void before(boolean beforeUi) {
        if (fixture == null || frame >= 2 || beforeUi == Neon.config.motionBlurIncludeUi) return;
        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        if (waitForWidth != 0 && target.width == waitForWidth) return;
        // Two asymmetric patterns detect vertical flips, missing draws and incorrect blending.
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(target.getColorTexture(), frame == 0 ? new Vector4f(1,0,0,1) : new Vector4f(0,0,1,1));
        encoder.clearColorAndDepthTextures(target.getColorTexture(), frame == 0 ? new Vector4f(0,1,0,1) : new Vector4f(1,1,0,1),
            target.getDepthTexture(), 0, 0, target.height / 2, target.width, target.height / 2);
        current = new PixelCheck(fixture + " frame " + frame, previous, frame == 0);
        PixelCheck capture = current;
        Screenshot.takeScreenshot(target, image -> capture.input = pixels(image));
        previousFrame = frameTime();
    }
    public void after(boolean beforeUi) {
        if (fixture == null || frame >= 2 || beforeUi == Neon.config.motionBlurIncludeUi || current == null) return;
        if (beforeUi) beforePasses++; else afterPasses++;
        PixelCheck capture = current;
        long elapsed = frameTime() - previousFrame;
        capture.weight = capture.first || elapsed <= 0 || elapsed > 250_000_000 ? 0 : Math.pow(.75, elapsed * 60.0 / 1e9);
        if (!capture.first && capture.weight <= 0) failure = new AssertionError("GPU fixture needs two consecutive rendered frames without a stall");
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> capture.output = pixels(image));
        previous = capture; frame++; current = null;
    }
    private int[] pixels(NativeImage image) {
        try (image) {
            return new int[]{image.getPixel(image.getWidth()/4, image.getHeight()/4), image.getPixel(image.getWidth()*3/4, image.getHeight()/4),
                image.getPixel(image.getWidth()/4, image.getHeight()*3/4), image.getPixel(image.getWidth()*3/4, image.getHeight()*3/4)};
        }
    }
    private final class PixelCheck {
        final String name;
        final PixelCheck history;
        final boolean first;
        volatile int[] input, output;
        double weight;
        boolean checked;
        PixelCheck(String name, PixelCheck history, boolean first) { this.name = name; this.history = history; this.first = first; }
        void verify() {
            if (history != null) history.verify();
            if (checked || input == null || output == null || history != null && history.output == null) return;
            for (int p = 0; p < input.length; p++) for (int shift : new int[]{0,8,16,24}) {
                int live = input[p] >> shift & 255;
                double expected = first ? live : live * (1-weight) + (history.output[p] >> shift & 255) * weight;
                int actual = output[p] >> shift & 255;
                if (Math.abs(expected - actual) > 3) throw new AssertionError(name + " pixel=" + p + " channel=" + shift + " expected=" + expected + " actual=" + actual + " weight=" + weight);
            }
            check(true, name + (first ? ": fresh image and correct texture orientation" : ": actual GPU pixels match Unity temporal blending"));
            checked = true; verified++;
        }
    }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        List<AbstractWidget> result = new ArrayList<>();
        for (var child : parent.children()) {
            if (child instanceof AbstractWidget widget) result.add(widget);
            if (child instanceof ContainerEventHandler container) result.addAll(widgets(container));
        }
        return result;
    }
    private Button button(String name) { return widgets(mc.gui.screen()).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(name)).map(w -> (Button) w).findFirst().orElseThrow(); }
    private AbstractWidget row(String label) {
        var widgets = widgets(mc.gui.screen());
        int y = widgets.stream().filter(w -> w instanceof StringWidget && w.getMessage().getString().equals(label)).findFirst().orElseThrow().getY();
        return widgets.stream().filter(w -> w instanceof Button && w.getY() == y).findFirst().orElseThrow();
    }
    private void shot(String name) { Screenshot.grab(mc.gameDirectory, "neon-motion-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {}); }

    boolean run() {
        age++;
        if (failure != null) throw new AssertionError(failure);
        if (previous != null) previous.verify();
        if (age > 600) throw new AssertionError("Timed out in motion blur stage " + stage);
        switch (stage) {
            case 0 -> {
                if (age < 5) return false;
                check(!Neon.config.motionBlurEnabled && renderer("compose") == null && renderer("uniforms") == null, "disabled motion blur allocates no GPU buffers");
                Neon.config.motionBlurEnabled = true; Neon.config.motionBlurStrength = 75; Neon.config.motionBlurIncludeUi = false;
                mc.gui.setScreen(null); begin("World only"); next();
            }
            case 1 -> { if (finished()) { Neon.config.motionBlurIncludeUi = true; begin("Include UI changed"); next(); } }
            case 2 -> {
                if (!finished()) return false;
                mc.gui.setScreen(Menus.fresh(null)); button("Visuals").onPress(null); button("Motion blur").onPress(null); next();
            }
            case 3 -> {
                if (age < 4) return false;
                check(!ready(), "Neon settings are excluded from motion blur");
                var slider = widgets(mc.gui.screen()).stream().filter(w -> w instanceof ExactSlider).findFirst().orElseThrow();
                slider.mouseClicked(new MouseButtonEvent(slider.getRight()-8, slider.getY()+8, new MouseButtonInfo(0,0)), false);
                check(InlineInput.slider(mc.gui.screen()) != null, "blur strength edits inline"); next();
            }
            case 4 -> {
                if (age < 4) return false;
                check(!ready(), "inline editing remains excluded from motion blur");
                InlineInput.field(mc.gui.screen()).setValue("83");
                InlineInput.slider(mc.gui.screen()).finishInline(true); check(Neon.config.motionBlurStrength == 83, "motion blur exact strength applies");
                undo(); check(Neon.config.motionBlurStrength == 75, "Undo restores motion blur strength");
                ((Button) row("Blur the UI")).onPress(null); check(!Neon.config.motionBlurIncludeUi, "native Include UI toggle works");
                undo();
                ((Button)widgets(mc.gui.screen()).stream().filter(w->w instanceof UiButton b&&b.hint().equals("Enable Motion blur")).findFirst().orElseThrow()).onPress(null); check(!Neon.config.motionBlurEnabled && widgets(mc.gui.screen()).stream().filter(w -> w instanceof ExactSlider).noneMatch(w -> w.active), "native module toggle disables dependent settings");
                undo(); check(Neon.config.motionBlurEnabled && Neon.config.motionBlurIncludeUi, "Undo restores module activation and UI mode");
                mc.gui.screen().mouseScrolled(mc.gui.screen().width/2.0, mc.gui.screen().height/2.0, 0, -20); next();
            }
            case 5 -> {
                if (age < 5) return false;
                shot("settings"); button("Settings").onPress(null); button("Profiles").onPress(null); next();
            }
            case 6 -> {
                if (age < 4) return false;
                check(!ready(), "profile screen is excluded from motion blur");
                mc.gui.setScreen(new HudEditor(Menus.fresh(null))); next();
            }
            case 7 -> {
                if (age < 4) return false;
                check(!ready(), "HUD editor is excluded from motion blur");
                mc.gui.setScreen(null); begin("Return from settings"); next();
            }
            case 8 -> { if (finished()) { LiveSmoke.focused = false; next(); } }
            case 9 -> {
                if (age < 4) return false;
                check(!ready(), "focus loss clears motion history"); LiveSmoke.focused = true; begin("Focus restored"); next();
            }
            case 10 -> { if (finished()) { mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); begin("Perspective changed"); next(); } }
            case 11 -> {
                if (!finished()) return false;
                oldTexture = ((RenderTarget) renderer("history")).getColorTexture();
                begin("Framebuffer resized"); waitForWidth = mc.gameRenderer.mainRenderTarget().width;
                mc.getWindow().setWindowed(waitForWidth == 900 ? 854 : 900, 520); next();
            }
            case 12 -> {
                if (!finished()) return false;
                check(oldTexture.isClosed(), "resize releases old history textures");
                Neon.config.motionBlurStrength = 0; oldTexture = ((RenderTarget) renderer("history")).getColorTexture(); next();
            }
            case 13 -> {
                if (age < 4) return false;
                check(oldTexture.isClosed() && renderer("compose") == null && renderer("uniforms") == null, "zero strength frees textures and uniform buffers");
                Neon.config.motionBlurStrength = 75; begin("Reenabled after zero"); next();
            }
            case 14 -> {
                if (!finished()) return false;
                Neon.config.motionBlurEnabled = false; oldTexture = ((RenderTarget) renderer("history")).getColorTexture(); next();
            }
            case 15 -> {
                if (age < 4) return false;
                check(oldTexture.isClosed() && renderer("vertices") == null, "disabling releases GPU resources");
                Neon.config.motionBlurEnabled = true; begin("Module reenabled"); next();
            }
            case 16 -> {
                if (!finished()) return false;
                reload = mc.reloadResourcePacks(); next();
            }
            case 17 -> {
                if (!reload.isDone() || mc.gui.overlay() != null) return false;
                reload.join(); check(Neon.config.motionBlurEnabled, "resource reload leaves motion blur enabled");
                MotionBlurRenderer.resetHistory(); begin("After resource reload"); next();
            }
            case 18 -> {
                if (!finished()) return false;
                mc.options.setCameraType(CameraType.FIRST_PERSON); Neon.zoom.setDown(true); Neon.freelook.setDown(true); next();
            }
            case 19 -> {
                mc.player.setYRot(mc.player.getYRot()+8); Neon.CAMERA.turn(20, 0);
                if (age < 20) return false;
                check(ready() && Neon.config.motionBlurEnabled && Neon.CAMERA.looking() && Neon.CAMERA.fov(90) < 40,
                    "motion blur renders alongside freelook and zoom");
                shot("freelook-zoom"); Neon.zoom.setDown(false); Neon.freelook.setDown(false); next();
            }
            case 20 -> {
                mc.player.setYRot(mc.player.getYRot()+12);
                if (age < 15) return false;
                shot("gameplay"); oldTexture = ((RenderTarget) renderer("history")).getColorTexture();
                mc.disconnect(new TitleScreen(), false); next();
            }
            case 21 -> {
                if (age < 6) return false;
                check(mc.level == null && renderer("compose") == null && renderer("uniforms") == null && oldTexture.isClosed(),
                    "disconnect frees all motion blur GPU resources");
                active = null; return true;
            }
        }
        return false;
    }
}
