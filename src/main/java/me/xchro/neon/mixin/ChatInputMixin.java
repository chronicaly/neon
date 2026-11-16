package me.xchro.neon.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import me.xchro.neon.Neon;
import me.xchro.neon.chat.BetterChat;
import me.xchro.neon.hud.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Unity's input-frame hook leaves native editing, completion and suggestions untouched. */
@Mixin(ChatScreen.class)
abstract class ChatInputMixin {
    @org.spongepowered.asm.mixin.Unique private static final HudChrome NEON_FRAME=new HudChrome();
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V",ordinal=0))
    private void neon$frame(GuiGraphicsExtractor g,int left,int top,int right,int bottom,int color,Operation<Void> original){
        if(!BetterChat.active()||!Neon.config.betterChat.inputFrame){original.call(g,left,top,right,bottom,color);return;}
        int scale=NeonHud.gameScale();g.pose().pushMatrix();g.pose().scale(1f/scale);
        try{NEON_FRAME.panel(g,new HudBounds(left*scale,top*scale,(right-left)*scale,(bottom-top)*scale),true,true);}finally{g.pose().popMatrix();}
    }
}
