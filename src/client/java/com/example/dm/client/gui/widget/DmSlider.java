package com.example.dm.client.gui.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * Custom-drawn slider (teal fill + handle) that maps its 0..1 value through
 * caller-provided read/apply hooks and label text.
 */
public class DmSlider extends AbstractSliderButton {
	private final Consumer<Double> onApply;
	private final Supplier<Component> label;

	public DmSlider(int x, int y, int width, int height, double initial, Supplier<Component> label, Consumer<Double> onApply) {
		super(x, y, width, height, Component.empty(), initial);
		this.label = label;
		this.onApply = onApply;
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		if (label != null) {
			setMessage(label.get());
		}
	}

	@Override
	protected void applyValue() {
		if (onApply != null) {
			onApply.accept(this.value);
		}
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		int w = getWidth();
		int h = getHeight();
		boolean hovered = isHoveredOrFocused();

		graphics.fill(x, y, x + w, y + h, Theme.FIELD_BG);
		graphics.outline(x, y, w, h, hovered ? Theme.ACCENT : Theme.FIELD_BORDER);

		int fillW = (int) (this.value * (w - 2));
		graphics.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, Theme.ACCENT_GLOW);

		int handleX = x + (int) (this.value * (w - 4));
		graphics.fill(handleX, y, handleX + 4, y + h, hovered ? Theme.ACCENT : Theme.ACCENT_DIM);

		Minecraft mc = Minecraft.getInstance();
		graphics.centeredText(mc.font, getMessage(), x + w / 2, y + (h - 8) / 2, Theme.TEXT);
	}
}
