package me.xchro.neon.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class BiomeHud extends HudCard {
    BiomeHud() { super(HudKind.BIOME); }
    @Override protected void update(Minecraft mc,boolean preview) {
        if(mc.player==null||mc.level==null||!mc.level.hasChunkAt(mc.player.blockPosition())) { lines=new String[]{"BIOME  —"};return; }
        String biome=mc.level.getBiome(mc.player.blockPosition()).unwrapKey().map(key->{
            var id=key.identifier();
            String fallback=id.getPath().replace('_',' ');
            fallback=Character.toUpperCase(fallback.charAt(0))+fallback.substring(1);
            return Component.translatableWithFallback("biome."+id.getNamespace()+"."+id.getPath().replace('/','.'),fallback).getString();
        }).orElse("Unknown");
        lines=new String[]{"BIOME",biome};
    }
}
