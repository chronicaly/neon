package me.xchro.neon;

import me.xchro.neon.config.*;
import me.xchro.neon.core.ActionFeedback;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ProfileCompletionTest {
    @TempDir Path directory;
    @Test void confirmationWaitsForDurableWriteAndReportsFailures() throws Exception {
        var fail=new AtomicBoolean();
        try(var writer=new QueuedSave((path,json)->{if(fail.get())throw new java.io.IOException("test write failure");Files.writeString(path,json);},(path,error)->{})){
            var store=new ProfileStore(directory.resolve("profiles.json"),writer);store.initialize(new NeonConfig());writer.flush().get(2,TimeUnit.SECONDS);
            var saved=store.putAsync("Premium",new NeonConfig());assertFalse(saved.isDone());writer.flush().get(2,TimeUnit.SECONDS);saved.get(2,TimeUnit.SECONDS);
            assertTrue(Files.readString(directory.resolve("profiles.json")).contains("Premium"));
            fail.set(true);var failed=store.putAsync("Failure",new NeonConfig());assertThrows(ExecutionException.class,()->writer.flush().get(2,TimeUnit.SECONDS));assertTrue(failed.isCompletedExceptionally());
            fail.set(false);writer.flush().get(2,TimeUnit.SECONDS);
        }
    }
    @Test void acknowledgementHoldsThenFadesWithoutChangingItsLabel() {
        var feedback=new ActionFeedback("Saved",0);assertEquals(1,feedback.amount(900_000_000));
        assertEquals(.5,feedback.amount(960_000_000));assertEquals(0,feedback.amount(1_020_000_000));assertEquals("Saved",feedback.label());
    }
}
