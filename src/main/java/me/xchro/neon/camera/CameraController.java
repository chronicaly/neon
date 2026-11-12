package me.xchro.neon.camera;

import me.xchro.neon.Neon;
import me.xchro.neon.core.*;
import net.minecraft.client.*;

/** Only owns render-camera rotation and temporary perspective, never the player rotation. */
public final class CameraController {
    private final Activation lookKey = new Activation(), zoomKey = new Activation();
    private final Tween magnification = new Tween();
    private Object level, player;
    private CameraType original, owned;
    private boolean looking, zooming;
    private double yaw, pitch, inputFactor = 1;
    public CameraController() { magnification.set(1); }
    public boolean ownsCamera() { return original != null; }
    public boolean looking() { return looking; }
    public void checkContext(Minecraft mc) {
        boolean contextChanged = level != mc.level || player != mc.player;
        if (contextChanged || !Neon.ready(mc)) reset(mc, true);
        else if (ownsCamera() && mc.options.getCameraType() != owned) reset(mc, false);
        level = mc.level; player = mc.player;
    }
    public void frame(Minecraft mc) {
        if (Neon.freelook == null) return;
        checkContext(mc);
        long now = System.nanoTime();
        boolean safe = Neon.ready(mc);
        boolean nextLook = lookKey.update(safe && Neon.config.freelookEnabled && !Neon.freelook.isUnbound(), Neon.freelook.isDown(), Neon.config.freelookMode);
        zooming = zoomKey.update(safe && Neon.config.zoomEnabled && !Neon.zoom.isUnbound(), Neon.zoom.isDown(), Neon.config.zoomMode());
        if (nextLook && !looking) {
            original = mc.options.getCameraType();
            owned = original.isFirstPerson() ? CameraType.THIRD_PERSON_BACK : original;
            yaw = mc.player.getYRot(); pitch = mc.player.getXRot();
            mc.options.setCameraType(owned);
            looking = true;
        } else if (!nextLook && looking) {
            releasePerspective(mc, true);
        }
        magnification.to(zooming ? Neon.config.zoomFactor : 1, now, Neon.config.zoomMillis);
        inputFactor = 1 / magnification.value(now);
    }
    public void turn(double dx, double dy) {
        yaw += dx * .15;
        pitch = Math.clamp(pitch + dy * .15, -90, 90);
    }
    public float yaw(float vanilla) { return ownsCamera() ? (float) yaw : vanilla; }
    public float pitch(float vanilla) { return ownsCamera() ? (float) pitch : vanilla; }
    public float fov(float vanilla) { return (float) Tween.zoomFov(vanilla, 1 / inputFactor); }
    public double mouse(double input) { return Neon.config.zoomSensitivity ? input * inputFactor : input; }
    public boolean scroll(double amount) {
        if (!zooming || !Neon.ready(Minecraft.getInstance()) || !Neon.config.zoomWheel || amount == 0) return false;
        double next = Math.clamp(Neon.config.zoomFactor + amount * Neon.config.zoomScrollStep, 1, 16);
        if (next == Neon.config.zoomFactor) return true;
        Neon.config.zoomFactor = next;
        Neon.save(); return true;
    }
    public void reset(Minecraft mc, boolean restore) {
        releasePerspective(mc, restore); lookKey.reset(); zoomKey.reset(); zooming = false;
        magnification.set(1); inputFactor = 1;
    }
    private void releasePerspective(Minecraft mc, boolean restore) {
        if (original != null && restore && mc.options.getCameraType() == owned) mc.options.setCameraType(original);
        original = owned = null; looking = false;
    }
    public boolean hidePlayer(Minecraft mc) {
        return ownsCamera() && mc.player != null
            && mc.player.getBoundingBox().inflate(.15).contains(mc.gameRenderer.mainCamera().position());
    }
}
