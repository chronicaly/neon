package me.xchro.neon.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemInHandRenderer.class)
public abstract class ViewmodelMixin {
    @WrapMethod(method="submitArmWithItem")
    private void neon$viewmodel(AbstractClientPlayer player,float partial,float pitch,InteractionHand hand,float swing,ItemStack stack,float equip,PoseStack pose,SubmitNodeCollector collector,int light,Operation<Void> original) {
        var options=Neon.config.viewmodel;
        boolean change=ClientModule.VIEWMODEL.active()&&(options.offhand||hand==InteractionHand.MAIN_HAND);
        if(!change) { original.call(player,partial,pitch,hand,swing,stack,equip,pose,collector,light);return; }
        var arm=hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
        pose.pushPose();
        try {
            pose.translate((arm==HumanoidArm.RIGHT?1:-1)*(float)options.horizontal,(float)options.vertical,(float)options.depth);
            float scale=(float)options.scale;pose.scale(scale,scale,scale);
            original.call(player,partial,pitch,hand,swing,stack,equip,pose,collector,light);
        } finally { pose.popPose(); }
    }
}
