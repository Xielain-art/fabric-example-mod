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
    private final Screen parent;
    private final EntitySelectionConsumer consumer;
    private String query;
    private TextFieldWidget searchField;
    private int page = 0;
    private int pageSize = 1;

    public EntityPickerScreen(Screen parent, EntitySelectionConsumer consumer, String initialQuery) {
        super(Text.literal("Pick Entity"));
        this.parent = parent;
        this.consumer = consumer;
        this.query = initialQuery == null ? "" : initialQuery;
    }

    @Override
    protected void init() {
        clearChildren();

        int gap = 4;
        int refreshWidth = 100;
        int searchButtonWidth = 60;

        int panelWidth = Math.max(220, Math.min(width - 60, 500));
        int panelX = (width - panelWidth) / 2;

        int topY = 28;
        int searchFieldWidth = panelWidth - refreshWidth - searchButtonWidth - gap * 2;

        if (searchFieldWidth < 60) {
            searchFieldWidth = 60;
            panelWidth = searchFieldWidth + refreshWidth + searchButtonWidth + gap * 2;
            panelX = (width - panelWidth) / 2;
        }

        searchField = new TextFieldWidget(textRenderer, panelX, topY, searchFieldWidth, 20, Text.literal("Search"));
        searchField.setText(query);
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Search"), button -> {
                    query = searchField.getText();
                    page = 0;
                    init();
                })
                .dimensions(panelX + searchFieldWidth + gap, topY, searchButtonWidth, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> GerbariumClientNetworking.requestEntities())
                .dimensions(panelX + searchFieldWidth + gap + searchButtonWidth + gap, topY, refreshWidth, 20)
                .build());

        int backButtonWidth = 120;
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> client.setScreen(parent))
                .dimensions(width / 2 - backButtonWidth / 2, height - 28, backButtonWidth, 20)
                .build());

        List<String> ids = ClientGerbariumData.entityIds();
        String q = query.toLowerCase(Locale.ROOT);
        List<String> filtered = ids.stream().filter(id -> q.isBlank() || id.toLowerCase(Locale.ROOT).contains(q)).toList();

        int listY = topY + 28;
        int rowHeight = 22;
        int bottomSpace = 38;
        int maxRows = Math.max(1, (height - listY - bottomSpace) / rowHeight);
        pageSize = maxRows;
        page = Math.max(0, Math.min(page, Math.max(0, (filtered.size() - 1) / maxRows)));

        if (filtered.size() > maxRows) {
            int maxPage = (filtered.size() - 1) / maxRows;
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); init(); })
                    .dimensions(panelX + panelWidth - 80, topY - 22, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(maxPage, page + 1); init(); })
                    .dimensions(panelX + panelWidth - 24, topY - 22, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + "/" + (maxPage + 1)), b -> {})
                    .dimensions(panelX + panelWidth - 58, topY - 22, 32, 20).build());
        }

        int start = page * maxRows;
        for (int i = 0; i < maxRows; i++) {
            int idx = start + i;
            if (idx >= filtered.size()) {
                break;
            }
            String id = filtered.get(idx);
            int rowY = listY + i * rowHeight;

            addDrawableChild(ButtonWidget.builder(Text.literal(trimToWidth(id, panelWidth - 20)), button -> {
                        consumer.onEntitySelected(id);
                        client.setScreen(parent);
                    })
                    .dimensions(panelX, rowY, panelWidth, 20)
                    .build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int gap = 4;
        int refreshWidth = 100;
        int searchButtonWidth = 60;
        int panelWidth = Math.max(220, Math.min(width - 60, 500));
        int searchFieldWidth = panelWidth - refreshWidth - searchButtonWidth - gap * 2;
        if (searchFieldWidth < 60) {
            panelWidth = 60 + refreshWidth + searchButtonWidth + gap * 2;
        }
        int panelX = (width - panelWidth) / 2;

        int panelTop = 10;
        int panelBottom = height - 35;
        int padding = 8;

        context.fill(panelX - padding, panelTop, panelX + panelWidth + padding, panelBottom, 0x66000000);
        context.fill(panelX - padding, panelTop, panelX + panelWidth + padding, panelTop + 2, 0xFF3ECF8E);
        context.drawCenteredTextWithShadow(textRenderer, this.title.getString(), width / 2, panelTop + 6, 0xFFFFFF);

        if (ClientGerbariumData.entityIds().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No entity IDs synced yet. Click Refresh.", width / 2, height / 2, 0xFFAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        String result = text;

        while (!result.isEmpty() && textRenderer.getWidth(result + suffix) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }

        return result + suffix;
    }
}
