package me.xchro.neon.smoke;

import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.ui.SettingsScreen;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Measures the packaged camera hook, including collision and perspective restoration. */
final class FreelookDistanceSmoke {
    private final Minecraft mc;
    private final NeonConfig original = ConfigStore.copy(Neon.config);
    private final CameraType perspective;
    private final float yaw, pitch;
    private volatile boolean worldReady;
    private int stage, age, checks;
    private double vanillaDistance;

    FreelookDistanceSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks;
        perspective = mc.options.getCameraType(); yaw = mc.player.getYRot(); pitch = mc.player.getXRot();
        Neon.freelook.setDown(false); Neon.zoom.setDown(false); Neon.CAMERA.reset(mc, true);
        Neon.config.freelookEnabled = true; Neon.config.freelookDistance = 16;
        mc.player.setYRot(0); mc.player.setXRot(0);
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        mc.getSingleplayerServer().execute(() -> {
            var world = mc.getSingleplayerServer().overworld();
            for (int x = -1; x <= 1; x++) for (int z = -20; z <= 20; z++) for (int y = 101; y <= 105; y++)
                world.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            worldReady = true;
        });
    }
    int checks() { return checks; }
    private void check(boolean ok, String name) {
        if (!ok) throw new AssertionError("Freelook distance " + stage + ": " + name);
        checks++; System.out.println("NEON_CHECK " + name);
    }
    private double distance() { return mc.gameRenderer.mainCamera().position().distanceTo(mc.player.getEyePosition()); }
    private void expect(double requested) {
        check(Neon.CAMERA.looking() && Math.abs(distance() - requested) < .02, "freelook camera reaches " + requested + " blocks (actual " + distance() + ")");
        check(mc.player.getYRot() == 0 && mc.player.getXRot() == 0, "distance changes preserve player aim");
    }
    boolean run() {
        if (!worldReady || ++age < 10) return false;
        switch (stage) {
            case 0 -> {
                vanillaDistance = distance(); check(Math.abs(vanillaDistance - 4) < .02, "normal third person ignores configured freelook distance");
                Neon.config.freelookDistance = 1; Neon.freelook.setDown(true);
            }
            case 1 -> { expect(1); Neon.config.freelookDistance = 4; }
            case 2 -> { expect(4); Neon.config.freelookDistance = 7.125; }
            case 3 -> { expect(7.125); Neon.config.freelookDistance = 16; }
            case 4 -> { expect(16); Neon.freelook.setDown(false); }
            case 5 -> {
                check(!Neon.CAMERA.ownsCamera() && Math.abs(distance() - vanillaDistance) < .02, "release restores vanilla third-person distance");
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); Neon.freelook.setDown(true);
            }
            case 6 -> { expect(16); check(mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT, "custom distance preserves front view"); Neon.freelook.setDown(false); }
            case 7 -> {
                check(Math.abs(distance() - vanillaDistance) < .02, "front view restores vanilla distance on release");
                mc.options.setCameraType(CameraType.FIRST_PERSON); Neon.freelook.setDown(true);
                worldReady = false;
                mc.getSingleplayerServer().execute(() -> {
                    var world = mc.getSingleplayerServer().overworld();
                    for (int x = -1; x <= 1; x++) for (int y = 101; y <= 105; y++) world.setBlockAndUpdate(new BlockPos(x, y, -1), Blocks.STONE.defaultBlockState());
                    worldReady = true;
                });
            }
            case 8 -> {
                check(Neon.CAMERA.looking() && distance() < 1, "16-block freelook retains nearby wall collision (actual " + distance() + ")");
                mc.gui.setScreen(Menus.fresh(null));
            }
            case 9 -> {
                check(!Neon.CAMERA.ownsCamera() && mc.options.getCameraType() == CameraType.FIRST_PERSON, "opening settings releases custom-distance freelook");
                Neon.freelook.setDown(false); Neon.applyConfig(original); mc.gui.setScreen(null);
                mc.player.setYRot(yaw); mc.player.setXRot(pitch); mc.options.setCameraType(perspective);
                worldReady = false;
                mc.getSingleplayerServer().execute(() -> {
                    var world = mc.getSingleplayerServer().overworld();
                    for (int x = -1; x <= 1; x++) for (int y = 101; y <= 105; y++) world.setBlockAndUpdate(new BlockPos(x, y, -1), Blocks.AIR.defaultBlockState());
                    worldReady = true;
                });
            }
            default -> { return true; }
        }
        stage++; age = 0; return false;
    }
}
