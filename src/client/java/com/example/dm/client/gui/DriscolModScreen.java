package com.example.dm.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.example.dm.client.gui.widget.DmButton;
import com.example.dm.client.gui.widget.Theme;
import com.example.dm.client.update.UpdateScreen;
import com.example.dm.client.update.UpdateService;

/**
 * Main DriscolMod hub. Each feature lives behind its own button.
 */
public class DriscolModScreen extends Screen {
	private static final int CARD_W = 224;
	private static final int CARD_H = 168;

	private final Screen parent;

	public DriscolModScreen() {
		this(null);
	}

	public DriscolModScreen(Screen parent) {
		super(Component.literal("DriscolMod"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cardX = (this.width - CARD_W) / 2;
		int cardY = (this.height - CARD_H) / 2;
		int bx = cardX + 17;
		int bw = CARD_W - 34;
		int y = cardY + 44;

		addRenderableWidget(DmButton.primary(bx, y, bw, 24, "Held Item Customizer",
			() -> this.minecraft.setScreen(new HeldItemEditorScreen(this))));
		y += 30;

		String updates = UpdateService.updateAvailable() ? "Updates  \u2022 new!" : "Updates";
		addRenderableWidget(DmButton.secondary(bx, y, bw, 22, updates,
			() -> this.minecraft.setScreen(new UpdateScreen(this))));
		y += 28;

		addRenderableWidget(DmButton.secondary(bx, y, bw, 22, "About",
			() -> this.minecraft.setScreen(new AboutScreen(this))));
		y += 28;

		addRenderableWidget(DmButton.secondary(bx, y, bw, 20, "Close", this::onClose));
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

		graphics.text(this.font, Component.literal("DriscolMod"), cardX + 16, cardY + 15, Theme.TEXT_ACCENT);
		graphics.text(this.font, "v" + UpdateService.currentVersion(),
			cardX + CARD_W - 16 - this.font.width("v" + UpdateService.currentVersion()), cardY + 15, Theme.TEXT_MUTED);
		Theme.separator(graphics, cardX + 16, cardY + 30, CARD_W - 32);

		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}
}
