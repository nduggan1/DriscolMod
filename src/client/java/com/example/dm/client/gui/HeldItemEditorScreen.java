package com.example.dm.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
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
	private static final int START_Y = 26;
	private static final int GAP = 18;
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
		int y = START_Y;

		addValueField(left, y, settings.posX, v -> settings.posX = v);
		y += GAP;
		addValueField(left, y, settings.posY, v -> settings.posY = v);
		y += GAP;
		addValueField(left, y, settings.posZ, v -> settings.posZ = v);
		y += GAP + 2;
		addValueField(left, y, settings.rotX, v -> settings.rotX = v);
		y += GAP;
		addValueField(left, y, settings.rotY, v -> settings.rotY = v);
		y += GAP;
		addValueField(left, y, settings.rotZ, v -> settings.rotZ = v);
		y += GAP + 2;
		addValueField(left, y, settings.scale, v -> settings.scale = v);
		y += GAP;
		addValueField(left, y, settings.swingSpeed, v -> settings.swingSpeed = v);
		y += GAP + 6;

		addRenderableWidget(new ResetThresholdSlider(left, y, 190, 20));
		y += 22;

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
		y += 26;

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
		this.rebuildWidgets();
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
		graphics.fill(0, 0, PANEL_WIDTH, this.height, 0xC0101018);
		graphics.fill(PANEL_WIDTH, 0, PANEL_WIDTH + 1, this.height, 0x80FFFFFF);
		this.minecraft.gui.extractDeferredSubtitles();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int y = START_Y;
		drawLabel(graphics, "Pos X", 16, y); y += GAP;
		drawLabel(graphics, "Pos Y", 16, y); y += GAP;
		drawLabel(graphics, "Pos Z", 16, y); y += GAP + 2;
		drawLabel(graphics, "Pitch", 16, y); y += GAP;
		drawLabel(graphics, "Yaw", 16, y); y += GAP;
		drawLabel(graphics, "Roll", 16, y); y += GAP + 2;
		drawLabel(graphics, "Scale", 16, y); y += GAP;
		drawLabel(graphics, "Swing", 16, y);

		if (!statusMessage.isEmpty()) {
			graphics.text(this.font, statusMessage, 16, this.height - 26, statusColor);
		}
		graphics.text(this.font, "Swing 1 = normal, 0.25 = slow-mo (visual)", 16, this.height - 14, HINT_COLOR);

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

	/** Slider mapping 0..1 to a 0-100% swing reset threshold. */
	private static final class ResetThresholdSlider extends AbstractSliderButton {
		ResetThresholdSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), HeldItemSettings.get().appliedResetFraction());
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			int pct = Math.round((float) this.value * 100.0F);
			setMessage(Component.literal("Reset threshold: " + pct + "%"));
		}

		@Override
		protected void applyValue() {
			HeldItemSettings.get().swingResetThreshold = (float) (this.value * 100.0);
		}
	}
}
