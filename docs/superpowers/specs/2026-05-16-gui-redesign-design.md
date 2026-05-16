# GUI Redesign: Adaptive Layout + Help System

**Date:** 2026-05-16  
**Project:** Gerbarium Regions Bridge (Fabric 1.20.1)  
**Scope:** Full GUI redesign with adaptive layout, scrollable lists, new visual style, and integrated Help system  

---

## 1. Current Problems

### 1.1 Hardcoded Pixel Layouts
All screens use manual pixel calculations with fixed constants:
- `rowSpacing = 45`, `listY = 148`, `footerY = height - 35`
- Layout breaks on small screens (GUI Scale Large) and wastes space on large screens
- Duplicated layout math across 7+ screens

### 1.2 Pagination Duplication
Same pagination code copied in 4 screens (`RegionsScreen`, `ZoneDetailsScreen`, `ZoneResourcesScreen`, `CompanionListScreen`). Each duplicates:
- Page button dimensions (`pageBtnWidth = 100`)
- Page math (`maxPage = (total - 1) / pageSize`)
- Button positioning logic

### 1.3 No Scrolling
Instead of scrollable lists, everything uses manual pagination. Users must click `<` / `>` buttons instead of using mouse wheel.

### 1.4 Scattered Help Text
Help/explanation text is embedded directly in `render()` methods:
- `boundaryHelp()` in `MobRuleEditScreen`
- `spawnWarning()` in `MobRuleEditScreen`
- `explainMode()` in `MobRuleBoundarySettingsScreen`
- Inline `drawWrapped()` calls with hardcoded Y positions

### 1.5 Inconsistent Footer Positioning
Footer buttons placed differently on every screen:
- Some use `height - 35`
- Some calculate based on pagination presence
- Some use `width / 2 - 60`

---

## 2. Design Goals

1. **Adaptive Layout** — UI must work correctly on all screen sizes from 320x240 to 4K
2. **Scrollable Lists** — Replace pagination with scrollable content where possible
3. **Unified Visual Style** — Single "Dark Emerald Console" theme across all screens
4. **Integrated Help** — Every settings screen has a Help button opening a dedicated HelpScreen
5. **DRY Layout Code** — Extract reusable layout utilities, eliminate duplication
6. **Maintainable Help Text** — Centralized help content registry, not scattered in render methods

---

## 3. Visual Style: "Dark Emerald Console"

### 3.1 Color Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `--bg-panel` | `0xCC1A1A2E` | Panel background (80% opacity) |
| `--border-panel` | `0xFF334155` | Panel border (1px) |
| `--accent-primary` | `0xFF4ADE80` | Section headers, titles, active states |
| `--accent-secondary` | `0xFF3B82F6` | Primary buttons, interactive accents |
| `--accent-hover` | `0xFF60A5FA` | Button hover state |
| `--text-primary` | `0xFFE2E8F0` | Main text |
| `--text-secondary` | `0xFF94A3B8` | Labels, descriptions |
| `--text-muted` | `0xFF64748B` | Disabled, hints |
| `--warning` | `0xFFF59E0B` | Warnings, cautions |
| `--error` | `0xFFEF4444` | Errors, validation failures |
| `--success` | `0xFF4ADE80` | Success states |

### 3.2 Spacing System

All spacing is multiples of 4px:

| Token | Value | Usage |
|-------|-------|-------|
| `space-xs` | 4px | Tight gaps, icon padding |
| `space-sm` | 8px | Between related elements |
| `space-md` | 12px | Standard widget padding |
| `space-lg` | 16px | Section internal padding |
| `space-xl` | 24px | Between sections |
| `space-2xl` | 32px | Panel edge padding |

### 3.3 Typography

- **Title:** 1.2x scale, `text-primary`, bold via `drawTextWithShadow`
- **Section Header:** `text-primary`, `accent-primary` color, with horizontal rule below
- **Label:** `text-secondary`, 1.0x scale
- **Description:** `text-secondary`, wrapped, max 2 lines per field
- **Error/Warning:** Respective colors, wrapped

