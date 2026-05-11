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
        clearChildren();

        int gap = 4; // Аккуратные отступы между элементами поиска
        int refreshWidth = 100;
        int searchButtonWidth = 60;

        // Адаптивная ширина панели (максимум 500 для эстетики, минимум для вмещения кнопок)
        int panelWidth = Math.max(220, Math.min(width - 60, 500));
        int panelX = (width - panelWidth) / 2;

        int topY = 28; // Позиция строки поиска (сдвинута выше для большей вместимости)

        int searchFieldWidth = panelWidth - refreshWidth - searchButtonWidth - gap * 2;

        // Защита от "схлопывания" строки поиска на больших масштабах GUI
        if (searchFieldWidth < 60) {
            searchFieldWidth = 60;
            panelWidth = searchFieldWidth + refreshWidth + searchButtonWidth + gap * 2;
            panelX = (width - panelWidth) / 2;
        }

        searchField = new TextFieldWidget(
                textRenderer,
                panelX,
                topY,
                searchFieldWidth,
                20,
                Text.literal("Search")
        );
        searchField.setText(query);
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Search"), button -> {
                    query = searchField.getText();
                    init();
                })
                .dimensions(panelX + searchFieldWidth + gap, topY, searchButtonWidth, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> GerbariumClientNetworking.requestEntities())
                .dimensions(panelX + searchFieldWidth + gap + searchButtonWidth + gap, topY, refreshWidth, 20)
                .build());

        // Кнопка Back всегда закреплена внизу
        int backButtonWidth = 120;
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> client.setScreen(parent))
                .dimensions(width / 2 - backButtonWidth / 2, height - 28, backButtonWidth, 20)
                .build());

        List<String> ids = ClientGerbariumData.entityIds();
        String q = query.toLowerCase(Locale.ROOT);

        int shown = 0;
        int listY = topY + 28; // Начало списка под строкой поиска
        int rowHeight = 22; // Высота шага для кнопок
        int bottomSpace = 38; // Зона, которую нельзя занимать (для кнопки Back)

        // Строгий расчет количества рядов, чтобы не вылезать за пределы экрана
        int maxRows = Math.max(1, (height - listY - bottomSpace) / rowHeight);

        for (String id : ids) {
            if (shown >= maxRows) {
                break;
            }

            if (!q.isBlank() && !id.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }

            int rowY = listY + shown * rowHeight;

            addDrawableChild(ButtonWidget.builder(Text.literal(trimToWidth(id, panelWidth - 20)), button -> {
                        parent.setEntityId(id);
                        client.setScreen(parent);
                    })
                    .dimensions(panelX, rowY, panelWidth, 20)
                    .build());

            shown++;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        // Пересчитываем ширину для отрисовки фона (аналогично init)
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
        int panelBottom = height - 35; // Фон заканчивается до кнопки Back
        int padding = 8;

        // Темная полупрозрачная подложка под весь контент
        context.fill(panelX - padding, panelTop, panelX + panelWidth + padding, panelBottom, 0x66000000);

        // Красивая зеленая акцентная полоса сверху (толщина 2px)
        context.fill(panelX - padding, panelTop, panelX + panelWidth + padding, panelTop + 2, 0xFF3ECF8E);

        // Заголовок
        context.drawCenteredTextWithShadow(textRenderer, this.title.getString(), width / 2, panelTop + 6, 0xFFFFFF);

        if (ClientGerbariumData.entityIds().isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    "No entity IDs synced yet. Click Refresh.",
                    width / 2,
                    height / 2,
                    0xFFAAAA
            );
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