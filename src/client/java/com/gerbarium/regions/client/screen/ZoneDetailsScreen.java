package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Optional;

public class ZoneDetailsScreen extends Screen {
    private final String zoneId;
    private int page = 0;
    private int pageSize = 0;
    private int totalRules = 0;

    public ZoneDetailsScreen(String zoneId) {
        super(Text.literal("Zone Details"));
        this.zoneId = zoneId;
    }

    @Override
    protected void init() {
        clearChildren();
        Optional<Zone> oz = ClientGerbariumData.findZone(zoneId);
        if (oz.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new RegionsScreen("")))
                    .dimensions(width / 2 - 60, height / 2 + 20, 120, 20).build());
            return;
        }
        Zone z = oz.get();
        ZoneDefaults.normalizeZone(z);

        int panelWidth = 500;
        int startX = (width - panelWidth) / 2;

        // --- Блок 1: Панель действий (Action Bar) ---
        int actionY = 40;
        int actionTotalW = 86 + 86 + 50 + 86 + 12; // ширины кнопок + отступы по 4px
        int actionStartX = width / 2 - actionTotalW / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal(z.enabled ? "Disable" : "Enable"), b -> GerbariumClientNetworking.toggleZone(zoneId))
                .dimensions(actionStartX, actionY, 86, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Select WE"), b -> GerbariumClientNetworking.selectZone(zoneId))
                .dimensions(actionStartX + 90, actionY, 86, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("TP"), b -> GerbariumClientNetworking.tpToZone(zoneId))
                .dimensions(actionStartX + 180, actionY, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Runtime"), b -> client.setScreen(new ZoneRuntimeSettingsScreen(zoneId)))
                .dimensions(actionStartX + 234, actionY, 86, 20).build());

        // --- Блок 2: Заголовок списка и кнопка добавления ---
        int headerY = 110;
        addDrawableChild(ButtonWidget.builder(Text.literal("Add Rule"), b -> client.setScreen(new MobRuleEditScreen(zoneId, null)))
                .dimensions(startX + panelWidth - 98, headerY - 5, 86, 20).build());

        // --- Блок 3: Сетка списка и пагинация ---
        int listY = 132;
        int rowH = 42;
        int bottomSpace = 85; // Место для пагинации и нижних кнопок
        int visible = Math.max(1, (height - listY - bottomSpace) / rowH);

        int total = z.mobs == null ? 0 : z.mobs.size();
        pageSize = visible;
        totalRules = total;
        page = Math.max(0, Math.min(page, Math.max(0, (total - 1) / visible)));

        // Пагинация (центрирована под списком)
        if (total > visible) {
            int maxPage = (total - 1) / visible;
            int pagY = listY + visible * rowH + 8;
            int pageBtnWidth = 100;
            int pagStartX = width / 2 - (pageBtnWidth + 56) / 2;

            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); init(); })
                    .dimensions(pagStartX, pagY, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + " / " + (maxPage + 1)), b -> {})
                    .dimensions(pagStartX + 28, pagY, pageBtnWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(maxPage, page + 1); init(); })
                    .dimensions(pagStartX + 32 + pageBtnWidth, pagY, 24, 20).build());
        }

        // Элементы списка
        int start = page * visible;
        for (int i = 0; i < visible; i++) {
            int idx = start + i;
            if (idx >= total) break;
            MobRule r = z.mobs.get(idx);
            int rowY = listY + i * rowH;

            // Кнопки прижаты к правому краю
            addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), b -> client.setScreen(new MobRuleEditScreen(zoneId, r)))
                    .dimensions(startX + panelWidth - 108, rowY + 11, 50, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Del"), b -> GerbariumClientNetworking.removeMobRule(zoneId, r.id))
                    .dimensions(startX + panelWidth - 54, rowY + 11, 42, 20).build());
        }

        // --- Блок 4: Футер ---
        int footerY = height - 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), b -> GerbariumClientNetworking.requestZones())
                .dimensions(width / 2 - 114, footerY, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new RegionsScreen(zoneId)))
                .dimensions(width / 2 + 4, footerY, 110, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        // Отрисовка подложки модального окна
        int panelWidth = 500;
        int startX = (width - panelWidth) / 2;
        int padding = 12;
        int panelTop = 15;
        int panelBottom = height - 15;

        // Темный фон
        context.fill(startX - padding, panelTop, startX + panelWidth + padding, panelBottom, 0x88000000);
        // Декоративная акцентная линия сверху
        context.fill(startX - padding, panelTop, startX + panelWidth + padding, panelTop + 2, 0xFF3ECF8E);

        // Заголовок
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelTop + 8, 0xFFFFFF);

        Optional<Zone> oz = ClientGerbariumData.findZone(zoneId);
        if (oz.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "Zone not found", width / 2, height / 2 - 10, 0xFF5555);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        Zone z = oz.get();
        ZoneDefaults.normalizeZone(z);

        // Информация о зоне (в две колонки)
        int infoY1 = 70;
        int infoY2 = 86;
        int col2X = startX + 240;

        context.drawTextWithShadow(textRenderer, "Zone: " + z.id, startX + 12, infoY1, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Dimension: " + z.dimension, col2X, infoY1, 0xCCCCCC);
        context.drawTextWithShadow(textRenderer, "Min: " + z.min.x + " " + z.min.y + " " + z.min.z, startX + 12, infoY2, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Max: " + z.max.x + " " + z.max.y + " " + z.max.z, col2X, infoY2, 0xAAAAAA);

        // Подзаголовок списка
        context.drawTextWithShadow(textRenderer, "Mob Rules", startX + 12, 110, 0xA5FFB5);

        // Рендер элементов списка
        int listY = 132;
        int rowH = 42;
        int visible = Math.max(1, (height - listY - 85) / rowH);
        int start = page * visible;

        for (int i = 0; z.mobs != null && i < visible; i++) {
            int idx = start + i;
            if (idx >= z.mobs.size()) break;
            MobRule r = z.mobs.get(idx);
            int rowY = listY + i * rowH;

            // Подсветка строки списка
            context.fill(startX + 4, rowY, startX + panelWidth - 4, rowY + rowH - 4, 0x33000000);

            String titleText = (r.name == null || r.name.isBlank() ? r.id : r.name) + " -> " + r.entity;
            String sumText = summary(r);

            // Обрезка текста, если он слишком длинный, чтобы не лез на кнопки
            int maxTextWidth = panelWidth - 124;
            if (textRenderer.getWidth(titleText) > maxTextWidth) {
                titleText = textRenderer.trimToWidth(titleText, maxTextWidth - 10) + "...";
            }
            if (textRenderer.getWidth(sumText) > maxTextWidth) {
                sumText = textRenderer.trimToWidth(sumText, maxTextWidth - 10) + "...";
            }

            context.drawTextWithShadow(textRenderer, titleText, startX + 12, rowY + 8, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, sumText, startX + 12, rowY + 22, 0xAAAAAA);
        }

        if (totalRules == 0) {
            context.drawCenteredTextWithShadow(textRenderer, "No rules added. Click 'Add Rule' to begin.", width / 2, listY + 20, 0xAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String summary(MobRule rule) {
        int companions = rule.companions == null ? 0 : rule.companions.size();
        String chance = rule.chance >= 1.0 ? "guaranteed" : ("chance " + (int) (rule.chance * 100) + "%");
        if (rule.spawnType.name().equals("UNIQUE")) {
            return "[UNIQUE] cooldown " + rule.respawnSeconds + "s | " + chance + " | companions " + companions;
        }
        String s = "[PACK] max " + rule.maxAlive + " | spawn " + rule.spawnCount + " | " + rule.refillMode + " | cooldown " + rule.respawnSeconds + "s | " + chance + " | companions " + companions;
        if (rule.refillMode.name().equals("TIMED") || rule.respawnSeconds < 300) s += " | farm risk";
        return s;
    }
}