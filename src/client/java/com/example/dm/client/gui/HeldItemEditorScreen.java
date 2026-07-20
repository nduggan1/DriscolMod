package com.example.dm.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import com.example.dm.client.config.HeldItemSettings;
import com.example.dm.client.config.PresetCodec;

/**
 * Left-side editor; right side stays clear so the first-person hand preview stays visible.
 * Everything edited here is a purely visual, client-side change.
 */
public class HeldItemEditorScreen extends Screen {
	private static final int PANEL_WIDTH = 230;
	private static final int FIELD_WIDTH = 130;
	private static final int LABEL_COLOR = 0xFFE8E8E8;
	private static final int HINT_COLOR = 0xFFAAAAAA;
	private static final int STATUS_OK = 0xFF7DFFA0;
	private static final int STATUS_ERR = 0xFFFF8080;

	private final List<EditBox> valueBoxes = new ArrayList<>();
	private EditBox presetBox;
	private String statusMessage = "";
	private int statusColor = HINT_COLOR;

	public HeldItemEditorScreen() {
		super(Component.literal("DriscolMod"));
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
		HeldItemSettings settings = HeldItemSettings.get();
		int left = 16;
		int y = 32;
		int gap = 20;

		addValueField(left, y, settings.posX, v -> settings.posX = v);
		y += gap;
		addValueField(left, y, settings.posY, v -> settings.posY = v);
		y += gap;
		addValueField(left, y, settings.posZ, v -> settings.posZ = v);
		y += gap + 4;
		addValueField(left, y, settings.rotX, v -> settings.rotX = v);
		y += gap;
		addValueField(left, y, settings.rotY, v -> settings.rotY = v);
		y += gap;
		addValueField(left, y, settings.rotZ, v -> settings.rotZ = v);
		y += gap + 4;
		addValueField(left, y, settings.scale, v -> settings.scale = v);
		y += gap;
		addValueField(left, y, settings.swingSpeed, v -> settings.swingSpeed = v);
		y += gap + 8;

		addRenderableWidget(
			Button.builder(Component.literal("Reset"), button -> resetValues())
				.bounds(left, y, 90, 20)
				.build()
		);
		addRenderableWidget(
			Button.builder(Component.literal("Save"), button -> {
				HeldItemSettings.save();
				setStatus("Saved", STATUS_OK);
			})
				.bounds(left + 100, y, 90, 20)
				.build()
		);
		y += 24;
		addRenderableWidget(
			Button.builder(Component.literal("Swing Test"), button -> {
				if (this.minecraft.player != null) {
					this.minecraft.player.swing(InteractionHand.MAIN_HAND);
				}
			})
				.bounds(left, y, 190, 20)
				.build()
		);
		y += 28;

		presetBox = new EditBox(this.font, left, y, 190, 18, Component.literal("Preset"));
		presetBox.setMaxLength(512);
		presetBox.setHint(Component.literal("Paste dm1:... preset"));
		addRenderableWidget(presetBox);
		y += 22;

		addRenderableWidget(
			Button.builder(Component.literal("Copy Preset"), button -> copyPreset())
				.bounds(left, y, 90, 20)
				.build()
		);
		addRenderableWidget(
			Button.builder(Component.literal("Apply Preset"), button -> applyPreset())
				.bounds(left + 100, y, 90, 20)
				.build()
		);
	}

	private void addValueField(int left, int y, float initial, Consumer<Float> setter) {
		EditBox box = new EditBox(this.font, left + 58, y - 4, FIELD_WIDTH, 18, Component.literal("value"));
		box.setMaxLength(16);
		box.setValue(formatFloat(initial));
		box.setResponder(text -> {
			Float parsed = tryParseFloat(text);
			if (parsed != null) {
				setter.accept(parsed);
			}
		});
		valueBoxes.add(addRenderableWidget(box));
	}

	private void resetValues() {
		HeldItemSettings settings = HeldItemSettings.get();
		settings.reset();
		refreshBoxesFromSettings();
		HeldItemSettings.save();
		setStatus("Reset to defaults", STATUS_OK);
	}

	private void refreshBoxesFromSettings() {
		HeldItemSettings settings = HeldItemSettings.get();
		float[] values = {
			settings.posX, settings.posY, settings.posZ,
			settings.rotX, settings.rotY, settings.rotZ,
			settings.scale, settings.swingSpeed
		};
		for (int i = 0; i < valueBoxes.size() && i < values.length; i++) {
			valueBoxes.get(i).setValue(formatFloat(values[i]));
		}
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
			refreshBoxesFromSettings();
			HeldItemSettings.save();
			if (presetBox != null) {
				presetBox.setValue(PresetCodec.encode(HeldItemSettings.get()));
			}
			setStatus("Preset applied", STATUS_OK);
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
		graphics.fill(0, 0, PANEL_WIDTH, this.height, 0xC0101018);
		graphics.fill(PANEL_WIDTH, 0, PANEL_WIDTH + 1, this.height, 0x80FFFFFF);
		this.minecraft.gui.extractDeferredSubtitles();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.text(this.font, this.title, 16, 10, LABEL_COLOR);

		int y = 32;
		int gap = 20;
		drawLabel(graphics, "Pos X", 16, y); y += gap;
		drawLabel(graphics, "Pos Y", 16, y); y += gap;
		drawLabel(graphics, "Pos Z", 16, y); y += gap + 4;
		drawLabel(graphics, "Pitch", 16, y); y += gap;
		drawLabel(graphics, "Yaw", 16, y); y += gap;
		drawLabel(graphics, "Roll", 16, y); y += gap + 4;
		drawLabel(graphics, "Scale", 16, y); y += gap;
		drawLabel(graphics, "Swing", 16, y);

		if (!statusMessage.isEmpty()) {
			graphics.text(this.font, statusMessage, 16, this.height - 40, statusColor);
		}
		graphics.text(this.font, "Swing visual speed: 1 = normal, 0.25 = slow-mo", 16, this.height - 28, HINT_COLOR);
		graphics.text(this.font, "Share with Copy / Apply Preset", 16, this.height - 16, HINT_COLOR);

		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	private void drawLabel(GuiGraphicsExtractor graphics, String text, int x, int y) {
		graphics.text(this.font, text, x, y, LABEL_COLOR);
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
