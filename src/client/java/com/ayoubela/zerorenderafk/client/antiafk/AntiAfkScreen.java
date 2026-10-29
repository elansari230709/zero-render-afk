package com.ayoubela.zerorenderafk.client.antiafk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

public final class AntiAfkScreen extends Screen {
    private final Screen parent;
    private final AntiAfkStore store;
    private final Runnable startAction;
    private boolean confirming;
    private Properties draft;
    private int page;
    private String error = "";
    private final List<Label> labels = new ArrayList<>();
    private int left, contentWidth;
    private record Label(String text, int x, int y, int color) { }

    public AntiAfkScreen(Screen parent, AntiAfkStore store, Runnable startAction) {
        super(Component.literal("Zero Render AFK"));
        this.parent = parent; this.store = store; this.startAction = startAction;
        this.draft = store.snapshot().toProperties();
    }
    @Override protected void init() {
        labels.clear();
        left = Math.max(12, (width - 420) / 2);
        contentWidth = Math.min(420, width - 24);
        if (width < 300 || height < 262) {
            label("Reduce GUI Scale or enlarge the window.", 60);
            button("Cancel", left, height - 28, contentWidth, this::onClose);
            return;
        }
        if (confirming) {
            label("Start AFK session?", 56);
            label("World rendering will stop and chunks unload.", 80);
            label("Enabled actions will send packets, commands", 96);
            label("or chat using your configured intervals.", 112);
            label("F8 or Escape disconnects from the server.", 140);
            label("You must rejoin to resume playing.", 156);
            label("Confirm saves settings and starts AFK.", 170);
            int half = (contentWidth - 8) / 2;
            coloredButton("Confirm & Start", left, height - 28, half, true, () -> {
                if (saveDraft()) {
                    minecraft.gui.setScreen(null);
                    startAction.run();
                }
            });
            button("Back", left + half + 8, height - 28, half, this::back);
            return;
        }
        String[] tabs = {"General", "Jump", "Rotation", "Command", "Chat"};
        int tabWidth = (contentWidth - 16) / 5;
        for (int i = 0; i < tabs.length; i++) {
            final int selected = i;
            Button tab = button(tabs[i], left + i * (tabWidth + 4), 28, tabWidth,
                () -> { page = selected; error = ""; rebuildWidgets(); });
            tab.active = page != i;
        }
        switch (page) {
            case 0 -> {
                toggle("enabled", "Anti-AFK", 60);
                label("Actions run only after you start a session.", 92);
                label("Each action has its own interval.", 108);
                label("F8 again disconnects. Rejoin to play.", 124);
                label("Server AFK detection varies.", 140);
                button("Reset draft to defaults", left, 164, contentWidth,
                    () -> { draft = new AntiAfkConfig().toProperties(); error = ""; rebuildWidgets(); });
            }
            case 1 -> {
                toggle("jump.enabled", "Packet jump (experimental)", 60);
                field("Interval (seconds)", "jump.seconds", 90, 16);
                field("Height (0.1 - 1.25)", "jump.height", 114, 16);
                label("Flat ground and clear space above only.", 146);
                label("No collision checks. Corrections stop jumps", 162);
                label("for the rest of this AFK session.", 178);
            }
            case 2 -> {
                toggle("rotation.enabled", "Rotation", 60);
                field("Interval (seconds)", "rotation.seconds", 90, 16);
                field("Yaw step (-180..180)", "rotation.yawStep", 114, 16);
                field("Pitch (-90..90)", "rotation.pitch", 138, 16);
                toggle("rotation.alternate", "Alternate with starting yaw", 164);
            }
            case 3 -> {
                toggle("command.enabled", "Command", 60);
                field("Interval (seconds)", "command.seconds", 90, 16);
                label("Command (leading / is optional)", 122);
                textField("command.text", left, 138, contentWidth, 256);
                label("Sends exactly this command every interval.", 170);
            }
            case 4 -> {
                toggle("chat.enabled", "Chat message", 60);
                field("Interval (seconds)", "chat.seconds", 90, 16);
                label("Message (no leading /)", 122);
                textField("chat.text", left, 138, contentWidth, 256);
                label("Sends exactly this message every interval.", 170);
            }
        }
        int half = (contentWidth - 8) / 2;
        coloredButton("Start AFK Session", left, height - 54, contentWidth, true, () -> {
            if (validateDraft() != null) {
                confirming = true;
                rebuildWidgets();
            }
        });
        button("Save", left, height - 28, half, () -> { if (saveDraft()) onClose(); });
        button("Cancel", left + half + 8, height - 28, half, this::onClose);
    }
    private void label(String text, int y) { labels.add(new Label(text, left, y, 0xFFDDDDDD)); }
    private Button button(String text, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(text), b -> action.run()).bounds(x, y, w, 20).build());
    }
    private Button coloredButton(String caption, int x, int y, int w, boolean enabled, Runnable action) {
        return addRenderableWidget(new ColorButton(caption, x, y, w, enabled, action));
    }
    private final class ColorButton extends Button {
        private final boolean enabled;
        ColorButton(String caption, int x, int y, int w, boolean enabled, Runnable action) {
            super(x, y, w, 20, Component.literal(caption), b -> action.run(), DEFAULT_NARRATION);
            this.enabled = enabled;
        }
        @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float alpha) {
            int x = getX(), y = getY(), w = getWidth();
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 20;
            int border = hover || isFocused() ? 0xFFFFFFFF : (enabled ? 0xFF64D98B : 0xFFF08080);
            int background = enabled ? (hover ? 0xFF237C48 : 0xFF195B35)
                                     : (hover ? 0xFF9A3636 : 0xFF742929);
            graphics.fillGradient(x, y, x + w, y + 20, border, border);
            graphics.fillGradient(x + 1, y + 1, x + w - 1, y + 19, background, background);
            graphics.text(font, getMessage(), x + (w - font.width(getMessage())) / 2, y + 6, 0xFFFFFFFF);
        }
    }
    private void toggle(String key, String caption, int y) {
        label(caption, y + 6);
        boolean value = Boolean.parseBoolean(draft.getProperty(key));
        coloredButton(value ? "ON" : "OFF", left + contentWidth - 64, y, 64, value, () -> {
            draft.setProperty(key, Boolean.toString(!Boolean.parseBoolean(draft.getProperty(key))));
            rebuildWidgets();
        });
    }
    private void field(String caption, String key, int y, int max) {
        label(caption, y + 6);
        textField(key, left + 146, y, contentWidth - 146, max);
    }
    private void textField(String key, int x, int y, int w, int max) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.literal(key));
        box.setMaxLength(max);
        box.setValue(draft.getProperty(key, ""));
        box.setResponder(value -> draft.setProperty(key, value));
        addRenderableWidget(box);
    }
    private AntiAfkConfig validateDraft() {
        try {
            AntiAfkConfig candidate = AntiAfkConfig.fromProperties(draft);
            error = candidate.validate();
            return error == null ? candidate : null;
        } catch (NumberFormatException e) {
            error = "Use numbers with a dot, for example 2.5.";
            return null;
        }
    }
    private boolean saveDraft() {
        AntiAfkConfig candidate = validateDraft();
        if (candidate == null) return false;
        try {
            store.save(candidate);
            return true;
        } catch (IOException e) {
            error = "Could not save. See the game log.";
            LoggerFactory.getLogger("zerorenderafk").error("Could not save anti-AFK settings", e);
            return false;
        }
    }
    private void back() { confirming = false; error = ""; rebuildWidgets(); }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(font, title, left, 10, 0xFFFFFFFF);
        for (Label label : labels) graphics.text(font, Component.literal(label.text()), label.x(), label.y(), label.color());
        if (error != null && !error.isEmpty()) {
            // Split short validation messages to remain usable at small GUI sizes.
            int limit = Math.max(20, contentWidth / 6);
            graphics.text(font, Component.literal(error.substring(0, Math.min(limit, error.length()))), left, height - 78, 0xFFFF7777);
            if (error.length() > limit)
                graphics.text(font, Component.literal(error.substring(limit)), left, height - 67, 0xFFFF7777);
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() {
        if (confirming) back();
        else minecraft.gui.setScreen(parent);
    }
}
