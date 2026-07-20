package com.example.dm.client.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.fabricmc.loader.api.FabricLoader;

import com.example.dm.DriscolMod;

/**
 * First-person held-item position/rotation offsets, persisted to config/dm.json.
 */
public final class HeldItemSettings {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("dm.json");

	private static HeldItemSettings instance = new HeldItemSettings();

	public float posX;
	public float posY;
	public float posZ;
	public float rotX;
	public float rotY;
	public float rotZ;

	public static HeldItemSettings get() {
		return instance;
	}

	public static void load() {
		if (!Files.isRegularFile(CONFIG_PATH)) {
			instance = new HeldItemSettings();
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			HeldItemSettings loaded = GSON.fromJson(reader, HeldItemSettings.class);
			instance = loaded != null ? loaded : new HeldItemSettings();
		} catch (IOException | JsonSyntaxException e) {
			DriscolMod.LOGGER.error("Failed to load held-item settings from {}", CONFIG_PATH, e);
			instance = new HeldItemSettings();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			DriscolMod.LOGGER.error("Failed to save held-item settings to {}", CONFIG_PATH, e);
		}
	}

	public void reset() {
		posX = 0.0F;
		posY = 0.0F;
		posZ = 0.0F;
		rotX = 0.0F;
		rotY = 0.0F;
		rotZ = 0.0F;
	}

	public void copyFrom(HeldItemSettings other) {
		posX = other.posX;
		posY = other.posY;
		posZ = other.posZ;
		rotX = other.rotX;
		rotY = other.rotY;
		rotZ = other.rotZ;
	}
}
