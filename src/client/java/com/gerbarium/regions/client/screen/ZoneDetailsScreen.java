package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.ResourceRule;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Optional;

public class ZoneDetailsScreen extends Screen implements GerbariumRefreshableScreen {
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

        int panelWidth = ScreenLayout.panelWidth(width, 680);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        int listY = 148;
        int rowH = 52;
        int bottomSpace = 90;
        int visible = Math.max(1, (height - listY - bottomSpace) / rowH);

        int actionY = 40;
        int gap = 4;
        int btnW = (panelWidth - gap * 2) / 3;
        addDrawableChild(ButtonWidget.builder(Text.literal(z.enabled ? "Disable" : "Enable"), b -> GerbariumClientNetworking.toggleZone(zoneId))
                .dimensions(startX, actionY, btnW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Select WE"), b -> GerbariumClientNetworking.selectZone(zoneId))
                .dimensions(startX + btnW + gap, actionY, btnW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("TP"), b -> GerbariumClientNetworking.tpToZone(zoneId))
                .dimensions(startX + (btnW + gap) * 2, actionY, btnW, 20).build());
        int row2Y = actionY + 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("Settings"), b -> client.setScreen(new ZoneRuntimeSettingsScreen(zoneId)))
                .dimensions(startX, row2Y, btnW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Add Rule"), b -> client.setScreen(new MobRuleEditScreen(zoneId, null)))
                .dimensions(startX + btnW + gap, row2Y, btnW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Resources"), b -> client.setScreen(new ZoneResourcesScreen(zoneId)))
                .dimensions(startX + (btnW + gap) * 2, row2Y, btnW, 20).build());

        int total = z.mobs == null ? 0 : z.mobs.size();
        pageSize = visible;
        totalRules = total;
        page = Math.max(0, Math.min(page, Math.max(0, (total - 1) / visible)));

        boolean hasPagination = total > visible;
        if (hasPagination) {
            int maxPage = (total - 1) / visible;
            int pagY = listY + visible * rowH + 6;
            int pageBtnWidth = 100;
            int pagStartX = width / 2 - (pageBtnWidth + 56) / 2;

            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); init(); })
                    .dimensions(pagStartX, pagY, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + " / " + (maxPage + 1)), b -> {})
                    .dimensions(pagStartX + 28, pagY, pageBtnWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(maxPage, page + 1); init(); })
                    .dimensions(pagStartX + 32 + pageBtnWidth, pagY, 24, 20).build());
        }

        int start = page * visible;
        for (int i = 0; i < visible; i++) {
            int idx = start + i;
            if (idx >= total) {
                break;
            }
            MobRule r = z.mobs.get(idx);
            int rowY = listY + i * rowH;
            addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), b -> client.setScreen(new MobRuleEditScreen(zoneId, r)))
                    .dimensions(startX + panelWidth - 108, rowY + 14, 50, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Del"), b -> GerbariumClientNetworking.removeMobRule(zoneId, r.id))
                    .dimensions(startX + panelWidth - 54, rowY + 14, 42, 20).build());
        }

        int footerY = hasPagination ? listY + visible * rowH + 34 : height - 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), b -> GerbariumClientNetworking.requestZones())
                .dimensions(width / 2 - 114, footerY, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new RegionsScreen(zoneId)))
                .dimensions(width / 2 + 4, footerY, 110, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelWidth = ScreenLayout.panelWidth(width, 680);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        ScreenLayout.drawPanel(context, startX, 15, panelWidth, height - 15);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 23, 0xFFFFFF);

        Optional<Zone> oz = ClientGerbariumData.findZone(zoneId);
        if (oz.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "Zone not found", width / 2, height / 2 - 10, 0xFF5555);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        Zone z = oz.get();
        ZoneDefaults.normalizeZone(z);
        context.drawTextWithShadow(textRenderer, "Zone: " + z.id, startX + 12, 68, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Dim: " + z.dimension, startX + panelWidth / 2, 68, 0xCCCCCC);
        context.drawTextWithShadow(textRenderer, "Min: " + z.min.x + " " + z.min.y + " " + z.min.z, startX + 12, 84, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Max: " + z.max.x + " " + z.max.y + " " + z.max.z, startX + panelWidth / 2, 84, 0xAAAAAA);

        context.drawTextWithShadow(textRenderer, "Mob Rules", startX + 12, 112, 0xA5FFB5);
        int resourceCount = z.resources == null ? 0 : z.resources.size();
        context.drawTextWithShadow(textRenderer, "Resources: " + resourceCount, startX + panelWidth / 2, 112, 0xA5FFB5);

        int listY = 148;
        int rowH = 52;
        int visible = Math.max(1, (height - listY - 90) / rowH);
        int start = page * visible;

        for (int i = 0; z.mobs != null && i < visible; i++) {
            int idx = start + i;
            if (idx >= z.mobs.size()) {
                break;
            }
            MobRule r = z.mobs.get(idx);
            int rowY = listY + i * rowH;
            context.fill(startX + 4, rowY, startX + panelWidth - 4, rowY + rowH - 4, 0x33000000);
            String titleText = ScreenLayout.trim(textRenderer, (r.name == null || r.name.isBlank() ? r.id : r.name) + " -> " + r.entity, panelWidth - 124);
            context.drawTextWithShadow(textRenderer, titleText, startX + 12, rowY + 8, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, summary(r), startX + 12, rowY + 24, 0xAAAAAA);
        }

        if (totalRules == 0) {
            context.drawCenteredTextWithShadow(textRenderer, "No rules added. Click 'Add Rule' to begin.", width / 2, listY + 20, 0xAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String summary(MobRule rule) {
        int companions = rule.companions == null ? 0 : rule.companions.size();
        String chance = rule.chance >= 1.0 ? "guaranteed" : ("chance " + (int) (rule.chance * 100) + "%");
        String boundary = boundarySummary(rule);
        if (rule.spawnType.name().equals("UNIQUE")) {
            return "[UNIQUE] " + boundary + " | cooldown " + rule.respawnSeconds + "s | " + chance + " | companions " + companions;
        }
        String s = "[PACK] " + boundary + " | max " + rule.maxAlive + " | spawn " + rule.spawnCount + " | " + rule.refillMode + " | cooldown " + rule.respawnSeconds + "s | " + chance + " | companions " + companions;
        if (rule.refillMode.name().equals("TIMED") || rule.respawnSeconds < 300) {
            s += " | farm risk";
        }
        return s;
    }

    private String boundarySummary(MobRule rule) {
        String mode = rule.boundaryMode == null || rule.boundaryMode.isBlank() ? MobRule.BOUNDARY_LEASH : rule.boundaryMode;
        return rule.boundaryMaxOutsideSeconds > 0 ? mode + ", " + rule.boundaryMaxOutsideSeconds + "s" : mode;
    }

    @Override
    public void refreshFromSync() {
        init();
    }
}
