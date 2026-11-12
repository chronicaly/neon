package me.xchro.neon.screenshot;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.win32.StdCallLibrary;

/** Uses CF_DIB because Minecraft explicitly starts Java in headless mode. */
public final class WindowsImageClipboard {
    private WindowsImageClipboard() {}
    private interface User extends StdCallLibrary {
        User INSTANCE=Native.load("user32",User.class);
        boolean OpenClipboard(Pointer window);
        boolean EmptyClipboard();
        Pointer SetClipboardData(int format,Pointer memory);
        boolean CloseClipboard();
    }
    private interface Kernel extends StdCallLibrary {
        Kernel INSTANCE=Native.load("kernel32",Kernel.class);
        Pointer GlobalAlloc(int flags,BaseTSD.SIZE_T size);
        Pointer GlobalLock(Pointer handle);
        boolean GlobalUnlock(Pointer handle);
        Pointer GlobalFree(Pointer handle);
    }
    public static void copy(long window,int width,int height,int[] argb)throws InterruptedException {
        if(width<=0||height<=0)throw new IllegalArgumentException("Invalid screenshot dimensions");
        int pixels=Math.multiplyExact(width,height),bytes=Math.multiplyExact(pixels,4);
        if(argb.length!=pixels)throw new IllegalArgumentException("Invalid screenshot pixel count");
        User user=User.INSTANCE;Kernel kernel=Kernel.INSTANCE;
        for(int attempt=0;!user.OpenClipboard(new Pointer(window));attempt++){
            if(attempt==9)throw new IllegalStateException("Clipboard is busy");Thread.sleep(50);
        }
        Pointer handle=null;boolean transferred=false;
        try{
            handle=kernel.GlobalAlloc(2,new BaseTSD.SIZE_T(40L+bytes));
            if(handle==null)throw new IllegalStateException("Could not allocate clipboard image");
            Pointer data=kernel.GlobalLock(handle);
            if(data==null)throw new IllegalStateException("Could not lock clipboard image");
            try{
                writeDib(data,width,height,argb);
            }finally{kernel.GlobalUnlock(handle);}
            if(!user.EmptyClipboard())throw new IllegalStateException("Could not empty clipboard");
            if(user.SetClipboardData(8,handle)==null)throw new IllegalStateException("Could not set clipboard image (Windows error "+Native.getLastError()+")");
            transferred=true; // Windows now owns the movable allocation.
        }finally{
            if(handle!=null&&!transferred)kernel.GlobalFree(handle);
            user.CloseClipboard();
        }
    }
    static void writeDib(Pointer data,int width,int height,int[] argb){
        data.clear(40);data.setInt(0,40);data.setInt(4,width);data.setInt(8,height);
        data.setShort(12,(short)1);data.setShort(14,(short)32);data.setInt(20,Math.multiplyExact(Math.multiplyExact(width,height),4));
        // Positive DIB height means bottom-up rows. ARGB ints are BGRA bytes on Windows.
        for(int y=0;y<height;y++)data.write(40L+(height-y-1L)*width*4,argb,y*width,width);
    }
}
