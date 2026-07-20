package com.example.dm.client.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Shareable preset codes: {@code dm1:<base64-json>}.
 */
public final class PresetCodec {
	public static final String PREFIX = "dm1:";
	private static final Gson GSON = new Gson();

	private PresetCodec() {
	}

	public static String encode(HeldItemSettings settings) {
		HeldItemSettings copy = new HeldItemSettings();
		copy.copyFrom(settings);
		String json = GSON.toJson(copy);
		String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
		return PREFIX + encoded;
	}

	public static HeldItemSettings decode(String raw) throws IllegalArgumentException {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("Empty preset");
		}

		String code = raw.trim();
		if (code.startsWith(PREFIX)) {
			code = code.substring(PREFIX.length());
		}

		try {
			byte[] bytes = Base64.getUrlDecoder().decode(code);
			String json = new String(bytes, StandardCharsets.UTF_8);
			HeldItemSettings loaded = GSON.fromJson(json, HeldItemSettings.class);
			if (loaded == null) {
				throw new IllegalArgumentException("Invalid preset data");
			}
			loaded.sanitize();
			return loaded;
		} catch (IllegalArgumentException | JsonSyntaxException e) {
			throw new IllegalArgumentException("Could not read preset code", e);
		}
	}
}
