package me.xchro.neon.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import me.xchro.neon.render.VisualTime;
import net.minecraft.client.ClientClockManager;
import net.minecraft.world.attribute.*;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.timeline.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnvironmentAttributeSystem.Builder.class)
public abstract class VisualTimeMixin {
    @WrapOperation(method="addTimelineLayerForAttribute",at=@At(value="INVOKE",target="Lnet/minecraft/world/timeline/Timeline;createTrackSampler(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/world/clock/ClockManager;)Lnet/minecraft/world/timeline/AttributeTrackSampler;"))
    private <T> AttributeTrackSampler<T,?> neon$visualClock(Timeline timeline,EnvironmentAttribute<T> attribute,ClockManager clock,Operation<AttributeTrackSampler<T,?>> original) {
        return original.call(timeline,attribute,clock instanceof ClientClockManager&&VisualTime.visual(attribute)?new VisualTime(clock):clock);
    }
}
