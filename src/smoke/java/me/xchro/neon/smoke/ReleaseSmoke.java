package me.xchro.neon.smoke;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import me.xchro.neon.Neon;
import net.minecraft.client.Minecraft;

/** Complete installation gate. A focused test or benchmark cannot produce this proof. */
final class ReleaseSmoke {
    private final Minecraft mc;
    private final Path production,verification;
    private final String productionHash,verificationHash;
    private int stage,checks;
    private PremiumSmoke premium;
    private ColorPopupSmoke colors;
    private FpsHudSmoke fps;
    private BetterChatSmoke chat;
    private BetterScreenshotSmoke screenshot;
    private PageLayoutSmoke pages;
    ReleaseSmoke(Minecraft mc,int checks)throws Exception{
        this.mc=mc;this.checks=checks;
        production=Path.of(Neon.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        verification=Path.of(ReleaseSmoke.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        productionHash=hash(production);verificationHash=hash(verification);
        if(!productionHash.equals(System.getProperty("neon.release.production"))||!verificationHash.equals(System.getProperty("neon.release.verification"))||Runtime.version().feature()!=25)
            throw new AssertionError("Release artifact identity or Java version does not match");
        System.out.println("NEON_RELEASE_JAR "+productionHash+" harness="+verificationHash);
    }
    private static String hash(Path file)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
    int checks(){return checks;}
    boolean run()throws Exception{
        switch(stage){
            case 0->{if(premium==null)premium=new PremiumSmoke(mc,checks);if(!premium.run())return false;checks=premium.checks();}
            case 1->{if(colors==null)colors=new ColorPopupSmoke(mc,checks);if(!colors.run())return false;checks=colors.checks();}
            case 2->{if(fps==null)fps=new FpsHudSmoke(mc,checks);if(!fps.run())return false;checks=fps.checks();}
            case 3->{if(chat==null)chat=new BetterChatSmoke(mc,checks);if(!chat.run())return false;checks=chat.checks();}
            case 4->{if(screenshot==null)screenshot=new BetterScreenshotSmoke(mc,checks);if(!screenshot.run())return false;checks=screenshot.checks();}
            case 5->{if(pages==null)pages=new PageLayoutSmoke(mc,checks);if(!pages.run())return false;checks=pages.checks();}
            default->{
                if(!hash(production).equals(productionHash)||!hash(verification).equals(verificationHash))throw new AssertionError("Loaded artifacts changed during verification");
                var proof=new com.google.gson.JsonObject();proof.addProperty("schema",1);proof.addProperty("passed",true);proof.addProperty("suite","release");
                proof.addProperty("minecraft","26.2");proof.addProperty("java",Runtime.version().feature());proof.addProperty("checks",checks);
                proof.addProperty("productionSha256",productionHash);proof.addProperty("verificationSha256",verificationHash);
                Files.writeString(mc.gameDirectory.toPath().resolve("neon-release-proof.json"),proof.toString());return true;
            }
        }
        stage++;return false;
    }
}
