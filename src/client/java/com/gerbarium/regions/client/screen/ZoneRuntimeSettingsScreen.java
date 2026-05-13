package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Optional;

public class ZoneRuntimeSettingsScreen extends Screen {
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
        super(Text.literal("Runtime Settings"));
        this.zoneId = zoneId;
    }

    @Override
    protected void init() {
        clearChildren();
        Optional<Zone> oz = ClientGerbariumData.findZone(zoneId);
        if (oz.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new ZoneDetailsScreen(zoneId)))
                    .dimensions(width / 2 - 60, height / 2 + 20, 120, 20).build());
            return;
        }

        Zone z = oz.get();
        ZoneDefaults.normalizeZone(z);

        int panelWidth = ScreenLayout.panelWidth(width, 540);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        int colW = (panelWidth - 16) / 2;
        int col1X = startX;
        int col2X = startX + colW + 16;
        int startY = 60;
        int rowSpacing = 45;

        range = field(col1X, startY, colW, z.activation.range);
        deactivate = field(col2X, startY, colW, z.activation.deactivateAfterSeconds);
        firstSpawn = field(col1X, startY + rowSpacing, colW, z.activation.firstSpawnDelaySeconds);
        reactivate = field(col2X, startY + rowSpacing, colW, z.activation.reactivationCooldownSeconds);
        minDist = field(col1X, startY + rowSpacing * 2, colW, z.spawn.minDistanceFromPlayer);
        maxDist = field(col2X, startY + rowSpacing * 2, colW, z.spawn.maxDistanceFromPlayer);
        attempts = field(col1X, startY + rowSpacing * 3, colW, z.spawn.maxPositionAttempts);
        loaded = addDrawableChild(CyclingButtonWidget.onOffBuilder(z.spawn.requireLoadedChunk)
                .build(col2X, startY + rowSpacing * 3, colW, 20, Text.literal("Require Loaded Chunk"), (b, v) -> {}));
        vanilla = addDrawableChild(CyclingButtonWidget.onOffBuilder(z.spawn.respectVanillaSpawnRules)
                .build(col1X, startY + rowSpacing * 4, panelWidth, 20, Text.literal("Respect Vanilla"), (b, v) -> {}));

        addDrawableChild(range);
        addDrawableChild(deactivate);
        addDrawableChild(firstSpawn);
        addDrawableChild(reactivate);
        addDrawableChild(minDist);
        addDrawableChild(maxDist);
        addDrawableChild(attempts);

        int footerY = height - 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(col1X, footerY, colW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new ZoneDetailsScreen(zoneId)))
                .dimensions(col2X, footerY, colW, 20).build());
    }

    private TextFieldWidget field(int x, int y, int w, int value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        f.setText(String.valueOf(value));
        return f;
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        Optional<Zone> oz = ClientGerbariumData.findZone(zoneId);
        if (oz.isEmpty()) {
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        int panelWidth = ScreenLayout.panelWidth(width, 540);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        int panelBottom = startX + panelWidth;
        ScreenLayout.drawPanel(context, startX, 15, panelWidth, height - 15);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 23, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Activation / spawn runtime controls", startX, 31, 0xA5FFB5);

        int startY = 60;
        int rowSpacing = 45;
        int colW = (panelWidth - 16) / 2;
        int col1X = startX;
        int col2X = startX + colW + 16;
        context.drawTextWithShadow(textRenderer, "Activation Range", col1X, startY - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Deactivate After Seconds", col2X, startY - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "First Spawn Delay", col1X, startY + rowSpacing - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Reactivation Cooldown", col2X, startY + rowSpacing - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Min Distance", col1X, startY + rowSpacing * 2 - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Max Distance", col2X, startY + rowSpacing * 2 - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Max Position Attempts", col1X, startY + rowSpacing * 3 - 11, 0xA5FFB5);

        if (!error.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2, height - 55, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
