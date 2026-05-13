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
            this.draft.id = CompanionRule.generateId();
        }
        if (this.draft.entity == null || this.draft.entity.isBlank()) {
            this.draft.entity = "minecraft:zombie";
        }
    }

    @Override
    protected void init() {
        clearChildren();

        int formWidth = ScreenLayout.panelWidth(width, 420);
        int startX = ScreenLayout.panelLeft(width, formWidth);
        int yOffset = 45;
        int rowSpacing = 40;

        idField = field(startX, yOffset, Math.max(120, formWidth - 100), draft.name == null ? "" : draft.name);
        addDrawableChild(idField);
        addDrawableChild(ButtonWidget.builder(Text.literal(advancedView ? "Advanced: On" : "Advanced: Off"), b -> {
            advancedView = !advancedView;
            init();
        }).dimensions(startX + Math.max(124, formWidth - 96), yOffset, 92, 20).build());

        yOffset += rowSpacing;
        entityField = field(startX, yOffset, Math.max(120, formWidth - 100), draft.entity);
        addDrawableChild(entityField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Pick Entity"), b -> {
            capture();
            client.setScreen(new EntityPickerScreen(this, this, draft.entity));
        }).dimensions(startX + Math.max(124, formWidth - 96), yOffset, 92, 20).build());

        yOffset += rowSpacing;
        int col1 = Math.max(80, (formWidth - 8) / 3);
        int col2 = Math.max(80, (formWidth - 8) / 3);
        int col3 = Math.max(80, formWidth - col1 - col2 - 8);

        countField = field(startX, yOffset, col1, String.valueOf(draft.count));
        radiusField = field(startX + col1 + 4, yOffset, col2, String.valueOf(draft.radius));
        chanceField = field(startX + col1 + col2 + 8, yOffset, col3, String.valueOf(draft.chance));
        addDrawableChild(countField);
        addDrawableChild(radiusField);
        addDrawableChild(chanceField);

        int footerY = height - 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(startX, footerY, formWidth / 2 - 4, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> client.setScreen(parent))
                .dimensions(startX + formWidth / 2 + 4, footerY, formWidth / 2 - 4, 20).build());
    }

    private TextFieldWidget field(int x, int y, int w, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        field.setText(value == null ? "" : value);
        return field;
    }

    private void capture() {
        draft.name = idField.getText().trim();
        draft.entity = entityField.getText().trim();
        draft.count = parseInt(countField, draft.count);
        draft.radius = parseInt(radiusField, draft.radius);
        draft.chance = parseDouble(chanceField, draft.chance);
    }

    private int parseInt(TextFieldWidget f, int d) { try { return Integer.parseInt(f.getText().trim()); } catch (Exception e) { return d; } }
    private double parseDouble(TextFieldWidget f, double d) { try { return Double.parseDouble(f.getText().trim()); } catch (Exception e) { return d; } }

    private void save() {
        capture();
        if (draft.id == null || draft.id.isBlank()) {
            draft.id = CompanionRule.generateId();
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

        int formWidth = ScreenLayout.panelWidth(width, 420);
        int startX = ScreenLayout.panelLeft(width, formWidth);
        ScreenLayout.drawPanel(context, startX, 15, formWidth, height - 15);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);

        int yOffset = 45;
        context.drawTextWithShadow(textRenderer, "Companion Name", startX, yOffset - 11, 0xA5FFB5);
        yOffset += 40;
        context.drawTextWithShadow(textRenderer, "Entity", startX, yOffset - 11, 0xA5FFB5);
        yOffset += 40;
        context.drawTextWithShadow(textRenderer, "Count", startX, yOffset - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Radius", startX + Math.max(80, (formWidth - 8) / 3) + 4, yOffset - 11, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Chance", startX + (Math.max(80, (formWidth - 8) / 3) * 2) + 8, yOffset - 11, 0xA5FFB5);

        if (advancedView) {
            context.drawTextWithShadow(textRenderer, "ID: " + (draft.id == null ? "" : draft.id), startX, yOffset + 30, 0x888888);
        }

        if (!error.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2, height - 55, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private static CompanionRule copy(CompanionRule src) {
        CompanionRule c = new CompanionRule();
        c.id = src.id;
        c.name = src.name;
        c.entity = src.entity;
        c.count = src.count;
        c.radius = src.radius;
        c.chance = src.chance;
        return c;
    }
}
