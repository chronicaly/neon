package me.xchro.neon.smoke;

import java.nio.file.*;
import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Boot the real mod from damaged files, then boot again from the repaired files. */
final class RecoverySmoke {
    private int ticks,checks;
    private boolean done;
    private void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println("NEON_CHECK "+name);}
    void start(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
        if(done||mc.player==null||++ticks<40)return;
        done=true;
        try{
            Neon.SAVES.flush().join();
            check(Neon.config.zoomFactor==7.125,"startup recovers the precise saved module value");
            check(Neon.config.theme.background==0x62112233&&Neon.config.theme.alphaEnabled&&Neon.config.theme.opacity==.63,"startup preserves the custom palette and alpha");
            check(Neon.config.fpsColors.get(FpsColor.VALUE)==0x12335577,"startup preserves the independent FPS color");
            check(Neon.profiles.names().equals(List.of("Healthy","Recovered")),"startup keeps healthy profiles, recovers damaged entries and leaves deleted entries absent");
            check(Neon.profiles.get("Healthy").zoomFactor==6&&Neon.profiles.get("Recovered").zoomFactor==9,"startup retains healthy edits and the recovered profile value");
            var field=Neon.class.getDeclaredField("store");field.setAccessible(true);var store=(ConfigStore)field.get(null);
            boolean restart=Boolean.getBoolean("neon.smoke.recoveryRestart");
            check(store.recoveryNotice().isEmpty()==restart&&Neon.profiles.recoveryNotice().isEmpty()==restart,"recovery notices appear only on the damaged boot");
            Path config=mc.gameDirectory.toPath().resolve("config");
            check(ConfigStore.encode(new ConfigStore(config.resolve("neon.json")).load()).equals(ConfigStore.encode(Neon.config)),"repaired settings survive a real file reload");
            check(Files.readString(config.resolve("neon.json")).equals(Files.readString(config.resolve("neon.json.bak"))),"primary settings and recovery copy agree");
            check(Files.readString(config.resolve("neon-profiles.json")).equals(Files.readString(config.resolve("neon-profiles.json.bak"))),"primary profiles and recovery copy agree");
            try(var files=Files.list(config)){
                check(files.filter(p->p.getFileName().toString().contains(".invalid-")).count()==2,"both original damaged files remain preserved across restart");
            }
            Files.writeString(mc.gameDirectory.toPath().resolve("neon-verification.txt"),"PASS "+checks+" startup recovery"+(restart?" restart":"")+" checks\n");
        }catch(Throwable failure){
            failure.printStackTrace();try{Files.writeString(mc.gameDirectory.toPath().resolve("neon-verification.txt"),"FAIL "+failure);}catch(Exception ignored){}
        }finally{mc.stop();}
    });}
}
