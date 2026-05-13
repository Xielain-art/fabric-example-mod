package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.model.CompanionRule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CompanionListScreen extends Screen {
    private final MobRuleEditScreen parent;
    private final String ruleId;
    private final List<CompanionRule> draft;
    private String error = "";
    private Set<String> duplicateIds = new HashSet<>();
    private int page = 0;
    private int pageSize = 1;

    public CompanionListScreen(MobRuleEditScreen parent, List<CompanionRule> companions, String ruleId) {
        super(Text.literal("Companions"));
        this.parent = parent;
        this.ruleId = ruleId;
        this.draft = companions == null ? new ArrayList<>() : new ArrayList<>(companions);
    }

    @Override
    protected void init() {
        clearChildren();

        int panelWidth = ScreenLayout.panelWidth(width, 560);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        int topY = 32;

        addDrawableChild(ButtonWidget.builder(Text.literal("Add Companion"), b -> client.setScreen(new CompanionEditScreen(this, null, -1)))
                .dimensions(startX, topY, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            String duplicate = findDuplicateId();
            if (duplicate != null) {
                error = "Duplicate companion id: " + duplicate + ". Companion ids must be unique.";
                return;
            }
            error = "";
            parent.setCompanions(draft);
            client.setScreen(parent);
        }).dimensions(startX + panelWidth - 150, topY, 150, 20).build());

        int rowH = 28;
        int listStartY = 84;
        int listEndY = height - 68;
        pageSize = Math.max(1, (listEndY - listStartY) / rowH);
        page = Math.max(0, Math.min(page, Math.max(0, (draft.size() - 1) / pageSize)));

        if (draft.size() > pageSize) {
            int maxPage = (draft.size() - 1) / pageSize;
            int pagY = listStartY + pageSize * rowH + 6;
            int pageBtnWidth = 100;
            int centerStartX = width / 2 - (pageBtnWidth + 56) / 2;

            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); init(); })
                    .dimensions(centerStartX, pagY, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + " / " + (maxPage + 1)), b -> {})
                    .dimensions(centerStartX + 28, pagY, pageBtnWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(maxPage, page + 1); init(); })
                    .dimensions(centerStartX + 32 + pageBtnWidth, pagY, 24, 20).build());
        }

        int start = page * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int idx = start + i;
            if (idx >= draft.size()) {
                break;
            }
            CompanionRule c = draft.get(idx);
            int rowY = listStartY + i * rowH;
            addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), b -> client.setScreen(new CompanionEditScreen(this, c, idx)))
                    .dimensions(startX + panelWidth - 116, rowY, 52, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> { draft.remove(idx); init(); })
                    .dimensions(startX + panelWidth - 60, rowY, 60, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent))
                .dimensions(width / 2 - 75, height - 35, 150, 20).build());
    }

    public void upsertCompanion(CompanionRule rule, int index) {
        if (index >= 0 && index < draft.size()) {
            draft.set(index, rule);
        } else {
            draft.add(rule);
        }
        error = "";
        init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelWidth = ScreenLayout.panelWidth(width, 560);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        ScreenLayout.drawPanel(context, startX, 15, panelWidth, height - 15);
        String titleLabel = ruleId == null || ruleId.isBlank() ? "(rule)" : ruleId;
        context.drawCenteredTextWithShadow(textRenderer, "Companions for: " + titleLabel, width / 2, 19, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "List / edit / validate companions", startX, 31, 0xA5FFB5);

        int rowH = 28;
        int listStartY = 84;
        int start = page * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int idx = start + i;
            if (idx >= draft.size()) {
                break;
            }
            CompanionRule c = draft.get(idx);
            boolean dup = duplicateIds.contains(lowerId(c.id));
            String chance = c.chance >= 1.0 ? "100%" : ((int) Math.round(c.chance * 100)) + "%";
            String title = c.name == null || c.name.isBlank() ? c.id : c.name;
            String text = title + " -> " + c.entity + " | count " + c.count + " | radius " + c.radius + " | chance " + chance;
            text = ScreenLayout.trim(textRenderer, text, panelWidth - 125);
            int rowY = listStartY + i * rowH;
            context.drawTextWithShadow(textRenderer, text, startX, rowY + 6, dup ? 0xFF7777 : 0xDDDDDD);
        }

        if (draft.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No companions added", width / 2, listStartY + 20, 0xAAAAAA);
        }

        if (!error.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2, height - 55, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String findDuplicateId() {
        Map<String, Integer> counts = new HashMap<>();
        duplicateIds = new HashSet<>();
        for (CompanionRule c : draft) {
            String key = lowerId(c.id);
            if (key.isBlank()) {
                continue;
            }
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        String firstDuplicate = null;
        for (CompanionRule c : draft) {
            String key = lowerId(c.id);
            if (!key.isBlank() && counts.getOrDefault(key, 0) > 1) {
                duplicateIds.add(key);
                if (firstDuplicate == null) {
                    firstDuplicate = c.id;
                }
            }
        }
        return firstDuplicate;
    }

    private String lowerId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
