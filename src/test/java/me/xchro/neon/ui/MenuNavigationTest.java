package me.xchro.neon.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MenuNavigationTest {
    @Test void categoriesAndSectionsRememberIndependentPositions() {
        var memory = new MenuNavigation();
        assertEquals(0, memory.page()); assertEquals("", memory.section(0));
        memory.select(0, "Zoom"); memory.remember("0/Zoom", 42);
        memory.select(2, "Colors"); memory.remember("2/Colors", 180);
        memory.remember("2/Profiles", 30);
        assertEquals(2, memory.page()); assertEquals("Zoom", memory.section(0));
        assertEquals("Colors", memory.section(2)); assertEquals(42, memory.position("0/Zoom"));
        assertEquals(180, memory.position("2/Colors")); assertEquals(30, memory.position("2/Profiles"));
        memory.select(2, ""); assertEquals("", memory.section(2));
        assertEquals(0, new MenuNavigation().position("2/Colors"));
    }
}
