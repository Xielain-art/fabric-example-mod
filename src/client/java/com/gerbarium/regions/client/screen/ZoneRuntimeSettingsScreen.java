package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.client.screen.help.HelpTopic;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Optional;

public class ZoneRuntimeSettingsScreen extends GerbariumScreen {
    private final String zoneId;
    private TextFieldWidget range;
    private TextFieldWidget deactivate;
    private TextFieldWidget firstSpawn;
    private TextFieldWidget reactivate;
    private TextFieldWidget minDist;
    private TextFieldWidget maxDist;
    private TextFieldWidget attempts;
    private CyclingButtonWidget<Boolean> loaded;
    private CyclingButtonWidget<Boolean> vanilla;
    private String error = "";

    public ZoneRuntimeSettingsScreen(String zoneId) {
        super(Text.literal("Zone Settings"), 540, HelpTopic.ZONE_RUNTIME_SETTINGS);
        this.zoneId = zoneId;
    }

    @Override
    protected void initContent() {
        Optional<Zone> oz = ClientGerbariumData.findZone(zoneId);
        if (oz.isEmpty()) {
            return;
        }
        Zone z = oz.get();
        ZoneDefaults.normalizeZone(z);

        AdaptiveLayout.ColumnMetrics cols = AdaptiveLayout.twoColumn(panelW, ScreenTheme.SPACE_LG);
        int col1W = cols.col1Width();
        int col2X = contentX + cols.col2Offset();
        int rowH = 40;
        int y = contentY;

        // Row 1
        range = addField(contentX, y, col1W, z.activation.range);
        deactivate = addField(col2X, y, col1W, z.activation.deactivateAfterSeconds);
        y += rowH;

        // Row 2
        firstSpawn = addField(contentX, y, col1W, z.activation.firstSpawnDelaySeconds);
        reactivate = addField(col2X, y, col1W, z.activation.reactivationCooldownSeconds);
        y += rowH;

        // Row 3
        minDist = addField(contentX, y, col1W, z.spawn.minDistanceFromPlayer);
        maxDist = addField(col2X, y, col1W, z.spawn.maxDistanceFromPlayer);
        y += rowH;

        // Row 4
        attempts = addField(contentX, y, col1W, z.spawn.maxPositionAttempts);
        loaded = CyclingButtonWidget.onOffBuilder(z.spawn.requireLoadedChunk)
                .build(col2X, y, col1W, 20, Text.literal(""), (b, v) -> {});
        addDrawableChild(loaded);
        y += rowH;

        // Row 5
        vanilla = CyclingButtonWidget.onOffBuilder(z.spawn.respectVanillaSpawnRules)
                .build(contentX, y, contentW, 20, Text.literal(""), (b, v) -> {});
        addDrawableChild(vanilla);
    }

    private TextFieldWidget addField(int x, int y, int w, int value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        f.setText(String.valueOf(value));
        addDrawableChild(f);
        return f;
    }

    @Override
    protected void initFooter() {
        AdaptiveLayout.ColumnMetrics cols = AdaptiveLayout.twoColumn(panelW, ScreenTheme.SPACE_LG);
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(contentX, footerY, cols.col1Width(), 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new ZoneDetailsScreen(zoneId)))
                .dimensions(contentX + cols.col2Offset(), footerY, cols.col1Width(), 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        AdaptiveLayout.ColumnMetrics cols = AdaptiveLayout.twoColumn(panelW, ScreenTheme.SPACE_LG);
        int col2X = contentX + cols.col2Offset();
        int rowH = 40;
        int y = contentY;

        // Labels
        context.drawTextWithShadow(textRenderer, "Activation Range", contentX, y - 11, ScreenTheme.TEXT_SECONDARY);
        context.drawTextWithShadow(textRenderer, "Deactivate After (s)", col2X, y - 11, ScreenTheme.TEXT_SECONDARY);
        y += rowH;
        context.drawTextWithShadow(textRenderer, "First Spawn Delay", contentX, y - 11, ScreenTheme.TEXT_SECONDARY);
        context.drawTextWithShadow(textRenderer, "Reactivation Cooldown", col2X, y - 11, ScreenTheme.TEXT_SECONDARY);
        y += rowH;
        context.drawTextWithShadow(textRenderer, "Min Distance", contentX, y - 11, ScreenTheme.TEXT_SECONDARY);
        context.drawTextWithShadow(textRenderer, "Max Distance", col2X, y - 11, ScreenTheme.TEXT_SECONDARY);
        y += rowH;
        context.drawTextWithShadow(textRenderer, "Max Attempts", contentX, y - 11, ScreenTheme.TEXT_SECONDARY);
        context.drawTextWithShadow(textRenderer, "Require Loaded", col2X, y - 11, ScreenTheme.TEXT_SECONDARY);
        y += rowH;
        context.drawTextWithShadow(textRenderer, "Respect Vanilla Spawn Rules", contentX, y - 11, ScreenTheme.TEXT_SECONDARY);

        if (!error.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2, footerY - 20, ScreenTheme.ERROR);
        }
    }

    private void save() {
        try {
            GerbariumClientNetworking.sendUpdateZoneSettings(zoneId,
                    Integer.parseInt(range.getText().trim()),
                    Integer.parseInt(deactivate.getText().trim()),
                    Integer.parseInt(firstSpawn.getText().trim()),
                    Integer.parseInt(reactivate.getText().trim()),
                    Integer.parseInt(minDist.getText().trim()),
                    Integer.parseInt(maxDist.getText().trim()),
                    Integer.parseInt(attempts.getText().trim()),
                    loaded.getValue(),
                    vanilla.getValue());
            client.setScreen(new ZoneDetailsScreen(zoneId));
        } catch (Exception e) {
            error = "Invalid numeric values";
        }
    }
}
