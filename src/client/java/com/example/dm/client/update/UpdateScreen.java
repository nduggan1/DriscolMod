package com.example.dm.client.update;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Simple updater UI. Opened via {@code /dmupdate} or the Mod Menu config button.
 * Works with or without Mod Menu installed.
 */
public class UpdateScreen extends Screen {
	private final Screen parent;
	private Button updateButton;

	public UpdateScreen(Screen parent) {
		super(Component.literal("DriscolMod Updates"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int y = this.height / 2 - 20;

		addRenderableWidget(
			Button.builder(Component.literal("Check for updates"), button -> UpdateService.checkAsync())
				.bounds(cx - 100, y, 200, 20)
				.build()
		);
		updateButton = addRenderableWidget(
			Button.builder(Component.literal("Update now"), button -> UpdateService.startUpdateAsync())
				.bounds(cx - 100, y + 24, 200, 20)
				.build()
		);
		addRenderableWidget(
			Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(cx - 100, y + 48, 200, 20)
				.build()
		);

		if (UpdateService.state() == UpdateService.State.IDLE) {
			UpdateService.checkAsync();
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		updateButton.active = UpdateService.updateAvailable();

		int cx = this.width / 2;
		int top = this.height / 2 - 60;
		graphics.centeredText(this.font, this.title, cx, top, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.literal("Installed: v" + UpdateService.currentVersion()), cx, top + 14, 0xFFC0C0C0);

		String msg = UpdateService.message();
		if (!msg.isEmpty()) {
			int color = switch (UpdateService.state()) {
				case UPDATE_AVAILABLE, STAGED -> 0xFF7DFFA0;
				case ERROR -> 0xFFFF8080;
				default -> 0xFFAAAAAA;
			};
			graphics.centeredText(this.font, Component.literal(msg), cx, top + 28, color);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}
}
