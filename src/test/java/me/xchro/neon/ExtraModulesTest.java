package me.xchro.neon;

import me.xchro.neon.config.*;
import me.xchro.neon.core.BindMode;
import me.xchro.neon.module.ClientModule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExtraModulesTest {
    @Test void visualControlsClampInvalidSavedValues() {
        var c=new NeonConfig();c.fovEffectStrength=Double.NaN;c.hurtCameraStrength=-1;c.viewBobbingStrength=4;
        c.fireLowering=5;c.glintStrength=-1;c.glintSpeed=Double.POSITIVE_INFINITY;c.hitColorStrength=3;
        c.visualTime=-1000;c.weather=null;c.weatherIntensity=9;c.fullbrightStrength=-2;c.blockOutlineWidth=99;
        c.viewmodel.horizontal=Double.NaN;c.viewmodel.vertical=8;c.viewmodel.depth=-9;c.viewmodel.scale=0;
        c.sanitize();
        assertEquals(0,c.fovEffectStrength);assertEquals(0,c.hurtCameraStrength);assertEquals(1,c.viewBobbingStrength);
        assertEquals(.8,c.fireLowering);assertEquals(0,c.glintStrength);assertEquals(.35,c.glintSpeed);assertEquals(1,c.hitColorStrength);
        assertEquals(23000,c.visualTime);assertEquals(NeonConfig.Weather.CLEAR,c.weather);assertEquals(1,c.weatherIntensity);
        assertEquals(0,c.fullbrightStrength);assertEquals(5,c.blockOutlineWidth);
        assertEquals(0,c.viewmodel.horizontal);assertEquals(1,c.viewmodel.vertical);assertEquals(-1,c.viewmodel.depth);assertEquals(.5,c.viewmodel.scale);
    }
    @Test void newModulesAreOptInAndProfilesRetainTheirModes() {
        var c=ConfigStore.decode("{\"theme\":{\"accent\":-123456},\"motionBlurStrength\":73}");
        for(var module:ClientModule.values())if(module.extra()) {
            assertFalse(module.enabled(c));module.enabled(c,true);module.mode(c,BindMode.REVERSE_HOLD);
        }
        var copy=ConfigStore.copy(c);
        for(var module:ClientModule.values())if(module.extra()) {
            assertTrue(module.enabled(copy));assertEquals(BindMode.REVERSE_HOLD,module.mode(copy));
        }
        assertEquals(-123456,copy.theme.accent);assertEquals(73,copy.motionBlurStrength);
    }
    @Test void malformedModuleAndReticleSettingsRecoverWithoutChangingOtherSettings() {
        var c=ConfigStore.decode("{\"modules\":null,\"crosshair\":{\"length\":500,\"gap\":-8,\"thickness\":0,\"color\":123},\"zoomFactor\":7}");
        assertFalse(ClientModule.CROSSHAIR.enabled(c));assertEquals(20,c.crosshair.length);assertEquals(0,c.crosshair.gap);
        assertEquals(.5,c.crosshair.thickness);assertEquals(123,c.crosshair.color);assertEquals(7,c.zoomFactor);
        assertNotNull(ConfigStore.decode("{\"crosshair\":null}").crosshair);
    }
}
