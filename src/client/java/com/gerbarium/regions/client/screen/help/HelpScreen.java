package com.gerbarium.regions.client.screen.help;

import com.gerbarium.regions.client.screen.AdaptiveLayout;
import com.gerbarium.regions.client.screen.ScreenLayout;
import com.gerbarium.regions.client.screen.ScreenTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class HelpScreen extends Screen {
    private final Screen parent;
    private final HelpTopic topic;
    private double scrollOffset = 0;
    private int contentHeight = 0;

    public HelpScreen(Screen parent, HelpTopic topic) {
        super(Text.literal("? " + topic.title));
        this.parent = parent;
        this.topic = topic;
    }

    @Override
    protected void init() {
        int panelW = AdaptiveLayout.panelWidth(width, 560);
        int panelX = AdaptiveLayout.panelLeft(width, panelW);
        int panelH = AdaptiveLayout.panelHeight(height);
        int panelY = AdaptiveLayout.panelTop();

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent))
            .dimensions(panelX + panelW / 2 - 60, AdaptiveLayout.footerY(panelY, panelH), 120, 20)
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelW = AdaptiveLayout.panelWidth(width, 560);
        int panelX = AdaptiveLayout.panelLeft(width, panelW);
        int panelH = AdaptiveLayout.panelHeight(height);
        int panelY = AdaptiveLayout.panelTop();

        ScreenTheme.drawPanel(context, panelX, panelY, panelW, panelH);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 18, ScreenTheme.ACCENT_PRIMARY);

        // Content area with clipping
        int contentX = AdaptiveLayout.contentX(panelX);
        int contentY = AdaptiveLayout.contentY(panelY);
        int contentW = AdaptiveLayout.contentWidth(panelW);
        int contentH = AdaptiveLayout.contentHeight(panelH);

        context.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);

        List<HelpRegistry.HelpSection> sections = HelpRegistry.get(topic);
        int y = contentY - (int) scrollOffset;

        for (HelpRegistry.HelpSection section : sections) {
            // Section title
            context.drawTextWithShadow(textRenderer, "\u00A7 " + section.title(), contentX, y, ScreenTheme.ACCENT_PRIMARY);
            y += textRenderer.fontHeight + ScreenTheme.SPACE_SM;

            // Content
            int textHeight = ScreenLayout.wrappedHeight(textRenderer, section.content(), contentW);
            ScreenLayout.drawWrapped(context, textRenderer, section.content(), contentX, y, contentW, ScreenTheme.TEXT_SECONDARY);
            y += textHeight + ScreenTheme.SPACE_XL;
        }

        contentHeight = y - (contentY - (int) scrollOffset);

        context.disableScissor();

        // Scrollbar
        if (contentHeight > contentH) {
            renderScrollbar(context, contentX + contentW - 3, contentY, contentH, contentHeight);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderScrollbar(DrawContext context, int x, int y, int trackHeight, int totalHeight) {
        float ratio = (float) scrollOffset / (totalHeight - trackHeight);
        int thumbHeight = Math.max(20, (trackHeight * trackHeight) / totalHeight);
        int thumbY = y + (int) (ratio * (trackHeight - thumbHeight));
        context.fill(x, y, x + 3, y + trackHeight, 0xFF334155);
        context.fill(x, thumbY, x + 3, thumbY + thumbHeight, ScreenTheme.ACCENT_PRIMARY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int panelW = AdaptiveLayout.panelWidth(width, 560);
        int panelH = AdaptiveLayout.panelHeight(height);
        int contentH = AdaptiveLayout.contentHeight(panelH);
        int maxScroll = Math.max(0, contentHeight - contentH);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - amount * 16));
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
