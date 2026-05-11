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
        int x = width / 2 - 170;
        int y = 40;
        idField = field(x, y, 340, draft.name == null ? "" : draft.name);
        entityField = field(x, y += 30, 244, draft.entity);
        countField = field(x, y += 30, 80, String.valueOf(draft.count));
        radiusField = field(x + 84, y, 80, String.valueOf(draft.radius));
        chanceField = field(x + 168, y, 76, String.valueOf(draft.chance));

        addDrawableChild(idField);
        addDrawableChild(entityField);
        addDrawableChild(countField);
        addDrawableChild(radiusField);
        addDrawableChild(chanceField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Pick Entity"), b -> {
            capture();
            client.setScreen(new EntityPickerScreen(this, this, draft.entity));
        }).dimensions(x + 248, 70, 92, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(advancedView ? "Advanced: On" : "Advanced: Off"), b -> {
            advancedView = !advancedView;
            init();
        }).dimensions(x + 248, 40, 92, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save()).dimensions(x, height - 30, 166, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> client.setScreen(parent)).dimensions(x + 174, height - 30, 166, 20).build());
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
        int x = width / 2 - 170;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Companion Name", x, 30, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Entity", x, 60, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Count", x, 90, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Radius", x + 84, 90, 0xA5FFB5);
        context.drawTextWithShadow(textRenderer, "Chance", x + 168, 90, 0xA5FFB5);
        if (advancedView) {
            context.drawTextWithShadow(textRenderer, "UID64: " + (draft.uid64 == null ? "" : draft.uid64), x, 120, 0x888888);
        }
        if (!error.isBlank()) {
            context.drawTextWithShadow(textRenderer, error, x, height - 44, 0xFF5555);
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
