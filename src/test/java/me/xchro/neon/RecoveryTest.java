package me.xchro.neon;

import me.xchro.neon.config.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RecoveryTest {
    @TempDir Path directory;
    private Path backup(Path path){return path.resolveSibling(path.getFileName()+".bak");}
    private List<String> archives()throws Exception{
        try(var files=Files.list(directory)){
            var result=new ArrayList<String>();
            for(Path file:files.filter(p->p.getFileName().toString().contains(".invalid-")).toList())result.add(Files.readString(file));
            return result;
        }
    }
    @Test void corruptedSettingsRecoverExactPaletteAlphaHudAndModuleValues()throws Exception{
        Path path=directory.resolve("neon.json");var store=new ConfigStore(path);var config=new NeonConfig();
        config.zoomFactor=7.125;config.theme.alphaEnabled=true;config.theme.background=0x62112233;
        config.theme.opacity=.63;config.armorAnchor=NeonConfig.ArmorAnchor.CUSTOM;config.armor.x=.37;
        config.fpsColors.put(FpsColor.VALUE,0x91335577);config.motionBlurStrength=27;store.save(config);
        String expected=ConfigStore.encode(config);Files.writeString(path,"{damaged");
        assertEquals(expected,ConfigStore.encode(store.load()));assertTrue(store.recoveryNotice().contains("Recovered settings"));
        assertEquals(expected,Files.readString(path));assertEquals(expected,Files.readString(backup(path)));
        assertEquals(List.of("{damaged"),archives());store.load();assertEquals("",store.recoveryNotice());assertEquals(1,archives().size());
    }
    @Test void missingPrimaryRecoversButValidPrimaryAlwaysWins()throws Exception{
        Path path=directory.resolve("neon.json");var store=new ConfigStore(path);var old=new NeonConfig();old.zoomFactor=9;store.save(old);
        Files.delete(path);assertEquals(9,store.load().zoomFactor);assertTrue(archives().isEmpty());
        Files.writeString(path,"{\"zoomFactor\":6}");assertEquals(6,store.load().zoomFactor);assertEquals("",store.recoveryNotice());
    }
    @Test void twoDamagedFilesArePreservedBeforeDefaultsAreUsed()throws Exception{
        Path path=directory.resolve("neon.json");Files.writeString(path,"null");Files.writeString(backup(path),"{broken");
        var store=new ConfigStore(path);assertEquals(4,store.load().zoomFactor);assertTrue(store.recoveryNotice().contains("using defaults"));
        assertEquals(Set.of("null","{broken"),new HashSet<>(archives()));
    }
    @Test void partialProfileRecoveryKeepsHealthyEditsOrderAndIntentionalDeletions()throws Exception{
        Path path=directory.resolve("profiles.json");
        Files.writeString(backup(path),"{\"version\":1,\"profiles\":{\"Healthy\":{\"zoomFactor\":3},\"Damaged\":{\"zoomFactor\":5},\"Deleted\":{\"zoomFactor\":11}}}");
        String broken="{\"version\":1,\"profiles\":{\"Healthy\":{\"zoomFactor\":9},\"Damaged\":{\"zoomFactor\":\"broken\"},\"NewBroken\":null}}";
        Files.writeString(path,broken);var store=new ProfileStore(path);store.initialize(new NeonConfig());
        assertEquals(List.of("Healthy","Damaged"),store.names());assertEquals(9,store.get("Healthy").zoomFactor);assertEquals(5,store.get("Damaged").zoomFactor);
        assertTrue(store.recoveryNotice().contains("1 restored, 1 skipped"));assertEquals(List.of(broken),archives());
        var reopened=new ProfileStore(path);reopened.initialize(new NeonConfig());assertEquals(store.names(),reopened.names());assertEquals("",reopened.recoveryNotice());
    }
    @Test void validEmptyProfileLibraryIsNeverRepopulatedFromBackup()throws Exception{
        Path path=directory.resolve("profiles.json");Files.writeString(path,"{\"profiles\":{}}");
        Files.writeString(backup(path),"{\"profiles\":{\"Deleted\":{}}}");var store=new ProfileStore(path);store.initialize(new NeonConfig());
        assertTrue(store.names().isEmpty());assertTrue(archives().isEmpty());assertEquals("",store.recoveryNotice());
        Files.writeString(backup(path),"{\"profiles\":{}}");Files.writeString(path,"{broken");store.initialize(new NeonConfig());
        assertTrue(store.names().isEmpty());assertTrue(store.recoveryNotice().contains("Recovered profiles"));
    }
    @Test void oneDamagedProfileWithoutBackupDoesNotDiscardHealthyProfiles()throws Exception{
        Path path=directory.resolve("profiles.json");Files.writeString(path,"{\"profiles\":{\"Keep\":{\"zoomFactor\":8},\"Bad\":null,\" \u0001\":{}}}");
        var store=new ProfileStore(path);store.initialize(new NeonConfig());
        assertEquals(List.of("Keep"),store.names());assertEquals(8,store.get("Keep").zoomFactor);assertTrue(store.recoveryNotice().contains("0 restored, 2 skipped"));
    }
    @Test void backupDamageCannotReplaceHealthyPrimaryEntries()throws Exception{
        Path path=directory.resolve("profiles.json");Files.writeString(path,"{\"profiles\":{\"Keep\":{\"zoomFactor\":8},\"Bad\":null}}");
        Files.writeString(backup(path),"{\"profiles\":{\"Keep\":null,\"Bad\":{\"zoomFactor\":6}}}");
        var store=new ProfileStore(path);store.initialize(new NeonConfig());assertEquals(8,store.get("Keep").zoomFactor);assertEquals(6,store.get("Bad").zoomFactor);assertEquals(2,archives().size());
    }
    @Test void acknowledgementWaitsForBothPrimaryAndRecoveryCopy()throws Exception{
        Path path=directory.resolve("neon.json");var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var writer=new QueuedSave((p,s)->{
            if(p.equals(backup(path))){entered.countDown();try{if(!release.await(3,TimeUnit.SECONDS))throw new java.io.IOException("Timed out");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new java.io.IOException(e);}}
            Files.writeString(p,s);
        },(p,e)->{})){
            var saved=new ConfigStore(path).queue(writer,new NeonConfig());var flushed=writer.flush();
            try{assertTrue(entered.await(2,TimeUnit.SECONDS));assertTrue(Files.exists(path));assertFalse(saved.isDone());assertFalse(flushed.isDone());}
            finally{release.countDown();}
            flushed.get(2,TimeUnit.SECONDS);saved.get(2,TimeUnit.SECONDS);assertEquals(Files.readString(path),Files.readString(backup(path)));
        }
    }
    @Test void failedRecoveryWriteRetainsNewestSnapshotAndNeverReportsSuccess()throws Exception{
        Path path=directory.resolve("neon.json");var failBackup=new AtomicBoolean(true);var failures=new AtomicInteger();
        try(var writer=new QueuedSave((p,s)->{if(p.equals(backup(path))&&failBackup.get())throw new java.io.IOException("Locked backup");Files.writeString(p,s);},(p,e)->failures.incrementAndGet())){
            var store=new ConfigStore(path);var config=new NeonConfig();config.zoomFactor=5;var failed=store.queue(writer,config);
            assertThrows(CompletionException.class,()->writer.flush().join());assertTrue(failed.isCompletedExceptionally());assertTrue(writer.hasPending());
            config.zoomFactor=12;var latest=store.queue(writer,config);failBackup.set(false);writer.flush().get(2,TimeUnit.SECONDS);latest.join();
            assertEquals(12,store.load().zoomFactor);assertEquals(12,ConfigStore.decode(Files.readString(backup(path))).zoomFactor);assertEquals(1,failures.get());assertFalse(writer.hasPending());
        }
    }
}
