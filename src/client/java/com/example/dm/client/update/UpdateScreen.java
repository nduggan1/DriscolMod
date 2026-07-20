package com.example.dm.client.update;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.example.dm.client.gui.widget.DmButton;
import com.example.dm.client.gui.widget.Theme;

/**
 * Updater UI. Opened via {@code /dmupdate}, the hub, or the Mod Menu config button.
 * Works with or without Mod Menu installed.
 */
public class UpdateScreen extends Screen {
	private static final int CARD_W = 300;
	private static final int CARD_H = 150;

	private final Screen parent;
	private DmButton updateButton;

	public UpdateScreen(Screen parent) {
		super(Component.literal("DriscolMod Updates"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cardX = (this.width - CARD_W) / 2;
		int cardY = (this.height - CARD_H) / 2;
		int bx = cardX + 20;
		int bw = CARD_W - 40;
		int y = cardY + CARD_H - 74;

		addRenderableWidget(DmButton.secondary(bx, y, bw, 20, "Check for updates", UpdateService::checkAsync));
		y += 24;
		updateButton = DmButton.primary(bx, y, bw, 20, "Update now", UpdateService::startUpdateAsync);
		addRenderableWidget(updateButton);
		y += 24;
		addRenderableWidget(DmButton.secondary(bx, y, bw, 18, "Back", this::onClose));

		if (UpdateService.state() == UpdateService.State.IDLE) {
			UpdateService.checkAsync();
		}
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
		graphics.text(this.font, Component.literal("Updates"), x, cardY + 14, Theme.TEXT_ACCENT);
		Theme.separator(graphics, x, cardY + 28, CARD_W - 32);
		graphics.text(this.font, "Installed: v" + UpdateService.currentVersion(), x, cardY + 36, Theme.TEXT_MUTED);

		String msg = UpdateService.message();
		if (!msg.isEmpty()) {
			int color = switch (UpdateService.state()) {
				case UPDATE_AVAILABLE, STAGED -> Theme.TEXT_ACCENT;
				case ERROR -> 0xFFFF8080;
				default -> Theme.TEXT_MUTED;
			};
			graphics.text(this.font, msg, x, cardY + 50, color);
		}

		updateButton.active = UpdateService.updateAvailable();

		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}
}
