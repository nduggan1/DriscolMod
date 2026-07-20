package com.example.dm.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A fully custom-drawn button (no vanilla sprite). Flat glassy look with a teal
 * hover glow; supports a "primary" style for the main hub actions.
 */
public class DmButton extends AbstractWidget {
	private final Runnable onPress;
	private final boolean primary;

	public DmButton(int x, int y, int width, int height, Component message, boolean primary, Runnable onPress) {
		super(x, y, width, height, message);
		this.onPress = onPress;
		this.primary = primary;
	}

	public static DmButton primary(int x, int y, int width, int height, String text, Runnable onPress) {
		return new DmButton(x, y, width, height, Component.literal(text), true, onPress);
	}

	public static DmButton secondary(int x, int y, int width, int height, String text, Runnable onPress) {
		return new DmButton(x, y, width, height, Component.literal(text), false, onPress);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		int w = getWidth();
		int h = getHeight();
		boolean hovered = isHoveredOrFocused();

		int bg;
		int border;
		if (!this.active) {
			bg = Theme.BTN_BG_DISABLED;
			border = Theme.FIELD_BORDER;
		} else if (hovered) {
			bg = Theme.BTN_BG_HOVER;
			border = Theme.ACCENT;
		} else {
			bg = primary ? Theme.BTN_BG_PRIMARY : Theme.BTN_BG;
			border = primary ? Theme.ACCENT_DIM : Theme.FIELD_BORDER;
		}

		graphics.fill(x, y, x + w, y + h, bg);
		graphics.outline(x, y, w, h, border);
		if (hovered && this.active) {
			// subtle accent bar on the left edge as a hover cue
			graphics.fill(x, y, x + 2, y + h, Theme.ACCENT);
		}

		int textColor = !this.active ? Theme.TEXT_MUTED : (hovered ? Theme.TEXT_ACCENT : Theme.TEXT);
		Minecraft mc = Minecraft.getInstance();
		graphics.centeredText(mc.font, getMessage(), x + w / 2, y + (h - 8) / 2, textColor);
	}

	@Override
	public void onClick(MouseButtonEvent mouseButtonEvent, boolean bl) {
		if (this.active && this.visible) {
			playDownSound(Minecraft.getInstance().getSoundManager());
			this.onPress.run();
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		defaultButtonNarrationText(narrationElementOutput);
	}
}
