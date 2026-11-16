package me.xchro.neon.input;

import me.xchro.neon.mixin.ToggleKeyAccessor;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.Minecraft;

/** Release a latched key when its Neon toggle is disabled; never overwrite vanilla options. */
public final class MovementToggles {
    private static boolean sprint,sneak;
    private MovementToggles() {}
    public static void tick(Minecraft mc) {
        boolean next=ClientModule.TOGGLE_SPRINT.active();
        if(sprint&&!next&&!mc.options.toggleSprint().get())((ToggleKeyAccessor)mc.options.keySprint).neon$resetToggle();
        sprint=next;
        next=ClientModule.TOGGLE_SNEAK.active();
        if(sneak&&!next&&!mc.options.toggleCrouch().get())((ToggleKeyAccessor)mc.options.keyShift).neon$resetToggle();
        sneak=next;
    }
}
