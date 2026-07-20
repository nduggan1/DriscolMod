package com.example.dm.client.gui;

import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import com.example.dm.client.config.HeldItemSettings;

/**
 * Left-side editor; right side stays clear so the first-person hand preview stays visible.
 * Values update live as you type; settings save automatically on close / Save.
 */
public class HeldItemEditorScreen extends Screen {
	private static final int PANEL_WIDTH = 210;
	private static final int FIELD_WIDTH = 120;
	private static final int LABEL_COLOR = 0xFFE8E8E8;
	private static final int HINT_COLOR = 0xFFAAAAAA;

	private EditBox posXBox;
	private EditBox posYBox;
	private EditBox posZBox;
	private EditBox rotXBox;
	private EditBox rotYBox;
	private EditBox rotZBox;

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
		HeldItemSettings settings = HeldItemSettings.get();
		int left = 16;
		int y = 36;
		int gap = 24;

		posXBox = addFloatField(left, y, "Pos X", settings.posX, v -> settings.posX = v);
		y += gap;
		posYBox = addFloatField(left, y, "Pos Y", settings.posY, v -> settings.posY = v);
		y += gap;
		posZBox = addFloatField(left, y, "Pos Z", settings.posZ, v -> settings.posZ = v);
		y += gap + 8;
		rotXBox = addFloatField(left, y, "Rot X", settings.rotX, v -> settings.rotX = v);
		y += gap;
		rotYBox = addFloatField(left, y, "Rot Y", settings.rotY, v -> settings.rotY = v);
		y += gap;
		rotZBox = addFloatField(left, y, "Rot Z", settings.rotZ, v -> settings.rotZ = v);
		y += gap + 12;

		addRenderableWidget(
			Button.builder(Component.literal("Reset"), button -> resetValues())
				.bounds(left, y, 90, 20)
				.build()
		);
		addRenderableWidget(
			Button.builder(Component.literal("Save"), button -> HeldItemSettings.save())
				.bounds(left + 100, y, 90, 20)
				.build()
		);
		y += 28;
		addRenderableWidget(
			Button.builder(Component.literal("Swing"), button -> {
				if (this.minecraft.player != null) {
					this.minecraft.player.swing(InteractionHand.MAIN_HAND);
				}
			})
				.bounds(left, y, 190, 20)
				.build()
		);
	}

	private EditBox addFloatField(int left, int y, String label, float initial, Consumer<Float> setter) {
		// Labels are drawn in extractRenderState; boxes sit to the right of the label column.
		EditBox box = new EditBox(this.font, left + 70, y - 4, FIELD_WIDTH, 18, Component.literal(label));
		box.setMaxLength(16);
		box.setValue(formatFloat(initial));
		box.setResponder(text -> {
			Float parsed = tryParseFloat(text);
			if (parsed != null) {
				setter.accept(parsed);
			}
		});
		return addRenderableWidget(box);
	}

	private void resetValues() {
		HeldItemSettings settings = HeldItemSettings.get();
		settings.reset();
		posXBox.setValue(formatFloat(0.0F));
		posYBox.setValue(formatFloat(0.0F));
		posZBox.setValue(formatFloat(0.0F));
		rotXBox.setValue(formatFloat(0.0F));
		rotYBox.setValue(formatFloat(0.0F));
		rotZBox.setValue(formatFloat(0.0F));
		HeldItemSettings.save();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		// Dark panel on the left only — leave the right clear for the hand.
		graphics.fill(0, 0, PANEL_WIDTH, this.height, 0xC0101018);
		graphics.fill(PANEL_WIDTH, 0, PANEL_WIDTH + 1, this.height, 0x80FFFFFF);
		this.minecraft.gui.extractDeferredSubtitles();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.text(this.font, this.title, 16, 12, LABEL_COLOR);
		graphics.text(this.font, "Hold an item — preview is on the right", 16, this.height - 28, HINT_COLOR);
		graphics.text(this.font, "Use Swing to test the attack arc", 16, this.height - 16, HINT_COLOR);

		drawLabel(graphics, "Pos X", 16, 36);
		drawLabel(graphics, "Pos Y", 16, 60);
		drawLabel(graphics, "Pos Z", 16, 84);
		drawLabel(graphics, "Rot X", 16, 116);
		drawLabel(graphics, "Rot Y", 16, 140);
		drawLabel(graphics, "Rot Z", 16, 164);

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
