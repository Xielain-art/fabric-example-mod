package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.client.screen.help.HelpTopic;
import com.gerbarium.regions.model.CompanionRule;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.RefillMode;
import com.gerbarium.regions.model.SpawnType;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class MobRuleEditScreen extends GerbariumScreen implements EntitySelectionConsumer {
    private static final int PAGE_COUNT = 5;

    private final String zoneId;
    private MobRule draft;
    private String error = "";
    private int page = 0;

    private TextFieldWidget ruleNameField;
    private TextFieldWidget entityField;
    private TextFieldWidget maxAliveField;
    private TextFieldWidget spawnCountField;
    private TextFieldWidget respawnField;
    private TextFieldWidget chanceField;
    private TextFieldWidget retryField;
    private TextFieldWidget afterDeathDelayField;
    private TextFieldWidget fixedXField;
    private TextFieldWidget fixedYField;
    private TextFieldWidget fixedZField;
    private TextFieldWidget positionAttemptsField;
    private TextFieldWidget minDistanceField;
    private TextFieldWidget playerActivationRangeField;

    private CyclingButtonWidget<Boolean> enabledButton;
    private CyclingButtonWidget<SpawnType> spawnTypeButton;
    private CyclingButtonWidget<RefillMode> refillModeButton;
    private CyclingButtonWidget<String> spawnModeButton;
    private CyclingButtonWidget<String> spawnTriggerButton;
    private CyclingButtonWidget<Boolean> allowSmallRoomButton;
    private CyclingButtonWidget<Boolean> spreadSpawnsButton;
    private CyclingButtonWidget<Boolean> despawnButton;
    private CyclingButtonWidget<Boolean> announceButton;
    private CyclingButtonWidget<Boolean> respawnAfterDeathButton;
    private CyclingButtonWidget<Boolean> respawnAfterDespawnButton;
    private CyclingButtonWidget<Boolean> requirePlayerNearbyButton;
    private CyclingButtonWidget<Boolean> requireChunkLoadedButton;
    private CyclingButtonWidget<Boolean> allowForceLoadButton;

    public MobRuleEditScreen(String zoneId, MobRule existingRule) {
        super(Text.literal(existingRule == null ? "Add Mob Rule" : "Edit Mob Rule"), 700, null);
        this.zoneId = zoneId;
        this.draft = existingRule == null ? MobRule.packDefaults("", "minecraft:zombie") : cloneRule(existingRule);
        ZoneDefaults.normalizeMobRule(this.draft);
    }

    @Override
    protected void init() {
        page = Math.max(0, Math.min(page, PAGE_COUNT - 1));
        updateHelpTopic();
        super.init();
    }

    private void updateHelpTopic() {
        HelpTopic topic = switch (page) {
            case 0 -> HelpTopic.MOB_RULE_BASICS;
            case 1 -> HelpTopic.MOB_RULE_SPAWN;
            case 2 -> HelpTopic.MOB_RULE_PLACEMENT;
            case 3 -> HelpTopic.MOB_RULE_BOUNDARY;
            default -> HelpTopic.MOB_RULE_ADVANCED;
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
            addBaseFields(contentX, topY);
        } else if (page == 1) {
            addSpawnPage(contentX, topY);
        } else if (page == 2) {
            addPlacementPage(contentX, topY);
        } else if (page == 3) {
            addBoundaryPage(contentX, topY);
        } else if (page == 4) {
            addAdvancedPage(contentX, topY);
        }
    }

    // Page 0: Basics - 4 rows
    private void addBaseFields(int startX, int topY) {
        int rowH = layoutRows(4);

        ruleNameField = field(startX, topY, Math.max(180, contentW - 130), draft.name == null ? "" : draft.name);
        addDrawableChild(ruleNameField);

        entityField = field(startX, topY + rowH, Math.max(180, contentW - 110), draft.entity);
        addDrawableChild(entityField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Pick Entity"), b -> {
            capture();
            client.setScreen(new EntityPickerScreen(this, this, draft.entity));
        }).dimensions(startX + Math.max(184, contentW - 102), topY + rowH, 96, 20).build());

        enabledButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(Boolean.TRUE.equals(draft.enabled))
                .build(startX, topY + rowH * 2, Math.max(140, contentW / 2 - 4), 20, Text.literal("Enabled"), (b, v) -> {}));

        spawnTypeButton = addDrawableChild(CyclingButtonWidget.<SpawnType>builder(v -> Text.literal(v.name()))
                .values(List.of(SpawnType.PACK, SpawnType.UNIQUE))
                .initially(draft.spawnType)
                .build(startX + Math.max(144, contentW / 2), topY + rowH * 2, Math.max(140, contentW / 2 - 4), 20, Text.literal("Spawn Type"), (b, v) -> {
                    capture();
                    draft.spawnType = v;
                    init();
                }));
    }

    // Page 1: Spawn - 6 rows
    private void addSpawnPage(int startX, int topY) {
        int rowH = layoutRows(6);
        int half = Math.max(110, (contentW - 8) / 2);

        refillModeButton = addDrawableChild(CyclingButtonWidget.<RefillMode>builder(v -> Text.literal(v.name()))
                .values(List.of(RefillMode.ON_ACTIVATION, RefillMode.TIMED, RefillMode.AFTER_DEATH))
                .initially(draft.refillMode)
                .build(startX, topY, contentW, 20, Text.literal("Refill Mode"), (b, v) -> {}));

        int y2 = topY + rowH;
        maxAliveField = field(startX, y2, half, String.valueOf(draft.maxAlive));
        spawnCountField = field(startX + half + 8, y2, half, String.valueOf(draft.spawnCount));
        addDrawableChild(maxAliveField);
        addDrawableChild(spawnCountField);

        int y3 = y2 + rowH;
        respawnField = field(startX, y3, half, String.valueOf(draft.respawnSeconds));
        chanceField = field(startX + half + 8, y3, half, String.valueOf(draft.chance));
        addDrawableChild(respawnField);
        addDrawableChild(chanceField);

        int y4 = y3 + rowH;
        retryField = field(startX, y4, half, String.valueOf(draft.failedSpawnRetrySeconds));
        afterDeathDelayField = field(startX + half + 8, y4, half, String.valueOf(draft.afterDeathDelaySeconds));
        addDrawableChild(retryField);
        addDrawableChild(afterDeathDelayField);

        int y5 = y4 + rowH;
        spawnTriggerButton = addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal)
                .values(List.of(MobRule.SPAWN_TRIGGER_TIMER, MobRule.SPAWN_TRIGGER_AFTER_DEATH, MobRule.SPAWN_TRIGGER_ON_ACTIVATION, MobRule.SPAWN_TRIGGER_MANUAL))
                .initially(draft.spawnTrigger == null ? MobRule.SPAWN_TRIGGER_TIMER : draft.spawnTrigger)
                .build(startX, y5, contentW, 20, Text.literal("Spawn Trigger"), (b, v) -> {}));

        int y6 = y5 + rowH;
        respawnAfterDeathButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.respawnAfterDeath)
                .build(startX, y6, half, 20, Text.literal("Respawn After Death"), (b, v) -> {}));
        respawnAfterDespawnButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.respawnAfterDespawn)
                .build(startX + half + 8, y6, half, 20, Text.literal("Respawn After Despawn"), (b, v) -> {}));
    }

    // Page 2: Placement - 6 rows
    private void addPlacementPage(int startX, int topY) {
        int rowH = layoutRows(6);
        int third = Math.max(70, (contentW - 16) / 3);
        int half = Math.max(110, (contentW - 8) / 2);

        spawnModeButton = addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal)
                .values(List.of(
                        MobRule.SPAWN_MODE_RANDOM_VALID_POSITION,
                        MobRule.SPAWN_MODE_CENTER,
                        MobRule.SPAWN_MODE_BOSS_ROOM,
                        MobRule.SPAWN_MODE_FIXED_POINT))
                .initially(draft.spawnMode == null ? MobRule.SPAWN_MODE_RANDOM_VALID_POSITION : draft.spawnMode)
                .build(startX, topY, contentW, 20, Text.literal("Spawn Mode"), (b, v) -> {
                    capture();
                    draft.spawnMode = v;
                    if (MobRule.SPAWN_MODE_BOSS_ROOM.equals(v)) {
                        draft.allowSmallRoom = true;
                        draft.spawnCount = Math.max(1, Math.min(draft.spawnCount, 1));
                        draft.maxAlive = Math.max(1, Math.min(draft.maxAlive, 1));
                    }
                    init();
                }));

        int y1 = topY + rowH;
        fixedXField = field(startX, y1, third, draft.fixedX == null ? "" : String.valueOf(draft.fixedX));
        fixedYField = field(startX + third + 8, y1, third, draft.fixedY == null ? "" : String.valueOf(draft.fixedY));
        fixedZField = field(startX + (third + 8) * 2, y1, third, draft.fixedZ == null ? "" : String.valueOf(draft.fixedZ));
        addDrawableChild(fixedXField);
        addDrawableChild(fixedYField);
        addDrawableChild(fixedZField);

        int y2 = y1 + rowH;
        positionAttemptsField = field(startX, y2, half, String.valueOf(draft.positionAttempts));
        minDistanceField = field(startX + half + 8, y2, half, String.valueOf(draft.minDistanceBetweenSpawns));
        addDrawableChild(positionAttemptsField);
        addDrawableChild(minDistanceField);

        int y3 = y2 + rowH;
        allowSmallRoomButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.allowSmallRoom)
                .build(startX, y3, half, 20, Text.literal("Allow Small Room"), (b, v) -> {}));
        spreadSpawnsButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.spreadSpawns)
                .build(startX + half + 8, y3, half, 20, Text.literal("Spread Spawns"), (b, v) -> {}));

        int y4 = y3 + rowH;
        requirePlayerNearbyButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.requirePlayerNearby)
                .build(startX, y4, half, 20, Text.literal("Require Player Nearby"), (b, v) -> {}));
        playerActivationRangeField = field(startX + half + 8, y4, half, String.valueOf(draft.playerActivationRange));
        addDrawableChild(playerActivationRangeField);

        int y5 = y4 + rowH;
        requireChunkLoadedButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.requireChunkLoaded)
                .build(startX, y5, half, 20, Text.literal("Require Chunk Loaded"), (b, v) -> {}));
        allowForceLoadButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.allowForceLoad)
                .build(startX + half + 8, y5, half, 20, Text.literal("Allow Force Load"), (b, v) -> {}));
    }

    // Page 3: Boundary & Safety - 3 rows
    private void addBoundaryPage(int startX, int topY) {
        int rowH = layoutRows(3);
        int half = Math.max(110, (contentW - 8) / 2);

        // Boundary summary + edit button
        String boundaryText = "Boundary: " + boundarySummary();
        addDrawableChild(ButtonWidget.builder(Text.literal(boundaryText), b -> {})
                .dimensions(startX, topY, Math.max(180, contentW - 102), 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), b -> {
            capture();
            client.setScreen(new MobRuleBoundarySettingsScreen(this, draft));
        }).dimensions(startX + Math.max(184, contentW - 96), topY, 96, 20).build());

        // Despawn
        despawnButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.despawnWhenZoneInactive)
                .build(startX, topY + rowH, contentW, 20, Text.literal("Despawn When Zone Inactive"), (b, v) -> {}));

        // Announce
        announceButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.announceOnSpawn)
                .build(startX, topY + rowH * 2, contentW, 20, Text.literal("Announce On Spawn"), (b, v) -> {}));
    }

    // Page 4: Advanced - 3 rows
    private void addAdvancedPage(int startX, int topY) {
        int rowH = layoutRows(3);
        int companionCount = draft.companions == null ? 0 : draft.companions.size();

        addDrawableChild(ButtonWidget.builder(Text.literal("Edit Companions"), b -> {
            capture();
            String label = draft.name == null || draft.name.isBlank() ? "(new rule)" : draft.name;
            client.setScreen(new CompanionListScreen(this, draft.companions, label));
        }).dimensions(startX, topY, 180, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Companions: " + companionCount), b -> {})
                .dimensions(startX + 188, topY, 140, 20).build());

        // ID
        addDrawableChild(ButtonWidget.builder(Text.literal("ID"), b -> {})
                .dimensions(startX, topY + rowH, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(draft.id == null ? "" : draft.id), b -> {})
                .dimensions(startX + 84, topY + rowH, Math.max(180, contentW - 84), 20).build());

        // Boss Preset
        addDrawableChild(ButtonWidget.builder(Text.literal("Boss Preset"), b -> {
            capture();
            draft.spawnType = SpawnType.UNIQUE;
            draft.refillMode = RefillMode.AFTER_DEATH;
            draft.spawnMode = MobRule.SPAWN_MODE_BOSS_ROOM;
            draft.spawnTrigger = MobRule.SPAWN_TRIGGER_AFTER_DEATH;
            draft.allowSmallRoom = true;
            draft.spawnCount = 1;
            draft.maxAlive = 1;
            draft.spreadSpawns = false;
            draft.respawnAfterDeath = true;
            draft.afterDeathDelaySeconds = 30;
            init();
        }).dimensions(startX, topY + rowH * 2, contentW, 20).build());
    }

    private int layoutRows(int requiredRows) {
        int available = footerY - contentY - 20;
        int preferred = height < 220 ? 26 : 30;
        if (requiredRows <= 0) return preferred;
        int calculated = available / requiredRows;
        return Math.max(24, Math.min(preferred, calculated));
    }

    private TextFieldWidget field(int x, int y, int w, String value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        f.setText(value == null ? "" : value);
        return f;
    }

    private String pageLabel() {
        return switch (page) {
            case 0 -> "Basics 1/5";
            case 1 -> "Spawn 2/5";
            case 2 -> "Placement 3/5";
            case 3 -> "Boundary 4/5";
            default -> "Advanced 5/5";
        };
    }

    private void capture() {
        if (ruleNameField != null) draft.name = ruleNameField.getText().trim();
        if (entityField != null) draft.entity = entityField.getText().trim();
        if (enabledButton != null) draft.enabled = enabledButton.getValue();
        if (spawnTypeButton != null) draft.spawnType = spawnTypeButton.getValue();
        if (refillModeButton != null) draft.refillMode = refillModeButton.getValue();
        if (spawnModeButton != null) draft.spawnMode = spawnModeButton.getValue();
        if (spawnTriggerButton != null) draft.spawnTrigger = spawnTriggerButton.getValue();
        if (maxAliveField != null) draft.maxAlive = parseInt(maxAliveField, draft.maxAlive);
        if (spawnCountField != null) draft.spawnCount = parseInt(spawnCountField, draft.spawnCount);
        if (respawnField != null) draft.respawnSeconds = parseInt(respawnField, draft.respawnSeconds);
        if (chanceField != null) draft.chance = parseDouble(chanceField, draft.chance);
        if (retryField != null) draft.failedSpawnRetrySeconds = parseInt(retryField, draft.failedSpawnRetrySeconds);
        if (afterDeathDelayField != null) draft.afterDeathDelaySeconds = parseInt(afterDeathDelayField, draft.afterDeathDelaySeconds);
        if (fixedXField != null) draft.fixedX = parseNullableInt(fixedXField);
        if (fixedYField != null) draft.fixedY = parseNullableInt(fixedYField);
        if (fixedZField != null) draft.fixedZ = parseNullableInt(fixedZField);
        if (positionAttemptsField != null) draft.positionAttempts = parseInt(positionAttemptsField, draft.positionAttempts);
        if (minDistanceField != null) draft.minDistanceBetweenSpawns = parseInt(minDistanceField, draft.minDistanceBetweenSpawns);
        if (allowSmallRoomButton != null) draft.allowSmallRoom = allowSmallRoomButton.getValue();
        if (spreadSpawnsButton != null) draft.spreadSpawns = spreadSpawnsButton.getValue();
        if (despawnButton != null) draft.despawnWhenZoneInactive = despawnButton.getValue();
        if (announceButton != null) draft.announceOnSpawn = announceButton.getValue();
        if (respawnAfterDeathButton != null) draft.respawnAfterDeath = respawnAfterDeathButton.getValue();
        if (respawnAfterDespawnButton != null) draft.respawnAfterDespawn = respawnAfterDespawnButton.getValue();
        if (requirePlayerNearbyButton != null) draft.requirePlayerNearby = requirePlayerNearbyButton.getValue();
        if (playerActivationRangeField != null) draft.playerActivationRange = parseInt(playerActivationRangeField, draft.playerActivationRange);
        if (requireChunkLoadedButton != null) draft.requireChunkLoaded = requireChunkLoadedButton.getValue();
        if (allowForceLoadButton != null) draft.allowForceLoad = allowForceLoadButton.getValue();
    }

    private int parseInt(TextFieldWidget f, int d) {
        try { return Integer.parseInt(f.getText().trim()); }
        catch (Exception e) { return d; }
    }

    private Integer parseNullableInt(TextFieldWidget f) {
        String text = f.getText().trim();
        if (text.isEmpty()) return null;
        try { return Integer.parseInt(text); }
        catch (Exception e) { return null; }
    }

    private double parseDouble(TextFieldWidget f, double d) {
        try { return Double.parseDouble(f.getText().trim()); }
        catch (Exception e) { return d; }
    }

    @Override
    public void onEntitySelected(String entityId) {
        draft.entity = entityId;
        if (entityField != null) entityField.setText(entityId);
    }

    public void setCompanions(List<CompanionRule> companions) {
        draft.companions = companions == null ? new ArrayList<>() : new ArrayList<>(companions);
    }

    private void save() {
        capture();
        try {
            ZoneDefaults.validateMobRule(draft);
            ZoneDefaults.normalizeMobRule(draft);
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
            return;
        }
        GerbariumClientNetworking.addMobRule(zoneId, draft);
    }

    @Override
    protected void initFooter() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(contentX, footerY, contentW / 2 - 4, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new ZoneDetailsScreen(zoneId)))
                .dimensions(contentX + contentW / 2 + 4, footerY, contentW / 2 - 4, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (page == 0) {
            int rowH = layoutRows(4);
            context.drawTextWithShadow(textRenderer, "Rule Name", contentX, contentY - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Entity", contentX, contentY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Enabled", contentX, contentY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Spawn Type", contentX + Math.max(144, contentW / 2), contentY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
        } else if (page == 1) {
            int rowH = layoutRows(6);
            int half = Math.max(110, (contentW - 8) / 2);
            int topY = contentY;
            context.drawTextWithShadow(textRenderer, "Refill Mode", contentX, topY - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Max Alive", contentX, topY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Spawn Count", contentX + half + 8, topY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Respawn Secs", contentX, topY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Chance (0..1)", contentX + half + 8, topY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Retry Seconds", contentX, topY + rowH * 3 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "After Death Delay", contentX + half + 8, topY + rowH * 3 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Spawn Trigger", contentX, topY + rowH * 4 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Respawn After Death", contentX, topY + rowH * 5 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Respawn After Despawn", contentX + half + 8, topY + rowH * 5 - 11, ScreenTheme.TEXT_SECONDARY);
        } else if (page == 2) {
            int rowH = layoutRows(6);
            int third = Math.max(70, (contentW - 16) / 3);
            int half = Math.max(110, (contentW - 8) / 2);
            int topY = contentY;
            context.drawTextWithShadow(textRenderer, "Spawn Mode", contentX, topY - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Fixed X", contentX, topY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Fixed Y", contentX + third + 8, topY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Fixed Z", contentX + (third + 8) * 2, topY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Attempts", contentX, topY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Min Distance", contentX + half + 8, topY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Small Room", contentX, topY + rowH * 3 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Spread", contentX + half + 8, topY + rowH * 3 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Require Player Nearby", contentX, topY + rowH * 4 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Player Range", contentX + half + 8, topY + rowH * 4 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Require Chunk Loaded", contentX, topY + rowH * 5 - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Allow Force Load", contentX + half + 8, topY + rowH * 5 - 11, ScreenTheme.TEXT_SECONDARY);
        } else if (page == 3) {
            int rowH = layoutRows(3);
            int topY = contentY;
            context.drawTextWithShadow(textRenderer, "Boundary Control", contentX, topY - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Despawn When Zone Inactive", contentX, topY + rowH - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Announce On Spawn", contentX, topY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
        } else {
            int rowH = layoutRows(3);
            int topY = contentY;
            context.drawTextWithShadow(textRenderer, "Companions", contentX, topY - 11, ScreenTheme.TEXT_SECONDARY);
            context.drawTextWithShadow(textRenderer, "Rule ID", contentX, topY + rowH - 11, ScreenTheme.TEXT_MUTED);
            context.drawTextWithShadow(textRenderer, "Boss Preset", contentX, topY + rowH * 2 - 11, ScreenTheme.TEXT_SECONDARY);
        }

        if (!error.isBlank()) {
            ScreenLayout.drawWrapped(context, textRenderer, error, contentX, footerY - 24, contentW, ScreenTheme.ERROR);
        }
    }

    private String boundarySummary() {
        String mode = draft.boundaryMode == null || draft.boundaryMode.isBlank() ? MobRule.BOUNDARY_LEASH : draft.boundaryMode;
        String extra = draft.boundaryMaxOutsideSeconds > 0 ? ", " + draft.boundaryMaxOutsideSeconds + "s" : "";
        return mode + extra + ", " + draft.boundaryCheckIntervalTicks + "t";
    }

    private static final Gson GSON = new Gson();

    private static MobRule cloneRule(MobRule src) {
        return GSON.fromJson(GSON.toJson(src), MobRule.class);
    }
}
