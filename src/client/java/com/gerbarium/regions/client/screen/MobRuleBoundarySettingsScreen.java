package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.client.screen.help.HelpTopic;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

public class MobRuleBoundarySettingsScreen extends GerbariumScreen {
    private final MobRule draft;
    private final MobRuleEditScreen parent;
    private String error = "";
    private String info = "";
    private CyclingButtonWidget<String> modeButton;
    private CyclingButtonWidget<Boolean> teleportBackButton;
    private TextFieldWidget maxOutsideField;
    private TextFieldWidget intervalField;

    public MobRuleBoundarySettingsScreen(MobRuleEditScreen parent, MobRule draft) {
        super(Text.literal("Boundary Control"), 460, HelpTopic.MOB_RULE_BOUNDARY);
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
    protected void initContent() {
        AdaptiveLayout.ColumnMetrics cols = AdaptiveLayout.twoColumn(panelW, ScreenTheme.SPACE_LG);
        int col1W = cols.col1Width();
        int col2X = contentX + cols.col2Offset();
        int y = contentY + 10;

        modeButton = addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal)
                .values(List.of(MobRule.BOUNDARY_NONE, MobRule.BOUNDARY_LEASH, MobRule.BOUNDARY_TELEPORT_BACK, MobRule.BOUNDARY_REMOVE_OUTSIDE))
                .initially(draft.boundaryMode)
                .build(contentX, y, contentW, 20, Text.literal("Boundary Mode"), (b, v) -> {
                    draft.boundaryMode = v;
                    updateInfo();
                }));
        y += 40;

        maxOutsideField = addField(contentX, y, col1W, String.valueOf(draft.boundaryMaxOutsideSeconds));
        intervalField = addField(col2X, y, col1W, String.valueOf(draft.boundaryCheckIntervalTicks));
        y += 40;

        teleportBackButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(draft.boundaryTeleportBack)
                .build(contentX, y, col1W, 20, Text.literal("Teleport Back"), (b, v) -> draft.boundaryTeleportBack = v));

        updateInfo();
    }

    private TextFieldWidget addField(int x, int y, int w, String value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        f.setText(value == null ? "" : value);
        addDrawableChild(f);
        return f;
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

    @Override
    protected void initFooter() {
        AdaptiveLayout.ColumnMetrics cols = AdaptiveLayout.twoColumn(panelW, ScreenTheme.SPACE_LG);
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(contentX, footerY, cols.col1Width(), 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent))
                .dimensions(contentX + cols.col2Offset(), footerY, cols.col1Width(), 20).build());
    }

    private void capture() {
        if (modeButton != null) draft.boundaryMode = modeButton.getValue();
        if (maxOutsideField != null) draft.boundaryMaxOutsideSeconds = parseInt(maxOutsideField, draft.boundaryMaxOutsideSeconds);
        if (intervalField != null) draft.boundaryCheckIntervalTicks = parseInt(intervalField, draft.boundaryCheckIntervalTicks);
        if (teleportBackButton != null) draft.boundaryTeleportBack = teleportBackButton.getValue();
    }

    private int parseInt(TextFieldWidget field, int fallback) {
        try { return Integer.parseInt(field.getText().trim()); }
        catch (Exception e) { return fallback; }
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
        super.render(context, mouseX, mouseY, delta);

        AdaptiveLayout.ColumnMetrics cols = AdaptiveLayout.twoColumn(panelW, ScreenTheme.SPACE_LG);
        int col2X = contentX + cols.col2Offset();
        int y = contentY + 10;

        context.drawTextWithShadow(textRenderer, "Boundary Mode", contentX, y - 11, ScreenTheme.TEXT_SECONDARY);
        y += 40;
        context.drawTextWithShadow(textRenderer, "Max Outside Seconds", contentX, y - 11, ScreenTheme.TEXT_SECONDARY);
        context.drawTextWithShadow(textRenderer, "Check Interval Ticks", col2X, y - 11, ScreenTheme.TEXT_SECONDARY);
        y += 40;
        context.drawTextWithShadow(textRenderer, "Teleport Back", contentX, y - 11, ScreenTheme.TEXT_SECONDARY);

        // Info text
        int infoY = footerY - 50;
        ScreenLayout.drawWrapped(context, textRenderer, info, contentX, infoY, contentW, ScreenTheme.WARNING);
        if (!error.isBlank()) {
            ScreenLayout.drawWrapped(context, textRenderer, error, contentX, infoY + 20, contentW, ScreenTheme.ERROR);
        }
    }
}
