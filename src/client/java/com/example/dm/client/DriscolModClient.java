package com.example.dm.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;

import com.example.dm.DriscolMod;
import com.example.dm.client.anim.SwingAnimator;
import com.example.dm.client.config.HeldItemSettings;
import com.example.dm.client.gui.HeldItemEditorScreen;
import com.example.dm.client.update.UpdateScreen;
import com.example.dm.client.update.UpdateService;

public class DriscolModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HeldItemSettings.load();
		UpdateService.init();
		UpdateService.checkAsync();
		registerCommands();
		ClientTickEvents.END_CLIENT_TICK.register(SwingAnimator::clientTick);
		DriscolMod.LOGGER.info("DriscolMod client ready — /dm or /Driscolmod");
	}

	private static void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommands.literal("dm").executes(context -> openEditor(context.getSource().getClient())));
			dispatcher.register(ClientCommands.literal("Driscolmod").executes(context -> openEditor(context.getSource().getClient())));
			dispatcher.register(ClientCommands.literal("dmupdate").executes(context -> openUpdates(context.getSource().getClient())));
		});
	}

	private static int openEditor(Minecraft client) {
		client.execute(() -> client.setScreen(new HeldItemEditorScreen()));
		return 1;
	}

	private static int openUpdates(Minecraft client) {
		client.execute(() -> client.setScreen(new UpdateScreen(client.screen)));
		return 1;
	}
}
