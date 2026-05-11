package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Optional;

public class ZoneDetailsScreen extends Screen {
    private final String zoneId;
    private int page = 0;
    private int pageSize = 0;
    private int totalRules = 0;

    public ZoneDetailsScreen(String zoneId) {
        super(Text.literal("Zone Details"));
        this.zoneId = zoneId;
    }

    @Override
    protected void init() {
        clearChildren();
        Optional<Zone> oz = ClientGerbariumData.findZone(zoneId);
        if (oz.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new RegionsScreen("")))
                    .dimensions(width / 2 - 60, height / 2 + 20, 120, 20).build());
            return;
        }
        Zone z = oz.get();
        ZoneDefaults.normalizeZone(z);

        int x = width / 2 - 180;
        addDrawableChild(ButtonWidget.builder(Text.literal(z.enabled ? "Disable" : "Enable"), b -> GerbariumClientNetworking.toggleZone(zoneId)).dimensions(x, 30, 86, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Select WE"), b -> GerbariumClientNetworking.selectZone(zoneId)).dimensions(x + 90, 30, 86, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("TP"), b -> GerbariumClientNetworking.tpToZone(zoneId)).dimensions(x + 180, 30, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Runtime"), b -> client.setScreen(new ZoneRuntimeSettingsScreen(zoneId))).dimensions(x + 234, 30, 86, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Add Rule"), b -> client.setScreen(new MobRuleEditScreen(zoneId, null))).dimensions(x + 324, 30, 86, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), b -> GerbariumClientNetworking.requestZones()).dimensions(width / 2 - 120, height - 30, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(new RegionsScreen(zoneId))).dimensions(width / 2 + 10, height - 30, 110, 20).build());

        int listY = 85;
        int rowH = 38;
        int visible = Math.max(1, (height - 140) / rowH);
        int total = z.mobs == null ? 0 : z.mobs.size();
        pageSize = visible;
        totalRules = total;
        page = Math.max(0, Math.min(page, Math.max(0, (total - 1) / visible)));

        if (total > visible) {
            int maxPage = (total - 1) / visible;
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); init(); }).dimensions(width - 126, listY, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(maxPage, page + 1); init(); }).dimensions(width - 42, listY, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + "/" + (maxPage + 1)), b -> {}).dimensions(width - 98, listY, 52, 20).build());
        }

        int start = page * visible;
        for (int i = 0; i < visible; i++) {
            int idx = start + i;
            if (idx >= total) break;
            MobRule r = z.mobs.get(idx);
            int y = listY + 26 + i * rowH;
            addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), b -> client.setScreen(new MobRuleEditScreen(zoneId, r))).dimensions(width - 126, y, 50, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Del"), b -> GerbariumClientNetworking.removeMobRule(zoneId, r.id)).dimensions(width - 70, y, 44, 20).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        Optional<Zone> oz = ClientGerbariumData.findZone(zoneId);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);
        if (oz.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "Zone not found", width / 2, height / 2 - 10, 0xFF5555);
            super.render(context, mouseX, mouseY, delta);
            return;
        }
        Zone z = oz.get();
        ZoneDefaults.normalizeZone(z);
        int x = 20;
        context.drawTextWithShadow(textRenderer, "Zone: " + z.id, x, 58, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Dimension: " + z.dimension, x, 70, 0xCCCCCC);
        context.drawTextWithShadow(textRenderer, "Min: " + z.min.x + " " + z.min.y + " " + z.min.z, x, 82, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Max: " + z.max.x + " " + z.max.y + " " + z.max.z, x, 94, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Mob Rules", 20, 118, 0xA5FFB5);

        int listY = 111;
        int rowH = 38;
        int visible = Math.max(1, (height - 140) / rowH);
        int start = page * visible;
        for (int i = 0; z.mobs != null && i < visible; i++) {
            int idx = start + i;
            if (idx >= z.mobs.size()) break;
            MobRule r = z.mobs.get(idx);
            int y = listY + 26 + i * rowH;
            String title = r.name == null || r.name.isBlank() ? r.id : r.name;
            context.drawTextWithShadow(textRenderer, title + " -> " + r.entity, 20, y, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, summary(r), 20, y + 12, 0xAAAAAA);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private String summary(MobRule rule) {
        int companions = rule.companions == null ? 0 : rule.companions.size();
        String chance = rule.chance >= 1.0 ? "guaranteed" : ("chance " + (int) (rule.chance * 100) + "%");
        if (rule.spawnType.name().equals("UNIQUE")) {
            return "[UNIQUE] cooldown " + rule.respawnSeconds + "s | " + chance + " | companions " + companions;
        }
        String s = "[PACK] max " + rule.maxAlive + " | spawn " + rule.spawnCount + " | " + rule.refillMode + " | cooldown " + rule.respawnSeconds + "s | " + chance + " | companions " + companions;
        if (rule.refillMode.name().equals("TIMED") || rule.respawnSeconds < 300) s += " | farm risk";
        return s;
    }
}
