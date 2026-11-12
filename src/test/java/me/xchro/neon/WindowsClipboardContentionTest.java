package me.xchro.neon;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;
import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.*;
import me.xchro.neon.screenshot.WindowsImageClipboard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/** Real cross-process contention. No clipboard contents are read, emptied or replaced. */
class WindowsClipboardContentionTest {
    public interface Clipboard extends StdCallLibrary {
        Clipboard INSTANCE=Native.load("user32",Clipboard.class);
        boolean OpenClipboard(Pointer window);
        boolean CloseClipboard();
        int GetClipboardSequenceNumber();
    }
    public static final class Holder {
        public static void main(String[] args)throws Exception {
            HWND window=window();
            if(!Clipboard.INSTANCE.OpenClipboard(window.getPointer()))throw new IllegalStateException("Clipboard already busy");
            try { System.out.println("LOCKED");System.out.flush();System.in.read(); }
            finally { Clipboard.INSTANCE.CloseClipboard();User32.INSTANCE.DestroyWindow(window); }
        }
    }
    private static HWND window(){
        HWND result=User32.INSTANCE.CreateWindowEx(0,"STATIC","Neon clipboard test",0,0,0,0,0,new HWND(Pointer.createConstant(-3L)),null,null,null);
        if(result==null)throw new IllegalStateException("Could not create isolated clipboard window: "+Native.getLastError());return result;
    }
    @Test void busyClipboardRetriesWithoutReplacingUserContents()throws Exception {
        assumeTrue(System.getProperty("os.name").startsWith("Windows"));
        var paths=new java.util.LinkedHashSet<String>();
        for(Class<?> type:new Class<?>[]{Holder.class,Native.class,BaseTSD.class})
            paths.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        HWND window=window();
        var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"-cp",String.join(File.pathSeparator,paths),Holder.class.getName()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try {
            var reader=process.inputReader();
            String ready=CompletableFuture.supplyAsync(()->{try{return reader.readLine();}catch(IOException e){throw new UncheckedIOException(e);}}).get(10,TimeUnit.SECONDS);
            assertEquals("LOCKED",ready,"A separate process must own the clipboard before invoking real production code");
            boolean unexpectedlyOpened=Clipboard.INSTANCE.OpenClipboard(window.getPointer());
            if(unexpectedlyOpened)Clipboard.INSTANCE.CloseClipboard();
            assertFalse(unexpectedlyOpened,"Prove real contention before allowing any production clipboard writer to run");
            assertTrue(process.isAlive(),"clipboard holder must remain alive");
            int sequence=Clipboard.INSTANCE.GetClipboardSequenceNumber();long start=System.nanoTime();
            var failure=assertThrows(IllegalStateException.class,()->WindowsImageClipboard.copy(Pointer.nativeValue(window.getPointer()),1,1,new int[]{0xff123456}));
            assertEquals("Clipboard is busy",failure.getMessage());
            assertTrue(System.nanoTime()-start>=400_000_000L,"busy handling retries instead of failing immediately");
            assertEquals(sequence,Clipboard.INSTANCE.GetClipboardSequenceNumber(),"clipboard remains untouched during contention");
        } finally {
            User32.INSTANCE.DestroyWindow(window);
            process.getOutputStream().close();
            if(!process.waitFor(5,TimeUnit.SECONDS)){process.destroyForcibly();process.waitFor();}
        }
    }
}
