package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.ResourceRule;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Optional;

public class ZoneResourcesScreen extends Screen {
    private final String zoneId;
    private int page = 0;
    private int pageSize = 0;
    private int totalRules = 0;

    public ZoneResourcesScreen(String zoneId) {
        super(Text.literal("Resource Rules"));
        this.zoneId = zoneId;
    }

    @Override
    protected void init() {
        clearChildren();
        Optional<Zone> oz = ClientGerbariumData.findZone(zoneId);
        if (oz.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new ZoneDetailsScreen(zoneId)))
                    .dimensions(width / 2 - 60, height / 2 + 20, 120, 20).build());
            return;
        }

        Zone z = oz.get();
        ZoneDefaults.normalizeZone(z);

        int panelWidth = ScreenLayout.panelWidth(width, 680);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        int actionY = 40;
        int listY = 72;
        int rowH = 52;
        int bottomSpace = 90;
        int visible = Math.max(1, (height - listY - bottomSpace) / rowH);

        addDrawableChild(ButtonWidget.builder(Text.literal("Add Resource"), b -> {
            ResourceRule newRule = ResourceRule.defaults(ResourceRule.generateId(), "New Resource");
            client.setScreen(new ResourceRuleEditScreen(zoneId, newRule, this));
        }).dimensions(startX + panelWidth - 120, actionY, 110, 20).build());

        int total = z.resources == null ? 0 : z.resources.size();
        pageSize = visible;
        totalRules = total;
        page = Math.max(0, Math.min(page, Math.max(0, (total - 1) / visible)));

        if (total > visible) {
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
            if (idx >= total) break;
            ResourceRule r = z.resources.get(idx);
            int rowY = listY + i * rowH;
            final int ruleIdx = idx;
            addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), b -> client.setScreen(new ResourceRuleEditScreen(zoneId, r, this)))
                    .dimensions(startX + panelWidth - 108, rowY + 14, 50, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Del"), b -> GerbariumClientNetworking.removeResourceRule(zoneId, r.id))
                    .dimensions(startX + panelWidth - 54, rowY + 14, 42, 20).build());
        }

        int footerY = height - 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new ZoneDetailsScreen(zoneId)))
                .dimensions(width / 2 - 60, footerY, 120, 20).build());
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

        context.drawTextWithShadow(textRenderer, "Zone: " + z.id + " | Resource Rules", startX + 12, 56, 0xA5FFB5);

        int listY = 72;
        int rowH = 52;
        int visible = Math.max(1, (height - listY - 90) / rowH);
        int start = page * visible;

        for (int i = 0; z.resources != null && i < visible; i++) {
            int idx = start + i;
            if (idx >= z.resources.size()) break;
            ResourceRule r = z.resources.get(idx);
            int rowY = listY + i * rowH;
            context.fill(startX + 4, rowY, startX + panelWidth - 4, rowY + rowH - 4, 0x33000000);
            String titleText = ScreenLayout.trim(textRenderer, (r.name == null || r.name.isBlank() ? r.id : r.name) + " [" + (r.enabled ? "ON" : "OFF") + "]", panelWidth - 124);
            context.drawTextWithShadow(textRenderer, titleText, startX + 12, rowY + 8, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, resourceSummary(r), startX + 12, rowY + 24, 0xAAAAAA);
        }

        if (totalRules == 0) {
            context.drawCenteredTextWithShadow(textRenderer, "No resource rules. Click 'Add Resource' to begin.", width / 2, listY + 20, 0xAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String resourceSummary(ResourceRule rule) {
        int targets = rule.targetBlocks == null ? 0 : rule.targetBlocks.size();
        int resources = rule.resourceBlocks == null ? 0 : rule.resourceBlocks.size();
        String mode = rule.activationMode == null ? "REAL_TIME" : rule.activationMode.name();
        return "targets=" + targets + " resources=" + resources + " max=" + rule.maxActiveBlocks + " respawn=" + rule.respawnSeconds + "s chance=" + rule.chance + " " + mode;
    }
}
