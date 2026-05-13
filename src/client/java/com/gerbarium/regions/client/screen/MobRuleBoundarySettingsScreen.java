package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

public class MobRuleBoundarySettingsScreen extends Screen {
    private final MobRule draft;
    private final MobRuleEditScreen parent;
    private String error = "";
    private String info = "";
    private CyclingButtonWidget<String> modeButton;
    private CyclingButtonWidget<Boolean> teleportBackButton;
    private TextFieldWidget maxOutsideField;
    private TextFieldWidget intervalField;

    public MobRuleBoundarySettingsScreen(MobRuleEditScreen parent, MobRule draft) {
        super(Text.literal("Boundary Control"));
        this.parent = parent;
        this.draft = draft;
        if (!ZoneDefaults.isValidBoundaryMode(this.draft.boundaryMode)) {
            this.draft.boundaryMode = ZoneDefaults.defaultBoundaryModeFor(this.draft.spawnType);
            this.error = "Unknown boundary mode reset to default.";
        }
        ZoneDefaults.normalizeMobRule(this.draft);
        if (this.draft.boundaryModeWasInvalid) {
            this.error = "Unknown boundary mode reset to default.";
        }
    }

    @Override
    protected void init() {
        clearChildren();

        int panelWidth = ScreenLayout.panelWidth(width, 460);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        int topY = 52;

        modeButton = addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal)
                .values(List.of(MobRule.BOUNDARY_NONE, MobRule.BOUNDARY_LEASH, MobRule.BOUNDARY_TELEPORT_BACK, MobRule.BOUNDARY_REMOVE_OUTSIDE))
                .initially(draft.boundaryMode)
                .build(startX, topY, panelWidth, 20, Text.literal("Boundary Mode"), (b, v) -> {
                    draft.boundaryMode = v;
                    updateInfo();
                }));

        int y2 = topY + 36;
        int half = (panelWidth - 10) / 2;
        maxOutsideField = field(startX, y2, half, String.valueOf(draft.boundaryMaxOutsideSeconds));
        intervalField = field(startX + half + 10, y2, half, String.valueOf(draft.boundaryCheckIntervalTicks));
        addDrawableChild(maxOutsideField);
        addDrawableChild(intervalField);

        int y3 = y2 + 36;
        teleportBackButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.boundaryTeleportBack)
                .build(startX, y3, 160, 20, Text.literal("Teleport Back"), (b, v) -> draft.boundaryTeleportBack = v));

        if (panelWidth >= 380) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                    .dimensions(startX, height - 35, 180, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent))
                    .dimensions(startX + 190, height - 35, 180, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                    .dimensions(startX, height - 58, panelWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent))
                    .dimensions(startX, height - 35, panelWidth, 20).build());
        }

        updateInfo();
    }

    private TextFieldWidget field(int x, int y, int w, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        field.setText(value == null ? "" : value);
        return field;
    }

    private void updateInfo() {
        String mode = modeButton == null ? draft.boundaryMode : modeButton.getValue();
        if (MobRule.BOUNDARY_REMOVE_OUTSIDE.equals(mode)) {
            info = "Warning: this may remove mobs if players pull them outside the zone.";
        } else if (MobRule.BOUNDARY_NONE.equals(mode)) {
            info = "Mob can leave the zone.";
        } else if (MobRule.BOUNDARY_TELEPORT_BACK.equals(mode)) {
            info = "Mob will be teleported back inside the zone.";
        } else {
            info = "Mob will be returned if it stays outside the zone too long.";
        }
    }

    private void capture() {
        if (modeButton != null) {
            draft.boundaryMode = modeButton.getValue();
        }
        if (maxOutsideField != null) {
            draft.boundaryMaxOutsideSeconds = parseInt(maxOutsideField, draft.boundaryMaxOutsideSeconds);
        }
        if (intervalField != null) {
            draft.boundaryCheckIntervalTicks = parseInt(intervalField, draft.boundaryCheckIntervalTicks);
        }
        if (teleportBackButton != null) {
            draft.boundaryTeleportBack = teleportBackButton.getValue();
        }
    }

    private int parseInt(TextFieldWidget field, int fallback) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception e) {
            return fallback;
        }
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
        draft.boundaryModeWasInvalid = false;
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelWidth = ScreenLayout.panelWidth(width, 460);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        ScreenLayout.drawPanel(context, startX, 20, panelWidth, height - 20);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Boundary settings only. No runtime behavior here.", startX, 31, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Mode", startX, 41, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Max Outside Seconds", startX, 77, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Check Interval Ticks", startX + (panelWidth - 10) / 2 + 10, 77, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Teleport Back", startX, 113, 0xA5FFB5);

        int helpY = panelWidth >= 380 ? height - 74 : height - 104;
        ScreenLayout.drawWrapped(context, textRenderer, explainMode(modeButton == null ? draft.boundaryMode : modeButton.getValue()), startX, helpY, panelWidth, 0xCCCCCC);
        if (!info.isBlank()) {
            ScreenLayout.drawWrapped(context, textRenderer, info, startX, helpY + 22, panelWidth, 0xAAAAAA);
        }
        if (!error.isBlank()) {
            ScreenLayout.drawWrapped(context, textRenderer, error, startX, helpY + 44, panelWidth, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String explainMode(String mode) {
        if (MobRule.BOUNDARY_REMOVE_OUTSIDE.equals(mode)) {
            return "REMOVE_OUTSIDE: Mob will be removed if it stays outside too long. Runtime should not count this as a normal death.";
        }
        if (MobRule.BOUNDARY_NONE.equals(mode)) {
            return "NONE: Mob can leave the zone.";
        }
        if (MobRule.BOUNDARY_TELEPORT_BACK.equals(mode)) {
            return "TELEPORT_BACK: Mob will be teleported back inside the zone.";
        }
        return "LEASH: Mob will be returned if it stays outside the zone too long.";
    }
}
