package me.xchro.neon.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class MenuPresentationMixin {
    @Shadow @Final private GuiRenderState guiRenderState;
    @Inject(method="extractRenderState",at=@At("RETURN"))
    private void neon$exitPresentation(CallbackInfo ci){me.xchro.neon.ui.MenuPresentation.drawExit(guiRenderState);}
}
