package com.gerbarium.regions.client.screen;

public final class AdaptiveLayout {
    private AdaptiveLayout() {}

    public static final int MIN_PANEL_WIDTH = 320;
    public static final int MAX_PANEL_WIDTH = 720;
    public static final int PANEL_MARGIN_X = 32;
    public static final int PANEL_MARGIN_Y = 12;
    public static final int HEADER_HEIGHT = 48;
    public static final int FOOTER_HEIGHT = 40;
    public static final int CONTENT_PADDING = 32;

    public static int panelWidth(int screenWidth, int preferredWidth) {
        int maxAvailable = screenWidth - PANEL_MARGIN_X * 2;
        int clampedPreferred = Math.max(MIN_PANEL_WIDTH, Math.min(preferredWidth, MAX_PANEL_WIDTH));
        return Math.min(clampedPreferred, maxAvailable);
    }

    public static int panelLeft(int screenWidth, int panelWidth) {
        return (screenWidth - panelWidth) / 2;
    }

    public static int panelHeight(int screenHeight) {
        return screenHeight - PANEL_MARGIN_Y * 2;
    }

    public static int panelTop() {
        return PANEL_MARGIN_Y;
    }

    public static int contentX(int panelX) {
        return panelX + CONTENT_PADDING;
    }

    public static int contentY(int panelY) {
        return panelY + HEADER_HEIGHT;
    }

    public static int contentWidth(int panelWidth) {
        return panelWidth - CONTENT_PADDING * 2;
    }

    public static int contentHeight(int panelHeight) {
        return panelHeight - HEADER_HEIGHT - FOOTER_HEIGHT - SPACE_LG;
    }

    public static int footerY(int panelY, int panelHeight) {
        return panelY + panelHeight - FOOTER_HEIGHT;
    }

    public static ColumnMetrics twoColumn(int panelWidth, int gap) {
        int contentW = contentWidth(panelWidth);
        int minColWidth = 140;
        if (contentW < minColWidth * 2 + gap) {
            return new ColumnMetrics(contentW, 0, 0, 1);
        }
        int colW = (contentW - gap) / 2;
        return new ColumnMetrics(colW, colW + gap, gap, 2);
    }

    public static int rowHeight(int availableHeight, int minRows) {
        int preferred = 36;
        int compact = 26;
        int maxHeight = availableHeight / Math.max(1, minRows);
        if (maxHeight >= preferred) return preferred;
        if (maxHeight >= compact) return compact;
        return Math.max(22, maxHeight);
    }

    public static int visibleRows(int availableHeight, int rowHeight) {
        return Math.max(1, availableHeight / rowHeight);
    }

    public static int buttonWidth(int panelWidth) {
        return Math.max(100, (contentWidth(panelWidth) - SPACE_SM) / 2);
    }

    public record ColumnMetrics(int col1Width, int col2Offset, int gap, int columns) {}

    // Alias for backward compatibility during transition
    public static final int SPACE_LG = ScreenTheme.SPACE_LG;
    public static final int SPACE_SM = ScreenTheme.SPACE_SM;
}
