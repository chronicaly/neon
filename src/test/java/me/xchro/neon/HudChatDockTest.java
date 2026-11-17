package me.xchro.neon;

import me.xchro.neon.hud.*;
import me.xchro.neon.config.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudChatDockTest {
    @Test void overlappingBottomPanelsStackAboveCoordinates(){
        var chat=new HudBounds(0,500,600,300);var coords=new HudBounds(0,776,330,24);
        var result=HudChatDock.stack(chat,coords);
        assertEquals(new HudBounds(0,470,600,300),result);assertEquals(6,coords.y()-result.y()-result.height());
        assertEquals(800,chat.y()+chat.height(),"saved/raw layout is unchanged");
    }
    @Test void separatedChatDoesNotMoveAndOversizedChatFits(){
        var coords=new HudBounds(0,330,320,30);var separate=new HudBounds(0,100,400,140);
        assertEquals(separate,HudChatDock.stack(separate,coords));
        assertEquals(new HudBounds(0,0,400,324),HudChatDock.stack(new HudBounds(0,0,400,360),coords));
    }
    @Test void iconPreferencesPreserveDefaultsAndPersistIndependently(){
        var c=ConfigStore.decode("{}");assertFalse(c.hideStatusEffects);assertFalse(c.potionHideVanillaIcons);
        c.hideStatusEffects=true;var copy=ConfigStore.copy(c);assertTrue(copy.hideStatusEffects);assertFalse(copy.potionHideVanillaIcons);
        c.hideStatusEffects=false;c.potionHideVanillaIcons=true;copy=ConfigStore.copy(c);assertFalse(copy.hideStatusEffects);assertTrue(copy.potionHideVanillaIcons);
    }
}
