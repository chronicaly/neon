package me.xchro.neon.mixin;
import me.xchro.neon.chat.ChatText;
import net.minecraft.client.gui.*;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Unity's rendering collector uses exactly the same pose and glyph advances for pointer hits. */
@Mixin(targets="net.minecraft.client.gui.GuiGraphicsExtractor$RenderingTextCollector")
abstract class ChatTextCollectorMixin {
    @Shadow @Final private GuiGraphicsExtractor this$0;
    @Shadow public abstract void accept(Style style);
    @Inject(method="accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V",at=@At("HEAD"),cancellable=true)
    private void neon$chat(TextAlignment align,int anchor,int y,ActiveTextCollector.Parameters parameters,FormattedCharSequence text,CallbackInfo ci){
        if(!ChatText.uses(text))return;
        float width=ChatText.width(text),left=switch(align){case LEFT->anchor;case CENTER->anchor-width/2;case RIGHT->anchor-width;};
        this$0.pose().pushMatrix();this$0.pose().set(parameters.pose());
        try{ChatText.draw(this$0,text,left,y,parameters.opacity());}finally{this$0.pose().popMatrix();}
        var access=(ChatGraphicsAccessor)this$0;
        var mouse=parameters.pose().invert(new Matrix3x2f()).transformPosition(new Vector2f(access.neon$mouseX(),access.neon$mouseY()));
        var clip=parameters.scissor();
        if(mouse.y>=y&&mouse.y<y+9&&(clip==null||clip.containsPoint(access.neon$mouseX(),access.neon$mouseY()))){Style style=ChatText.at(text,mouse.x-left);if(style!=null)accept(style);}
        ci.cancel();
    }
}
