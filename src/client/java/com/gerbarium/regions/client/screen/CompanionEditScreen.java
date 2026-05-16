package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.screen.help.HelpTopic;
import com.gerbarium.regions.model.CompanionRule;
import com.gerbarium.regions.model.ZoneDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class CompanionEditScreen extends GerbariumScreen implements EntitySelectionConsumer {
    private final CompanionListScreen parent;
    private final int editIndex;
    private final CompanionRule draft;
    private String error = "";
    private boolean advancedView = false;

    private TextFieldWidget nameField;
    private TextFieldWidget entityField;
    private TextFieldWidget countField;
    private TextFieldWidget radiusField;
    private TextFieldWidget chanceField;

    public CompanionEditScreen(CompanionListScreen parent, CompanionRule existing, int editIndex) {
        super(Text.literal(existing == null ? "Add Companion" : "Edit Companion"), 420, HelpTopic.COMPANION_EDIT);
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
    protected void initContent() {
        int col3W = Math.max(80, (contentW - 16) / 3);
        int y = contentY;

        // Row 1: Name + Advanced toggle
        nameField = addField(contentX, y, Math.max(180, contentW - 100), draft.name == null ? "" : draft.name);
        addDrawableChild(ButtonWidget.builder(Text.literal(advancedView ? "Advanced: On" : "Advanced: Off"), b -> {
            advancedView = !advancedView;
            init();
        }).dimensions(contentX + Math.max(184, contentW - 96), y, 92, 20).build());
        y += 40;

        // Row 2: Entity + Pick
        entityField = addField(contentX, y, Math.max(180, contentW - 100), draft.entity);
        addDrawableChild(ButtonWidget.builder(Text.literal("Pick Entity"), b -> {
            capture();
            client.setScreen(new EntityPickerScreen(this, this, draft.entity));
        }).dimensions(contentX + Math.max(184, contentW - 96), y, 92, 20).build());
        y += 40;

        // Row 3: Count, Radius, Chance
        countField = addField(contentX, y, col3W, String.valueOf(draft.count));
        radiusField = addField(contentX + col3W + ScreenTheme.SPACE_SM, y, col3W, String.valueOf(draft.radius));
        chanceField = addField(contentX + (col3W + ScreenTheme.SPACE_SM) * 2, y, col3W, String.valueOf(draft.chance));
    }

    private TextFieldWidget addField(int x, int y, int w, String value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        f.setText(value == null ? "" : value);
        addDrawableChild(f);
        return f;
    }

    @Override
    protected void initFooter() {
        AdaptiveLayout.ColumnMetrics cols = AdaptiveLayout.twoColumn(panelW, ScreenTheme.SPACE_LG);
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(contentX, footerY, cols.col1Width(), 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> client.setScreen(parent))
                .dimensions(contentX + cols.col2Offset(), footerY, cols.col1Width(), 20).build());
    }

    private void capture() {
        draft.name = nameField.getText().trim();
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
        super.render(context, mouseX, mouseY, delta);

        int col3W = Math.max(80, (contentW - 16) / 3);
        int y = contentY;

        context.drawTextWithShadow(textRenderer, "Companion Name", contentX, y - 11, ScreenTheme.TEXT_SECONDARY);
        y += 40;
        context.drawTextWithShadow(textRenderer, "Entity", contentX, y - 11, ScreenTheme.TEXT_SECONDARY);
        y += 40;
        context.drawTextWithShadow(textRenderer, "Count", contentX, y - 11, ScreenTheme.TEXT_SECONDARY);
        context.drawTextWithShadow(textRenderer, "Radius", contentX + col3W + ScreenTheme.SPACE_SM, y - 11, ScreenTheme.TEXT_SECONDARY);
        context.drawTextWithShadow(textRenderer, "Chance", contentX + (col3W + ScreenTheme.SPACE_SM) * 2, y - 11, ScreenTheme.TEXT_SECONDARY);

        if (advancedView) {
            context.drawTextWithShadow(textRenderer, "ID: " + (draft.id == null ? "" : draft.id), contentX, y + 30, ScreenTheme.TEXT_MUTED);
        }

        if (!error.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2, footerY - 20, ScreenTheme.ERROR);
        }
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
