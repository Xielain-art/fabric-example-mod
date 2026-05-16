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

public class BlockPickerScreen extends Screen {
    private final Screen parent;
    private final BlockSelectionConsumer consumer;
    private String query;
    private TextFieldWidget searchField;
    private int page = 0;

    public BlockPickerScreen(Screen parent, BlockSelectionConsumer consumer, String initialQuery) {
        super(Text.literal("Pick Block"));
        this.parent = parent;
        this.consumer = consumer;
        this.query = initialQuery == null ? "" : initialQuery;
    }

    @Override
    protected void init() {
        clearChildren();

        int panelWidth = ScreenLayout.panelWidth(width, 560);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        int topY = 40;

        int gap = 4;
        int refreshWidth = 84;
        int searchButtonWidth = 66;
        int searchFieldWidth = panelWidth - refreshWidth - searchButtonWidth - gap * 2;

        searchField = new TextFieldWidget(textRenderer, startX, topY, searchFieldWidth, 20, Text.literal("Search"));
        searchField.setText(query);
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Search"), button -> {
                    query = searchField.getText();
                    page = 0;
                    init();
                })
                .dimensions(startX + searchFieldWidth + gap, topY, searchButtonWidth, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> GerbariumClientNetworking.requestBlocks())
                .dimensions(startX + searchFieldWidth + gap + searchButtonWidth + gap, topY, refreshWidth, 20)
                .build());

        List<String> ids = ClientGerbariumData.blockIds();
        String q = query.toLowerCase(Locale.ROOT);
        List<String> filtered = ids.stream().filter(id -> q.isBlank() || id.toLowerCase(Locale.ROOT).contains(q)).toList();

        int listY = topY + 34;
        int bottomSpace = 82;
        int rowHeight = 24;
        int maxRows = Math.max(1, (height - listY - bottomSpace) / rowHeight);
        int maxPage = Math.max(0, (filtered.size() - 1) / maxRows);
        page = Math.max(0, Math.min(page, maxPage));

        int start = page * maxRows;
        for (int i = 0; i < maxRows; i++) {
            int idx = start + i;
            if (idx >= filtered.size()) {
                break;
            }
            String id = filtered.get(idx);
            int rowY = listY + i * rowHeight;
            String label = ScreenLayout.trim(textRenderer, id, panelWidth - 20);

            addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> {
                        consumer.onBlockSelected(id);
                        client.setScreen(parent);
                    })
                    .dimensions(startX, rowY, panelWidth, 20)
                    .build());
        }

        if (filtered.size() > maxRows) {
            int pagY = listY + maxRows * rowHeight + 6;
            int pageBtnWidth = 80;
            int pagStartX = width / 2 - (pageBtnWidth + 56) / 2;

            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); init(); })
                    .dimensions(pagStartX, pagY, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + " / " + (maxPage + 1)), b -> {})
                    .dimensions(pagStartX + 28, pagY, pageBtnWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(maxPage, page + 1); init(); })
                    .dimensions(pagStartX + 32 + pageBtnWidth, pagY, 24, 20).build());
        }

        int backWidth = Math.min(160, panelWidth);
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> client.setScreen(parent))
                .dimensions(width / 2 - backWidth / 2, height - 32, backWidth, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelWidth = ScreenLayout.panelWidth(width, 560);
        int startX = ScreenLayout.panelLeft(width, panelWidth);
        ScreenTheme.drawPanel(context, startX, 15, panelWidth, height - 15);
        context.drawCenteredTextWithShadow(textRenderer, title.getString(), width / 2, 23, ScreenTheme.ACCENT_PRIMARY);
        context.drawTextWithShadow(textRenderer, "Search block ids", startX, 28, ScreenTheme.ACCENT_PRIMARY);

        if (ClientGerbariumData.blockIds().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No block IDs synced yet. Click Refresh.", width / 2, height / 2 - 10, ScreenTheme.TEXT_MUTED);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
