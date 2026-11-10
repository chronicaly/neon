package me.xchro.neon;

import me.xchro.neon.core.ClickerActivation;
import me.xchro.neon.config.ConfigStore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClickerActivationTest {
    @Test void toggleSurvivesMenusFocusLossAndUnavailableWorldUntilExplicitStop() {
        var state = new ClickerActivation();
        assertFalse(state.update(true, true, false, true));
        assertTrue(state.update(true, true, true, true));
        for (int tick = 0; tick < 100; tick++) assertTrue(state.update(true, false, tick % 2 == 0, true));
        assertTrue(state.update(true, true, false, true));
        assertTrue(state.update(true, true, false, true));
        assertFalse(state.update(true, true, true, true));
    }
    @Test void typingDoesNotStartAnInactiveSessionOrQueueAToggleOnReturn() {
        var state = new ClickerActivation();
        state.update(true, true, false, true);
        assertFalse(state.update(true, false, true, true));
        assertFalse(state.update(true, true, true, true));
        state.update(true, true, false, true);
        assertTrue(state.update(true, true, true, true));
    }
    @Test void explicitDisableWorksInEveryContextAndDoesNotAutomaticallyReenable() {
        var state = new ClickerActivation();
        state.update(true, true, false, true); state.update(true, true, true, true);
        assertFalse(state.update(false, false, false, true));
        assertFalse(state.update(true, false, false, true));
        assertFalse(state.update(true, true, false, true));
        assertTrue(state.update(true, true, true, true));
    }
    @Test void holdCanContinueInMenusAndStopsWhenReleased() {
        var state = new ClickerActivation();
        state.update(true, true, false, false);
        assertTrue(state.update(true, true, true, false));
        assertTrue(state.update(true, false, true, false));
        assertFalse(state.update(true, false, false, false));
        assertFalse(state.update(true, false, true, false));
    }
    @Test void legacyStopOptionsDoNotSurviveMigrationOrAlterRemainingSettings() {
        var config = ConfigStore.decode("{\"clickerInChat\":false,\"clickerInBackground\":false,\"clickerProtectTools\":true,\"toggleClicker\":true,\"leftIntervalTicks\":7}");
        String encoded = ConfigStore.encode(config);
        assertFalse(encoded.contains("clickerInChat")); assertFalse(encoded.contains("clickerInBackground"));
        assertFalse(encoded.contains("clickerProtectTools"));
        assertTrue(config.toggleClicker); assertEquals(7, config.leftIntervalTicks);
    }
}
