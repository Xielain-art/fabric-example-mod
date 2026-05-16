package com.gerbarium.regions.client.screen.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.ArrayList;
import java.util.List;

public class ScrollablePanel implements Element, net.minecraft.client.gui.Drawable, Selectable {
    private final int x, y, width, height;
    private final List<ClickableWidget> children = new ArrayList<>();
    private double scrollOffset = 0;
    private int contentHeight = 0;

    public ScrollablePanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void addChild(ClickableWidget widget, int relativeY) {
        widget.setY(y + relativeY);
        children.add(widget);
        contentHeight = Math.max(contentHeight, relativeY + widget.getHeight());
    }

    public void setContentHeight(int height) {
        this.contentHeight = height;
    }

    public int getContentHeight() {
        return contentHeight;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.enableScissor(x, y, x + width, y + height);

        for (ClickableWidget child : children) {
            int originalY = child.getY();
            child.setY(originalY - (int) scrollOffset);
            child.render(context, mouseX, mouseY, delta);
            child.setY(originalY);
        }

        context.disableScissor();

        // Scrollbar
        if (contentHeight > height) {
            renderScrollbar(context);
        }
    }

    private void renderScrollbar(DrawContext context) {
        int scrollbarX = x + width - 4;
        int trackHeight = height - 4;
        float scrollRatio = (float) scrollOffset / (contentHeight - height);
        int thumbHeight = Math.max(20, (height * height) / contentHeight);
        int thumbY = y + 2 + (int) (scrollRatio * (trackHeight - thumbHeight));

        // Track
        context.fill(scrollbarX, y + 2, scrollbarX + 3, y + 2 + trackHeight, 0xFF334155);
        // Thumb
        context.fill(scrollbarX, thumbY, scrollbarX + 3, thumbY + thumbHeight, 0xFF4ADE80);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isMouseOver(mouseX, mouseY)) {
            int maxScroll = Math.max(0, contentHeight - height);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - amount * 16));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            for (ClickableWidget child : children) {
                int originalY = child.getY();
                child.setY(originalY - (int) scrollOffset);
                if (child.mouseClicked(mouseX, mouseY, button)) {
                    child.setY(originalY);
                    return true;
                }
                child.setY(originalY);
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (ClickableWidget child : children) {
            int originalY = child.getY();
            child.setY(originalY - (int) scrollOffset);
            if (child.mouseReleased(mouseX, mouseY, button)) {
                child.setY(originalY);
                return true;
            }
            child.setY(originalY);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (ClickableWidget child : children) {
            int originalY = child.getY();
            child.setY(originalY - (int) scrollOffset);
            if (child.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                child.setY(originalY);
                return true;
            }
            child.setY(originalY);
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (ClickableWidget child : children) {
            if (child.charTyped(chr, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ClickableWidget child : children) {
            if (child.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        for (ClickableWidget child : children) {
            if (child.keyReleased(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {}

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {}

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public List<? extends Element> children() {
        return children;
    }
}
