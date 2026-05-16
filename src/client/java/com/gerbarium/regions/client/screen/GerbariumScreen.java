package com.gerbarium.regions.client.screen;

import com.gerbarium.regions.client.screen.help.HelpTopic;
import com.gerbarium.regions.client.screen.widget.HelpButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class GerbariumScreen extends Screen {
    protected int panelX, panelY, panelW, panelH;
    protected int contentX, contentY, contentW, contentH;
    protected int footerY;
    protected final int preferredWidth;
    protected HelpTopic helpTopic;

    protected GerbariumScreen(Text title, int preferredWidth) {
        super(title);
        this.preferredWidth = preferredWidth;
    }

    protected GerbariumScreen(Text title, int preferredWidth, HelpTopic helpTopic) {
        this(title, preferredWidth);
        this.helpTopic = helpTopic;
    }

    @Override
    protected void init() {
        computeLayout();
        clearChildren();
        initHeader();
        initContent();
        initFooter();
    }

    protected void computeLayout() {
        panelW = AdaptiveLayout.panelWidth(width, preferredWidth);
        panelX = AdaptiveLayout.panelLeft(width, panelW);
        panelH = AdaptiveLayout.panelHeight(height);
        panelY = AdaptiveLayout.panelTop();

        contentX = AdaptiveLayout.contentX(panelX);
        contentY = AdaptiveLayout.contentY(panelY);
        contentW = AdaptiveLayout.contentWidth(panelW);
        contentH = AdaptiveLayout.contentHeight(panelH);
        footerY = AdaptiveLayout.footerY(panelY, panelH);
    }

    protected void initHeader() {
        // Title is drawn in render()
        if (helpTopic != null) {
            addDrawableChild(new HelpButton(panelX + panelW - 28, panelY + 14, helpTopic));
        }
    }

    public void setHelpTopic(HelpTopic topic) {
        this.helpTopic = topic;
    }

    protected abstract void initContent();

    protected abstract void initFooter();

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        ScreenTheme.drawPanel(context, panelX, panelY, panelW, panelH);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 18, ScreenTheme.ACCENT_PRIMARY);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
