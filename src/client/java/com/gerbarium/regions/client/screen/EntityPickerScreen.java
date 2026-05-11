package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

public class EntityPickerScreen extends Screen {
    private final MobRuleEditScreen parent;
    private String query;
    private TextFieldWidget searchField;

    public EntityPickerScreen(MobRuleEditScreen parent, String initialQuery) {
        super(Text.literal("Pick Entity"));
        this.parent = parent;
        this.query = initialQuery == null ? "" : initialQuery;
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        searchField = new TextFieldWidget(textRenderer, centerX - 160, 35, 240, 20, Text.literal("Search"));
        searchField.setText(query);
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Search"), button -> {
                    query = searchField.getText();
                    init();
                })
                .dimensions(centerX + 90, 35, 70, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh IDs"), button -> GerbariumClientNetworking.requestEntities())
                .dimensions(centerX + 170, 35, 90, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> client.setScreen(parent))
                .dimensions(centerX - 50, height - 30, 100, 20)
                .build());

        List<String> ids = ClientGerbariumData.entityIds();
        String q = query.toLowerCase(Locale.ROOT);

        int shown = 0;
        int startX = centerX - 180;
        int startY = 70;

        for (String id : ids) {
            if (shown >= 12) break;

            if (!q.isBlank() && !id.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }

            int rowY = startY + shown * 22;
            addDrawableChild(ButtonWidget.builder(Text.literal(id), button -> {
                        parent.setEntityId(id);
                        client.setScreen(parent);
                    })
                    .dimensions(startX, rowY, 360, 20)
                    .build());

            shown++;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        context.drawCenteredTextWithShadow(textRenderer, "Pick Entity ID", width / 2, 15, 0xFFFFFF);

        if (ClientGerbariumData.entityIds().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No entity IDs synced yet. Click Refresh IDs.", width / 2, height / 2, 0xFFAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}