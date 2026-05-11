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
    private TextFieldWidget range; private TextFieldWidget deactivate; private TextFieldWidget firstSpawn; private TextFieldWidget reactivate;
    private TextFieldWidget minDist; private TextFieldWidget maxDist; private TextFieldWidget attempts;
    private CyclingButtonWidget<Boolean> loaded; private CyclingButtonWidget<Boolean> vanilla;
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
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new ZoneDetailsScreen(zoneId))).dimensions(width / 2 - 60, height / 2 + 20, 120, 20).build());
            return;
        }
        Zone z = oz.get(); ZoneDefaults.normalizeZone(z);
        int x = width / 2 - 170; int y = 34;
        range = field(x, y += 20, 160, z.activation.range);
        deactivate = field(x + 180, y, 160, z.activation.deactivateAfterSeconds);
        firstSpawn = field(x, y += 32, 160, z.activation.firstSpawnDelaySeconds);
        reactivate = field(x + 180, y, 160, z.activation.reactivationCooldownSeconds);
        minDist = field(x, y += 32, 160, z.spawn.minDistanceFromPlayer);
        maxDist = field(x + 180, y, 160, z.spawn.maxDistanceFromPlayer);
        attempts = field(x, y += 32, 160, z.spawn.maxPositionAttempts);
        loaded = addDrawableChild(CyclingButtonWidget.onOffBuilder(z.spawn.requireLoadedChunk).build(x + 180, y, 160, 20, Text.literal("Require Loaded Chunk"), (b, v) -> {}));
        vanilla = addDrawableChild(CyclingButtonWidget.onOffBuilder(z.spawn.respectVanillaSpawnRules).build(x, y += 32, 160, 20, Text.literal("Respect Vanilla"), (b, v) -> {}));

        addDrawableChild(range); addDrawableChild(deactivate); addDrawableChild(firstSpawn); addDrawableChild(reactivate); addDrawableChild(minDist); addDrawableChild(maxDist); addDrawableChild(attempts);
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save()).dimensions(x, height - 32, 166, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new ZoneDetailsScreen(zoneId))).dimensions(x + 174, height - 32, 166, 20).build());
    }

    private TextFieldWidget field(int x, int y, int w, int value) { TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal("")); f.setText(String.valueOf(value)); return f; }

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
        int x = width / 2 - 170; int y = 54;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Activation Range", x, y - 10, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Deactivate After Seconds", x + 180, y - 10, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "First Spawn Delay", x, y + 22, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Reactivation Cooldown", x + 180, y + 22, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Min Distance", x, y + 54, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Max Distance", x + 180, y + 54, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Max Position Attempts", x, y + 86, 0xA5FFB5);
        if (!error.isBlank()) context.drawTextWithShadow(textRenderer, error, x, height - 46, 0xFF5555);
        super.render(context, mouseX, mouseY, delta);
    }
}
