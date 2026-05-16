package com.gerbarium.regions.client.screen.widget;

import com.gerbarium.regions.client.screen.ScreenLayout;
import com.gerbarium.regions.client.screen.ScreenTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;

public class FormField implements Element, net.minecraft.client.gui.Drawable, Selectable {
    private final TextRenderer textRenderer;
    private final String label;
    private final String description;
    private final ClickableWidget widget;
    private final int x, y, width;
    private final int labelHeight;
    private final int descHeight;

    public FormField(TextRenderer textRenderer, String label, ClickableWidget widget,
                     int x, int y, int width, String description) {
        this.textRenderer = textRenderer;
        this.label = label;
        this.widget = widget;
        this.x = x;
        this.y = y;
        this.width = width;
        this.description = description;
        this.labelHeight = textRenderer.fontHeight + 2;
        this.descHeight = (description != null && !description.isBlank())
            ? ScreenLayout.wrappedHeight(textRenderer, description, width) + 4
            : 0;

        // Position widget below label
        widget.setX(x);
        widget.setY(y + labelHeight);
    }

    public FormField(TextRenderer textRenderer, String label, ClickableWidget widget,
                     int x, int y, int width) {
        this(textRenderer, label, widget, x, y, width, null);
    }

    public int getTotalHeight() {
        return labelHeight + widget.getHeight() + descHeight;
    }

    public int getWidgetY() {
        return y + labelHeight;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Label
        context.drawTextWithShadow(textRenderer, label, x, y, ScreenTheme.TEXT_SECONDARY);
        // Widget
        widget.render(context, mouseX, mouseY, delta);
        // Description
        if (descHeight > 0) {
            ScreenLayout.drawWrapped(context, textRenderer, description, x,
                y + labelHeight + widget.getHeight() + 2, width, ScreenTheme.TEXT_MUTED);
        }
    }

    @Override
    public void setFocused(boolean focused) {
        widget.setFocused(focused);
    }

    @Override
    public boolean isFocused() {
        return widget.isFocused();
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        widget.appendNarrations(builder);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return widget.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return widget.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return widget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return widget.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return widget.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return widget.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return widget.charTyped(chr, modifiers);
    }
}
