package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.client.screen.help.HelpTopic;
import com.gerbarium.regions.model.PlacementMode;
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

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ResourceRuleEditScreen extends GerbariumScreen implements BlockSelectionConsumer {
    private static final int PAGE_COUNT = 6;

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
    private String targetInput = "minecraft:stone";
    private String resourceInput = "minecraft:diamond_ore";
    private String resourceWeightInput = "1";

    private TextFieldWidget maxActiveField;
    private TextFieldWidget spawnCountField;
    private TextFieldWidget respawnField;
    private TextFieldWidget chanceField;
    private TextFieldWidget minYField;
    private TextFieldWidget maxYField;
    private TextFieldWidget minDistanceField;
    private CyclingButtonWidget<PlacementMode> placementModeButton;

    private TextFieldWidget restoreDelayField;
    private TextFieldWidget maxAttemptsField;
    private CyclingButtonWidget<ReplaceMode> replaceModeButton;
    private CyclingButtonWidget<RestoreMode> restoreModeButton;
    private CyclingButtonWidget<Boolean> dropOriginalButton;
    private CyclingButtonWidget<Boolean> restoreIfNotMinedButton;

    private CyclingButtonWidget<Boolean> requireLoadedButton;
    private CyclingButtonWidget<Boolean> respectProtectedButton;
    private CyclingButtonWidget<Boolean> preventPlayerPlacedButton;
    private CyclingButtonWidget<Boolean> allowBlockEntitiesButton;
    private BlockPickTarget blockPickTarget = BlockPickTarget.RESOURCE;

    public ResourceRuleEditScreen(String zoneId, ResourceRule existingRule, Screen parent) {
        super(Text.literal(existingRule == null ? "Add Resource Rule" : "Edit Resource Rule"), 700, null);
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
        page = Math.max(0, Math.min(page, PAGE_COUNT - 1));
        updateHelpTopic();
        super.init();
    }

    private void updateHelpTopic() {
        HelpTopic topic = switch (page) {
            case 0 -> HelpTopic.RESOURCE_RULE_BASICS;
            case 1 -> HelpTopic.RESOURCE_RULE_BLOCKS;
            case 2 -> HelpTopic.RESOURCE_RULE_BLOCKS;
            case 3 -> HelpTopic.RESOURCE_RULE_LIMITS;
            case 4 -> HelpTopic.RESOURCE_RULE_SAFETY;
            default -> HelpTopic.RESOURCE_RULE_SAFETY;
        };
        setHelpTopic(topic);
    }

    @Override
    protected void initHeader() {
        int pagY = panelY + 14;
        int buttonW = 64;
        int labelW = 100;
        int totalW = buttonW * 2 + labelW + 16;
        int pagX = panelX + panelW - totalW - (helpTopic != null ? 28 : 0);

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { capture(); page = Math.max(0, page - 1); init(); })
                .dimensions(pagX, pagY, buttonW, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(pageLabel()), b -> {})
                .dimensions(pagX + buttonW + 4, pagY, labelW, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { capture(); page = Math.min(PAGE_COUNT - 1, page + 1); init(); })
                .dimensions(pagX + buttonW + 4 + labelW + 4, pagY, buttonW, 18).build());

        super.initHeader();
    }

    @Override
    protected void initContent() {
        int topY = contentY;

        if (page == 0) {
            addBasicsPage(contentX, topY);
        } else if (page == 1) {
            addTargetBlocksPage(contentX, topY);
        } else if (page == 2) {
            addResourceBlocksPage(contentX, topY);
        } else if (page == 3) {
            addLimitsPage(contentX, topY);
        } else if (page == 4) {
            addModesPage(contentX, topY);
        } else if (page == 5) {
            addSafetyPage(contentX, topY);
        }
    }

    // Page 0: Basics — 4 rows
    private void addBasicsPage(int startX, int topY) {
        int rowH = layoutRows(4);

        idField = field(startX, topY, contentW, draft.id == null ? "" : draft.id);
        idField.setEditable(false);
        addDrawableChild(idField);

        nameField = field(startX, topY + rowH, contentW, draft.name == null ? "" : draft.name);
        addDrawableChild(nameField);

        enabledButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.enabled)
                .build(startX, topY + rowH * 2, Math.max(140, contentW / 2 - 4), 20, Text.literal("Enabled"), (b, v) -> {}));

        activationModeButton = addDrawableChild(CyclingButtonWidget.<ResourceActivationMode>builder(v -> Text.literal(v.name()))
                .values(List.of(ResourceActivationMode.REAL_TIME, ResourceActivationMode.WHILE_ZONE_ACTIVE))
                .initially(draft.activationMode)
                .build(startX + Math.max(144, contentW / 2), topY + rowH * 2, Math.max(140, contentW / 2 - 4), 20, Text.literal("Activation Mode"), (b, v) -> {}));
    }

    // Page 1: Target Blocks — 3 rows
    private void addTargetBlocksPage(int startX, int topY) {
        int rowH = layoutRows(3);
        int fieldW = Math.max(160, contentW - 172);

        addTargetField = field(startX, topY, fieldW, targetInput);
        addDrawableChild(addTargetField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Pick"), b -> {
            capture();
            blockPickTarget = BlockPickTarget.TARGET;
            client.setScreen(new BlockPickerScreen(this, this, addTargetField.getText()));
        }).dimensions(startX + fieldW + 6, topY, 54, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Add Target"), b -> {
            String blockId = addTargetField.getText().trim();
            if (!blockId.isEmpty() && blockId.contains(":")) {
                draft.targetBlocks.add(blockId);
                targetInput = "";
                addTargetField.setText(targetInput);
                error = "";
            } else {
                error = "Block ID must be namespace:path format";
            }
        }).dimensions(startX + fieldW + 66, topY, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Remove Last Target"), b -> {
            if (!draft.targetBlocks.isEmpty()) {
                draft.targetBlocks.remove(draft.targetBlocks.size() - 1);
            }
        }).dimensions(startX, topY + rowH, contentW, 20).build());
    }

    // Page 2: Resource Blocks — 3 rows
    private void addResourceBlocksPage(int startX, int topY) {
        int rowH = layoutRows(3);
        int blockFieldW = Math.max(160, contentW - 224);

        addResourceBlockField = field(startX, topY, blockFieldW, resourceInput);
        addDrawableChild(addResourceBlockField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Pick"), b -> {
            capture();
            blockPickTarget = BlockPickTarget.RESOURCE;
            client.setScreen(new BlockPickerScreen(this, this, addResourceBlockField.getText()));
        }).dimensions(startX + blockFieldW + 6, topY, 54, 20).build());
        addResourceWeightField = field(startX + blockFieldW + 66, topY, 50, resourceWeightInput);
        addDrawableChild(addResourceWeightField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> {
            String blockId = addResourceBlockField.getText().trim();
            int weight = parseInt(addResourceWeightField, 1);
            if (!blockId.isEmpty() && blockId.contains(":") && weight > 0) {
                WeightedBlock wb = new WeightedBlock();
                wb.block = blockId;
                wb.weight = weight;
                draft.resourceBlocks.add(wb);
                resourceInput = "";
                addResourceBlockField.setText(resourceInput);
                resourceWeightInput = "1";
                addResourceWeightField.setText(resourceWeightInput);
                error = "";
            } else {
                error = "Block ID must be namespace:path, weight > 0";
            }
        }).dimensions(startX + blockFieldW + 122, topY, contentW - blockFieldW - 122, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Remove Last Resource"), b -> {
            if (!draft.resourceBlocks.isEmpty()) {
                draft.resourceBlocks.remove(draft.resourceBlocks.size() - 1);
            }
        }).dimensions(startX, topY + rowH, contentW, 20).build());
    }

    // Page 3: Limits — 5 rows
    private void addLimitsPage(int startX, int topY) {
        int rowH = layoutRows(5);
        int half = Math.max(110, (contentW - 10) / 2);

        maxActiveField = field(startX, topY, half, String.valueOf(draft.maxActiveBlocks));
        spawnCountField = field(startX + half + 10, topY, half, String.valueOf(draft.spawnCount));
        addDrawableChild(maxActiveField);
        addDrawableChild(spawnCountField);

        respawnField = field(startX, topY + rowH, half, String.valueOf(draft.respawnSeconds));
        chanceField = field(startX + half + 10, topY + rowH, half, String.valueOf(draft.chance));
        addDrawableChild(respawnField);
        addDrawableChild(chanceField);

        minYField = field(startX, topY + rowH * 2, half, draft.minY == null ? "" : String.valueOf(draft.minY));
        maxYField = field(startX + half + 10, topY + rowH * 2, half, draft.maxY == null ? "" : String.valueOf(draft.maxY));
        addDrawableChild(minYField);
        addDrawableChild(maxYField);

        minDistanceField = field(startX, topY + rowH * 3, half, String.valueOf(draft.minDistanceBetweenResources));
        addDrawableChild(minDistanceField);

        placementModeButton = addDrawableChild(CyclingButtonWidget.<PlacementMode>builder(v -> Text.literal(v.name()))
                .values(List.of(PlacementMode.RANDOM_SCATTER))
                .initially(draft.placementMode)
                .build(startX + half + 10, topY + rowH * 3, half, 20, Text.literal("Placement Mode"), (b, v) -> {}));
    }

    // Page 4: Modes — 5 rows
    private void addModesPage(int startX, int topY) {
        int rowH = layoutRows(5);
        int half = Math.max(110, (contentW - 10) / 2);

        replaceModeButton = addDrawableChild(CyclingButtonWidget.<ReplaceMode>builder(v -> Text.literal(v.name()))
                .values(List.of(ReplaceMode.ONLY_TARGET_BLOCKS, ReplaceMode.AIR_OR_REPLACEABLE, ReplaceMode.TARGET_BLOCKS_OR_AIR))
                .initially(draft.replaceMode)
                .build(startX, topY, contentW, 20, Text.literal("Replace Mode"), (b, v) -> {}));

        restoreModeButton = addDrawableChild(CyclingButtonWidget.<RestoreMode>builder(v -> Text.literal(v.name()))
                .values(List.of(RestoreMode.RESTORE_ORIGINAL))
                .initially(draft.restoreMode)
                .build(startX, topY + rowH, contentW, 20, Text.literal("Restore Mode"), (b, v) -> {}));

        restoreDelayField = field(startX, topY + rowH * 2, half, String.valueOf(draft.restoreDelaySeconds));
        maxAttemptsField = field(startX + half + 10, topY + rowH * 2, half, String.valueOf(draft.maxPositionAttempts));
        addDrawableChild(restoreDelayField);
        addDrawableChild(maxAttemptsField);

        dropOriginalButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.dropOriginalBlockOnReplace)
                .build(startX, topY + rowH * 3, half, 20, Text.literal("Drop Original On Replace"), (b, v) -> {}));
        restoreIfNotMinedButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.restoreIfNotMined)
                .build(startX + half + 10, topY + rowH * 3, half, 20, Text.literal("Restore If Not Mined"), (b, v) -> {}));
    }

    // Page 5: Safety — 4 rows
    private void addSafetyPage(int startX, int topY) {
        int rowH = layoutRows(4);
        int half = Math.max(110, (contentW - 10) / 2);

        requireLoadedButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.requireLoadedChunk)
                .build(startX, topY, half, 20, Text.literal("Require Loaded Chunk"), (b, v) -> {}));
        respectProtectedButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.respectProtectedBlocks)
                .build(startX + half + 10, topY, half, 20, Text.literal("Respect Protected Blocks"), (b, v) -> {}));

        preventPlayerPlacedButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.preventPlayerPlacedBlocks)
                .build(startX, topY + rowH, half, 20, Text.literal("Prevent Player Placed Blocks"), (b, v) -> {}));
        allowBlockEntitiesButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.allowBlockEntities)
                .build(startX + half + 10, topY + rowH, half, 20, Text.literal("Allow Block Entities"), (b, v) -> {}));
    }

    private int layoutRows(int requiredRows) {
        int available = footerY - contentY - 20;
        int preferred = height < 220 ? 26 : 32;
        if (requiredRows <= 0) return preferred;
        int calculated = available / requiredRows;
        return Math.max(24, Math.min(preferred, calculated));
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

    private Integer parseNullableInt(TextFieldWidget f, Integer fallback) {
        String value = f.getText().trim();
        if (value.isEmpty()) return null;
        try { return Integer.parseInt(value); }
        catch (Exception e) { return fallback; }
    }

    private double parseDouble(TextFieldWidget f, double fallback) {
        try { return Double.parseDouble(f.getText().trim()); }
        catch (Exception e) { return fallback; }
    }

    private String pageLabel() {
        return switch (page) {
            case 0 -> "Basics 1/6";
            case 1 -> "Targets 2/6";
            case 2 -> "Resources 3/6";
            case 3 -> "Limits 4/6";
            case 4 -> "Modes 5/6";
            default -> "Safety 6/6";
        };
    }

    private void capture() {
        if (idField != null) draft.id = idField.getText().trim();
        if (nameField != null) draft.name = nameField.getText().trim();
        if (enabledButton != null) draft.enabled = enabledButton.getValue();
        if (activationModeButton != null) draft.activationMode = activationModeButton.getValue();
        if (addTargetField != null) targetInput = addTargetField.getText();
        if (addResourceBlockField != null) resourceInput = addResourceBlockField.getText();
        if (addResourceWeightField != null) resourceWeightInput = addResourceWeightField.getText();

        if (maxActiveField != null) draft.maxActiveBlocks = parseInt(maxActiveField, draft.maxActiveBlocks);
        if (spawnCountField != null) draft.spawnCount = parseInt(spawnCountField, draft.spawnCount);
        if (respawnField != null) draft.respawnSeconds = parseInt(respawnField, draft.respawnSeconds);
        if (chanceField != null) draft.chance = parseDouble(chanceField, draft.chance);
        if (minYField != null) draft.minY = parseNullableInt(minYField, draft.minY);
        if (maxYField != null) draft.maxY = parseNullableInt(maxYField, draft.maxY);
        if (replaceModeButton != null) draft.replaceMode = replaceModeButton.getValue();
        if (restoreModeButton != null) draft.restoreMode = restoreModeButton.getValue();
        if (placementModeButton != null) draft.placementMode = placementModeButton.getValue();
        if (minDistanceField != null) draft.minDistanceBetweenResources = parseInt(minDistanceField, draft.minDistanceBetweenResources);

        if (restoreDelayField != null) draft.restoreDelaySeconds = parseInt(restoreDelayField, draft.restoreDelaySeconds);
        if (maxAttemptsField != null) draft.maxPositionAttempts = parseInt(maxAttemptsField, draft.maxPositionAttempts);
        if (dropOriginalButton != null) draft.dropOriginalBlockOnReplace = dropOriginalButton.getValue();
        if (restoreIfNotMinedButton != null) draft.restoreIfNotMined = restoreIfNotMinedButton.getValue();
        if (requireLoadedButton != null) draft.requireLoadedChunk = requireLoadedButton.getValue();
        if (respectProtectedButton != null) draft.respectProtectedBlocks = respectProtectedButton.getValue();
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
    protected void initFooter() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(contentX, footerY, contentW / 2 - 4, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent))
                .dimensions(contentX + contentW / 2 + 4, footerY, contentW / 2 - 4, 20).build());
    }

    @Override
    public void onBlockSelected(String blockId) {
        if (blockPickTarget == BlockPickTarget.TARGET) {
            targetInput = blockId;
            if (addTargetField != null) addTargetField.setText(blockId);
            return;
        }
        resourceInput = blockId;
        if (addResourceBlockField != null) addResourceBlockField.setText(blockId);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (page == 0) {
            int rowH = layoutRows(4);
            context.drawTextWithShadow(textRenderer, "Rule ID (read-only)", contentX, contentY - 11, ScreenTheme.TEXT_MUTED);
            context.drawTextWithShadow(textRenderer, "Name", contentX, contentY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Enabled", contentX, contentY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Activation Mode", contentX + Math.max(144, contentW / 2), contentY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
        } else if (page == 1) {
            context.drawTextWithShadow(textRenderer, "Target Blocks", contentX, contentY - 11, ScreenTheme.TEXT_SECONDARY);
            String targets = draft.targetBlocks.isEmpty() ? "(none)" : String.join(", ", draft.targetBlocks);
            ScreenLayout.drawWrapped(context, textRenderer, targets, contentX, contentY + layoutRows(3) * 2 + 4, contentW, ScreenTheme.TEXT_SECONDARY);
        } else if (page == 2) {
            context.drawTextWithShadow(textRenderer, "Resource Blocks", contentX, contentY - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Weight", contentX + Math.max(160, contentW - 224) + 66, contentY - 11, ScreenTheme.TEXT_SECONDARY);
            StringBuilder sb = new StringBuilder();
            for (WeightedBlock wb : draft.resourceBlocks) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(wb.block).append(" x").append(wb.weight);
            }
            ScreenLayout.drawWrapped(context, textRenderer, sb.length() == 0 ? "(none)" : sb.toString(), contentX, contentY + layoutRows(3) * 2 + 4, contentW, ScreenTheme.TEXT_SECONDARY);
        } else if (page == 3) {
            int rowH = layoutRows(5);
            int half = Math.max(110, (contentW - 10) / 2);
            int topY = contentY;
            context.drawTextWithShadow(textRenderer, "Max Active Blocks", contentX, topY - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Spawn Count", contentX + half + 10, topY - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Respawn Seconds", contentX, topY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Chance (0..1)", contentX + half + 10, topY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Min Y (blank = zone min)", contentX, topY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Max Y (blank = zone max)", contentX + half + 10, topY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Min Distance", contentX, topY + rowH * 3 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Placement Mode", contentX + half + 10, topY + rowH * 3 - 11, ScreenTheme.TEXT_SECONDARY);
        } else if (page == 4) {
            int rowH = layoutRows(5);
            int half = Math.max(110, (contentW - 10) / 2);
            int topY = contentY;
            context.drawTextWithShadow(textRenderer, "Replace Mode", contentX, topY - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Restore Mode", contentX, topY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Restore Delay Seconds", contentX, topY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Max Position Attempts", contentX + half + 10, topY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Drop Original On Replace", contentX, topY + rowH * 3 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Restore If Not Mined", contentX + half + 10, topY + rowH * 3 - 11, ScreenTheme.TEXT_SECONDARY);
        } else if (page == 5) {
            int rowH = layoutRows(4);
            int half = Math.max(110, (contentW - 10) / 2);
            int topY = contentY;
            context.drawTextWithShadow(textRenderer, "Require Loaded Chunk", contentX, topY - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Respect Protected Blocks", contentX + half + 10, topY - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Prevent Player Placed Blocks", contentX, topY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Allow Block Entities", contentX + half + 10, topY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
        }

        if (!error.isBlank()) {
            ScreenLayout.drawWrapped(context, textRenderer, error, contentX, footerY - 24, contentW, ScreenTheme.ERROR);
        }
    }

    private static final Gson GSON = new Gson();

    private static ResourceRule cloneRule(ResourceRule src) {
        return GSON.fromJson(GSON.toJson(src), ResourceRule.class);
    }

    private enum BlockPickTarget {
        TARGET,
        RESOURCE
    }
}
