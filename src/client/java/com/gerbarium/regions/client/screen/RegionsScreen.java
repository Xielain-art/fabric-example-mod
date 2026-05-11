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
    private String selectedZoneId;

    public RegionsScreen(String preferredZoneId) {
        super(Text.literal("Gerbarium Regions"));
        this.selectedZoneId = preferredZoneId == null ? "" : preferredZoneId;
    }

    @Override
    protected void init() {
        clearChildren();

        if (ClientGerbariumData.zonesFile().zones.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> GerbariumClientNetworking.requestZones())
                    .dimensions(width / 2 - 50, height / 2 + 20, 100, 20)
                    .build());
            return;
        }

        if (selectedZoneId.isBlank()) {
            selectedZoneId = ClientGerbariumData.zonesFile().zones.get(0).id;
        }

        int leftX = 20;
        int y = 45;

        int shownZones = 0;
        for (Zone zone : ClientGerbariumData.zonesFile().zones) {
            if (shownZones >= 10) break;

            int buttonY = y + shownZones * 24;
            addDrawableChild(ButtonWidget.builder(Text.literal(zone.id), button -> {
                        selectedZoneId = zone.id;
                        init();
                    })
                    .dimensions(leftX, buttonY, 160, 20)
                    .build());

            shownZones++;
        }

        Optional<Zone> optionalZone = ClientGerbariumData.findZone(selectedZoneId);
        if (optionalZone.isPresent()) {
            Zone zone = optionalZone.get();

            int rightX = 210;
            int buttonY = 62;

            addDrawableChild(ButtonWidget.builder(Text.literal(zone.enabled ? "Disable" : "Enable"), button -> GerbariumClientNetworking.toggleZone(zone.id))
                    .dimensions(rightX, buttonY, 100, 20)
                    .build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Select WE"), button -> GerbariumClientNetworking.selectZone(zone.id))
                    .dimensions(rightX + 110, buttonY, 100, 20)
                    .build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Add Rule"), button -> client.setScreen(new MobRuleEditScreen(zone.id, null)))
                    .dimensions(rightX + 220, buttonY, 100, 20)
                    .build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> GerbariumClientNetworking.requestZones())
                    .dimensions(rightX + 330, buttonY, 100, 20)
                    .build());

            int rulesY = 128;

            if (zone.mobs != null) {
                int shownRules = 0;

                for (MobRule rule : zone.mobs) {
                    if (shownRules >= 7) break;

                    String ruleId = safeRuleId(rule);
                    int rowY = rulesY + shownRules * 26;

                    addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), button -> client.setScreen(new MobRuleEditScreen(zone.id, rule)))
                            .dimensions(rightX + 250, rowY - 4, 55, 20)
                            .build());

                    addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), button -> GerbariumClientNetworking.removeMobRule(zone.id, ruleId))
                            .dimensions(rightX + 310, rowY - 4, 75, 20)
                            .build());

                    shownRules++;
                }
            }
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(width / 2 - 50, height - 30, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        context.drawCenteredTextWithShadow(textRenderer, "Gerbarium Regions", width / 2, 15, 0xFFFFFF);

        context.drawTextWithShadow(textRenderer, "Zones", 20, 32, 0xA5FFB5);

        Optional<Zone> optionalZone = ClientGerbariumData.findZone(selectedZoneId);

        if (ClientGerbariumData.zonesFile().zones.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No zones found. Create one with /gerb zone create <id>", width / 2, height / 2 - 10, 0xFFFFFF);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        if (optionalZone.isPresent()) {
            Zone zone = optionalZone.get();

            int rightX = 210;

            context.drawTextWithShadow(textRenderer, "Zone: " + zone.id, rightX, 32, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, "Enabled: " + zone.enabled, rightX, 88, zone.enabled ? 0x55FF55 : 0xFF5555);
            context.drawTextWithShadow(textRenderer, "Dimension: " + zone.dimension, rightX, 100, 0xCCCCCC);
            context.drawTextWithShadow(textRenderer, "Min: " + zone.min.x + " " + zone.min.y + " " + zone.min.z, rightX, 112, 0xCCCCCC);
            context.drawTextWithShadow(textRenderer, "Max: " + zone.max.x + " " + zone.max.y + " " + zone.max.z, rightX, 124, 0xCCCCCC);

            context.drawTextWithShadow(textRenderer, "Mob Rules", rightX, 150, 0xA5FFB5);

            if (zone.mobs == null || zone.mobs.isEmpty()) {
                context.drawTextWithShadow(textRenderer, "No mob rules. Click Add Rule.", rightX, 170, 0xAAAAAA);
            } else {
                int shownRules = 0;

                for (MobRule rule : zone.mobs) {
                    if (shownRules >= 7) break;

                    int rowY = 166 + shownRules * 26;
                    String ruleId = safeRuleId(rule);

                    context.drawTextWithShadow(textRenderer, ruleId + " -> " + rule.entity, rightX, rowY, 0xFFFFFF);
                    context.drawTextWithShadow(textRenderer,
                            "max=" + rule.maxAlive + " count=" + rule.spawnCount + " delay=" + rule.respawnSeconds + " chance=" + rule.chance,
                            rightX,
                            rowY + 11,
                            0xAAAAAA
                    );

                    shownRules++;
                }
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
}