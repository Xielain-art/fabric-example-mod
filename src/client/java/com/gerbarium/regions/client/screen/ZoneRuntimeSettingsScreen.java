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

        // Параметры сетки
        int panelWidth = 400;
        int startX = (width - panelWidth) / 2;
        int colW = (panelWidth - 16) / 2; // 192 пикселя на колонку
        int col1X = startX;
        int col2X = startX + colW + 16;
        int startY = 60;
        int rowSpacing = 45;

        // Строка 1
        range = field(col1X, startY, colW, z.activation.range);
        deactivate = field(col2X, startY, colW, z.activation.deactivateAfterSeconds);

        // Строка 2
        int y2 = startY + rowSpacing;
        firstSpawn = field(col1X, y2, colW, z.activation.firstSpawnDelaySeconds);
        reactivate = field(col2X, y2, colW, z.activation.reactivationCooldownSeconds);

        // Строка 3
        int y3 = y2 + rowSpacing;
        minDist = field(col1X, y3, colW, z.spawn.minDistanceFromPlayer);
        maxDist = field(col2X, y3, colW, z.spawn.maxDistanceFromPlayer);

        // Строка 4
        int y4 = y3 + rowSpacing;
        attempts = field(col1X, y4, colW, z.spawn.maxPositionAttempts);
        loaded = addDrawableChild(CyclingButtonWidget.onOffBuilder(z.spawn.requireLoadedChunk)
                .build(col2X, y4, colW, 20, Text.literal("Require Loaded Chunk"), (b, v) -> {}));

        // Строка 5 (кнопка на всю ширину панели)
        int y5 = y4 + rowSpacing;
        vanilla = addDrawableChild(CyclingButtonWidget.onOffBuilder(z.spawn.respectVanillaSpawnRules)
                .build(col1X, y5, panelWidth, 20, Text.literal("Respect Vanilla"), (b, v) -> {}));

        addDrawableChild(range);
        addDrawableChild(deactivate);
        addDrawableChild(firstSpawn);
        addDrawableChild(reactivate);
        addDrawableChild(minDist);
        addDrawableChild(maxDist);
        addDrawableChild(attempts);

        // Футер
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

        int panelWidth = 400;
        int startX = (width - panelWidth) / 2;
        int colW = (panelWidth - 16) / 2;
        int col1X = startX;
        int col2X = startX + colW + 16;
        int startY = 60;
        int rowSpacing = 45;

        // Отрисовка фона панели
        int padding = 12;
        int panelTop = 15;
        int y5 = startY + rowSpacing * 4;
        int panelBottom = y5 + 35; // Высота до конца контента + отступ

        context.fill(startX - padding, panelTop, startX + panelWidth + padding, panelBottom, 0x88000000);
        context.fill(startX - padding, panelTop, startX + panelWidth + padding, panelTop + 2, 0xFF3ECF8E);

        // Заголовок
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelTop + 8, 0xFFFFFF);

        // Подписи (Labels)
        int labelOffset = 11;

        // Строка 1
        context.drawTextWithShadow(textRenderer, "Activation Range", col1X, startY - labelOffset, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Deactivate After Seconds", col2X, startY - labelOffset, 0xA5FFB5);

        // Строка 2
        int y2 = startY + rowSpacing;
        context.drawTextWithShadow(textRenderer, "First Spawn Delay", col1X, y2 - labelOffset, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Reactivation Cooldown", col2X, y2 - labelOffset, 0xA5FFB5);

        // Строка 3
        int y3 = y2 + rowSpacing;
        context.drawTextWithShadow(textRenderer, "Min Distance", col1X, y3 - labelOffset, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Max Distance", col2X, y3 - labelOffset, 0xA5FFB5);

        // Строка 4
        int y4 = y3 + rowSpacing;
        context.drawTextWithShadow(textRenderer, "Max Position Attempts", col1X, y4 - labelOffset, 0xA5FFB5);

        // Вывод ошибок (центрируется над кнопками под панелью)
        if (!error.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2, height - 55, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}