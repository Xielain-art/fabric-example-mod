package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.MobRule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class MobRuleEditScreen extends Screen {
    private final String zoneId;
    private final MobRule existingRule;

    private String draftRuleId;
    private String draftEntity;
    private String draftMaxAlive;
    private String draftSpawnCount;
    private String draftRespawnSeconds;
    private String draftChance;

    private TextFieldWidget ruleIdField;
    private TextFieldWidget entityField;
    private TextFieldWidget maxAliveField;
    private TextFieldWidget spawnCountField;
    private TextFieldWidget respawnSecondsField;
    private TextFieldWidget chanceField;

    private String error = "";

    public MobRuleEditScreen(String zoneId, MobRule existingRule) {
        super(Text.literal(existingRule == null ? "Add Mob Rule" : "Edit Mob Rule"));

        this.zoneId = zoneId;
        this.existingRule = existingRule;

        this.draftRuleId = existingRule == null ? "" : safeRuleId(existingRule);
        this.draftEntity = existingRule == null ? "minecraft:zombie" : existingRule.entity;
        this.draftMaxAlive = existingRule == null ? "5" : String.valueOf(existingRule.maxAlive);
        this.draftSpawnCount = existingRule == null ? "1" : String.valueOf(existingRule.spawnCount);
        this.draftRespawnSeconds = existingRule == null ? "60" : String.valueOf(existingRule.respawnSeconds);
        this.draftChance = existingRule == null ? "1.0" : String.valueOf(existingRule.chance);
    }

    @Override
    protected void init() {
        clearChildren();

        // Адаптивная ширина панели (от 300 до 500)
        int panelWidth = Math.max(300, Math.min(width - 40, 500));
        int panelX = (width - panelWidth) / 2;

        int contentX = panelX + 20;
        int contentWidth = panelWidth - 40;

        // Динамическое вычисление стартовой высоты для центрирования формы
        int formHeight = 220;
        int topY = Math.max(35, (height - formHeight) / 2);

        int pickButtonWidth = 90;
        int gap = 8;

        int fullFieldWidth = contentWidth;
        int entityFieldWidth = contentWidth - pickButtonWidth - gap;

        int halfGap = 12;
        int halfFieldWidth = (contentWidth - halfGap) / 2;

        // Ряды с полями (шаг 44 пикселя)
        int row1 = topY + 12;
        int row2 = topY + 56;
        int row3 = topY + 100;
        int row4 = topY + 144;
        int row5 = topY + 196; // Кнопки Save / Cancel

        ruleIdField = field(contentX, row1, fullFieldWidth, draftRuleId);
        entityField = field(contentX, row2, entityFieldWidth, draftEntity);

        maxAliveField = field(contentX, row3, halfFieldWidth, draftMaxAlive);
        spawnCountField = field(contentX + halfFieldWidth + halfGap, row3, halfFieldWidth, draftSpawnCount);

        respawnSecondsField = field(contentX, row4, halfFieldWidth, draftRespawnSeconds);
        chanceField = field(contentX + halfFieldWidth + halfGap, row4, halfFieldWidth, draftChance);

        addDrawableChild(ruleIdField);
        addDrawableChild(entityField);
        addDrawableChild(maxAliveField);
        addDrawableChild(spawnCountField);
        addDrawableChild(respawnSecondsField);
        addDrawableChild(chanceField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Pick Entity"), button -> {
                    captureDraft();
                    client.setScreen(new EntityPickerScreen(this, draftEntity));
                })
                .dimensions(contentX + entityFieldWidth + gap, row2, pickButtonWidth, 20)
                .build());

        int saveCancelWidth = Math.min(120, halfFieldWidth);
        int buttonsX = width / 2 - saveCancelWidth - gap / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> save())
                .dimensions(buttonsX, row5, saveCancelWidth, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> client.setScreen(new RegionsScreen(zoneId)))
                .dimensions(buttonsX + saveCancelWidth + gap, row5, saveCancelWidth, 20)
                .build());
    }

    private TextFieldWidget field(int x, int y, int width, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, 20, Text.literal(""));
        field.setText(value);
        return field;
    }

    private void captureDraft() {
        draftRuleId = ruleIdField.getText();
        draftEntity = entityField.getText();
        draftMaxAlive = maxAliveField.getText();
        draftSpawnCount = spawnCountField.getText();
        draftRespawnSeconds = respawnSecondsField.getText();
        draftChance = chanceField.getText();
    }

    public void setEntityId(String entityId) {
        this.draftEntity = entityId;
    }

    private void save() {
        captureDraft();

        try {
            String ruleId = draftRuleId.trim();
            String entity = draftEntity.trim();
            int maxAlive = Integer.parseInt(draftMaxAlive.trim());
            int spawnCount = Integer.parseInt(draftSpawnCount.trim());
            int respawnSeconds = Integer.parseInt(draftRespawnSeconds.trim());
            double chance = Double.parseDouble(draftChance.trim());

            if (ruleId.isBlank()) {
                error = "Rule ID cannot be empty.";
                return;
            }

            if (entity.isBlank() || !entity.contains(":")) {
                error = "Entity ID must look like minecraft:zombie.";
                return;
            }

            if (maxAlive < 1) {
                error = "Max Alive must be 1 or higher.";
                return;
            }

            if (spawnCount < 1) {
                error = "Spawn Count must be 1 or higher.";
                return;
            }

            if (respawnSeconds < 1) {
                error = "Respawn Seconds must be 1 or higher.";
                return;
            }

            if (chance < 0.0 || chance > 1.0) {
                error = "Chance must be between 0.0 and 1.0.";
                return;
            }

            GerbariumClientNetworking.addMobRule(zoneId, ruleId, entity, maxAlive, spawnCount, respawnSeconds, chance);
        } catch (NumberFormatException e) {
            error = "Numbers are invalid.";
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelWidth = Math.max(300, Math.min(width - 40, 500));
        int panelX = (width - panelWidth) / 2;
        int contentX = panelX + 20;
        int contentWidth = panelWidth - 40;

        int formHeight = 220;
        int topY = Math.max(35, (height - formHeight) / 2);

        int halfGap = 12;
        int halfFieldWidth = (contentWidth - halfGap) / 2;

        int panelTop = topY - 18;
        int panelBottom = topY + formHeight + 12;

        // Фон панели
        context.fill(panelX, panelTop, panelX + panelWidth, panelBottom, 0x66000000);

        // Зеленая акцентная полоса сверху
        context.fill(panelX, panelTop, panelX + panelWidth, panelTop + 2, 0xFF3ECF8E);

        // Заголовок окна
        context.drawCenteredTextWithShadow(
                textRenderer,
                existingRule == null ? "Add Mob Rule" : "Edit Mob Rule",
                width / 2,
                panelTop - 18,
                0xFFFFFF
        );

        // Отрисовка лейблов (чуть выше самих полей)
        drawLabel(context, "Rule ID", contentX, topY);
        drawLabel(context, "Entity ID", contentX, topY + 44);

        drawLabel(context, "Max Alive", contentX, topY + 88);
        drawLabel(context, "Spawn Count", contentX + halfFieldWidth + halfGap, topY + 88);

        drawLabel(context, "Respawn Seconds", contentX, topY + 132);
        drawLabel(context, "Chance 0.0 - 1.0", contentX + halfFieldWidth + halfGap, topY + 132);

        // Отображение Zone ID
        context.drawTextWithShadow(
                textRenderer,
                "Zone: " + zoneId,
                contentX,
                panelBottom - 16,
                0x888888
        );

        // Отображение ошибки по центру
        if (!error.isBlank()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    error,
                    width / 2,
                    topY + 176, // Над кнопками Save/Cancel
                    0xFF5555
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawLabel(DrawContext context, String label, int x, int y) {
        context.drawTextWithShadow(textRenderer, label, x, y, 0xA5FFB5);
    }

    private String safeRuleId(MobRule rule) {
        if (rule.id == null || rule.id.isBlank()) {
            return "legacy_" + rule.entity.replace(':', '_');
        }

        return rule.id;
    }
}