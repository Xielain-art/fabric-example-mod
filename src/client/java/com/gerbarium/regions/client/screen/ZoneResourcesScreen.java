package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.ResourceRule;
import com.gerbarium.regions.model.WeightedBlock;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ZoneResourcesScreen extends Screen implements GerbariumRefreshableScreen {
    private final String zoneId;
    private int page = 0;
    private int pageSize = 0;
    private int totalRules = 0;
    private String query = "";
    private TextFieldWidget searchField;

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
        int topY = 40;

        // Search bar (like EntityPickerScreen)
        int gap = 4;
        int refreshWidth = 84;
        int searchButtonWidth = 66;
        int searchFieldWidth = panelWidth - refreshWidth - searchButtonWidth - gap * 2;

        searchField = new TextFieldWidget(textRenderer, startX, topY, searchFieldWidth, 20, Text.literal("Search"));
        searchField.setText(query);
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Search"), button -> {
                    query = searchField.getText();
                    page = 0;
                    init();
                })
                .dimensions(startX + searchFieldWidth + gap, topY, searchButtonWidth, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> GerbariumClientNetworking.requestZones())
                .dimensions(startX + searchFieldWidth + gap + searchButtonWidth + gap, topY, refreshWidth, 20)
                .build());

        // Filter resources
        List<ResourceRule> allResources = z.resources == null ? List.of() : z.resources;
        String q = query.toLowerCase(Locale.ROOT);
        List<ResourceRule> filtered = q.isBlank() ? allResources : allResources.stream().filter(r -> {
            if (r.id != null && r.id.toLowerCase(Locale.ROOT).contains(q)) return true;
            if (r.name != null && r.name.toLowerCase(Locale.ROOT).contains(q)) return true;
            if (r.targetBlocks != null) for (String tb : r.targetBlocks) if (tb.toLowerCase(Locale.ROOT).contains(q)) return true;
            if (r.resourceBlocks != null) for (WeightedBlock rb : r.resourceBlocks) if (rb.block != null && rb.block.toLowerCase(Locale.ROOT).contains(q)) return true;
            return false;
        }).toList();

        // Add Resource button
        int actionY = topY + 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("Add Resource"), b -> {
            client.setScreen(new ResourceRuleEditScreen(zoneId, null, this));
        }).dimensions(startX + panelWidth - 120, actionY, 110, 20).build());

        // List
        int listY = actionY + 28;
        int rowH = 52;
        int bottomSpace = 90;
        int visible = Math.max(1, (height - listY - bottomSpace) / rowH);

        int total = filtered.size();
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
            if (idx >= total) break;
            ResourceRule r = filtered.get(idx);
            int rowY = listY + i * rowH;
            addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), b -> client.setScreen(new ResourceRuleEditScreen(zoneId, r, this)))
                    .dimensions(startX + panelWidth - 108, rowY + 14, 50, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Del"), b -> GerbariumClientNetworking.removeResourceRule(zoneId, r.id))
                    .dimensions(startX + panelWidth - 54, rowY + 14, 42, 20).build());
        }

        int footerY = hasPagination ? listY + visible * rowH + 34 : height - 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new ZoneDetailsScreen(zoneId)))
                .dimensions(width / 2 - 60, footerY, 120, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelWidth = ScreenLayout.panelWidth(width, 680);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        ScreenTheme.drawPanel(context, startX, 15, panelWidth, height - 15);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 23, ScreenTheme.ACCENT_PRIMARY);

        Optional<Zone> oz = ClientGerbariumData.findZone(zoneId);
        if (oz.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "Zone not found", width / 2, height / 2 - 10, ScreenTheme.ERROR);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        Zone z = oz.get();
        ZoneDefaults.normalizeZone(z);

        // Filter resources for display
        List<ResourceRule> allResources = z.resources == null ? List.of() : z.resources;
        String q = query.toLowerCase(Locale.ROOT);
        List<ResourceRule> filtered = q.isBlank() ? allResources : allResources.stream().filter(r -> {
            if (r.id != null && r.id.toLowerCase(Locale.ROOT).contains(q)) return true;
            if (r.name != null && r.name.toLowerCase(Locale.ROOT).contains(q)) return true;
            if (r.targetBlocks != null) for (String tb : r.targetBlocks) if (tb.toLowerCase(Locale.ROOT).contains(q)) return true;
            if (r.resourceBlocks != null) for (WeightedBlock rb : r.resourceBlocks) if (rb.block != null && rb.block.toLowerCase(Locale.ROOT).contains(q)) return true;
            return false;
        }).toList();

        int actionY = 40 + 24;
        context.drawTextWithShadow(textRenderer, "Zone: " + z.id + " | Resource Rules (" + filtered.size() + ")", startX + 12, actionY + 4, ScreenTheme.ACCENT_PRIMARY);

        int listY = actionY + 28;
        int rowH = 52;
        int visible = Math.max(1, (height - listY - 90) / rowH);
        int start = page * visible;

        for (int i = 0; i < visible; i++) {
            int idx = start + i;
            if (idx >= filtered.size()) break;
            ResourceRule r = filtered.get(idx);
            int rowY = listY + i * rowH;
            context.fill(startX + 4, rowY, startX + panelWidth - 4, rowY + rowH - 4, 0x33000000);
            String titleText = ScreenLayout.trim(textRenderer, (r.name == null || r.name.isBlank() ? r.id : r.name) + " [" + (r.enabled ? "ON" : "OFF") + "]", panelWidth - 124);
            context.drawTextWithShadow(textRenderer, titleText, startX + 12, rowY + 8, ScreenTheme.TEXT_PRIMARY);
            context.drawTextWithShadow(textRenderer, resourceSummary(r), startX + 12, rowY + 24, ScreenTheme.TEXT_MUTED);
        }

        if (totalRules == 0) {
            String msg = query.isBlank() ? "No resource rules. Click 'Add Resource' to begin." : "No results for '" + query + "'.";
            context.drawCenteredTextWithShadow(textRenderer, msg, width / 2, listY + 20, ScreenTheme.TEXT_MUTED);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String resourceSummary(ResourceRule rule) {
        int targets = rule.targetBlocks == null ? 0 : rule.targetBlocks.size();
        int resources = rule.resourceBlocks == null ? 0 : rule.resourceBlocks.size();
        String mode = rule.activationMode == null ? "REAL_TIME" : rule.activationMode.name();
        return "targets=" + targets + " resources=" + resources + " max=" + rule.maxActiveBlocks + " respawn=" + rule.respawnSeconds + "s chance=" + rule.chance + " " + mode;
    }

    @Override
    public void refreshFromSync() {
        init();
    }
}