### 3.4 Panel Design

```
┌─────────────────────────────────────────┐ ← border-panel (1px)
│  ┌───────────────────────────────────┐  │
│  │ Title / Header                    │  │ ← accent-primary
│  ├───────────────────────────────────┤  │ ← horizontal rule
│  │                                   │  │
│  │ Content area                      │  │ ← bg-panel fill
│  │                                   │  │
│  │                                   │  │
│  ├───────────────────────────────────┤  │
│  │ [Save] [Back] [? Help]            │  │ ← footer
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

- Corner radius: 4px (simulated via draw order, not actual rounded rects)
- Inner padding: `space-2xl` (32px) left/right, `space-lg` (16px) top/bottom
- Footer: fixed height 32px + padding, always at bottom of panel

---

## 4. Architecture

### 4.1 Package Structure

```
client/screen/
  GerbariumScreen.java          ← new: abstract base screen with common logic
  ScreenLayout.java             ← refactored: layout math only
  ScreenTheme.java              ← new: colors, spacing, drawing helpers
  
  widget/
    ScrollablePanel.java        ← new: scrollable content container
    FormField.java              ← new: label + widget pair
    SectionHeader.java          ← new: section title with divider
    HelpButton.java             ← new: ? button opening HelpScreen
    
  help/
    HelpScreen.java             ← new: scrollable help display
    HelpRegistry.java           ← new: topic → content mapping
    HelpTopics.java             ← new: enum of all help topics
    
  # Existing screens (all refactored):
  RegionsScreen.java
  ZoneDetailsScreen.java
  ZoneRuntimeSettingsScreen.java
  MobRuleEditScreen.java
  MobRuleBoundarySettingsScreen.java
  ResourceRuleEditScreen.java
  CompanionEditScreen.java
  CompanionListScreen.java
  ZoneResourcesScreen.java
```

### 4.2 GerbariumScreen (Abstract Base)

```java
public abstract class GerbariumScreen extends Screen {
    protected int panelX, panelY, panelW, panelH;
    protected int contentX, contentY, contentW, contentH;
    protected int footerY;
    
    @Override
    protected final void init() {
        computeLayout();
        initHeader();
        initContent();
        initFooter();
    }
    
    protected void computeLayout() {
        // Compute panel dimensions based on screen size
        panelW = Math.clamp(preferredWidth(), 320, Math.min(720, width - 32));
        panelX = (width - panelW) / 2;
        panelH = height - 24; // 12px margin top/bottom
        panelY = 12;
        
        contentX = panelX + 32;
        contentY = panelY + 48; // 16px padding + 32px header
        contentW = panelW - 64;
        contentH = panelH - 80; // 16+16 padding + 32px footer + 16px gap
        footerY = panelY + panelH - 40;
    }
    
    protected abstract int preferredWidth();
    protected abstract void initHeader();
    protected abstract void initContent();
    protected abstract void initFooter();
    
    // Helper: add Help button if this screen has help topic
    protected void addHelpButton(HelpTopic topic) {
        if (topic != null) {
            addDrawableChild(new HelpButton(panelX + panelW - 28, panelY + 12, topic));
        }
    }
}
```

### 4.3 AdaptiveLayout (Utility Class)

Refactored `ScreenLayout` → static utility methods:

```java
public final class AdaptiveLayout {
    private AdaptiveLayout() {}
    
    /** Compute 2-column layout, auto-fallback to 1 column if too narrow */
    public static ColumnMetrics twoColumn(int panelWidth, int gap) {
        int minColWidth = 140;
        if (panelWidth < minColWidth * 2 + gap) {
            return new ColumnMetrics(panelWidth, 0, 0, 1); // single column
        }
        int colW = (panelWidth - gap) / 2;
        return new ColumnMetrics(colW, colW + gap, gap, 2);
    }
    
