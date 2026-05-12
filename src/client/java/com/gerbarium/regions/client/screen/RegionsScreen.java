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

        // Основные параметры панели
        int panelWidth = 400;
        int startX = (width - panelWidth) / 2;
        int topY = 25;

        // Кнопка Refresh в правом верхнем углу панели
        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), b -> GerbariumClientNetworking.requestZones())
                .dimensions(startX + panelWidth - 80, topY, 80, 20).build());

        // Расчет сетки списка
        int listY = topY + 30; // Отступ под заголовок и кнопку Refresh
        int bottomSpace = 85;  // Отступ снизу для пагинации и кнопки Close
        int rowH = 24;

        pageSize = Math.max(1, (height - listY - bottomSpace) / rowH);

        int total = ClientGerbariumData.zonesFile().zones.size();
        totalRows = total;
        page = Math.max(0, Math.min(page, Math.max(0, (total - 1) / pageSize)));

        // Пагинация (Центрирована под списком)
        if (total > pageSize) {
            int maxPage = (total - 1) / pageSize;
            int pagY = listY + pageSize * rowH + 6;
            int pageBtnWidth = 100;
            int centerStartX = width / 2 - (pageBtnWidth + 56) / 2; // 56 = ширина кнопок со стрелками и отступов

            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); init(); })
                    .dimensions(centerStartX, pagY, 24, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + " / " + (maxPage + 1)), b -> {})
                    .dimensions(centerStartX + 28, pagY, pageBtnWidth, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(maxPage, page + 1); init(); })
                    .dimensions(centerStartX + 32 + pageBtnWidth, pagY, 24, 20).build());
        }

        // Кнопки зон
        int start = page * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int idx = start + i;
            if (idx >= total) break;
            Zone zone = ClientGerbariumData.zonesFile().zones.get(idx);
            int rowY = listY + i * rowH;

            String label = zone.id + (zone.enabled ? "" : " [OFF]");
            // Защита от слишком длинных названий
            if (textRenderer.getWidth(label) > panelWidth - 16) {
                label = textRenderer.trimToWidth(label, panelWidth - 24) + "...";
            }

            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                client.setScreen(new ZoneDetailsScreen(zone.id));
            }).dimensions(startX, rowY, panelWidth, 20).build());
        }

        // Кнопка закрытия окна (Футер)
        int closeWidth = 160;
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(width / 2 - closeWidth / 2, height - 35, closeWidth, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        // Отрисовка подложки окна
        int panelWidth = 400;
        int startX = (width - panelWidth) / 2;
        int padding = 12;
        int panelTop = 15;
        int panelBottom = height - 15;

        // Темный фон
        context.fill(startX - padding, panelTop, startX + panelWidth + padding, panelBottom, 0x88000000);
        // Акцентная линия сверху
        context.fill(startX - padding, panelTop, startX + panelWidth + padding, panelTop + 2, 0xFF3ECF8E);

        // Заголовок (Выровнен по левому краю внутри панели, рядом с кнопкой Refresh)
        context.drawTextWithShadow(textRenderer, title.getString(), startX, panelTop + 15, 0xFFFFFF);

        if (ClientGerbariumData.zonesFile().zones.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No zones found. Use /gerb zone create <id>", width / 2, height / 2 - 10, 0xFFAAAA);
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