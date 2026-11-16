package me.xchro.neon.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import java.util.Optional;

/** Unity's silhouette composite pipeline, isolated from its entity ESP infrastructure. */
final class BlockHighlightPipeline {
    static final RenderPipeline POST_OUTLINE=RenderPipeline.builder()
        .withLocation(Identifier.parse("neon:pipeline/block_outline"))
        .withVertexShader(Identifier.parse("neon:core/esp_outline"))
        .withFragmentShader(Identifier.parse("neon:core/esp_outline"))
        .withVertexBinding(0,DefaultVertexFormat.POSITION).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withBindGroupLayout(BindGroupLayout.builder().withSampler("u_Texture").withSampler("u_EntityDepth")
            .withSampler("u_MainDepth").withSampler("u_Bloom").withUniform("PostData",UniformType.UNIFORM_BUFFER)
            .withUniform("OutlineData",UniformType.UNIFORM_BUFFER).build())
        .withDepthStencilState(Optional.empty()).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withCull(false).build();
}
