package me.xchro.neon.mixin;
import me.xchro.neon.chat.ChatText;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
abstract class ChatFontMixin {
    @Inject(method="lambda$new$0",at=@At("HEAD"),cancellable=true)
    private void neon$advance(int cp,Style style,CallbackInfoReturnable<Float> cir){if(ChatText.marked(style))cir.setReturnValue(ChatText.advance(cp,style));}
    @Inject(method="prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",at=@At("HEAD"),cancellable=true)
    private void neon$areas(FormattedCharSequence text,float x,float y,int color,boolean shadow,boolean empty,int background,CallbackInfoReturnable<Font.PreparedText> cir){if(ChatText.uses(text))cir.setReturnValue(ChatText.areas(text,x,y));}
}
