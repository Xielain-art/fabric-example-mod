package com.gerbarium.regions.client.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class ScreenTheme {
    private ScreenTheme() {}

    // Colors
    public static final int BG_PANEL = 0xCC1A1A2E;
    public static final int BORDER_PANEL = 0xFF334155;
    public static final int ACCENT_PRIMARY = 0xFF4ADE80;
    public static final int ACCENT_SECONDARY = 0xFF3B82F6;
    public static final int ACCENT_HOVER = 0xFF60A5FA;
    public static final int TEXT_PRIMARY = 0xFFE2E8F0;
    public static final int TEXT_SECONDARY = 0xFF94A3B8;
    public static final int TEXT_MUTED = 0xFF64748B;
    public static final int WARNING = 0xFFF59E0B;
    public static final int ERROR = 0xFFEF4444;
    public static final int SUCCESS = 0xFF4ADE80;

    // Spacing
    public static final int SPACE_XS = 4;
    public static final int SPACE_SM = 8;
    public static final int SPACE_MD = 12;
    public static final int SPACE_LG = 16;
    public static final int SPACE_XL = 24;
    public static final int SPACE_2XL = 32;

    public static void drawPanel(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, BG_PANEL);
        // Top accent line
        context.fill(x + 1, y + 1, x + w - 1, y + 3, ACCENT_PRIMARY);
        // Border
        drawBorder(context, x, y, w, h, BORDER_PANEL);
    }

    public static void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.drawHorizontalLine(x, x + w - 1, y, color);
        context.drawHorizontalLine(x, x + w - 1, y + h - 1, color);
        context.drawVerticalLine(x, y, y + h - 1, color);
        context.drawVerticalLine(x + w - 1, y, y + h - 1, color);
    }

    public static void drawSectionHeader(DrawContext context, TextRenderer textRenderer,
                                          String text, int x, int y, int maxWidth) {
        context.drawTextWithShadow(textRenderer, text, x, y, ACCENT_PRIMARY);
        int textWidth = textRenderer.getWidth(text);
        int lineY = y + textRenderer.fontHeight / 2;
        context.fill(x + textWidth + SPACE_SM, lineY, x + maxWidth, lineY + 1, BORDER_PANEL);
    }

    public static void drawWrappedDescription(DrawContext context, TextRenderer textRenderer,
                                               String text, int x, int y, int maxWidth) {
        ScreenLayout.drawWrapped(context, textRenderer, text, x, y, maxWidth, TEXT_SECONDARY);
    }

    public static int buttonColor(boolean hovered) {
        return hovered ? ACCENT_HOVER : ACCENT_SECONDARY;
    }
}
