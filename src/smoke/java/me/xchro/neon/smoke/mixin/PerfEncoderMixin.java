package me.xchro.neon.smoke.mixin;
import me.xchro.neon.smoke.PerformanceSmoke;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.systems.*;
import java.nio.ByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public abstract class PerfEncoderMixin {
    @Inject(method = "writeToTexture", at = @At("HEAD"))
    private void upload(GpuTexture t, ByteBuffer data, int mip, int layer, int x, int y, int w, int h, CallbackInfo ci) { PerformanceSmoke.upload(w, h, t.getFormat().blockSize()); }
    @Inject(method = "copyTextureToTexture", at = @At("HEAD"))
    private void copy(GpuTexture a, GpuTexture b, int mip, int x, int y, int tx, int ty, int w, int h, CallbackInfo ci) { PerformanceSmoke.copy(w, h); }
    @Inject(method = "createRenderPass", at = @At("HEAD"))
    private void pass(RenderPassDescriptor desc, CallbackInfoReturnable<RenderPassBackend> ci) { PerformanceSmoke.pass(); }
}
