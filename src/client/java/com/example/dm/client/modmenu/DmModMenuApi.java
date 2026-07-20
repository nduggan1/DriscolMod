package com.example.dm.client.modmenu;

import org.jetbrains.annotations.Nullable;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.UpdateChannel;
import com.terraformersmc.modmenu.api.UpdateChecker;
import com.terraformersmc.modmenu.api.UpdateInfo;

import net.minecraft.network.chat.Component;

import com.example.dm.client.gui.DriscolModScreen;
import com.example.dm.client.update.UpdateService;

/**
 * Mod Menu integration: adds a config button (our update screen) and an update badge.
 * Only loaded when Mod Menu is present, so it's an optional dependency.
 */
public class DmModMenuApi implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return DriscolModScreen::new;
	}

	@Override
	public UpdateChecker getUpdateChecker() {
		return () -> {
			boolean available = UpdateService.checkNow();
			if (!available) {
				return null;
			}
			return new UpdateInfo() {
				@Override
				public boolean isUpdateAvailable() {
					return true;
				}

				@Override
				@Nullable
				public Component getUpdateMessage() {
					return Component.literal("v" + UpdateService.latestVersion()
						+ " available — open DriscolMod config to update in place");
				}

				@Override
				@Nullable
				public String getDownloadLink() {
					// Intentionally null: we update the jar in place instead of opening a browser link.
					return null;
				}

				@Override
				public UpdateChannel getUpdateChannel() {
					return UpdateChannel.RELEASE;
				}
			};
		};
	}
}
