package me.xchro.neon.ui;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public final class ConfirmSheet extends PixelScreen {
    private final Screen parent;
    private final String body;
    private final Consumer<Boolean> answer;
    private int x, y, w;
    ConfirmSheet(Screen parent, String title, String body, Consumer<Boolean> answer) {
        super(Ui.label(title)); this.parent = parent; this.body = body; this.answer = answer;
    }
    @Override protected void init() {
        w = Math.min(320, canvasWidth() - 32); x = (canvasWidth() - w) / 2; y = (canvasHeight() - 132) / 2;
        int bw = (w - 40) / 2;
        addRenderableWidget(new UiButton("No", x + 16, y + 91, bw, 26, () -> answer.accept(false)));
        addRenderableWidget(new UiButton("Yes", x + bw + 24, y + 91, bw, 26, UiButton.Kind.PRIMARY, () -> answer.accept(true)));
    }
    @Override protected void pixelBackground(GuiGraphicsExtractor g, int mx, int my, float delta) { Ui.background(g, canvasWidth(), canvasHeight()); }
    @Override protected void renderPixels(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Ui.window(g, x, y, w, 132);
        Ui.text(g, Ui.fit(title.getString(), w - 36), x + 18, y + 20, Ui.TEXT);
        int lineY = y + 44; String line = "";
        for (String word : body.split(" ")) {
            if (!line.isEmpty() && Ui.width(line + " " + word) > w - 36) { Ui.text(g, line, x + 18, lineY, Ui.MUTED); line = ""; lineY += 13; }
            line = line.isEmpty() ? word : line + " " + word;
        }
        Ui.text(g, line, x + 18, lineY, Ui.MUTED);
        renderWidgets(g, mx, my, delta);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
