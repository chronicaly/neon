package me.xchro.neon.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import me.xchro.neon.Neon;
import me.xchro.neon.core.FrameHistory;
import me.xchro.neon.ui.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

/** Port of chronicaly's Unity frame accumulation renderer. See NOTICE for provenance. */
public final class MotionBlurRenderer {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.parse("neon:pipeline/motion_blur"))
        .withVertexShader(Identifier.parse("neon:core/motion_blur"))
        .withFragmentShader(Identifier.parse("neon:core/motion_blur"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withBindGroupLayout(BindGroupLayout.builder().withSampler("LiveFrame").withSampler("HistoryFrame")
            .withUniform("BlurData", UniformType.UNIFORM_BUFFER).build())
        .withColorTargetState(ColorTargetState.DEFAULT).withCull(false).build();

    private record Weight(float value) implements DynamicUniformStorage.DynamicUniform {
        @Override public void write(ByteBuffer bytes) { Std140Builder.intoBuffer(bytes).putVec4(value, 0, 0, 0); }
    }

    private static final FrameHistory TIMING = new FrameHistory();
    private static DynamicUniformStorage<Weight> uniforms;
    private static TextureTarget compose, history;
    private static GpuBuffer vertices;
    private static Screen lastScreen;
    private static Object lastLevel;
    private static Vec3 lastCamera;
    private static CameraType lastPerspective;
    private static boolean lastIncludeUi, lastPaused;

    private MotionBlurRenderer() {}

    public static void resetHistory() {
        TIMING.reset();
        lastCamera = null; lastLevel = null; lastScreen = null; lastPerspective = null;
    }

    public static void render(boolean beforeUi) {
        Minecraft mc = Minecraft.getInstance();
        var config = Neon.config;
        if (!Neon.MODULE_BINDINGS.active(me.xchro.neon.module.ClientModule.MOTION_BLUR) || config.motionBlurStrength == 0 || mc.level == null || mc.player == null) {
            close(); return;
        }
        // Exactly one pass per rendered frame, on the selected side of vanilla's UI draw.
        if (beforeUi == config.motionBlurIncludeUi) return;
        Screen screen = mc.gui.screen();
        if (!mc.isWindowActive() || !mc.player.isAlive() || mc.gui.overlay() != null
            || screen instanceof SettingsScreen || screen instanceof HudEditor
            || screen instanceof ProfilesScreen || screen instanceof NumberScreen || screen instanceof ConfirmSheet) {
            resetHistory(); return;
        }
        Vec3 camera = mc.gameRenderer.mainCamera().position();
        CameraType perspective = mc.options.getCameraType();
        if (lastLevel != mc.level || lastScreen != screen || lastPerspective != perspective
            || lastIncludeUi != config.motionBlurIncludeUi || lastPaused != mc.isPaused()
            || lastCamera != null && lastCamera.distanceToSqr(camera) > 4096) resetHistory();

        try {
            var main = mc.gameRenderer.mainRenderTarget();
            ensure(main.width, main.height, main.getColorTexture().getFormat());
            float weight = TIMING.weight(System.nanoTime(), config.motionBlurStrength);
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            if (weight == 0) encoder.clearColorTexture(history.getColorTexture(), new Vector4f(0));
            var data = uniforms.writeUniform(new Weight(weight));
            try (var pass = encoder.createRenderPass(() -> "Neon motion blur", compose.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(PIPELINE);
                pass.setUniform("BlurData", data);
                var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
                pass.bindTexture("LiveFrame", main.getColorTextureView(), sampler);
                pass.bindTexture("HistoryFrame", history.getColorTextureView(), sampler);
                pass.setVertexBuffer(0, vertices.slice());
                pass.draw(6, 1, 0, 0);
            } finally { uniforms.endFrame(); }
            encoder.copyTextureToTexture(compose.getColorTexture(), main.getColorTexture(), 0, 0, 0, 0, 0, main.width, main.height);
            TextureTarget previous = history; history = compose; compose = previous;
            lastLevel = mc.level; lastScreen = screen; lastCamera = camera;
            lastPerspective = perspective; lastIncludeUi = config.motionBlurIncludeUi; lastPaused = mc.isPaused();
        } catch (RuntimeException error) {
            config.motionBlurEnabled = false;
            Neon.LOG.error("Motion blur failed and has been switched off", error);
            close(); Neon.save();
            mc.gui.hud.setOverlayMessage(Component.literal("Neon: motion blur switched off after a rendering error. Check the game log."), false);
        }
    }

    private static void ensure(int width, int height, GpuFormat format) {
        if (compose != null && (compose.width != width || compose.height != height || compose.getColorTexture().getFormat() != format)) close();
        if (compose == null) {
            compose = new TextureTarget("Neon motion blur compose", width, height, false, format);
            history = new TextureTarget("Neon motion blur history", width, height, false, format);
            uniforms = new DynamicUniformStorage<>("Neon motion blur", new Std140SizeCalculator().putVec4().get(), 4);
            ByteBuffer data = ByteBuffer.allocateDirect(72).order(ByteOrder.nativeOrder());
            for (float value : new float[]{-1,-1,0, -1,1,0, 1,1,0, -1,-1,0, 1,1,0, 1,-1,0}) data.putFloat(value);
            data.flip();
            vertices = RenderSystem.getDevice().createBuffer(() -> "Neon motion blur quad", GpuBuffer.USAGE_VERTEX, data);
            TIMING.reset();
        }
    }

    public static void close() {
        resetHistory();
        if (compose != null) { compose.destroyBuffers(); compose = null; }
        if (history != null) { history.destroyBuffers(); history = null; }
        if (vertices != null) { vertices.close(); vertices = null; }
        if (uniforms != null) { uniforms.close(); uniforms = null; }
    }
}
