package com.gerbarium.regions.client.screen.widget;

import com.gerbarium.regions.client.screen.help.HelpScreen;
import com.gerbarium.regions.client.screen.help.HelpTopic;
import com.gerbarium.regions.client.screen.ScreenTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class HelpButton extends ButtonWidget {
    private final HelpTopic topic;

    public HelpButton(int x, int y, HelpTopic topic) {
        super(x, y, 20, 20, Text.literal("?"), btn -> {}, DEFAULT_NARRATION_SUPPLIER);
        this.topic = topic;
    }

    @Override
    public void onPress() {
        Screen current = MinecraftClient.getInstance().currentScreen;
        MinecraftClient.getInstance().setScreen(new HelpScreen(current, topic));
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        int color = isHovered() ? ScreenTheme.ACCENT_HOVER : ScreenTheme.ACCENT_SECONDARY;
        context.fill(getX(), getY(), getX() + width, getY() + height, color);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), getX() + width / 2, getY() + 6, 0xFFFFFFFF);
    }
}
