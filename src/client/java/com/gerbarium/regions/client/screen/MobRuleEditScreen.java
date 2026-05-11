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
    private final String zoneId;
    private MobRule draft;
    private String error = "";
    private int page = 0;

    private TextFieldWidget ruleIdField;
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
        page = Math.max(0, Math.min(page, 2));

        int x = width / 2 - 220;
        int w = 440;
        int top = 34;

        ruleIdField = field(x, top, w, draft.name == null ? "" : draft.name);
        entityField = field(x, top + 34, w - 104, draft.entity);

        enabledButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(Boolean.TRUE.equals(draft.enabled))
                .build(x, top + 68, 100, 20, Text.literal("Enabled"), (b, v) -> {}));

        spawnTypeButton = addDrawableChild(CyclingButtonWidget.<SpawnType>builder(v -> Text.literal(v.name()))
                .values(List.of(SpawnType.PACK, SpawnType.UNIQUE))
                .initially(draft.spawnType)
                .build(x + 104, top + 68, 140, 20, Text.literal("Spawn Type"), (b, v) -> {
                    capture();
                    draft.spawnType = v;
                    init();
                }));

        addDrawableChild(ButtonWidget.builder(Text.literal("Pick Entity"), b -> {
            capture();
            client.setScreen(new EntityPickerScreen(this, this, draft.entity));
        }).dimensions(x + w - 100, top + 34, 100, 20).build());

        if (page == 1) {
            buildSecondPage(x, top);
        } else if (page == 2) {
            buildThirdPage(x, top);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { capture(); page = Math.max(0, page - 1); init(); })
                .dimensions(x + 170, 10, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + "/3"), b -> {})
                .dimensions(x + 198, 10, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { capture(); page = Math.min(2, page + 1); init(); })
                .dimensions(x + 282, 10, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Advanced"), b -> {
                capture();
                page = 2;
                init();
        }).dimensions(x + 310, 10, 132, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(x, height - 32, 216, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new ZoneDetailsScreen(zoneId)))
                .dimensions(x + 224, height - 32, 216, 20).build());

        addDrawableChild(ruleIdField);
        addDrawableChild(entityField);
    }

    private void buildSecondPage(int x, int top) {
        refillModeButton = addDrawableChild(CyclingButtonWidget.<RefillMode>builder(v -> Text.literal(v.name()))
                .values(List.of(RefillMode.ON_ACTIVATION, RefillMode.TIMED, RefillMode.AFTER_DEATH))
                .initially(draft.refillMode)
                .build(x, top + 112, 440, 20, Text.literal("Refill Mode"), (b, v) -> {}));

        maxAliveField = field(x, top + 146, 106, String.valueOf(draft.maxAlive));
        spawnCountField = field(x + 110, top + 146, 106, String.valueOf(draft.spawnCount));
        respawnField = field(x + 220, top + 146, 106, String.valueOf(draft.respawnSeconds));
        chanceField = field(x + 330, top + 146, 110, String.valueOf(draft.chance));
        addDrawableChild(maxAliveField);
        addDrawableChild(spawnCountField);
        addDrawableChild(respawnField);
        addDrawableChild(chanceField);

        retryField = field(x, top + 180, 216, String.valueOf(draft.failedSpawnRetrySeconds));
        addDrawableChild(retryField);

        despawnButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.despawnWhenZoneInactive)
                .build(x + 220, top + 180, 220, 20, Text.literal("Despawn When Zone Inactive"), (b, v) -> {}));

        announceButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.announceOnSpawn)
                .build(x, top + 214, 440, 20, Text.literal("Announce On Spawn"), (b, v) -> {}));

        if (draft.spawnType == SpawnType.PACK) {
            addDrawableChild(ButtonWidget.builder(Text.literal("PACK note: TIMED/low respawn can be farmable"), b -> {})
                    .dimensions(x, top + 248, 360, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.literal("UNIQUE note: values are editable; defaults are just presets"), b -> {})
                    .dimensions(x, top + 248, 440, 20).build());
        }
    }

    private void buildThirdPage(int x, int top) {
        int companionCount = draft.companions == null ? 0 : draft.companions.size();

        addDrawableChild(ButtonWidget.builder(Text.literal("Edit Companions"), b -> {
            capture();
            String label = draft.name == null || draft.name.isBlank() ? "(new rule)" : draft.name;
            client.setScreen(new CompanionListScreen(this, draft.companions, label));
        }).dimensions(x, top + 112, 180, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Companions: " + companionCount), b -> {})
                .dimensions(x + 188, top + 112, 120, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("UID64"), b -> {})
                .dimensions(x, top + 146, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(draft.uid64 == null ? "" : draft.uid64), b -> {})
                .dimensions(x + 84, top + 146, 356, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back to Basics"), b -> {
            capture();
            page = 0;
            init();
        }).dimensions(x, top + 180, 140, 20).build());
    }

    private TextFieldWidget field(int x, int y, int w, String value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        f.setText(value == null ? "" : value);
        return f;
    }

    private void capture() {
        if (ruleIdField != null) {
            draft.name = ruleIdField.getText().trim();
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
        try { return Integer.parseInt(f.getText().trim()); } catch (Exception e) { return d; }
    }

    private double parseDouble(TextFieldWidget f, double d) {
        try { return Double.parseDouble(f.getText().trim()); } catch (Exception e) { return d; }
    }

    @Override
    public void onEntitySelected(String entityId) {
        this.draft.entity = entityId;
        if (this.entityField != null) {
            this.entityField.setText(entityId);
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
        int x = width / 2 - 220;
        int top = 34;
        int panelH = page == 0 ? 140 : (page == 1 ? 310 : 210);

        context.fill(x - 8, 18, x + 448, 18 + panelH, 0x77000000);
        context.fill(x - 8, 18, x + 448, 20, 0xFF3ECF8E);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Rule Name", x, top - 10, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Entity", x, top + 24, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Enabled", x, top + 58, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Spawn Type", x + 104, top + 58, 0xA5FFB5);
        if (page == 1) {
            context.drawTextWithShadow(textRenderer, "Refill Mode", x, top + 102, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Max Alive", x, top + 136, 0x7FD7A5);
            context.drawTextWithShadow(textRenderer, "Spawn Count", x + 110, top + 136, 0x7FD7A5);
            context.drawTextWithShadow(textRenderer, "Respawn Seconds", x + 220, top + 136, 0x7FD7A5);
            context.drawTextWithShadow(textRenderer, "Chance (0..1)", x + 330, top + 136, 0x7FD7A5);
            context.drawTextWithShadow(textRenderer, "Failed Spawn Retry Seconds", x, top + 170, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Despawn When Zone Inactive", x + 220, top + 170, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Announce On Spawn", x, top + 204, 0xA5FFB5);
            if (draft.spawnType == SpawnType.PACK) {
                context.drawTextWithShadow(textRenderer, "PACK note: TIMED/low respawn can be farmable", x, top + 238, 0xFFAA55);
            } else {
                context.drawTextWithShadow(textRenderer, "UNIQUE note: these values are editable; defaults are only presets", x, top + 238, 0xFFCC66);
            }
        } else if (page == 2) {
            context.drawTextWithShadow(textRenderer, "Advanced / Companions", x, top + 102, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "UID64", x, top + 136, 0x888888);
            context.drawTextWithShadow(textRenderer, "Companions", x, top + 170, 0xA5FFB5);
        }

        if (page == 1 && refillModeButton != null && refillModeButton.getValue() == RefillMode.TIMED) {
            context.drawTextWithShadow(textRenderer, "TIMED can create farmable zones. Prefer ON_ACTIVATION for normal mobs.", x, height - 70, 0xFFAA55);
        }

        if (page == 1 && draft.spawnType == SpawnType.PACK && parseInt(respawnField, draft.respawnSeconds) < 300) {
            context.drawTextWithShadow(textRenderer, "Low respawnSeconds may make this zone farmable.", x, height - 58, 0xFFAA55);
        }

        if (!error.isBlank()) {
            context.drawTextWithShadow(textRenderer, error, x, height - 46, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private static MobRule cloneRule(MobRule src) {
        MobRule r = new MobRule();
        r.id = src.id;
        r.uid64 = src.uid64;
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
        r.companions = src.companions == null ? new ArrayList<>() : new ArrayList<>(src.companions);
        return r;
    }
}
