package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.ReplaceMode;
import com.gerbarium.regions.model.ResourceActivationMode;
import com.gerbarium.regions.model.ResourceRule;
import com.gerbarium.regions.model.RestoreMode;
import com.gerbarium.regions.model.WeightedBlock;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ResourceRuleEditScreen extends Screen {
    private static final int PAGE_COUNT = 4;

    private final String zoneId;
    private final ResourceRule draft;
    private final Screen parent;
    private String error = "";
    private int page = 0;
    private final boolean isEditing;

    private TextFieldWidget idField;
    private TextFieldWidget nameField;
    private CyclingButtonWidget<Boolean> enabledButton;
    private CyclingButtonWidget<ResourceActivationMode> activationModeButton;

    private TextFieldWidget addTargetField;
    private TextFieldWidget addResourceBlockField;
    private TextFieldWidget addResourceWeightField;

    private TextFieldWidget maxActiveField;
    private TextFieldWidget spawnCountField;
    private TextFieldWidget respawnField;
    private TextFieldWidget chanceField;
    private TextFieldWidget minYField;
    private TextFieldWidget maxYField;
    private CyclingButtonWidget<ReplaceMode> replaceModeButton;
    private CyclingButtonWidget<RestoreMode> restoreModeButton;

    private TextFieldWidget restoreDelayField;
    private TextFieldWidget maxAttemptsField;
    private CyclingButtonWidget<Boolean> requireLoadedButton;
    private CyclingButtonWidget<Boolean> respectProtectedButton;
    private CyclingButtonWidget<Boolean> dropOriginalButton;
    private CyclingButtonWidget<Boolean> restoreIfNotMinedButton;
    private CyclingButtonWidget<Boolean> preventPlayerPlacedButton;
    private CyclingButtonWidget<Boolean> allowBlockEntitiesButton;

    public ResourceRuleEditScreen(String zoneId, ResourceRule existingRule, Screen parent) {
        super(Text.literal(existingRule == null ? "Add Resource Rule" : "Edit Resource Rule"));
        this.zoneId = zoneId;
        this.parent = parent;
        this.isEditing = existingRule != null;
        if (existingRule == null) {
            this.draft = ResourceRule.defaults(ResourceRule.generateId(), "New Resource");
        } else {
            this.draft = cloneRule(existingRule);
        }
        ZoneDefaults.normalizeResourceRule(this.draft);
    }

    @Override
    protected void init() {
        clearChildren();
        page = Math.max(0, Math.min(page, PAGE_COUNT - 1));

        int panelWidth = ScreenLayout.panelWidth(width, 700);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        int topY = 56;

        addPageNav(startX, panelWidth);

        if (page == 0) {
            addBasicsPage(startX, topY, panelWidth);
        } else if (page == 1) {
            addTargetsPage(startX, topY, panelWidth);
        } else if (page == 2) {
            addLimitsPage(startX, topY, panelWidth);
        } else if (page == 3) {
            addSafetyPage(startX, topY, panelWidth);
        }

        int footerY = height - 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(startX, footerY, panelWidth / 2 - 4, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent))
                .dimensions(startX + panelWidth / 2 + 4, footerY, panelWidth / 2 - 4, 20).build());
    }

    private void addPageNav(int startX, int panelWidth) {
        int pagY = 24;
        int buttonW = 72;
        int labelW = 120;
        int totalW = buttonW * 2 + labelW + 24;
        int pagX = startX + panelWidth - totalW;

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { capture(); page = Math.max(0, page - 1); init(); })
                .dimensions(pagX, pagY, buttonW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(pageLabel()), b -> {})
                .dimensions(pagX + buttonW + 4, pagY, labelW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { capture(); page = Math.min(PAGE_COUNT - 1, page + 1); init(); })
                .dimensions(pagX + buttonW + 4 + labelW + 4, pagY, buttonW, 20).build());
    }

    private String pageLabel() {
        return switch (page) {
            case 0 -> "Basics 1/4";
            case 1 -> "Targets 2/4";
            case 2 -> "Limits 3/4";
            default -> "Safety 4/4";
        };
    }

    private void addBasicsPage(int startX, int topY, int panelWidth) {
        int rowH = 40;

        idField = field(startX, topY, panelWidth, draft.id == null ? "" : draft.id);
        idField.setEditable(false);
        addDrawableChild(idField);

        nameField = field(startX, topY + rowH, panelWidth, draft.name == null ? "" : draft.name);
        addDrawableChild(nameField);

        enabledButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.enabled)
                .build(startX, topY + rowH * 2, 140, 20, Text.literal("Enabled"), (b, v) -> {}));

        activationModeButton = addDrawableChild(CyclingButtonWidget.<ResourceActivationMode>builder(v -> Text.literal(v.name()))
                .values(List.of(ResourceActivationMode.REAL_TIME, ResourceActivationMode.WHILE_ZONE_ACTIVE))
                .initially(draft.activationMode)
                .build(startX, topY + rowH * 3, 240, 20, Text.literal("Activation Mode"), (b, v) -> {}));
    }

    private void addTargetsPage(int startX, int topY, int panelWidth) {
        int half = (panelWidth - 10) / 2;
        int rowH = 28;

        addTargetField = field(startX, topY, half, "minecraft:stone");
        addDrawableChild(addTargetField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Add Target"), b -> {
            String blockId = addTargetField.getText().trim();
            if (!blockId.isEmpty() && blockId.contains(":")) {
                draft.targetBlocks.add(blockId);
                addTargetField.setText("");
                error = "";
            } else {
                error = "Block ID must be namespace:path format";
            }
        }).dimensions(startX + half + 10, topY, half, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Remove Last Target"), b -> {
            if (!draft.targetBlocks.isEmpty()) {
                draft.targetBlocks.remove(draft.targetBlocks.size() - 1);
            }
        }).dimensions(startX, topY + rowH, panelWidth, 20).build());

        int resY = topY + rowH * 2 + 16;
        addResourceBlockField = field(startX, resY, half, "minecraft:diamond_ore");
        addDrawableChild(addResourceBlockField);
        addResourceWeightField = field(startX + half + 10, resY, 60, "1");
        addDrawableChild(addResourceWeightField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> {
            String blockId = addResourceBlockField.getText().trim();
            int weight = parseInt(addResourceWeightField, 1);
            if (!blockId.isEmpty() && blockId.contains(":") && weight > 0) {
                WeightedBlock wb = new WeightedBlock();
                wb.block = blockId;
                wb.weight = weight;
                draft.resourceBlocks.add(wb);
                addResourceBlockField.setText("");
                addResourceWeightField.setText("1");
                error = "";
            } else {
                error = "Block ID must be namespace:path, weight > 0";
            }
        }).dimensions(startX + half + 76, resY, panelWidth - half - 76, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Remove Last Resource"), b -> {
            if (!draft.resourceBlocks.isEmpty()) {
                draft.resourceBlocks.remove(draft.resourceBlocks.size() - 1);
            }
        }).dimensions(startX, resY + rowH, panelWidth, 20).build());
    }

    private void addLimitsPage(int startX, int topY, int panelWidth) {
        int half = (panelWidth - 10) / 2;
        int rowH = 36;

        maxActiveField = field(startX, topY, half, String.valueOf(draft.maxActiveBlocks));
        spawnCountField = field(startX + half + 10, topY, half, String.valueOf(draft.spawnCount));
        addDrawableChild(maxActiveField);
        addDrawableChild(spawnCountField);

        respawnField = field(startX, topY + rowH, half, String.valueOf(draft.respawnSeconds));
        chanceField = field(startX + half + 10, topY + rowH, half, String.valueOf(draft.chance));
        addDrawableChild(respawnField);
        addDrawableChild(chanceField);

        minYField = field(startX, topY + rowH * 2, half, String.valueOf(draft.minY));
        maxYField = field(startX + half + 10, topY + rowH * 2, half, String.valueOf(draft.maxY));
        addDrawableChild(minYField);
        addDrawableChild(maxYField);

        replaceModeButton = addDrawableChild(CyclingButtonWidget.<ReplaceMode>builder(v -> Text.literal(v.name()))
                .values(List.of(ReplaceMode.ONLY_TARGET_BLOCKS))
                .initially(draft.replaceMode)
                .build(startX, topY + rowH * 3, 240, 20, Text.literal("Replace Mode"), (b, v) -> {}));

        restoreModeButton = addDrawableChild(CyclingButtonWidget.<RestoreMode>builder(v -> Text.literal(v.name()))
                .values(List.of(RestoreMode.RESTORE_ORIGINAL))
                .initially(draft.restoreMode)
                .build(startX, topY + rowH * 4, 240, 20, Text.literal("Restore Mode"), (b, v) -> {}));
    }

    private void addSafetyPage(int startX, int topY, int panelWidth) {
        int rowH = 32;

        restoreDelayField = field(startX, topY, 180, String.valueOf(draft.restoreDelaySeconds));
        maxAttemptsField = field(startX + 200, topY, 180, String.valueOf(draft.maxPositionAttempts));
        addDrawableChild(restoreDelayField);
        addDrawableChild(maxAttemptsField);

        int toggleY = topY + rowH + 8;
        requireLoadedButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.requireLoadedChunk)
                .build(startX, toggleY, 220, 20, Text.literal("Require Loaded Chunk"), (b, v) -> {}));
        respectProtectedButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.respectProtectedBlocks)
                .build(startX, toggleY + rowH, 220, 20, Text.literal("Respect Protected Blocks"), (b, v) -> {}));
        dropOriginalButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.dropOriginalBlockOnReplace)
                .build(startX, toggleY + rowH * 2, 220, 20, Text.literal("Drop Original On Replace"), (b, v) -> {}));
        restoreIfNotMinedButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.restoreIfNotMined)
                .build(startX, toggleY + rowH * 3, 220, 20, Text.literal("Restore If Not Mined"), (b, v) -> {}));
        preventPlayerPlacedButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.preventPlayerPlacedBlocks)
                .build(startX, toggleY + rowH * 4, 220, 20, Text.literal("Prevent Player Placed Blocks"), (b, v) -> {}));
        allowBlockEntitiesButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.allowBlockEntities)
                .build(startX, toggleY + rowH * 5, 220, 20, Text.literal("Allow Block Entities"), (b, v) -> {}));
    }

    private TextFieldWidget field(int x, int y, int w, String value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        f.setText(value == null ? "" : value);
        return f;
    }

    private int parseInt(TextFieldWidget f, int fallback) {
        try { return Integer.parseInt(f.getText().trim()); }
        catch (Exception e) { return fallback; }
    }

    private double parseDouble(TextFieldWidget f, double fallback) {
        try { return Double.parseDouble(f.getText().trim()); }
        catch (Exception e) { return fallback; }
    }

    private void capture() {
        if (idField != null) draft.id = idField.getText().trim();
        if (nameField != null) draft.name = nameField.getText().trim();
        if (enabledButton != null) draft.enabled = enabledButton.getValue();
        if (activationModeButton != null) draft.activationMode = activationModeButton.getValue();

        if (maxActiveField != null) draft.maxActiveBlocks = parseInt(maxActiveField, draft.maxActiveBlocks);
        if (spawnCountField != null) draft.spawnCount = parseInt(spawnCountField, draft.spawnCount);
        if (respawnField != null) draft.respawnSeconds = parseInt(respawnField, draft.respawnSeconds);
        if (chanceField != null) draft.chance = parseDouble(chanceField, draft.chance);
        if (minYField != null) draft.minY = parseInt(minYField, draft.minY);
        if (maxYField != null) draft.maxY = parseInt(maxYField, draft.maxY);
        if (replaceModeButton != null) draft.replaceMode = replaceModeButton.getValue();
        if (restoreModeButton != null) draft.restoreMode = restoreModeButton.getValue();

        if (restoreDelayField != null) draft.restoreDelaySeconds = parseInt(restoreDelayField, draft.restoreDelaySeconds);
        if (maxAttemptsField != null) draft.maxPositionAttempts = parseInt(maxAttemptsField, draft.maxPositionAttempts);
        if (requireLoadedButton != null) draft.requireLoadedChunk = requireLoadedButton.getValue();
        if (respectProtectedButton != null) draft.respectProtectedBlocks = respectProtectedButton.getValue();
        if (dropOriginalButton != null) draft.dropOriginalBlockOnReplace = dropOriginalButton.getValue();
        if (restoreIfNotMinedButton != null) draft.restoreIfNotMined = restoreIfNotMinedButton.getValue();
        if (preventPlayerPlacedButton != null) draft.preventPlayerPlacedBlocks = preventPlayerPlacedButton.getValue();
        if (allowBlockEntitiesButton != null) draft.allowBlockEntities = allowBlockEntitiesButton.getValue();
    }

    private void save() {
        capture();
        try {
            ZoneDefaults.normalizeResourceRule(draft);
            ZoneDefaults.validateResourceRule(draft);
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
            return;
        }
        if (isEditing) {
            GerbariumClientNetworking.updateResourceRule(zoneId, draft);
        } else {
            GerbariumClientNetworking.addResourceRule(zoneId, draft);
        }
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelWidth = ScreenLayout.panelWidth(width, 700);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        ScreenLayout.drawPanel(context, startX, 15, panelWidth, height - 15);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);

        context.drawTextWithShadow(textRenderer, pageLabel(), startX, 31, 0xA5FFB5);

        if (page == 0) {
            context.drawTextWithShadow(textRenderer, "Rule ID (read-only)", startX, 45, 0x888888);
            context.drawTextWithShadow(textRenderer, "Name", startX, 85, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Enabled", startX, 125, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Activation Mode", startX + 150, 125, 0xA5FFB5);
        } else if (page == 1) {
            context.drawTextWithShadow(textRenderer, "Target Blocks", startX, 45, 0xA5FFB5);
            String targets = draft.targetBlocks.isEmpty() ? "(none)" : String.join(", ", draft.targetBlocks);
            ScreenLayout.drawWrapped(context, textRenderer, "Current: " + targets, startX, 106, panelWidth, 0xCCCCCC);
            context.drawTextWithShadow(textRenderer, "Resource Blocks (block + weight)", startX, 140, 0xA5FFB5);
            StringBuilder sb = new StringBuilder();
            for (WeightedBlock wb : draft.resourceBlocks) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(wb.block).append(" x").append(wb.weight);
            }
            ScreenLayout.drawWrapped(context, textRenderer, "Current: " + (sb.length() == 0 ? "(none)" : sb.toString()), startX, 202, panelWidth, 0xCCCCCC);
        } else if (page == 2) {
            int half = (panelWidth - 10) / 2;
            context.drawTextWithShadow(textRenderer, "Max Active Blocks", startX, 45, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Spawn Count", startX + half + 10, 45, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Respawn Seconds", startX, 81, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Chance (0..1)", startX + half + 10, 81, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Min Y", startX, 117, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Max Y", startX + half + 10, 117, 0xA5FFB5);
        } else if (page == 3) {
            context.drawTextWithShadow(textRenderer, "Restore Delay Seconds", startX, 45, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Max Position Attempts", startX + 200, 45, 0xA5FFB5);
        }

        if (!error.isBlank()) {
            ScreenLayout.drawWrapped(context, textRenderer, error, startX, height - 64, panelWidth, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private static ResourceRule cloneRule(ResourceRule src) {
        ResourceRule r = new ResourceRule();
        r.id = src.id;
        r.name = src.name;
        r.enabled = src.enabled;
        r.targetBlocks = src.targetBlocks == null ? new ArrayList<>() : new ArrayList<>(src.targetBlocks);
        r.resourceBlocks = src.resourceBlocks == null ? new ArrayList<>() : new ArrayList<>();
        if (src.resourceBlocks != null) {
            for (WeightedBlock wb : src.resourceBlocks) {
                WeightedBlock copy = new WeightedBlock();
                copy.block = wb.block;
                copy.weight = wb.weight;
                r.resourceBlocks.add(copy);
            }
        }
        r.maxActiveBlocks = src.maxActiveBlocks;
        r.spawnCount = src.spawnCount;
        r.respawnSeconds = src.respawnSeconds;
        r.chance = src.chance;
        r.minY = src.minY;
        r.maxY = src.maxY;
        r.replaceMode = src.replaceMode;
        r.restoreMode = src.restoreMode;
        r.activationMode = src.activationMode;
        r.restoreDelaySeconds = src.restoreDelaySeconds;
        r.maxPositionAttempts = src.maxPositionAttempts;
        r.requireLoadedChunk = src.requireLoadedChunk;
        r.respectProtectedBlocks = src.respectProtectedBlocks;
        r.dropOriginalBlockOnReplace = src.dropOriginalBlockOnReplace;
        r.restoreIfNotMined = src.restoreIfNotMined;
        r.preventPlayerPlacedBlocks = src.preventPlayerPlacedBlocks;
        r.allowBlockEntities = src.allowBlockEntities;
        return r;
    }
}
