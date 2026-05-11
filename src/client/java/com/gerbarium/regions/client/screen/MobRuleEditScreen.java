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
        int centerX = width / 2;
        int x = centerX - 120;
        int y = 55;

        ruleIdField = field(x, y, draftRuleId);
        entityField = field(x, y + 35, draftEntity);
        maxAliveField = field(x, y + 70, draftMaxAlive);
        spawnCountField = field(x, y + 105, draftSpawnCount);
        respawnSecondsField = field(x, y + 140, draftRespawnSeconds);
        chanceField = field(x, y + 175, draftChance);

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
                .dimensions(x + 250, y + 35, 100, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> save())
                .dimensions(centerX - 105, height - 35, 100, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> client.setScreen(new RegionsScreen(zoneId)))
                .dimensions(centerX + 5, height - 35, 100, 20)
                .build());
    }

    private TextFieldWidget field(int x, int y, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 240, 20, Text.literal(""));
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

            if (maxAlive < 1 || spawnCount < 1 || respawnSeconds < 1 || chance < 0.0 || chance > 1.0) {
                error = "Invalid values.";
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

        int centerX = width / 2;
        int x = centerX - 120;
        int y = 42;

        context.drawCenteredTextWithShadow(textRenderer, existingRule == null ? "Add Mob Rule" : "Edit Mob Rule", centerX, 15, 0xFFFFFF);

        context.drawTextWithShadow(textRenderer, "Rule ID", x, y, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Entity ID", x, y + 35, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Max Alive", x, y + 70, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Spawn Count", x, y + 105, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Respawn Seconds", x, y + 140, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Chance 0.0 - 1.0", x, y + 175, 0xA5FFB5);

        if (!error.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, error, centerX, height - 55, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String safeRuleId(MobRule rule) {
        if (rule.id == null || rule.id.isBlank()) {
            return "legacy_" + rule.entity.replace(':', '_');
        }

        return rule.id;
    }
}