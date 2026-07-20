package com.example.dm.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import com.example.dm.client.config.HeldItemSettings;
import com.example.dm.client.config.PresetCodec;
import com.example.dm.client.gui.widget.DmButton;
import com.example.dm.client.gui.widget.DmSlider;
import com.example.dm.client.gui.widget.Theme;

/**
 * Left-side glassy editor; the right side stays clear so the first-person hand
 * preview shows your edits live. Everything here is purely visual and client-side.
 */
public class HeldItemEditorScreen extends Screen {
	private static final int PANEL_X = 10;
	private static final int PANEL_Y = 10;
	private static final int PANEL_W = 236;
	private static final int PAD = 14;

	private static final int LABEL_X = PANEL_X + PAD;
	private static final int MINUS_X = PANEL_X + PAD + 44;
	private static final int FIELD_X = PANEL_X + PAD + 62;
	private static final int FIELD_W = 96;
	private static final int PLUS_X = FIELD_X + FIELD_W + 2;
	private static final int ROW_H = 20;

	private static final int STATUS_OK = 0xFF7DFFA0;
	private static final int STATUS_ERR = 0xFFFF8080;

	private final Screen parent;
	private final List<EditBox> valueBoxes = new ArrayList<>();
	private final List<Row> rows = new ArrayList<>();
	private EditBox presetBox;
	private String statusMessage = "";
	private int statusColor = Theme.TEXT_MUTED;
	private int panelH;
	private int presetY;
	private int statusY;

	private record Row(String label, int y) {
	}

