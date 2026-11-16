package me.xchro.neon.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.xchro.neon.Neon;
import me.xchro.neon.chat.BetterChat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Hide only the tag's painted bar; native trust data, icons and hover details remain intact. */
@Mixin(targets={"net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess",
    "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"})
abstract class ChatMessageBarMixin {
    @WrapWithCondition(method="handleTag",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private boolean neon$messageBar(GuiGraphicsExtractor graphics,int left,int top,int right,int bottom,int color){
        return !BetterChat.active()||Neon.config.betterChat.messageBar;
    }
}
