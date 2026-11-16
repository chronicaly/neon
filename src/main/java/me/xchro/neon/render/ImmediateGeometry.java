package me.xchro.neon.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Small immediate draws using Minecraft's staged GPU buffers. */
public final class ImmediateGeometry {
    private static StagedVertexBuffer buffer;
    private static StagedVertexBuffer.Draw draw;

    private ImmediateGeometry() {}
    public static void close(){if(buffer!=null){buffer.close();buffer=null;}draw=null;}

    public static VertexConsumer begin(PrimitiveTopology topology, VertexFormat format) {
        if(buffer==null)buffer=new StagedVertexBuffer(() -> "Neon block geometry",4096);
        draw = buffer.appendDraw(format, topology);
        return buffer.getVertexBuilder(draw);
    }

    public static void draw(RenderType type) {
        try {
            buffer.upload();
            var info = buffer.getExecuteInfo(draw);
            if (info != null) type.prepare().drawFromBuffer(info);
        } finally {
            buffer.endFrame();
            draw = null;
        }
    }
}
