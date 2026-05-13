package com.gerbarium.regions.client.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class ScreenLayout {
    private ScreenLayout() {
    }

    public static int panelWidth(int screenWidth, int preferredWidth) {
        return Math.max(280, Math.min(preferredWidth, screenWidth - 32));
    }

    public static int panelLeft(int screenWidth, int panelWidth) {
        return (screenWidth - panelWidth) / 2;
    }

    public static void drawPanel(DrawContext context, int left, int top, int width, int bottom) {
        int padding = 12;
        context.fill(left - padding, top, left + width + padding, bottom, 0x88000000);
        context.fill(left - padding, top, left + width + padding, top + 2, 0xFF3ECF8E);
    }

    public static void drawWrapped(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int maxWidth, int color) {
        if (text == null || text.isBlank()) {
            return;
        }

        int lineY = y;
        for (String line : wrap(textRenderer, text, maxWidth)) {
            context.drawTextWithShadow(textRenderer, line, x, lineY, color);
            lineY += textRenderer.fontHeight + 2;
        }
    }

    public static int wrappedHeight(TextRenderer textRenderer, String text, int maxWidth) {
        return wrap(textRenderer, text, maxWidth).size() * (textRenderer.fontHeight + 2);
    }

    public static String trim(TextRenderer textRenderer, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        String result = text;
        while (!result.isEmpty() && textRenderer.getWidth(result + suffix) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + suffix;
    }

    private static List<String> wrap(TextRenderer textRenderer, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }

        String[] paragraphs = text.split("\\r?\\n");
        for (String paragraph : paragraphs) {
            String remaining = paragraph.trim();
            while (!remaining.isEmpty()) {
                if (textRenderer.getWidth(remaining) <= maxWidth) {
                    lines.add(remaining);
                    break;
                }

                int cut = remaining.length();
                while (cut > 0 && textRenderer.getWidth(remaining.substring(0, cut)) > maxWidth) {
                    cut--;
                }

                if (cut <= 0) {
                    break;
                }

                int space = remaining.lastIndexOf(' ', cut);
                if (space <= 0) {
                    space = cut;
                }

                lines.add(remaining.substring(0, space).trim());
                remaining = remaining.substring(space).trim();
            }
        }

        return lines;
    }
}
