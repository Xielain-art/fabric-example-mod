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
        int x = width / 2 - 220;
        int y = 36;

        addDrawableChild(ButtonWidget.builder(Text.literal("Add Companion"), b ->
                client.setScreen(new CompanionEditScreen(this, null, -1))).dimensions(x, y, 140, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            String duplicate = findDuplicateUid();
            if (duplicate != null) {
                error = "Duplicate companion uid64: " + duplicate + ". Companion uid64 values must be unique.";
                return;
            }
            error = "";
            parent.setCompanions(draft);
            client.setScreen(parent);
        }).dimensions(x + 300, y, 140, 20).build());

        y += 30;
        int rowH = 24;
        int maxVisible = Math.max(1, (height - 120) / rowH);
        pageSize = maxVisible;
        page = Math.max(0, Math.min(page, Math.max(0, (draft.size() - 1) / maxVisible)));

        if (draft.size() > maxVisible) {
            int maxPage = (draft.size() - 1) / maxVisible;
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); init(); }).dimensions(x + 220, 36, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(maxPage, page + 1); init(); }).dimensions(x + 374, 36, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + "/" + (maxPage + 1)), b -> {}).dimensions(x + 248, 36, 122, 20).build());
        }

        int start = page * maxVisible;
        for (int i = 0; i < maxVisible; i++) {
            int idxData = start + i;
            if (idxData >= draft.size()) {
                break;
            }
            CompanionRule c = draft.get(idxData);
            int rowY = y + i * 24;
            int idx = idxData;
            addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), b -> client.setScreen(new CompanionEditScreen(this, c, idx))).dimensions(x + 330, rowY, 50, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> { draft.remove(idx); init(); }).dimensions(x + 384, rowY, 56, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent)).dimensions(width / 2 - 60, height - 30, 120, 20).build());
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
        int x = width / 2 - 220;
        int y = 70;
        String titleLabel = ruleId == null || ruleId.isBlank() ? "(rule)" : ruleId;
        context.drawCenteredTextWithShadow(textRenderer, "Companions for: " + titleLabel, width / 2, 10, 0xFFFFFF);
        int rowH = 24;
        int maxVisible = Math.max(1, (height - 120) / rowH);
        int start = page * maxVisible;
        for (int i = 0; i < maxVisible; i++) {
            int idx = start + i;
            if (idx >= draft.size()) {
                break;
            }
            CompanionRule c = draft.get(idx);
            boolean dup = duplicateIds.contains(lowerId(c.uid64));
            String chance = c.chance >= 1.0 ? "100%" : ((int) Math.round(c.chance * 100)) + "%";
            String title = c.name == null || c.name.isBlank() ? c.id : c.name;
            context.drawTextWithShadow(textRenderer, title + " -> " + c.entity + " | count " + c.count + " | radius " + c.radius + " | chance " + chance, x, y, dup ? 0xFF7777 : 0xDDDDDD);
            y += 24;
        }
        if (draft.isEmpty()) {
            context.drawTextWithShadow(textRenderer, "No companions", x, y, 0xAAAAAA);
        }
        if (!error.isBlank()) {
            context.drawTextWithShadow(textRenderer, error, x, height - 46, 0xFF5555);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private String findDuplicateUid() {
        Map<String, Integer> counts = new HashMap<>();
        duplicateIds = new HashSet<>();
        for (CompanionRule c : draft) {
            String key = lowerId(c.uid64);
            if (key.isBlank()) {
                continue;
            }
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        String firstDuplicate = null;
        for (CompanionRule c : draft) {
            String key = lowerId(c.uid64);
            if (!key.isBlank() && counts.getOrDefault(key, 0) > 1) {
                duplicateIds.add(key);
                if (firstDuplicate == null) {
                    firstDuplicate = c.uid64;
                }
            }
        }
        return firstDuplicate;
    }

    private String lowerId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
