package com.example.dm.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.example.dm.client.gui.widget.DmButton;
import com.example.dm.client.gui.widget.Theme;
import com.example.dm.client.update.UpdateService;

public class AboutScreen extends Screen {
	private static final int CARD_W = 260;
	private static final int CARD_H = 176;
	private final Screen parent;

	public AboutScreen(Screen parent) {
		super(Component.literal("About DriscolMod"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cardX = (this.width - CARD_W) / 2;
		int cardY = (this.height - CARD_H) / 2;
		addRenderableWidget(DmButton.secondary(cardX + 20, cardY + CARD_H - 30, CARD_W - 40, 20, "Back", this::onClose));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		Theme.screenBackground(graphics, this.width, this.height);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int cardX = (this.width - CARD_W) / 2;
		int cardY = (this.height - CARD_H) / 2;
		Theme.panel(graphics, cardX, cardY, CARD_W, CARD_H);

		int x = cardX + 16;
		int y = cardY + 16;
		graphics.text(this.font, Component.literal("DriscolMod  v" + UpdateService.currentVersion()), x, y, Theme.TEXT_ACCENT);
		y += 14;
		Theme.separator(graphics, x, y, CARD_W - 32);
		y += 10;
		graphics.text(this.font, "First-person held-item customizer.", x, y, Theme.TEXT); y += 12;
		graphics.text(this.font, "Position, rotation, scale & swing speed.", x, y, Theme.TEXT_MUTED); y += 16;
		graphics.text(this.font, "Commands:", x, y, Theme.TEXT); y += 12;
		graphics.text(this.font, "  /dm  \u00b7  /Driscolmod  \u00b7  /dmupdate", x, y, Theme.TEXT_MUTED); y += 16;
		graphics.text(this.font, "Everything is purely visual & client-side.", x, y, Theme.TEXT_MUTED); y += 12;
		graphics.text(this.font, "Updates apply in place on next launch.", x, y, Theme.TEXT_MUTED);

		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}
}
