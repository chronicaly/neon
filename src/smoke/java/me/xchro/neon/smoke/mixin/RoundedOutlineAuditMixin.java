package me.xchro.neon.smoke.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import me.xchro.neon.core.OutlineTravel;
import me.xchro.neon.smoke.ModuleGridSmoke;
@Pseudo
@Mixin(targets="me.xchro.neon.ui.RoundedUi",remap=false)
public abstract class RoundedOutlineAuditMixin {
    @Inject(method="outline",at=@At("HEAD"))
    private static void outline(GuiGraphicsExtractor g,float x,float y,float w,float h,OutlineTravel travel,int primary,int secondary,float opacity,CallbackInfo ci){
        var p=g.pose();int scale=Minecraft.getInstance().getWindow().getGuiScale();
        int left=Math.round((p.m00()*x+p.m10()*y+p.m20())*scale),top=Math.round((p.m01()*x+p.m11()*y+p.m21())*scale);
        int right=Math.round((p.m00()*(x+w)+p.m10()*(y+h)+p.m20())*scale),bottom=Math.round((p.m01()*(x+w)+p.m11()*(y+h)+p.m21())*scale);
        ModuleGridSmoke.border(left,top,right-left,bottom-top);
    }
}
