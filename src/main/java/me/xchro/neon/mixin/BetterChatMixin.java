package me.xchro.neon.mixin;

import java.util.List;
import java.util.function.Predicate;
import me.xchro.neon.Neon;
import me.xchro.neon.chat.BetterChat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Unity's display-only duplicate grouping and entry animation, with Neon's receive-only pings. */
@Mixin(ChatComponent.class)
abstract class BetterChatMixin {
    @Final @Shadow private List<GuiMessage> allMessages;
    @Final @Shadow private List<GuiMessage.Line> trimmedMessages;
    @Shadow private Predicate<GuiMessage> visibleMessageFilter;
    @Shadow private void addMessageToDisplayQueue(GuiMessage message) {}
    @Shadow private void refreshTrimmedMessages() {}
    @Unique private long neon$messageTime;

    @ModifyVariable(method="addMessageToDisplayQueue", at=@At("HEAD"), argsOnly=true)
    private GuiMessage neon$style(GuiMessage message) {
        if (!BetterChat.active()) return message;
        return new GuiMessage(message.addedTime(), BetterChat.display(message.content(), message.source()),
            message.signature(), message.source(), message.tag());
    }
    @Inject(method="addMessageToQueue", at=@At("TAIL"))
    private void neon$received(GuiMessage message, CallbackInfo ci) {
        neon$messageTime = System.nanoTime();
        BetterChat.received(message);
        if (BetterChat.active() && Neon.config.betterChat.duplicates) refreshTrimmedMessages();
    }
    @Inject(method="refreshTrimmedMessages", at=@At("HEAD"), cancellable=true)
    private void neon$duplicates(CallbackInfo ci) {
        if (!BetterChat.active() || !Neon.config.betterChat.duplicates) return;
        trimmedMessages.clear(); var chronological = allMessages.reversed();
        for (int i = 0; i < chronological.size();) {
            GuiMessage first = chronological.get(i), latest = first; int count = 1, j = i + 1;
            while (j < chronological.size()) {
                GuiMessage other = chronological.get(j);
                if (other.source() != first.source() || !other.content().equals(first.content())) break;
                latest = other; count++; j++;
            }
            if (visibleMessageFilter.test(latest)) {
                Component text = count > 1 ? latest.content().copy().append(Component.literal(" [x" + count + "]").withColor(0x98abb4)) : latest.content();
                addMessageToDisplayQueue(new GuiMessage(latest.addedTime(), text, latest.signature(), latest.source(), latest.tag()));
            }
            i = j;
        }
        ci.cancel();
    }
    @Inject(method="extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at=@At("HEAD"))
    private void neon$animate(GuiGraphicsExtractor g, Font font, int ticks, int mx, int my, ChatComponent.DisplayMode mode, boolean cursors, CallbackInfo ci) {
        g.pose().pushMatrix();
        if (BetterChat.active() && Neon.config.betterChat.animation && mode == ChatComponent.DisplayMode.BACKGROUND) {
            double progress = Math.clamp((System.nanoTime() - neon$messageTime) / 1e6 / Neon.config.betterChat.animationMillis, 0, 1);
            g.pose().translate(0, (float) (Math.pow(1 - progress, 3) * 9));
        }
    }
    @Inject(method="extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at=@At("TAIL"))
    private void neon$animationEnd(GuiGraphicsExtractor g, Font font, int ticks, int mx, int my, ChatComponent.DisplayMode mode, boolean cursors, CallbackInfo ci) { g.pose().popMatrix(); }
}
