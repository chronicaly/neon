package me.xchro.neon.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.resources.Identifier;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.Optional;

/** Separable, half-resolution bloom for the selected-block silhouette. */
public final class BlockBloomRenderer {
    private static final RenderPipeline BLUR = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("neon", "pipeline/esp_bloom"))
            .withVertexShader(Identifier.fromNamespaceAndPath("neon", "core/esp_outline"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("neon", "core/esp_bloom"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withSampler("u_Texture")
                    .withSampler("u_EntityDepth")
                    .withSampler("u_MainDepth")
                    .withUniform("PostData", UniformType.UNIFORM_BUFFER)
                    .withUniform("BlurData", UniformType.UNIFORM_BUFFER).build())
            .withDepthStencilState(Optional.empty())
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .build();

    public static final class Buffers {
        private RenderTarget horizontal;
        private RenderTarget vertical;
        private DynamicUniformStorage<Post> posts;
        private DynamicUniformStorage<Blur> blurs;

        private void ensure(int width, int height) {
            int w = Math.max(1, (width + 1) / 2);
            int h = Math.max(1, (height + 1) / 2);
            if (horizontal == null) {
                posts = new DynamicUniformStorage<>("Neon block bloom post", 16, 8);
                blurs = new DynamicUniformStorage<>("Neon block bloom style", 48, 8);
                horizontal = new TextureTarget("Neon block bloom horizontal", w, h, false, GpuFormat.RGBA8_UNORM);
                vertical = new TextureTarget("Neon block bloom vertical", w, h, false, GpuFormat.RGBA8_UNORM);
            } else if (horizontal.width != w || horizontal.height != h) {
                horizontal.resize(w, h);
                vertical.resize(w, h);
            }
        }

        public RenderTarget result() { return vertical; }

        public void close() {
            if (horizontal != null) horizontal.destroyBuffers();
            if (vertical != null) vertical.destroyBuffers();
            if (posts != null) posts.close();
            if (blurs != null) blurs.close();
            posts = null;
            blurs = null;
            horizontal = null;
            vertical = null;
        }
    }

    private record Post(float width, float height) implements DynamicUniformStorage.DynamicUniform {
        public void write(ByteBuffer buffer) { Std140Builder.intoBuffer(buffer).putVec4(width, height, 0, 0); }
    }

    private record Blur(float x, float y, boolean mask, boolean throughWalls, boolean separateObscuredColor,
                        Color obscuredColor) implements DynamicUniformStorage.DynamicUniform {
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putVec4(x, y, mask ? 1 : 0, throughWalls ? 1 : 0)
                    .putVec4(separateObscuredColor ? 1 : 0, 0, 0, 0)
                    .putVec4(obscuredColor.getRed() / 255f, obscuredColor.getGreen() / 255f,
                            obscuredColor.getBlue() / 255f, obscuredColor.getAlpha() / 255f);
        }
    }


    private BlockBloomRenderer() {}

    public static RenderTarget blur(RenderTarget mask, RenderTarget main, GpuBuffer vertices, Buffers buffers,
                                    boolean throughWalls, boolean separateObscuredColor, Color obscuredColor) {
        buffers.ensure(mask.width, mask.height);
        try {
            pass(buffers, mask, mask, main, buffers.horizontal, vertices,
                    new Blur(2f / mask.width, 0, true, throughWalls, separateObscuredColor, obscuredColor));
            pass(buffers, buffers.horizontal, mask, main, buffers.vertical, vertices,
                    new Blur(0, 1f / buffers.horizontal.height, false, throughWalls, false, obscuredColor));
            return buffers.vertical;
        } finally {
            buffers.posts.endFrame();
            buffers.blurs.endFrame();
        }
    }

    private static void pass(Buffers buffers, RenderTarget source, RenderTarget mask, RenderTarget main, RenderTarget output,
                             GpuBuffer vertices, Blur blur) {
        var post = buffers.posts.writeUniform(new Post(source.width, source.height));
        var style = buffers.blurs.writeUniform(blur);
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        try (var pass = encoder.createRenderPass(() -> "Neon block bloom blur", output.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(BLUR);
            pass.setUniform("PostData", post);
            pass.setUniform("BlurData", style);
            pass.bindTexture("u_Texture", source.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.bindTexture("u_EntityDepth", mask.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.bindTexture("u_MainDepth", main.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.setVertexBuffer(0, vertices.slice());
            pass.draw(6, 1, 0, 0);
        }
    }
}
