package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.model.CompanionRule;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class CompanionEditScreen extends Screen implements EntitySelectionConsumer {
    private final CompanionListScreen parent;
    private final int editIndex;
    private final CompanionRule draft;
    private String error = "";
    private boolean advancedView = false;

    private TextFieldWidget idField;
    private TextFieldWidget entityField;
    private TextFieldWidget countField;
    private TextFieldWidget radiusField;
    private TextFieldWidget chanceField;

    public CompanionEditScreen(CompanionListScreen parent, CompanionRule existing, int editIndex) {
        super(Text.literal(existing == null ? "Add Companion" : "Edit Companion"));
        this.parent = parent;
        this.editIndex = editIndex;
        this.draft = existing == null ? new CompanionRule() : copy(existing);
        if (existing == null) {
            this.draft.uid64 = CompanionRule.generateUid64();
            this.draft.id = this.draft.uid64;
        }
        if (this.draft.entity == null || this.draft.entity.isBlank()) {
            this.draft.entity = "minecraft:zombie";
        }
    }

    @Override
    protected void init() {
        clearChildren();

        // Централизация и сетка
        int formWidth = 340;
        int startX = (width - formWidth) / 2;
        int yOffset = 45;
        int rowSpacing = 40;

        // Строка 1: Имя и кнопка Advanced
        idField = field(startX, yOffset, 244, draft.name == null ? "" : draft.name);
        addDrawableChild(idField);
        addDrawableChild(ButtonWidget.builder(Text.literal(advancedView ? "Advanced: On" : "Advanced: Off"), b -> {
            advancedView = !advancedView;
            init();
        }).dimensions(startX + 248, yOffset, 92, 20).build());

        // Строка 2: Сущность и кнопка Pick Entity
        yOffset += rowSpacing;
        entityField = field(startX, yOffset, 244, draft.entity);
        addDrawableChild(entityField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Pick Entity"), b -> {
            capture();
            client.setScreen(new EntityPickerScreen(this, this, draft.entity));
        }).dimensions(startX + 248, yOffset, 92, 20).build());

        // Строка 3: Параметры (Count, Radius, Chance) - разделены на 3 равные колонки
        yOffset += rowSpacing;
        int col1 = 110;
        int col2 = 110;
        int col3 = 112; // 110 + 110 + 112 + 8 (отступы) = 340

        countField = field(startX, yOffset, col1, String.valueOf(draft.count));
        radiusField = field(startX + col1 + 4, yOffset, col2, String.valueOf(draft.radius));
        chanceField = field(startX + col1 + col2 + 8, yOffset, col3, String.valueOf(draft.chance));

        addDrawableChild(countField);
        addDrawableChild(radiusField);
        addDrawableChild(chanceField);

        // Футер: Кнопки сохранения и отмены
        int footerY = height - 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(startX, footerY, 168, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> client.setScreen(parent))
                .dimensions(startX + 172, footerY, 168, 20).build());
    }

    private TextFieldWidget field(int x, int y, int w, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        field.setText(value);
        return field;
    }

    private void capture() {
        draft.name = idField.getText().trim();
        draft.entity = entityField.getText().trim();
        draft.count = parseInt(countField, draft.count);
        draft.radius = parseInt(radiusField, draft.radius);
        draft.chance = parseDouble(chanceField, draft.chance);
    }

    private int parseInt(TextFieldWidget f, int d) { try { return Integer.parseInt(f.getText().trim()); } catch (Exception e){ return d; } }
    private double parseDouble(TextFieldWidget f, double d){ try { return Double.parseDouble(f.getText().trim()); } catch (Exception e){ return d; } }

    private void save() {
        capture();
        if (draft.uid64 == null || draft.uid64.isBlank()) {
            draft.uid64 = CompanionRule.generateUid64();
            draft.id = draft.uid64;
        }
        try {
            ZoneDefaults.validateCompanionRule(draft);
            ZoneDefaults.normalizeCompanionRule(draft);
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
            return;
        }
        parent.upsertCompanion(draft, editIndex);
        client.setScreen(parent);
    }

    @Override
    public void onEntitySelected(String entityId) {
        draft.entity = entityId;
        if (entityField != null) {
            entityField.setText(entityId);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int formWidth = 340;
        int startX = (width - formWidth) / 2;
        int yOffset = 45;
        int rowSpacing = 40;

        // Заголовок
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);

        // Подписи 1 строки
        context.drawTextWithShadow(textRenderer, "Companion Name", startX, yOffset - 11, 0xA5FFB5);

        // Подписи 2 строки
        yOffset += rowSpacing;
        context.drawTextWithShadow(textRenderer, "Entity", startX, yOffset - 11, 0xA5FFB5);

        // Подписи 3 строки
        yOffset += rowSpacing;
        int col1 = 110;
        int col2 = 110;
        context.drawTextWithShadow(textRenderer, "Count", startX, yOffset - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Radius", startX + col1 + 4, yOffset - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Chance", startX + col1 + col2 + 8, yOffset - 11, 0xA5FFB5);

        // Блок Advanced
        if (advancedView) {
            context.drawTextWithShadow(textRenderer, "UID64: " + (draft.uid64 == null ? "" : draft.uid64), startX, yOffset + 30, 0x888888);
        }

        // Блок с ошибкой (центрируется над кнопками)
        if (!error.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2, height - 55, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private static CompanionRule copy(CompanionRule src) {
        CompanionRule c = new CompanionRule();
        c.id = src.id;
        c.uid64 = src.uid64;
        c.name = src.name;
        c.entity = src.entity;
        c.count = src.count;
        c.radius = src.radius;
        c.chance = src.chance;
        return c;
    }
}