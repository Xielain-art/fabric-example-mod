package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.Zone;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Optional;

public class RegionsScreen extends Screen {
    private static String worldEditSelectedZoneId = "";
    private static int zoneScroll = 0;
    private static int ruleScroll = 0;

    private String selectedZoneId;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private int leftX;
    private int leftWidth;

    private int rightX;
    private int rightWidth;

    private int zoneListY;
    private int ruleListY;

    private int visibleZoneRows;
    private int visibleRuleRows;

    public RegionsScreen(String preferredZoneId) {
        super(Text.literal("Gerbarium Regions"));
        this.selectedZoneId = preferredZoneId == null ? "" : preferredZoneId;
    }

    @Override
    protected void init() {
        clearChildren();
        calculateLayout();

        if (ClientGerbariumData.zonesFile().zones.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> GerbariumClientNetworking.requestZones())
                    .dimensions(width / 2 - 55, height / 2 + 16, 110, 20)
                    .build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                    .dimensions(width / 2 - 55, height / 2 + 42, 110, 20)
                    .build());
            return;
        }

        if (selectedZoneId.isBlank() || ClientGerbariumData.findZone(selectedZoneId).isEmpty()) {
            selectedZoneId = ClientGerbariumData.zonesFile().zones.get(0).id;
            ruleScroll = 0;
        }

        buildZoneButtons();

        Optional<Zone> optionalZone = ClientGerbariumData.findZone(selectedZoneId);
        if (optionalZone.isEmpty()) {
            return;
        }

        Zone zone = optionalZone.get();

        buildActionButtons(zone);
        buildRuleButtons(zone);
    }

    private void buildZoneButtons() {
        int totalZones = ClientGerbariumData.zonesFile().zones.size();
        zoneScroll = clamp(zoneScroll, 0, Math.max(0, totalZones - visibleZoneRows));

        if (totalZones > visibleZoneRows) {
            int btnSize = 16;
            int btnY = panelY + 12; // Уровень заголовка "Zones"

            addDrawableChild(ButtonWidget.builder(Text.literal("↑"), button -> {
                        zoneScroll = clamp(zoneScroll - 1, 0, Math.max(0, totalZones - visibleZoneRows));
                        init();
                    })
                    .dimensions(leftX + leftWidth - btnSize * 2 - 4, btnY, btnSize, btnSize)
                    .build());

            addDrawableChild(ButtonWidget.builder(Text.literal("↓"), button -> {
                        zoneScroll = clamp(zoneScroll + 1, 0, Math.max(0, totalZones - visibleZoneRows));
                        init();
                    })
                    .dimensions(leftX + leftWidth - btnSize, btnY, btnSize, btnSize)
                    .build());
        }

        for (int i = 0; i < visibleZoneRows; i++) {
            int zoneIndex = zoneScroll + i;

            if (zoneIndex >= totalZones) {
                break;
            }

            Zone zone = ClientGerbariumData.zonesFile().zones.get(zoneIndex);
            int buttonY = zoneListY + i * 24;

            int prefix = zone.id.equalsIgnoreCase(selectedZoneId) ? 10 : 0;

            addDrawableChild(ButtonWidget.builder(Text.literal(trimToWidth(zone.id, leftWidth - prefix - 8)), button -> {
                        if (!worldEditSelectedZoneId.isBlank() && !zone.id.equalsIgnoreCase(worldEditSelectedZoneId)) {
                            worldEditSelectedZoneId = "";
                            GerbariumClientNetworking.deselectZone();
                        }

                        selectedZoneId = zone.id;
                        ruleScroll = 0;

                        GerbariumClientNetworking.setPreferredZoneId(zone.id);
                        init();
                    })
                    .dimensions(leftX + prefix, buttonY, leftWidth - prefix, 20)
                    .build());
        }
    }

    private void buildActionButtons(Zone zone) {
        int buttonGap = 4;
        // Аккуратно распределяем 3 кнопки в ряд
        int buttonW = (rightWidth - buttonGap * 2) / 3;

        int row1 = zoneListY;
        int row2 = row1 + 24;

        addActionButton(0, row1, buttonW, buttonGap, Text.literal(zone.enabled ? "Disable" : "Enable"),
                button -> GerbariumClientNetworking.toggleZone(zone.id));

        boolean selectedInWorldEdit = zone.id.equalsIgnoreCase(worldEditSelectedZoneId);
        addActionButton(1, row1, buttonW, buttonGap, Text.literal(selectedInWorldEdit ? "Deselect WE" : "Select WE"),
                button -> {
                    if (zone.id.equalsIgnoreCase(worldEditSelectedZoneId)) {
                        worldEditSelectedZoneId = "";
                        GerbariumClientNetworking.deselectZone();
                    } else {
                        worldEditSelectedZoneId = zone.id;
                        GerbariumClientNetworking.selectZone(zone.id);
                    }
                    init();
                });

        addActionButton(2, row1, buttonW, buttonGap, Text.literal("Add Rule"),
                button -> client.setScreen(new MobRuleEditScreen(zone.id, null)));

        addActionButton(0, row2, buttonW, buttonGap, Text.literal("TP to Zone"),
                button -> GerbariumClientNetworking.tpToZone(zone.id));

        addActionButton(1, row2, buttonW, buttonGap, Text.literal("Refresh"),
                button -> GerbariumClientNetworking.requestZones());

        addActionButton(2, row2, buttonW, buttonGap, Text.literal("Delete Zone"),
                button -> {
                    if (zone.id.equalsIgnoreCase(worldEditSelectedZoneId)) {
                        worldEditSelectedZoneId = "";
                        GerbariumClientNetworking.deselectZone();
                    }

                    selectedZoneId = "";
                    ruleScroll = 0;

                    GerbariumClientNetworking.setPreferredZoneId("");
                    GerbariumClientNetworking.deleteZone(zone.id);
                });

        // Кнопка Close в правом нижнем углу всей панели
        int closeW = 80;
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(panelX + panelWidth - closeW - 10, panelY + panelHeight - 26, closeW, 20)
                .build());
    }

    private void buildRuleButtons(Zone zone) {
        int totalRules = zone.mobs == null ? 0 : zone.mobs.size();
        ruleScroll = clamp(ruleScroll, 0, Math.max(0, totalRules - visibleRuleRows));

        if (totalRules > visibleRuleRows) {
            int btnSize = 16;
            int btnY = ruleListY - 22; // Уровень заголовка "Mob Rules"

            addDrawableChild(ButtonWidget.builder(Text.literal("↑"), button -> {
                        ruleScroll = clamp(ruleScroll - 1, 0, Math.max(0, totalRules - visibleRuleRows));
                        init();
                    })
                    .dimensions(rightX + rightWidth - btnSize * 2 - 4, btnY, btnSize, btnSize)
                    .build());

            addDrawableChild(ButtonWidget.builder(Text.literal("↓"), button -> {
                        ruleScroll = clamp(ruleScroll + 1, 0, Math.max(0, totalRules - visibleRuleRows));
                        init();
                    })
                    .dimensions(rightX + rightWidth - btnSize, btnY, btnSize, btnSize)
                    .build());
        }

        if (zone.mobs == null) {
            return;
        }

        for (int i = 0; i < visibleRuleRows; i++) {
            int ruleIndex = ruleScroll + i;

            if (ruleIndex >= zone.mobs.size()) {
                break;
            }

            MobRule rule = zone.mobs.get(ruleIndex);
            String ruleId = safeRuleId(rule);

            int rowY = ruleListY + i * 36;

            int removeW = 55;
            int editW = 45;
            int removeX = rightX + rightWidth - removeW;
            int editX = removeX - editW - 4;

            addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), button -> client.setScreen(new MobRuleEditScreen(zone.id, rule)))
                    .dimensions(editX, rowY + 2, editW, 20)
                    .build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), button -> GerbariumClientNetworking.removeMobRule(zone.id, ruleId))
                    .dimensions(removeX, rowY + 2, removeW, 20)
                    .build());
        }
    }

    private void calculateLayout() {
        panelWidth = Math.min(width - 60, 880);
        panelHeight = Math.min(height - 40, 420);

        // Динамическое центрирование по экрану
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        leftWidth = Math.max(140, (int) (panelWidth * 0.3)); // Примерно 30% под зоны
        int gap = 15;

        leftX = panelX + 12;

        rightX = leftX + leftWidth + gap;
        rightWidth = panelWidth - leftWidth - gap * 2 - 12;

        zoneListY = panelY + 36;

        // Высота левой секции под кнопки зон
        visibleZoneRows = Math.max(2, (panelHeight - 45) / 24);

        // Правая секция: действия (2 ряда) + отступ + инфо (4 строки) + отступ = ~130px от zoneListY
        ruleListY = zoneListY + 48 + 12 + 50 + 26;

        // Оставшаяся высота для правил (учитывая кнопку Close снизу)
        visibleRuleRows = Math.max(1, (panelY + panelHeight - ruleListY - 32) / 36);
    }

    private void addActionButton(int index, int y, int buttonW, int gap, Text text, ButtonWidget.PressAction action) {
        addDrawableChild(ButtonWidget.builder(text, action)
                .dimensions(rightX + index * (buttonW + gap), y, buttonW, 20)
                .build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        calculateLayout();

        // Проверка над областью списков зон
        boolean overZones = mouseX >= leftX
                && mouseX <= leftX + leftWidth
                && mouseY >= zoneListY
                && mouseY <= zoneListY + visibleZoneRows * 24;

        if (overZones) {
            int totalZones = ClientGerbariumData.zonesFile().zones.size();
            int maxScroll = Math.max(0, totalZones - visibleZoneRows);
            zoneScroll = clamp(zoneScroll - (int) Math.signum(amount), 0, maxScroll);
            init();
            return true;
        }

        // Проверка над областью списков правил
        Optional<Zone> optionalZone = ClientGerbariumData.findZone(selectedZoneId);
        boolean overRules = mouseX >= rightX
                && mouseX <= rightX + rightWidth
                && mouseY >= ruleListY
                && mouseY <= ruleListY + visibleRuleRows * 36;

        if (overRules && optionalZone.isPresent() && optionalZone.get().mobs != null) {
            int totalRules = optionalZone.get().mobs.size();
            int maxScroll = Math.max(0, totalRules - visibleRuleRows);
            ruleScroll = clamp(ruleScroll - (int) Math.signum(amount), 0, maxScroll);
            init();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        calculateLayout();
        renderBackground(context);

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xAA000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 2, 0xFF3ECF8E);

        // Выравнивание заголовков относительно верха панели
        context.drawCenteredTextWithShadow(textRenderer, "Gerbarium Regions", width / 2, panelY - 14, 0xFFFFFF);

        context.drawTextWithShadow(textRenderer, "Zones", leftX, panelY + 16, 0xA5FFB5);

        if (ClientGerbariumData.zonesFile().zones.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No zones found. Create one with /gerb zone create <id>", width / 2, height / 2 - 10, 0xFFFFFF);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        int totalZones = ClientGerbariumData.zonesFile().zones.size();
        if (totalZones > visibleZoneRows) {
            String pagesText = (zoneScroll + 1) + "-" + Math.min(zoneScroll + visibleZoneRows, totalZones) + "/" + totalZones;
            context.drawTextWithShadow(textRenderer, pagesText, leftX + 45, panelY + 16, 0x888888);
        }

        Optional<Zone> optionalZone = ClientGerbariumData.findZone(selectedZoneId);

        if (optionalZone.isEmpty()) {
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        Zone zone = optionalZone.get();

        context.drawTextWithShadow(textRenderer, "Zone: " + trimToWidth(zone.id, rightWidth - 50), rightX, panelY + 16, 0xFFFFFF);

        // Блок информации отцентрован между кнопками действий и правилами
        int infoY = zoneListY + 54;

        context.drawTextWithShadow(textRenderer, "Enabled: " + zone.enabled, rightX, infoY, zone.enabled ? 0x55FF55 : 0xFF5555);
        context.drawTextWithShadow(textRenderer, "Dimension: " + trimToWidth(zone.dimension, rightWidth), rightX, infoY + 12, 0xCCCCCC);
        context.drawTextWithShadow(textRenderer, "Min: " + zone.min.x + " " + zone.min.y + " " + zone.min.z, rightX, infoY + 24, 0xCCCCCC);
        context.drawTextWithShadow(textRenderer, "Max: " + zone.max.x + " " + zone.max.y + " " + zone.max.z, rightX, infoY + 36, 0xCCCCCC);

        context.drawTextWithShadow(textRenderer, "Mob Rules", rightX, ruleListY - 18, 0xA5FFB5);

        if (zone.mobs == null || zone.mobs.isEmpty()) {
            context.drawTextWithShadow(textRenderer, "No mob rules. Click Add Rule.", rightX, ruleListY, 0xAAAAAA);
        } else {
            int totalRules = zone.mobs.size();

            if (totalRules > visibleRuleRows) {
                String pagesText = (ruleScroll + 1) + "-" + Math.min(ruleScroll + visibleRuleRows, totalRules) + "/" + totalRules;
                context.drawTextWithShadow(textRenderer, pagesText, rightX + 65, ruleListY - 18, 0x888888);
            }

            for (int i = 0; i < visibleRuleRows; i++) {
                int ruleIndex = ruleScroll + i;

                if (ruleIndex >= totalRules) {
                    break;
                }

                MobRule rule = zone.mobs.get(ruleIndex);
                int rowY = ruleListY + i * 36;
                String ruleId = safeRuleId(rule);

                // Ограничиваем текст так, чтобы он не залезал на кнопки Edit/Remove (~110px запаса)
                int textMaxWidth = rightWidth - 110;

                context.drawTextWithShadow(
                        textRenderer,
                        trimToWidth(ruleId + " -> " + rule.entity, textMaxWidth),
                        rightX,
                        rowY + 2,
                        0xFFFFFF
                );

                context.drawTextWithShadow(
                        textRenderer,
                        trimToWidth("max=" + rule.maxAlive + " count=" + rule.spawnCount + " delay=" + rule.respawnSeconds + " chance=" + rule.chance, textMaxWidth),
                        rightX,
                        rowY + 14,
                        0xAAAAAA
                );
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String safeRuleId(MobRule rule) {
        if (rule.id == null || rule.id.isBlank()) {
            return "legacy_" + rule.entity.replace(':', '_');
        }

        return rule.id;
    }

    private String trimToWidth(String text, int maxWidth) {
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

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}