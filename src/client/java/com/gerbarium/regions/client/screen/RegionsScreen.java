package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.Zone;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class RegionsScreen extends Screen implements GerbariumRefreshableScreen {
    private final String preferredZoneId;
    private boolean triedPreferredZoneOpen = false;
    private int page = 0;
    private int pageSize = 1;
    private int totalRows = 0;

    public RegionsScreen(String preferredZoneId) {
        super(Text.literal("Gerbarium Zones"));
        this.preferredZoneId = normalizeZoneId(preferredZoneId);
    }

    @Override
    protected void init() {
        if (!triedPreferredZoneOpen && !preferredZoneId.isBlank() && ClientGerbariumData.findZone(preferredZoneId).isPresent()) {
            triedPreferredZoneOpen = true;
            client.setScreen(new ZoneDetailsScreen(preferredZoneId));
            return;
        }
        triedPreferredZoneOpen = true;

        clearChildren();

        int panelWidth = ScreenLayout.panelWidth(width, 480);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        int topY = 24;
        int listY = topY + 34;
        int rowH = 26;
        int bottomSpace = 86;

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), b -> GerbariumClientNetworking.requestZones())
                .dimensions(startX + panelWidth - 86, topY, 86, 20).build());

        pageSize = Math.max(1, (height - listY - bottomSpace) / rowH);

        int total = ClientGerbariumData.zonesFile().zones.size();
        totalRows = total;
        page = Math.max(0, Math.min(page, Math.max(0, (total - 1) / pageSize)));

        boolean hasPagination = total > pageSize;
        if (hasPagination) {
            int maxPage = (total - 1) / pageSize;
            int pagY = listY + pageSize * rowH + 6;
            int pageBtnWidth = 100;
            int pagStartX = width / 2 - (pageBtnWidth + 56) / 2;

            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); init(); })
                    .dimensions(pagStartX, pagY, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + " / " + (maxPage + 1)), b -> {})
                    .dimensions(pagStartX + 28, pagY, pageBtnWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(maxPage, page + 1); init(); })
                    .dimensions(pagStartX + 32 + pageBtnWidth, pagY, 24, 20).build());
        }

        int start = page * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int idx = start + i;
            if (idx >= total) {
                break;
            }
            Zone zone = ClientGerbariumData.zonesFile().zones.get(idx);
            int rowY = listY + i * rowH;
            String zoneId = normalizeZoneId(zone.id);
            String title = zoneId.isBlank() ? safeTitle(zone.name) : zoneId;
            String label = ScreenLayout.trim(textRenderer, title + (zone.enabled ? "" : " [OFF]"), panelWidth - 24);

            ButtonWidget button = ButtonWidget.builder(Text.literal(label), b -> {
                        if (!zoneId.isBlank()) {
                            client.setScreen(new ZoneDetailsScreen(zoneId));
                        }
                    })
                    .dimensions(startX, rowY, panelWidth, 20)
                    .build();
            button.active = !zoneId.isBlank();
            addDrawableChild(button);
        }

        int closeWidth = Math.min(160, panelWidth);
        int closeX = width / 2 - closeWidth / 2;
        int footerY = hasPagination ? listY + pageSize * rowH + 34 : height - 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(closeX, footerY, closeWidth, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelWidth = ScreenLayout.panelWidth(width, 480);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        ScreenTheme.drawPanel(context, startX, 15, panelWidth, height - 15);
        context.drawTextWithShadow(textRenderer, title.getString(), startX, 30, ScreenTheme.ACCENT_PRIMARY);

        if (ClientGerbariumData.zonesFile().zones.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No zones found. Use /gerb zone create <id>", width / 2, height / 2 - 10, 0xFFAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int maxPage = Math.max(0, (totalRows - 1) / pageSize);
        if (maxPage > 0) {
            page = Math.max(0, Math.min(maxPage, page - (int) Math.signum(amount)));
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private static String normalizeZoneId(String zoneId) {
        if (zoneId == null) {
            return "";
        }
        String normalized = zoneId.trim();
        return normalized.equalsIgnoreCase("null") ? "" : normalized;
    }

    private static String safeTitle(String title) {
        if (title == null) {
            return "(unnamed zone)";
        }
        String normalized = title.trim();
        if (normalized.isBlank() || normalized.equalsIgnoreCase("null")) {
            return "(unnamed zone)";
        }
        return normalized;
    }

    @Override
    public void refreshFromSync() {
        init();
    }
}
