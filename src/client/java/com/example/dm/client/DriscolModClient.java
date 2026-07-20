package com.example.dm.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;

import net.minecraft.client.Minecraft;

import com.example.dm.DriscolMod;
import com.example.dm.client.config.HeldItemSettings;
import com.example.dm.client.gui.HeldItemEditorScreen;

public class DriscolModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HeldItemSettings.load();
		registerCommands();
		DriscolMod.LOGGER.info("DriscolMod client ready — /dm or /Driscolmod");
	}

	private static void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			var openEditor = ClientCommands.literal("dm").executes(context -> openEditor(context.getSource().getClient()));
			dispatcher.register(openEditor);
			dispatcher.register(ClientCommands.literal("Driscolmod").executes(context -> openEditor(context.getSource().getClient())));
		});
	}

	private static int openEditor(Minecraft client) {
		client.execute(() -> client.setScreen(new HeldItemEditorScreen()));
		return 1;
	}
}
