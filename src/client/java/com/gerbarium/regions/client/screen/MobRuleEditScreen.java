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

    // Константы для сетки UI
    private final int formW = 450;
    private final int rowH = 34;

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

        int startX = (width - formW) / 2;
        int topY = 55;

        // Координаты строк верхней (базовой) части формы
        int ruleY = topY;
        int entityY = topY + rowH;
        int enableY = topY + rowH * 2;

        // Базовые поля (Всегда отображаются)
        ruleIdField = field(startX, ruleY, formW, draft.name == null ? "" : draft.name);

        entityField = field(startX, entityY, formW - 110, draft.entity);
        addDrawableChild(ButtonWidget.builder(Text.literal("Pick Entity"), b -> {
            capture();
            client.setScreen(new EntityPickerScreen(this, this, draft.entity));
        }).dimensions(startX + formW - 100, entityY, 100, 20).build());

        enabledButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(Boolean.TRUE.equals(draft.enabled))
                .build(startX, enableY, 140, 20, Text.literal("Enabled"), (b, v) -> {}));

        spawnTypeButton = addDrawableChild(CyclingButtonWidget.<SpawnType>builder(v -> Text.literal(v.name()))
                .values(List.of(SpawnType.PACK, SpawnType.UNIQUE))
                .initially(draft.spawnType)
                .build(startX + 150, enableY, formW - 150, 20, Text.literal("Spawn Type"), (b, v) -> {
                    capture();
                    draft.spawnType = v;
                    init();
                }));

        addDrawableChild(ruleIdField);
        addDrawableChild(entityField);

        // Динамическая часть (Страницы 1 и 2)
        int dynY = enableY + rowH + 8; // Небольшой дополнительный отступ для отделения блоков

        if (page == 1) {
            buildSecondPage(startX, dynY);
        } else if (page == 2) {
            buildThirdPage(startX, dynY);
        }

        // Блок пагинации (перенесен в правую верхнюю часть рамки, чтобы не пересекаться с заголовком)
        int pagY = 25;
        int pagX = startX + formW - 240;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { capture(); page = Math.max(0, page - 1); init(); })
                .dimensions(pagX, pagY, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + "/3"), b -> {})
                .dimensions(pagX + 28, pagY, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { capture(); page = Math.min(2, page + 1); init(); })
                .dimensions(pagX + 102, pagY, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Advanced"), b -> {
            capture(); page = 2; init();
        }).dimensions(pagX + 130, pagY, 110, 20).build());

        // Футер: Кнопки сохранения и возврата
        int footY = height - 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(startX, footY, formW / 2 - 4, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new ZoneDetailsScreen(zoneId)))
                .dimensions(startX + formW / 2 + 4, footY, formW / 2 - 4, 20).build());
    }

    private void buildSecondPage(int x, int y) {
        refillModeButton = addDrawableChild(CyclingButtonWidget.<RefillMode>builder(v -> Text.literal(v.name()))
                .values(List.of(RefillMode.ON_ACTIVATION, RefillMode.TIMED, RefillMode.AFTER_DEATH))
                .initially(draft.refillMode)
                .build(x, y, formW, 20, Text.literal("Refill Mode"), (b, v) -> {}));

        int y2 = y + rowH;
        int qW = 108; // Ширина элемента в колонке (108*4 + 3*6 = 450)

        maxAliveField = field(x, y2, qW, String.valueOf(draft.maxAlive));
        spawnCountField = field(x + qW + 6, y2, qW, String.valueOf(draft.spawnCount));
        respawnField = field(x + (qW + 6) * 2, y2, qW, String.valueOf(draft.respawnSeconds));
        chanceField = field(x + (qW + 6) * 3, y2, qW, String.valueOf(draft.chance));

        addDrawableChild(maxAliveField);
        addDrawableChild(spawnCountField);
        addDrawableChild(respawnField);
        addDrawableChild(chanceField);

        int y3 = y2 + rowH;
        int halfW = (formW - 10) / 2; // 220
        retryField = field(x, y3, halfW, String.valueOf(draft.failedSpawnRetrySeconds));
        addDrawableChild(retryField);

        despawnButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.despawnWhenZoneInactive)
                .build(x + halfW + 10, y3, halfW, 20, Text.literal("Despawn When Zone Inactive"), (b, v) -> {}));

        int y4 = y3 + rowH;
        announceButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.announceOnSpawn)
                .build(x, y4, formW, 20, Text.literal("Announce On Spawn"), (b, v) -> {}));

        int y5 = y4 + rowH;
        if (draft.spawnType == SpawnType.PACK) {
            addDrawableChild(ButtonWidget.builder(Text.literal("PACK note: TIMED/low respawn can be farmable"), b -> {})
                    .dimensions(x, y5, formW, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.literal("UNIQUE note: values are editable; defaults are just presets"), b -> {})
                    .dimensions(x, y5, formW, 20).build());
        }
    }

    private void buildThirdPage(int x, int y) {
        int companionCount = draft.companions == null ? 0 : draft.companions.size();

        addDrawableChild(ButtonWidget.builder(Text.literal("Edit Companions"), b -> {
            capture();
            String label = draft.name == null || draft.name.isBlank() ? "(new rule)" : draft.name;
            client.setScreen(new CompanionListScreen(this, draft.companions, label));
        }).dimensions(x, y, 180, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Companions: " + companionCount), b -> {})
                .dimensions(x + 188, y, 120, 20).build());

        int y2 = y + rowH;
        addDrawableChild(ButtonWidget.builder(Text.literal("ID"), b -> {})
                .dimensions(x, y2, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(draft.id == null ? "" : draft.id), b -> {})
                .dimensions(x + 84, y2, formW - 84, 20).build());

        int y3 = y2 + rowH;
        addDrawableChild(ButtonWidget.builder(Text.literal("Back to Basics"), b -> {
            capture(); page = 0; init();
        }).dimensions(x, y3, 140, 20).build());
    }

    private TextFieldWidget field(int x, int y, int w, String value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        f.setText(value == null ? "" : value);
        return f;
    }

    private void capture() {
        if (ruleIdField != null) draft.name = ruleIdField.getText().trim();
        if (entityField != null) draft.entity = entityField.getText().trim();
        if (enabledButton != null) draft.enabled = enabledButton.getValue();
        if (spawnTypeButton != null) draft.spawnType = spawnTypeButton.getValue();
        if (refillModeButton != null) draft.refillMode = refillModeButton.getValue();
        if (maxAliveField != null) draft.maxAlive = parseInt(maxAliveField, draft.maxAlive);
        if (spawnCountField != null) draft.spawnCount = parseInt(spawnCountField, draft.spawnCount);
        if (respawnField != null) draft.respawnSeconds = parseInt(respawnField, draft.respawnSeconds);
        if (chanceField != null) draft.chance = parseDouble(chanceField, draft.chance);
        if (retryField != null) draft.failedSpawnRetrySeconds = parseInt(retryField, draft.failedSpawnRetrySeconds);
        if (despawnButton != null) draft.despawnWhenZoneInactive = despawnButton.getValue();
        if (announceButton != null) draft.announceOnSpawn = announceButton.getValue();
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
        int startX = (width - formW) / 2;
        int topY = 55;

        int ruleY = topY;
        int entityY = topY + rowH;
        int enableY = topY + rowH * 2;
        int dynY = enableY + rowH + 8;

        // Динамический расчет высоты фона
        int contentBottomY = enableY + 20; // Высота для нулевой страницы
        if (page == 1) contentBottomY = dynY + rowH * 4 + 20;
        else if (page == 2) contentBottomY = dynY + rowH * 2 + 20;

        int pad = 12;
        context.fill(startX - pad, 18, startX + formW + pad, contentBottomY + pad, 0x88000000);
        context.fill(startX - pad, 18, startX + formW + pad, 20, 0xFF3ECF8E);

        // Заголовок
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);

        // Подписи базовой части
        context.drawTextWithShadow(textRenderer, "Rule Name", startX, ruleY - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Entity", startX, entityY - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Enabled", startX, enableY - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Spawn Type", startX + 150, enableY - 11, 0xA5FFB5);

        // Подписи динамической части
        if (page == 1) {
            int qW = 108;
            int halfW = (formW - 10) / 2; // 220

            context.drawTextWithShadow(textRenderer, "Refill Mode", startX, dynY - 11, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Max Alive", startX, dynY + rowH - 11, 0x7FD7A5);
            context.drawTextWithShadow(textRenderer, "Spawn Count", startX + qW + 6, dynY + rowH - 11, 0x7FD7A5);
            context.drawTextWithShadow(textRenderer, "Respawn Secs", startX + (qW + 6) * 2, dynY + rowH - 11, 0x7FD7A5);
            context.drawTextWithShadow(textRenderer, "Chance (0..1)", startX + (qW + 6) * 3, dynY + rowH - 11, 0x7FD7A5);

            context.drawTextWithShadow(textRenderer, "Failed Spawn Retry Seconds", startX, dynY + rowH * 2 - 11, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Despawn When Zone Inactive", startX + halfW + 10, dynY + rowH * 2 - 11, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "Announce On Spawn", startX, dynY + rowH * 3 - 11, 0xA5FFB5);

            if (draft.spawnType == SpawnType.PACK) {
                context.drawTextWithShadow(textRenderer, "PACK note: TIMED/low respawn can be farmable", startX, dynY + rowH * 4 - 11, 0xFFAA55);
            } else {
                context.drawTextWithShadow(textRenderer, "UNIQUE note: these values are editable presets", startX, dynY + rowH * 4 - 11, 0xFFCC66);
            }
        } else if (page == 2) {
            context.drawTextWithShadow(textRenderer, "Companions", startX, dynY - 11, 0xA5FFB5);
            context.drawTextWithShadow(textRenderer, "ID", startX, dynY + rowH - 11, 0x888888);
            context.drawTextWithShadow(textRenderer, "Advanced Controls", startX, dynY + rowH * 2 - 11, 0xA5FFB5);
        }

        // Предупреждения (Отрисовываются над нижними кнопками)
        int warningY = height - 65;
        if (page == 1 && refillModeButton != null && refillModeButton.getValue() == RefillMode.TIMED) {
            context.drawCenteredTextWithShadow(textRenderer, "TIMED can create farmable zones. Prefer ON_ACTIVATION for normal mobs.", width / 2, warningY, 0xFFAA55);
            warningY -= 14;
        }

        if (page == 1 && draft.spawnType == SpawnType.PACK && parseInt(respawnField, draft.respawnSeconds) < 300) {
            context.drawCenteredTextWithShadow(textRenderer, "Low respawnSeconds may make this zone farmable.", width / 2, warningY, 0xFFAA55);
        }

        if (!error.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2, height - 50, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
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
        r.companions = src.companions == null ? new ArrayList<>() : new ArrayList<>(src.companions);
        return r;
    }
}