    /** Compute row height based on available space */
    public static int rowHeight(int availableHeight, int minRows) {
        int preferred = 36; // standard row height
        int compact = 26;   // compact row height
        int maxHeight = availableHeight / Math.max(1, minRows);
        if (maxHeight >= preferred) return preferred;
        if (maxHeight >= compact) return compact;
        return Math.max(22, maxHeight);
    }
    
    /** Compute how many rows fit */
    public static int visibleRows(int availableHeight, int rowHeight) {
        return Math.max(1, availableHeight / rowHeight);
    }
    
    public record ColumnMetrics(int col1Width, int col2Offset, int gap, int columns) {}
}
```

### 4.4 ScrollablePanel

Reusable scrollable container for lists and forms:

```java
public class ScrollablePanel extends AbstractParentElement implements Drawable, Selectable {
    private final int x, y, width, height;
    private final List<Element> children = new ArrayList<>();
    private double scrollOffset = 0;
    private int contentHeight = 0;
    
    public void addChild(Element child, int childY, int childHeight) {
        children.add(child);
        contentHeight = Math.max(contentHeight, childY + childHeight);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isMouseOver(mouseX, mouseY)) {
            int maxScroll = Math.max(0, contentHeight - height);
            scrollOffset = Math.clamp(scrollOffset - amount * 16, 0, maxScroll);
            return true;
        }
        return false;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Clip to panel bounds
        context.enableScissor(x, y, x + width, y + height);
        for (Element child : children) {
            // Render with Y offset
            // (actual implementation handles element positioning)
        }
        context.disableScissor();
        
        // Render scrollbar if needed
        if (contentHeight > height) {
            renderScrollbar(context);
        }
    }
}
```

**Note:** For Minecraft 1.20.1 Fabric, we use `context.enableScissor()` for clipping. Scrollbar is a simple rect on the right edge.

### 4.5 ScreenTheme

Centralized drawing helpers:

```java
public final class ScreenTheme {
    // Colors (all public static final int)
    public static final int BG_PANEL = 0xCC1A1A2E;
    public static final int BORDER_PANEL = 0xFF334155;
    public static final int ACCENT_PRIMARY = 0xFF4ADE80;
    public static final int ACCENT_SECONDARY = 0xFF3B82F6;
    // ... etc
    
    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        // Fill background
        ctx.fill(x, y, x + w, y + h, BG_PANEL);
        // Border
        ctx.drawHorizontalLine(x, x + w - 1, y, BORDER_PANEL);
        ctx.drawHorizontalLine(x, x + w - 1, y + h - 1, BORDER_PANEL);
        ctx.drawVerticalLine(x, y, y + h - 1, BORDER_PANEL);
        ctx.drawVerticalLine(x + w - 1, y, y + h - 1, BORDER_PANEL);
        // Top accent line
        ctx.fill(x + 1, y + 1, x + w - 1, y + 3, ACCENT_PRIMARY);
    }
    
    public static void drawSectionHeader(DrawContext ctx, TextRenderer tr, 
                                          String text, int x, int y, int maxWidth) {
        ctx.drawTextWithShadow(tr, text, x, y, ACCENT_PRIMARY);
        int textW = tr.getWidth(text);
        ctx.fill(x + textW + 8, y + tr.fontHeight / 2, x + maxWidth, y + tr.fontHeight / 2 + 1, BORDER_PANEL);
    }
    
    public static void drawWrappedDescription(DrawContext ctx, TextRenderer tr,
                                               String text, int x, int y, int maxWidth) {
        ScreenLayout.drawWrapped(ctx, tr, text, x, y, maxWidth, TEXT_SECONDARY);
    }
}
```

---

## 5. Help System Design

### 5.1 HelpTopic Enum

```java
public enum HelpTopic {
    ZONE_RUNTIME_SETTINGS("Zone Runtime Settings", "help.zone_runtime"),
    MOB_RULE_BASICS("Mob Rule: Basics", "help.mob_rule.basics"),
    MOB_RULE_SPAWN("Mob Rule: Spawn Settings", "help.mob_rule.spawn"),
    MOB_RULE_PLACEMENT("Mob Rule: Placement", "help.mob_rule.placement"),
    MOB_RULE_ADVANCED("Mob Rule: Advanced", "help.mob_rule.advanced"),
    MOB_RULE_BOUNDARY("Mob Rule: Boundary Control", "help.mob_rule.boundary"),
    RESOURCE_RULE_BASICS("Resource Rule: Basics", "help.resource_rule.basics"),
    RESOURCE_RULE_BLOCKS("Resource Rule: Blocks", "help.resource_rule.blocks"),
    RESOURCE_RULE_LIMITS("Resource Rule: Limits", "help.resource_rule.limits"),
    RESOURCE_RULE_SAFETY("Resource Rule: Safety", "help.resource_rule.safety"),
    COMPANION_EDIT("Companion Settings", "help.companion");
    
