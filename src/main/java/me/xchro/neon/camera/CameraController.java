package me.xchro.neon.camera;

import me.xchro.neon.Neon;
import me.xchro.neon.core.*;
import net.minecraft.client.*;

/** Only owns render-camera rotation and temporary perspective, never the player rotation. */
public final class CameraController {
    private final Activation lookKey = new Activation(), zoomKey = new Activation();
    private final Tween distance = new Tween(), returning = new Tween(), magnification = new Tween();
    private Object level, player;
    private CameraType original, owned;
    private boolean looking, zooming;
    private double yaw, pitch, exitYaw, exitPitch, inputFactor = 1;
    private double distanceFactor;
    public CameraController() { magnification.set(1); }
    public boolean ownsCamera() { return original != null; }
    public boolean looking() { return looking; }
    public double distanceFactor() { return distanceFactor; }
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
        boolean nextLook = lookKey.update(safe, Neon.freelook.isDown(), false);
        zooming = zoomKey.update(safe, Neon.zoom.isDown(), false);
        if (nextLook && !looking) {
            if (!ownsCamera()) {
                original = mc.options.getCameraType();
                owned = original.isFirstPerson() ? CameraType.THIRD_PERSON_BACK : original;
                yaw = mc.player.getYRot(); pitch = mc.player.getXRot();
                distance.set(original.isFirstPerson() ? 0 : 1);
                mc.options.setCameraType(owned);
            }
            distance.to(1, now, Neon.config.freelookMillis);
            returning.set(0); looking = true;
        } else if (!nextLook && looking) {
            looking = false; exitYaw = yaw; exitPitch = pitch;
            returning.set(0); returning.to(1, now, Neon.config.freelookMillis);
            distance.to(original.isFirstPerson() ? 0 : 1, now, Neon.config.freelookMillis);
        }
        if (ownsCamera()) {
            distanceFactor = distance.value(now);
            if (!looking) {
                double blend = returning.value(now);
                yaw = Tween.angle(exitYaw, mc.player.getYRot(), blend);
                pitch = exitPitch + (mc.player.getXRot() - exitPitch) * blend;
                if (blend >= 1) releasePerspective(mc, true);
            }
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
        Neon.config.zoomFactor = Math.clamp(Neon.config.zoomFactor + amount * .5, 1, 16);
        Neon.save(); return true;
    }
    public void reset(Minecraft mc, boolean restore) {
        releasePerspective(mc, restore); lookKey.reset(); zoomKey.reset(); zooming = false;
        magnification.set(1); inputFactor = 1;
    }
    private void releasePerspective(Minecraft mc, boolean restore) {
        if (original != null && restore && mc.options.getCameraType() == owned) mc.options.setCameraType(original);
        original = owned = null; looking = false; distanceFactor = 0;
    }
    public boolean hidePlayer(Minecraft mc) {
        return ownsCamera() && mc.player != null
            && mc.player.getBoundingBox().inflate(.15).contains(mc.gameRenderer.mainCamera().position());
    }
}
