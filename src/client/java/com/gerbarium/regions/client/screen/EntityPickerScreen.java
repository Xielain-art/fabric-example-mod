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

        // Основные параметры контейнера
        int panelWidth = Math.min(480, width - 40);
        int startX = (width - panelWidth) / 2;
        int topY = 40; // Y позиция для строки поиска

        // --- БЛОК 1: Строка поиска ---
        int gap = 4;
        int refreshWidth = 80;
        int searchButtonWidth = 60;
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

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> GerbariumClientNetworking.requestEntities())
                .dimensions(startX + searchFieldWidth + gap + searchButtonWidth + gap, topY, refreshWidth, 20)
                .build());

        // --- БЛОК 2: Фильтрация и Список сущностей ---
        List<String> ids = ClientGerbariumData.entityIds();
        String q = query.toLowerCase(Locale.ROOT);
        List<String> filtered = ids.stream().filter(id -> q.isBlank() || id.toLowerCase(Locale.ROOT).contains(q)).toList();

        int listY = topY + 30; // Начало списка элементов
        int bottomSpace = 75;  // Пространство внизу для пагинации и кнопки Back
        int rowHeight = 24;    // Высота одной кнопки (20) + отступ (4)

        int maxRows = Math.max(1, (height - listY - bottomSpace) / rowHeight);
        pageSize = maxRows;

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

            addDrawableChild(ButtonWidget.builder(Text.literal(trimToWidth(id, panelWidth - 20)), button -> {
                        consumer.onEntitySelected(id);
                        client.setScreen(parent);
                    })
                    .dimensions(startX, rowY, panelWidth, 20)
                    .build());
        }

        // --- БЛОК 3: Пагинация (Центрирована под списком) ---
        if (filtered.size() > maxRows) {
            int pagY = listY + maxRows * rowHeight + 6;
            int pageBtnWidth = 80;
            int pagStartX = width / 2 - (pageBtnWidth + 56) / 2; // 56 = две кнопки по 24 + отступы по 4

            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); init(); })
                    .dimensions(pagStartX, pagY, 24, 20).build());

            // Заглушка-кнопка для отображения номера страницы в едином стиле
            addDrawableChild(ButtonWidget.builder(Text.literal("Page " + (page + 1) + " / " + (maxPage + 1)), b -> {})
                    .dimensions(pagStartX + 28, pagY, pageBtnWidth, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(maxPage, page + 1); init(); })
                    .dimensions(pagStartX + 32 + pageBtnWidth, pagY, 24, 20).build());
        }

        // --- БЛОК 4: Кнопка Back (Футер) ---
        int backButtonWidth = 150;
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> client.setScreen(parent))
                .dimensions(width / 2 - backButtonWidth / 2, height - 32, backButtonWidth, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        // Отрисовка подложки модального окна
        int panelWidth = Math.min(480, width - 40);
        int startX = (width - panelWidth) / 2;
        int padding = 12;

        int panelTop = 15;
        int panelBottom = height - 15;

        // Темный фон
        context.fill(startX - padding, panelTop, startX + panelWidth + padding, panelBottom, 0x88000000);
        // Декоративная акцентная линия сверху
        context.fill(startX - padding, panelTop, startX + panelWidth + padding, panelTop + 2, 0xFF3ECF8E);

        // Заголовок
        context.drawCenteredTextWithShadow(textRenderer, this.title.getString(), width / 2, panelTop + 8, 0xFFFFFF);

        // Сообщение о пустом списке
        if (ClientGerbariumData.entityIds().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No entity IDs synced yet. Click Refresh.", width / 2, height / 2 - 10, 0xFFAAAA);
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