    public final String title;
    public final String translationKey;
    
    HelpTopic(String title, String translationKey) {
        this.title = title;
        this.translationKey = translationKey;
    }
}
```

### 5.2 HelpRegistry

Centralized content storage (content can later be moved to translation files):

```java
public class HelpRegistry {
    private static final Map<HelpTopic, List<HelpSection>> registry = new EnumMap<>(HelpTopic.class);
    
    public static void register(HelpTopic topic, HelpSection... sections) {
        registry.put(topic, List.of(sections));
    }
    
    public static List<HelpSection> get(HelpTopic topic) {
        return registry.getOrDefault(topic, List.of());
    }
    
    public static void init() {
        // Register all help content here
        register(HelpTopic.ZONE_RUNTIME_SETTINGS,
            section("Activation Range",
                "Radius in blocks within which a player must be for the zone to activate. " +
                "When a player enters this range, the zone becomes active and mob spawning begins."),
            section("Deactivate After Seconds",
                "How many seconds the zone stays active after the last player leaves the activation range. " +
                "Set to 0 to deactivate immediately when no players are nearby."),
            // ... more sections
        );
        // ... register all topics
    }
    
    private static HelpSection section(String title, String content) {
        return new HelpSection(title, content);
    }
    
    public record HelpSection(String title, String content) {}
}
```

### 5.3 HelpScreen

```java
public class HelpScreen extends Screen {
    private final Screen parent;
    private final HelpTopic topic;
    private ScrollablePanel contentPanel;
    
    public HelpScreen(Screen parent, HelpTopic topic) {
        super(Text.literal("? " + topic.title));
        this.parent = parent;
        this.topic = topic;
    }
    
    @Override
    protected void init() {
        // Panel layout (same as other screens)
        int panelW = ScreenLayout.panelWidth(width, 560);
        int panelX = ScreenLayout.panelLeft(width, panelW);
        int panelH = height - 24;
        int panelY = 12;
        
        // Scrollable content area
        contentPanel = new ScrollablePanel(panelX + 24, panelY + 48, panelW - 48, panelH - 96);
        
        // Build content
        int y = 0;
        for (HelpRegistry.HelpSection section : HelpRegistry.get(topic)) {
            // Add section title (drawn in render, not a widget)
            // Add wrapped text
            int textH = ScreenLayout.wrappedHeight(textRenderer, section.content(), panelW - 48);
            y += 20 + textH + 16; // title + content + gap
        }
        contentPanel.setContentHeight(y);
        
        // Back button
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent))
            .dimensions(panelX + panelW / 2 - 60, panelY + panelH - 36, 120, 20)
            .build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        // Draw panel
        ScreenTheme.drawPanel(context, panelX, panelY, panelW, panelH);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 18, ScreenTheme.ACCENT_PRIMARY);
        
        // Render scrollable content
        contentPanel.render(context, mouseX, mouseY, delta);
        
        super.render(context, mouseX, mouseY, delta);
    }
}
```

### 5.4 HelpButton

```java
public class HelpButton extends ButtonWidget {
    private final HelpTopic topic;
    
