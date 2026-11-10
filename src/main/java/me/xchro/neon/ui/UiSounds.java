package me.xchro.neon.ui;

import java.io.BufferedInputStream;
import java.util.concurrent.*;
import javax.sound.sampled.*;
import me.xchro.neon.Neon;

/** Neon's short original cue uses its own mixer line and volume, never Minecraft's sound settings. */
public final class UiSounds {
    private static final ExecutorService AUDIO=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Neon UI audio");t.setDaemon(true);return t;});
    private static Clip clip;
    private static boolean unavailable;
    private static long last;
    private UiSounds(){}
    private static void prepare(){
        if(clip!=null||unavailable)return;
        try(var data=new BufferedInputStream(UiSounds.class.getResourceAsStream("/assets/neon/sounds/ui/enable.wav"));var audio=AudioSystem.getAudioInputStream(data)){
            clip=AudioSystem.getClip();clip.open(audio);
        }catch(Exception failure){unavailable=true;if(clip!=null){clip.close();clip=null;}Neon.LOG.warn("Neon UI audio device unavailable",failure);}
    }
    public static void warm(){AUDIO.execute(UiSounds::prepare);}
    public static void moduleEnabled(){
        double volume=Neon.config.theme.soundVolume;long now=System.nanoTime();
        if(volume<=0||now-last<35_000_000L)return;last=now;play(volume);
    }
    private static void play(double volume){
        AUDIO.execute(()->{
            prepare();if(clip==null)return;clip.stop();clip.setFramePosition(0);
            if(clip.isControlSupported(FloatControl.Type.MASTER_GAIN)){
                var gain=(FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(Math.clamp((float)(20*Math.log10(Math.max(.001,volume))),gain.getMinimum(),gain.getMaximum()));
            }
            clip.start();
        });
    }
    public static void close(){if(!AUDIO.isShutdown()){AUDIO.execute(()->{if(clip!=null){clip.close();clip=null;}});AUDIO.shutdown();}}
}
