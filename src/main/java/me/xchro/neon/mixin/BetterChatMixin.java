package me.xchro.neon.mixin;

import java.util.List;
import java.util.function.Predicate;
import me.xchro.neon.Neon;
import me.xchro.neon.chat.BetterChat;
import me.xchro.neon.chat.GroupedChat;
import me.xchro.neon.hud.ChatHud;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Unity's display-only duplicate grouping and entry animation, with Neon's receive-only pings. */
@Mixin(ChatComponent.class)
abstract class BetterChatMixin {
    @Final @Shadow private List<GuiMessage> allMessages;
    @Final @Shadow private List<GuiMessage.Line> trimmedMessages;
    @Shadow private Predicate<GuiMessage> visibleMessageFilter;
    @Shadow private int chatScrollbarPos;
    @Shadow private boolean newMessageSinceScroll;
    @Shadow public abstract int getWidth();
    @Shadow private double getScale() { return 1; }
    @Shadow public abstract boolean isChatFocused();
    @Shadow public abstract int getLinesPerPage();
    @Unique private long neon$messageTime;
    @Unique private final GroupedChat neon$groups = new GroupedChat();

    // This pose is shared by drawing and native clickable-style collection.
    @ModifyArg(method="lambda$extractRenderState$0",at=@At(value="INVOKE",target="Lorg/joml/Matrix3x2f;translate(FF)Lorg/joml/Matrix3x2f;"),index=0)
    private static float neon$messageInset(float inset){return Neon.config.betterChat.messageBar?inset:0;}
    @ModifyConstant(method="lambda$extractRenderState$1",constant=@Constant(intValue=-4))
    private static int neon$messageBackgroundInset(int inset){return Neon.config.betterChat.messageBar?inset:0;}
    @Inject(method="getLinesPerPage",at=@At("RETURN"),cancellable=true)
    private void neon$dockedLines(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir){
        if(me.xchro.neon.hud.HudChatDock.active())cir.setReturnValue(ChatHud.visibleLines(cir.getReturnValue()));
    }

    @WrapWithCondition(method="extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",at={
        @At(value="INVOKE",target="Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V",ordinal=2),
        @At(value="INVOKE",target="Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V",ordinal=3)})
    private boolean neon$hideScrollbar(ChatComponent.ChatGraphicsAccess graphics,int x1,int y1,int x2,int y2,int color){return !BetterChat.active()&&!ChatHud.enabled();}
    @Inject(method="extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",at=@At("HEAD"))
    private void neon$position(ChatComponent.ChatGraphicsAccess graphics,int height,int ticks,ChatComponent.DisplayMode mode,CallbackInfo ci){graphics.updatePose(pose->ChatHud.transform(pose,height));}
    @ModifyVariable(method="extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",at=@At("STORE"),ordinal=2)
    private float neon$background(float opacity){return ChatHud.enabled()?0:opacity;}

    @ModifyVariable(method="addMessageToDisplayQueue", at=@At("HEAD"), argsOnly=true)
    private GuiMessage neon$style(GuiMessage message) {
        if (!BetterChat.active()) return message;
        return new GuiMessage(message.addedTime(), BetterChat.display(message.content(), message.source()),
            message.signature(), message.source(), message.tag());
    }
    @WrapWithCondition(method="addMessage", at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V"))
    private boolean neon$deferGroupedDisplay(ChatComponent chat, GuiMessage message) {
        return !BetterChat.active() || !Neon.config.betterChat.duplicates;
    }
    @Inject(method="addMessageToQueue", at=@At("TAIL"))
    private void neon$received(GuiMessage message, CallbackInfo ci) {
        neon$messageTime = System.nanoTime();
        BetterChat.received(message);
        if (BetterChat.active() && Neon.config.betterChat.duplicates) neon$updateGroups(true);
    }
    @Inject(method="clearMessages",at=@At("HEAD"))
    private void neon$clearGroups(boolean history, CallbackInfo ci) { neon$groups.clear(); }
    @Unique private void neon$updateGroups(boolean received) {
        int previousSize = trimmedMessages.size(), previousScroll = chatScrollbarPos;
        var anchor = previousScroll > 0 && previousScroll < previousSize ? trimmedMessages.get(previousScroll) : null;
        var lines = neon$groups.update(allMessages, visibleMessageFilter, net.minecraft.client.Minecraft.getInstance().font,
            (int)Math.floor(getWidth() / getScale()));
        trimmedMessages.clear(); trimmedMessages.addAll(lines);
        if (previousScroll > 0) {
            int index = anchor == null ? -1 : lines.indexOf(anchor);
            chatScrollbarPos = Math.clamp(index >= 0 ? index : previousScroll + lines.size() - previousSize,
                0, Math.max(0, lines.size() - getLinesPerPage()));
            if (received && isChatFocused()) newMessageSinceScroll = true;
        }
    }
    @Inject(method="refreshTrimmedMessages", at=@At("HEAD"), cancellable=true)
    private void neon$duplicates(CallbackInfo ci) {
        // Resize, font/settings changes, filtering and deletion must rebuild affected layout.
        neon$groups.clear();
        if (!BetterChat.active() || !Neon.config.betterChat.duplicates) return;
        neon$updateGroups(false);
        ci.cancel();
    }
    @Inject(method="extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at=@At("HEAD"))
    private void neon$animate(GuiGraphicsExtractor g, Font font, int ticks, int mx, int my, ChatComponent.DisplayMode mode, boolean cursors, CallbackInfo ci) {
        g.pose().pushMatrix();
        if (BetterChat.active() && Neon.config.betterChat.animation && mode == ChatComponent.DisplayMode.BACKGROUND) {
            double progress = Math.clamp((System.nanoTime() - neon$messageTime) / 1e6 / Neon.config.betterChat.animationMillis, 0, 1);
            g.pose().translate(0, (float) (Math.pow(1 - progress, 3) * 9));
        }
        ChatHud.frame(g,ticks,mode,trimmedMessages,chatScrollbarPos,getLinesPerPage());
    }
    @Inject(method="extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at=@At("TAIL"))
    private void neon$animationEnd(GuiGraphicsExtractor g, Font font, int ticks, int mx, int my, ChatComponent.DisplayMode mode, boolean cursors, CallbackInfo ci) { g.pose().popMatrix(); }
}