    public HelpButton(int x, int y, HelpTopic topic) {
        super(x, y, 20, 20, Text.literal("?"), b -> {}, DEFAULT_NARRATION_SUPPLIER);
        this.topic = topic;
    }
    
    @Override
    public void onPress() {
        MinecraftClient.getInstance().setScreen(new HelpScreen(MinecraftClient.getInstance().currentScreen, topic));
    }
    
    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        // Circular button with ? icon
        int color = isHovered() ? ScreenTheme.ACCENT_HOVER : ScreenTheme.ACCENT_SECONDARY;
        context.fill(getX(), getY(), getX() + width, getY() + height, color);
        context.drawCenteredTextWithShadow(textRenderer, getMessage(), getX() + width / 2, getY() + 6, 0xFFFFFFFF);
    }
}
```

---

## 6. Screen-by-Screen Changes

### 6.1 RegionsScreen → minimal changes (no settings, no Help button)

Changes:
- Use `ScreenTheme.drawPanel()` instead of `ScreenLayout.drawPanel()`
- Keep pagination (list is short, no need for scroll)
- No Help button (this is a list, not settings)

### 6.2 ZoneDetailsScreen → moderate changes

Changes:
- Use `GerbariumScreen` base class
- Use `ScreenTheme` for drawing
- Keep pagination for mob rules list
- Add Help button? **No** — this is an overview screen

### 6.3 ZoneRuntimeSettingsScreen → major refactor

Changes:
- Extend `GerbariumScreen`
- Form layout via `AdaptiveLayout.twoColumn()`
- `FormField` wrappers for each setting (label + text field + optional description)
- **Help button** → `HelpTopic.ZONE_RUNTIME_SETTINGS`
- Footer: `[Save] [Back] [?]`

### 6.4 MobRuleEditScreen → major refactor

Changes:
- Extend `GerbariumScreen`
- Page navigation stays (4 pages), but each page uses adaptive layout
- Remove inline help text from `render()` — move to `HelpRegistry`
- **Help button** → dynamic based on current page:
  - Page 0: `HelpTopic.MOB_RULE_BASICS`
  - Page 1: `HelpTopic.MOB_RULE_SPAWN`
  - Page 2: `HelpTopic.MOB_RULE_PLACEMENT`
  - Page 3: `HelpTopic.MOB_RULE_ADVANCED`
- Footer: `[Save] [Back] [?]`

### 6.5 MobRuleBoundarySettingsScreen → moderate refactor

Changes:
- Extend `GerbariumScreen`
- Remove inline `explainMode()` and `updateInfo()` — move to `HelpRegistry`
- **Help button** → `HelpTopic.MOB_RULE_BOUNDARY`
- Footer: `[Save] [Back] [?]`

### 6.6 ResourceRuleEditScreen → major refactor

Changes:
- Extend `GerbariumScreen`
- 5 pages with adaptive layout
- Remove inline help text — move to `HelpRegistry`
- **Help button** → dynamic per page
- Footer: `[Save] [Back] [?]`

### 6.7 CompanionEditScreen → moderate refactor

Changes:
- Extend `GerbariumScreen`
- Use `FormField` components
- **Help button** → `HelpTopic.COMPANION_EDIT`
- Footer: `[Save] [Cancel] [?]`

### 6.8 CompanionListScreen → minor refactor

Changes:
- Use `ScreenTheme` for drawing
- No Help button (list/overview screen)

### 6.9 ZoneResourcesScreen → minor refactor

Changes:
- Use `ScreenTheme` for drawing
- No Help button (list/overview screen)

---

## 7. Migration Strategy

### Phase 1: Foundation (new files)
1. Create `ScreenTheme.java` with colors and drawing helpers
2. Create `AdaptiveLayout.java` (refactor `ScreenLayout`)
3. Create `ScrollablePanel.java`
4. Create `GerbariumScreen.java` abstract base
5. Create `HelpRegistry.java`, `HelpTopics.java`, `HelpScreen.java`, `HelpButton.java`

### Phase 2: Simple screens first
1. Refactor `RegionsScreen` (simplest, validates foundation)
2. Refactor `ZoneDetailsScreen`
3. Refactor `CompanionListScreen`
4. Refactor `ZoneResourcesScreen`

### Phase 3: Settings screens (with Help)
1. Refactor `ZoneRuntimeSettingsScreen` + add HelpTopic
2. Refactor `MobRuleBoundarySettingsScreen` + add HelpTopic
3. Refactor `CompanionEditScreen` + add HelpTopic

### Phase 4: Complex multi-page screens
1. Refactor `MobRuleEditScreen` (4 pages) + add 4 HelpTopics
2. Refactor `ResourceRuleEditScreen` (5 pages) + add 4 HelpTopics

### Phase 5: Cleanup
1. Remove old `ScreenLayout.drawPanel()` and unused methods
2. Delete inline help methods (`boundaryHelp()`, `spawnWarning()`, etc.)
3. Verify all screens work at GUI scales: Small, Normal, Large, Auto

---

## 8. Testing Checklist

- [ ] All screens render without crashes at 320x240
- [ ] All screens render without crashes at 1920x1080
- [ ] All screens work with GUI Scale: Small, Normal, Large, Auto
- [ ] Help button opens correct topic on each settings screen
- [ ] HelpScreen Back button returns to correct parent screen
- [ ] Form validation still works (invalid numbers show errors)
- [ ] Save/Back buttons work on all screens
- [ ] Pagination still works on list screens
- [ ] ScrollablePanel scrolls with mouse wheel
- [ ] No visual glitches or overlapping elements

---

## 9. Open Questions

1. **ScrollablePanel vs vanilla ScrollableWidget:** Minecraft 1.20.1 Fabric has limited scrollable widget support. We'll implement custom `ScrollablePanel` using `DrawContext.enableScissor()`.
2. **Help text language:** Initially English hardcoded in `HelpRegistry`. Later can be moved to `en_us.json` translation files.
3. **FormField widget type:** For 1.20.1, we'll compose existing widgets (TextFieldWidget, CyclingButtonWidget) rather than creating fully custom widgets.

---

## 10. Files to Create/Modify

### New Files (10)
- `client/screen/GerbariumScreen.java`
- `client/screen/ScreenTheme.java`
- `client/screen/AdaptiveLayout.java` (replaces ScreenLayout)
- `client/screen/widget/ScrollablePanel.java`
- `client/screen/widget/FormField.java`
- `client/screen/widget/SectionHeader.java`
- `client/screen/widget/HelpButton.java`
- `client/screen/help/HelpScreen.java`
- `client/screen/help/HelpRegistry.java`
- `client/screen/help/HelpTopics.java`

### Modified Files (9)
- `client/screen/ScreenLayout.java` → deprecated, redirect to AdaptiveLayout
- `client/screen/RegionsScreen.java`
- `client/screen/ZoneDetailsScreen.java`
- `client/screen/ZoneRuntimeSettingsScreen.java`
- `client/screen/MobRuleEditScreen.java`
- `client/screen/MobRuleBoundarySettingsScreen.java`
- `client/screen/ResourceRuleEditScreen.java`
- `client/screen/CompanionEditScreen.java`
- `client/screen/CompanionListScreen.java`
- `client/screen/ZoneResourcesScreen.java`

---

*Spec ready for review. Please confirm or request changes before implementation planning.*
