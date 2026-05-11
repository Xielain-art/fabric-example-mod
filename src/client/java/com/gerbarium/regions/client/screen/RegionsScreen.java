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

        int listX = width / 2 - 170;
        int listY = 50;
        int listW = 340;
        int rowH = 24;
        int visible = Math.max(1, (height - 130) / rowH);
        pageSize = visible;

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), b -> GerbariumClientNetworking.requestZones())
                .dimensions(width / 2 - 170, 20, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(width / 2 + 60, 20, 110, 20).build());

        int total = ClientGerbariumData.zonesFile().zones.size();
        totalRows = total;
        page = Math.max(0, Math.min(page, Math.max(0, (total - 1) / visible)));

        if (total > visible) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); init(); })
                    .dimensions(width / 2 - 80, height - 32, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(Math.max(0, (total - 1) / visible), page + 1); init(); })
                    .dimensions(width / 2 + 56, height - 32, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + "/" + ((total - 1) / visible + 1)), b -> {})
                    .dimensions(width / 2 - 50, height - 32, 100, 20).build());
        }

        int start = page * visible;
        for (int i = 0; i < visible; i++) {
            int idx = start + i;
            if (idx >= total) break;
            Zone zone = ClientGerbariumData.zonesFile().zones.get(idx);
            int y = listY + i * rowH;
            addDrawableChild(ButtonWidget.builder(Text.literal(zone.id + (zone.enabled ? "" : " [OFF]")), b -> {
                client.setScreen(new ZoneDetailsScreen(zone.id));
            }).dimensions(listX, y, listW, 20).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);
        if (ClientGerbariumData.zonesFile().zones.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No zones found. Use /gerb zone create <id>", width / 2, height / 2, 0xFFAAAA);
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