	public HeldItemEditorScreen(Screen parent) {
		super(Component.literal("Held Item Customizer"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	@Override
	protected void init() {
		valueBoxes.clear();
		rows.clear();
		HeldItemSettings s = HeldItemSettings.get();
		int y = PANEL_Y + 34;

		y = addValueRow(y, "Pos X", 0.5F, () -> s.posX, v -> s.posX = v);
		y = addValueRow(y, "Pos Y", 0.5F, () -> s.posY, v -> s.posY = v);
		y = addValueRow(y, "Pos Z", 0.5F, () -> s.posZ, v -> s.posZ = v);
		y += 4;
		y = addValueRow(y, "Pitch", 1.0F, () -> s.rotX, v -> s.rotX = v);
		y = addValueRow(y, "Yaw", 1.0F, () -> s.rotY, v -> s.rotY = v);
		y = addValueRow(y, "Roll", 1.0F, () -> s.rotZ, v -> s.rotZ = v);
		y += 4;
		y = addValueRow(y, "Scale", 0.1F, () -> s.scale, v -> s.scale = v);
		y = addValueRow(y, "Swing", 0.05F, () -> s.swingSpeed, v -> s.swingSpeed = v);
		y += 8;

		addRenderableWidget(new DmSlider(LABEL_X, y, PANEL_W - 2 * PAD, 18,
			s.appliedResetFraction(),
			() -> Component.literal("Reset threshold: " + Math.round(s.swingResetThreshold) + "%"),
			v -> s.swingResetThreshold = (float) (v * 100.0)));
		y += 24;

		int halfW = (PANEL_W - 2 * PAD - 8) / 2;
		addRenderableWidget(DmButton.secondary(LABEL_X, y, halfW, 20, "Reset", this::resetValues));
		addRenderableWidget(DmButton.secondary(LABEL_X + halfW + 8, y, halfW, 20, "Save", () -> {
			HeldItemSettings.save();
			setStatus("Saved", STATUS_OK);
		}));
		y += 24;

		addRenderableWidget(DmButton.secondary(LABEL_X, y, PANEL_W - 2 * PAD, 20, "Swing Test", () -> {
			if (this.minecraft.player != null) {
				this.minecraft.player.swing(InteractionHand.MAIN_HAND);
			}
		}));
		y += 24;

		this.presetY = y;
		presetBox = new EditBox(this.font, LABEL_X + 4, y + 3, PANEL_W - 2 * PAD - 8, 12, Component.literal("Preset"));
		presetBox.setBordered(false);
		presetBox.setMaxLength(512);
		presetBox.setTextColor(Theme.TEXT);
		presetBox.setHint(Component.literal("Paste dm1:... preset"));
		addRenderableWidget(presetBox);
		y += 24;

		addRenderableWidget(DmButton.secondary(LABEL_X, y, halfW, 20, "Copy", this::copyPreset));
		addRenderableWidget(DmButton.secondary(LABEL_X + halfW + 8, y, halfW, 20, "Apply", this::applyPreset));
		y += 24;

		this.statusY = y;
		y += 12;

		addRenderableWidget(DmButton.secondary(LABEL_X, y, PANEL_W - 2 * PAD, 18, "\u2039 Back", this::onClose));
		y += 22;

		this.panelH = y - PANEL_Y + PAD - 4;
	}

	private int addValueRow(int y, String label, float step, Supplier<Float> getter, Consumer<Float> setter) {
		rows.add(new Row(label, y));

		EditBox box = new EditBox(this.font, FIELD_X + 4, y - 3, FIELD_W - 6, 14, Component.literal(label));
		box.setBordered(false);
		box.setMaxLength(16);
		box.setTextColor(Theme.TEXT);
		box.setValue(formatFloat(getter.get()));
		box.setResponder(text -> {
			Float parsed = tryParseFloat(text);
			if (parsed != null) {
				setter.accept(parsed);
			}
		});
		addRenderableWidget(box);
		valueBoxes.add(box);

		addRenderableWidget(new DmButton(MINUS_X, y - 4, 16, 16, Component.literal("\u2212"), false,
			() -> nudge(box, getter, setter, -step)));
		addRenderableWidget(new DmButton(PLUS_X, y - 4, 16, 16, Component.literal("+"), false,
			() -> nudge(box, getter, setter, step)));

		return y + ROW_H;
	}

	private void nudge(EditBox box, Supplier<Float> getter, Consumer<Float> setter, float delta) {
		float next = getter.get() + delta;
		setter.accept(next);
		box.setValue(formatFloat(next));
	}

	private void resetValues() {
		HeldItemSettings.get().reset();
		HeldItemSettings.save();
		setStatus("Reset to defaults", STATUS_OK);
		this.rebuildWidgets();
	}

	private void copyPreset() {
		String code = PresetCodec.encode(HeldItemSettings.get());
		this.minecraft.keyboardHandler.setClipboard(code);
		if (presetBox != null) {
			presetBox.setValue(code);
		}
		setStatus("Preset copied to clipboard", STATUS_OK);
	}

	private void applyPreset() {
		String raw = presetBox != null ? presetBox.getValue() : "";
		if (raw == null || raw.isBlank()) {
			raw = this.minecraft.keyboardHandler.getClipboard();
		}
		try {
			HeldItemSettings decoded = PresetCodec.decode(raw);
			HeldItemSettings.get().copyFrom(decoded);
			HeldItemSettings.save();
			setStatus("Preset applied", STATUS_OK);
			this.rebuildWidgets();
		} catch (IllegalArgumentException e) {
			setStatus("Invalid preset code", STATUS_ERR);
		}
	}

	private void setStatus(String message, int color) {
		this.statusMessage = message;
		this.statusColor = color;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		Theme.panel(graphics, PANEL_X, PANEL_Y, PANEL_W, panelH);
		this.minecraft.gui.extractDeferredSubtitles();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.text(this.font, Component.literal("Held Item Customizer"), PANEL_X + PAD, PANEL_Y + 12, Theme.TEXT_ACCENT);
		Theme.separator(graphics, PANEL_X + PAD, PANEL_Y + 26, PANEL_W - 2 * PAD);

		for (Row row : rows) {
			graphics.text(this.font, row.label(), LABEL_X, row.y() + 1, Theme.TEXT);
			Theme.field(graphics, FIELD_X, row.y() - 4, FIELD_W, 16, false);
		}

		Theme.field(graphics, LABEL_X, presetY, PANEL_W - 2 * PAD, 18, presetBox != null && presetBox.isFocused());

		if (!statusMessage.isEmpty()) {
			graphics.text(this.font, statusMessage, PANEL_X + PAD, statusY, statusColor);
		}

		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public void onClose() {
		HeldItemSettings.save();
		this.minecraft.setScreen(parent);
	}

	@Override
	public void removed() {
		HeldItemSettings.save();
	}

	private static String formatFloat(float value) {
		return String.format(Locale.ROOT, "%.3f", value);
	}

	private static Float tryParseFloat(String text) {
		if (text == null || text.isBlank() || text.equals("-") || text.equals(".") || text.equals("-.")) {
			return null;
		}
		try {
			return Float.parseFloat(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
