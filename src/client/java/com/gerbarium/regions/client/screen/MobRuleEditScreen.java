package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.CompanionRule;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.RefillMode;
import com.gerbarium.regions.model.SpawnType;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MobRuleEditScreen extends Screen implements EntitySelectionConsumer {
    private static final int PAGE_COUNT = 3;

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

    private CyclingButtonWidget<Boolean> enabledButton;
    private CyclingButtonWidget<SpawnType> spawnTypeButton;
    private CyclingButtonWidget<RefillMode> refillModeButton;
    private CyclingButtonWidget<Boolean> despawnButton;
    private CyclingButtonWidget<Boolean> announceButton;

    public MobRuleEditScreen(String zoneId, MobRule existingRule) {
        super(Text.literal(existingRule == null ? "Add Mob Rule" : "Edit Mob Rule"));
        this.zoneId = zoneId;
        this.draft = existingRule == null ? MobRule.packDefaults("", "minecraft:zombie") : cloneRule(existingRule);
        ZoneDefaults.normalizeMobRule(this.draft);
    }

    @Override
    protected void init() {
        clearChildren();
        page = Math.max(0, Math.min(page, PAGE_COUNT - 1));

        int panelWidth = ScreenLayout.panelWidth(width, 700);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        int topY = 56;

        addPageNav(startX, panelWidth);
        addBaseFields(startX, topY, panelWidth);
        if (page == 1) {
            addSpawnPage(startX, topY + 120, panelWidth);
        } else if (page == 2) {
            addAdvancedPage(startX, topY + 120, panelWidth);
        }

        int footerY = height - 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(startX, footerY, panelWidth / 2 - 4, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new ZoneDetailsScreen(zoneId)))
                .dimensions(startX + panelWidth / 2 + 4, footerY, panelWidth / 2 - 4, 20).build());
    }

    private void addPageNav(int startX, int panelWidth) {
        int pagY = 24;
        int buttonW = 72;
        int labelW = 108;
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
            case 0 -> "Basics 1/3";
            case 1 -> "Spawn 2/3";
            default -> "Advanced 3/3";
        };
    }

    private void addBaseFields(int startX, int topY, int panelWidth) {
        int rowH = 40;
        int labelColor = 0xA5FFB5;

        ruleNameField = field(startX, topY, Math.max(180, panelWidth - 130), draft.name == null ? "" : draft.name);
        addDrawableChild(ruleNameField);

        entityField = field(startX, topY + rowH, Math.max(180, panelWidth - 110), draft.entity);
        addDrawableChild(entityField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Pick Entity"), b -> {
            capture();
            client.setScreen(new EntityPickerScreen(this, this, draft.entity));
        }).dimensions(startX + Math.max(184, panelWidth - 102), topY + rowH, 96, 20).build());

        enabledButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(Boolean.TRUE.equals(draft.enabled))
                .build(startX, topY + rowH * 2, 140, 20, Text.literal("Enabled"), (b, v) -> {}));

        spawnTypeButton = addDrawableChild(CyclingButtonWidget.<SpawnType>builder(v -> Text.literal(v.name()))
                .values(List.of(SpawnType.PACK, SpawnType.UNIQUE))
                .initially(draft.spawnType)
                .build(startX + 150, topY + rowH * 2, Math.max(120, panelWidth - 150), 20, Text.literal("Spawn Type"), (b, v) -> {
                    capture();
                    draft.spawnType = v;
                    if (v == SpawnType.UNIQUE) {
                        draft.refillMode = RefillMode.AFTER_DEATH;
                    }
                    init();
                }));

        int boundaryY = topY + rowH * 3;
        String boundaryText = "Boundary: " + boundarySummary();
        addDrawableChild(ButtonWidget.builder(Text.literal(boundaryText), b -> {})
                .dimensions(startX, boundaryY, Math.max(180, panelWidth - 102), 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), b -> {
            capture();
            client.setScreen(new MobRuleBoundarySettingsScreen(this, draft));
        }).dimensions(startX + Math.max(184, panelWidth - 96), boundaryY, 96, 20).build());
    }

    private void addSpawnPage(int startX, int topY, int panelWidth) {
        int rowH = 36;
        int half = Math.max(120, (panelWidth - 6) / 2);

        refillModeButton = addDrawableChild(CyclingButtonWidget.<RefillMode>builder(v -> Text.literal(v.name()))
                .values(List.of(RefillMode.ON_ACTIVATION, RefillMode.TIMED, RefillMode.AFTER_DEATH))
                .initially(draft.refillMode)
                .build(startX, topY, panelWidth, 20, Text.literal("Refill Mode"), (b, v) -> {}));

        int y2 = topY + rowH;
        maxAliveField = field(startX, y2, half, String.valueOf(draft.maxAlive));
        spawnCountField = field(startX + half + 6, y2, half, String.valueOf(draft.spawnCount));
        respawnField = field(startX, y2 + rowH, half, String.valueOf(draft.respawnSeconds));
        chanceField = field(startX + half + 6, y2 + rowH, half, String.valueOf(draft.chance));
        addDrawableChild(maxAliveField);
        addDrawableChild(spawnCountField);
        addDrawableChild(respawnField);
        addDrawableChild(chanceField);

        int y3 = y2 + rowH * 2;
        retryField = field(startX, y3, panelWidth, String.valueOf(draft.failedSpawnRetrySeconds));
        addDrawableChild(retryField);

        despawnButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.despawnWhenZoneInactive)
                .build(startX, y3 + rowH, panelWidth, 20, Text.literal("Despawn When Zone Inactive"), (b, v) -> {}));

        int y4 = y3 + rowH * 2;
        announceButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.announceOnSpawn)
                .build(startX, y4, panelWidth, 20, Text.literal("Announce On Spawn"), (b, v) -> {}));

    }

    private void addAdvancedPage(int startX, int topY, int panelWidth) {
        int rowH = 36;
        int companionCount = draft.companions == null ? 0 : draft.companions.size();
        addDrawableChild(ButtonWidget.builder(Text.literal("Edit Companions"), b -> {
            capture();
            String label = draft.name == null || draft.name.isBlank() ? "(new rule)" : draft.name;
            client.setScreen(new CompanionListScreen(this, draft.companions, label));
        }).dimensions(startX, topY, 180, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Companions: " + companionCount), b -> {})
                .dimensions(startX + 188, topY, 140, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("ID"), b -> {})
                .dimensions(startX, topY + rowH, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(draft.id == null ? "" : draft.id), b -> {})
                .dimensions(startX + 84, topY + rowH, Math.max(180, panelWidth - 84), 20).build());

    }

    private TextFieldWidget field(int x, int y, int w, String value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        f.setText(value == null ? "" : value);
        return f;
    }

    private void capture() {
        if (ruleNameField != null) {
            draft.name = ruleNameField.getText().trim();
        }
        if (entityField != null) {
            draft.entity = entityField.getText().trim();
        }
        if (enabledButton != null) {
            draft.enabled = enabledButton.getValue();
        }
        if (spawnTypeButton != null) {
            draft.spawnType = spawnTypeButton.getValue();
        }
        if (refillModeButton != null) {
            draft.refillMode = refillModeButton.getValue();
        }
        if (maxAliveField != null) {
            draft.maxAlive = parseInt(maxAliveField, draft.maxAlive);
        }
        if (spawnCountField != null) {
            draft.spawnCount = parseInt(spawnCountField, draft.spawnCount);
        }
        if (respawnField != null) {
            draft.respawnSeconds = parseInt(respawnField, draft.respawnSeconds);
        }
        if (chanceField != null) {
            draft.chance = parseDouble(chanceField, draft.chance);
        }
        if (retryField != null) {
            draft.failedSpawnRetrySeconds = parseInt(retryField, draft.failedSpawnRetrySeconds);
        }
        if (despawnButton != null) {
            draft.despawnWhenZoneInactive = despawnButton.getValue();
        }
        if (announceButton != null) {
            draft.announceOnSpawn = announceButton.getValue();
        }
    }

    private int parseInt(TextFieldWidget f, int d) {
        try {
            return Integer.parseInt(f.getText().trim());
        } catch (Exception e) {
            return d;
        }
    }

    private double parseDouble(TextFieldWidget f, double d) {
        try {
            return Double.parseDouble(f.getText().trim());
        } catch (Exception e) {
            return d;
        }
    }

    @Override
    public void onEntitySelected(String entityId) {
        draft.entity = entityId;
        if (entityField != null) {
            entityField.setText(entityId);
        }
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelWidth = ScreenLayout.panelWidth(width, 700);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        ScreenLayout.drawPanel(context, startX, 15, panelWidth, height - 15);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);

        context.drawTextWithShadow(textRenderer, "Page " + (page + 1) + " of " + PAGE_COUNT, startX, 29, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Use the page buttons to switch sections", startX + 110, 29, 0x888888);

        if (page == 0) {
            context.drawTextWithShadow(textRenderer, "Rule Name", startX, 45, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Entity", startX, 85, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Enabled", startX, 125, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Spawn Type", startX + 150, 125, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Boundary Control", startX, 165, 0xA5FFB5);
            ScreenLayout.drawWrapped(context, textRenderer, boundaryHelp(), startX, 190, panelWidth, 0xAAAAAA);
        } else if (page == 1) {
            int half = Math.max(120, (panelWidth - 6) / 2);
            context.drawTextWithShadow(textRenderer, "Refill Mode", startX, 45, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Max Alive", startX, 81, 0x7FD7A5);
            context.drawTextWithShadow(textRenderer, "Spawn Count", startX + half + 6, 81, 0x7FD7A5);
            context.drawTextWithShadow(textRenderer, "Respawn Secs", startX, 117, 0x7FD7A5);
            context.drawTextWithShadow(textRenderer, "Chance (0..1)", startX + half + 6, 117, 0x7FD7A5);
            context.drawTextWithShadow(textRenderer, "Retry Seconds", startX, 153, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Despawn When Zone Inactive", startX, 189, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Announce On Spawn", startX, 225, 0xA5FFB5);
            ScreenLayout.drawWrapped(context, textRenderer, spawnWarning(), startX, 255, panelWidth, draft.spawnType == SpawnType.PACK ? 0xFFAA55 : 0xFFCC66);
        } else {
            context.drawTextWithShadow(textRenderer, "Companions", startX, 45, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "ID", startX, 81, 0x888888);
            context.drawTextWithShadow(textRenderer, "Advanced Controls", startX, 117, 0xA5FFB5);
            ScreenLayout.drawWrapped(context, textRenderer, "This page is for companion editing and rule identity only.", startX, 145, panelWidth, 0xAAAAAA);
        }

        if (!error.isBlank()) {
            ScreenLayout.drawWrapped(context, textRenderer, error, startX, height - 64, panelWidth, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String boundaryHelp() {
        return switch (draft.boundaryMode == null ? MobRule.BOUNDARY_LEASH : draft.boundaryMode) {
            case MobRule.BOUNDARY_NONE -> "NONE: Mob can leave the zone.";
            case MobRule.BOUNDARY_TELEPORT_BACK -> "TELEPORT_BACK: Mob will be teleported back inside the zone.";
            case MobRule.BOUNDARY_REMOVE_OUTSIDE -> "REMOVE_OUTSIDE: Warning - may remove mobs if players pull them outside the zone.";
            default -> "LEASH: Mob will be returned if it stays outside the zone too long.";
        };
    }

    private String spawnWarning() {
        if (draft.spawnType == SpawnType.PACK) {
            return "PACK note: low respawn or timed refill may be farmable.";
        }
        return "UNIQUE note: values are editable, but the defaults are tuned for bosses.";
    }

    private String boundarySummary() {
        String mode = draft.boundaryMode == null || draft.boundaryMode.isBlank() ? MobRule.BOUNDARY_LEASH : draft.boundaryMode;
        String extra = draft.boundaryMaxOutsideSeconds > 0 ? ", " + draft.boundaryMaxOutsideSeconds + "s" : "";
        return mode + extra + ", " + draft.boundaryCheckIntervalTicks + "t";
    }

    private static MobRule cloneRule(MobRule src) {
        MobRule r = new MobRule();
        r.id = src.id;
        r.name = src.name;
        r.entity = src.entity;
        r.enabled = src.enabled;
        r.spawnType = src.spawnType;
        r.refillMode = src.refillMode;
        r.maxAlive = src.maxAlive;
        r.spawnCount = src.spawnCount;
        r.respawnSeconds = src.respawnSeconds;
        r.chance = src.chance;
        r.cooldownStart = src.cooldownStart;
        r.spawnWhenReady = src.spawnWhenReady;
        r.failedSpawnRetrySeconds = src.failedSpawnRetrySeconds;
        r.despawnWhenZoneInactive = src.despawnWhenZoneInactive;
        r.announceOnSpawn = src.announceOnSpawn;
        r.boundaryMode = src.boundaryMode;
        r.boundaryMaxOutsideSeconds = src.boundaryMaxOutsideSeconds;
        r.boundaryCheckIntervalTicks = src.boundaryCheckIntervalTicks;
        r.boundaryTeleportBack = src.boundaryTeleportBack;
        r.boundaryModeWasInvalid = src.boundaryModeWasInvalid;
        r.companions = src.companions == null ? new ArrayList<>() : new ArrayList<>(src.companions);
        return r;
    }
}
