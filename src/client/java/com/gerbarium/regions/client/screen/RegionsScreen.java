package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.Zone;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class RegionsScreen extends Screen {
    private int page = 0;
    private int pageSize = 1;
    private int totalRows = 0;

    public RegionsScreen(String preferredZoneId) {
        super(Text.literal("Gerbarium Zones"));
    }

    @Override
    protected void init() {
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

        if (total > pageSize) {
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
            String label = ScreenLayout.trim(textRenderer, zone.id + (zone.enabled ? "" : " [OFF]"), panelWidth - 24);

            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> client.setScreen(new ZoneDetailsScreen(zone.id)))
                    .dimensions(startX, rowY, panelWidth, 20).build());
        }

        int closeWidth = Math.min(160, panelWidth);
        int closeX = width / 2 - closeWidth / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(closeX, height - 35, closeWidth, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelWidth = ScreenLayout.panelWidth(width, 480);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        ScreenLayout.drawPanel(context, startX, 15, panelWidth, height - 15);
        context.drawTextWithShadow(textRenderer, title.getString(), startX, 30, 0xFFFFFF);

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
}
