package com.example.dm.client.update;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import com.example.dm.DriscolMod;

/**
 * Checks GitHub Releases for a newer DriscolMod build and updates the jar in place.
 *
 * <p>A running jar can't overwrite itself (the JVM keeps it open, and Windows locks it),
 * so instead we download the new jar into a hidden staging folder and spawn a tiny detached
 * helper script that waits for the game to close, deletes the old jar, and moves the new one
 * into the mods folder. The result is a single, updated jar the next time you launch — nothing
 * extra is left behind and nothing changes about how the mod plays.
 */
public final class UpdateService {
	public enum State {
		DISABLED,
		IDLE,
		CHECKING,
		UP_TO_DATE,
		UPDATE_AVAILABLE,
		DOWNLOADING,
		STAGED,
		ERROR
	}

	private static final String REPO = "nduggan1/DriscolMod";
	private static final String LATEST_RELEASE_API = "https://api.github.com/repos/" + REPO + "/releases/latest";
	private static final String STAGING_DIR_NAME = ".dm-update";

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.connectTimeout(Duration.ofSeconds(15))
		.build();

	private static volatile State state = State.IDLE;
	private static volatile String currentVersion = "0.0.0";
	private static volatile String latestVersion = "";
	private static volatile String message = "";
	private static volatile String downloadUrl = "";
	private static volatile String assetName = "";

	private static Path jarPath;
	private static Path modsDir;
	private static boolean enabled;

	private UpdateService() {
	}

	public static State state() {
		return state;
	}

	public static String currentVersion() {
		return currentVersion;
	}

	public static String latestVersion() {
		return latestVersion;
	}

	public static String message() {
		return message;
	}

	public static boolean updateAvailable() {
		return state == State.UPDATE_AVAILABLE;
	}

	/** Called once on client init. Locates our jar and re-arms a pending swap if one exists. */
	public static void init() {
		ModContainer container = FabricLoader.getInstance().getModContainer(DriscolMod.MOD_ID).orElse(null);
		currentVersion = container != null ? container.getMetadata().getVersion().getFriendlyString() : "0.0.0";

		jarPath = resolveJarPath(container);
		enabled = !FabricLoader.getInstance().isDevelopmentEnvironment()
			&& jarPath != null
			&& Files.isRegularFile(jarPath)
			&& jarPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar");

		if (!enabled) {
			state = State.DISABLED;
			message = "Updates disabled (dev environment)";
			return;
		}

		modsDir = jarPath.getParent();
		state = State.IDLE;
		rearmPendingSwap();
	}

	private static Path resolveJarPath(ModContainer container) {
		if (container == null) {
			return null;
		}
		try {
			List<Path> paths = container.getOrigin().getPaths();
			return paths.isEmpty() ? null : paths.get(0);
		} catch (Throwable t) {
			return null;
		}
	}

	public static void checkAsync() {
		if (!enabled) {
			return;
		}
		Thread thread = new Thread(UpdateService::checkNow, "DriscolMod-UpdateCheck");
		thread.setDaemon(true);
		thread.start();
	}

	/** Blocking version-check; safe to call off the main thread (e.g. Mod Menu's checker thread). */
	public static boolean checkNow() {
		if (!enabled) {
			return false;
		}
		state = State.CHECKING;
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(LATEST_RELEASE_API))
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.header("User-Agent", "DriscolMod-Updater")
				.timeout(Duration.ofSeconds(20))
				.GET()
				.build();

			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				state = State.ERROR;
				message = "GitHub check failed (HTTP " + response.statusCode() + ")";
				return false;
			}

			JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
			String tag = json.has("tag_name") && !json.get("tag_name").isJsonNull()
				? json.get("tag_name").getAsString()
				: "";
			latestVersion = stripLeadingV(tag);

			String jarUrl = "";
			String jarName = "";
			if (json.has("assets") && json.get("assets").isJsonArray()) {
				JsonArray assets = json.getAsJsonArray("assets");
				for (int i = 0; i < assets.size(); i++) {
					JsonObject asset = assets.get(i).getAsJsonObject();
					String name = asset.get("name").getAsString();
					String lower = name.toLowerCase(Locale.ROOT);
					if (lower.endsWith(".jar") && !lower.endsWith("-sources.jar")) {
						jarUrl = asset.get("browser_download_url").getAsString();
						jarName = name;
						break;
					}
				}
			}
			downloadUrl = jarUrl;
			assetName = jarName;

			if (latestVersion.isEmpty() || jarUrl.isEmpty()) {
				state = State.ERROR;
				message = "No downloadable release found";
				return false;
			}

			if (compareVersions(latestVersion, currentVersion) > 0) {
				state = State.UPDATE_AVAILABLE;
				message = "Update available: v" + latestVersion + " (you have v" + currentVersion + ")";
				return true;
			}

			state = State.UP_TO_DATE;
			message = "Up to date (v" + currentVersion + ")";
			return false;
		} catch (IOException | InterruptedException | RuntimeException e) {
			DriscolMod.LOGGER.error("Update check failed", e);
			state = State.ERROR;
			message = "Update check failed: " + e.getClass().getSimpleName();
			return false;
		}
	}

	/** Downloads the new jar and arms the in-place swap. Runs on a background thread. */
	public static void startUpdateAsync() {
		if (!enabled || state != State.UPDATE_AVAILABLE) {
			return;
		}
		Thread thread = new Thread(UpdateService::startUpdate, "DriscolMod-UpdateDownload");
		thread.setDaemon(true);
		thread.start();
	}

	private static void startUpdate() {
		state = State.DOWNLOADING;
		message = "Downloading v" + latestVersion + "...";
		try {
			Path staging = modsDir.resolve(STAGING_DIR_NAME);
			Files.createDirectories(staging);
			Path staged = staging.resolve(assetName);

			HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl))
				.header("User-Agent", "DriscolMod-Updater")
				.timeout(Duration.ofMinutes(5))
				.GET()
				.build();

			HttpResponse<Path> response = HTTP.send(request, HttpResponse.BodyHandlers.ofFile(
				staged, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
			if (response.statusCode() != 200 || !Files.isRegularFile(staged) || Files.size(staged) == 0) {
				state = State.ERROR;
				message = "Download failed (HTTP " + response.statusCode() + ")";
				return;
			}

			Path dest = modsDir.resolve(assetName);
			spawnSwapHelper(jarPath, staged, dest);

			state = State.STAGED;
			message = "Update ready — restart the game to finish (v" + latestVersion + ")";
		} catch (IOException | InterruptedException | RuntimeException e) {
			DriscolMod.LOGGER.error("Update download failed", e);
			state = State.ERROR;
			message = "Download failed: " + e.getClass().getSimpleName();
		}
	}

	/** If a newer staged jar is already downloaded, re-arm the swap (self-heals a killed helper). */
	private static void rearmPendingSwap() {
		try {
			Path staging = modsDir.resolve(STAGING_DIR_NAME);
			if (!Files.isDirectory(staging)) {
				return;
			}
			try (var stream = Files.list(staging)) {
				List<Path> jars = stream
					.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
					.toList();
				for (Path staged : jars) {
					String stagedVersion = versionFromFileName(staged.getFileName().toString());
					if (compareVersions(stagedVersion, currentVersion) > 0) {
						Path dest = modsDir.resolve(staged.getFileName().toString());
						spawnSwapHelper(jarPath, staged, dest);
						state = State.STAGED;
						latestVersion = stagedVersion;
						message = "Update ready — restart the game to finish (v" + stagedVersion + ")";
					} else {
						Files.deleteIfExists(staged);
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			DriscolMod.LOGGER.warn("Could not re-arm pending update", e);
		}
	}

	private static void spawnSwapHelper(Path oldJar, Path stagedJar, Path destJar) throws IOException {
		boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
		String old = oldJar.toAbsolutePath().toString();
		String staged = stagedJar.toAbsolutePath().toString();
		String dest = destJar.toAbsolutePath().toString();

		if (windows) {
			Path bat = Files.createTempFile("dm-update-", ".bat");
			String script = "@echo off\r\n"
				+ ":waitloop\r\n"
				+ "del \"" + old + "\" >nul 2>&1\r\n"
				+ "if exist \"" + old + "\" (\r\n"
				+ "  ping -n 2 127.0.0.1 >nul\r\n"
				+ "  goto waitloop\r\n"
				+ ")\r\n"
				+ "move /y \"" + staged + "\" \"" + dest + "\" >nul 2>&1\r\n"
				+ "del \"%~f0\" >nul 2>&1\r\n";
			Files.writeString(bat, script);
			new ProcessBuilder("cmd.exe", "/c", "start", "", "/min", "\"" + bat.toAbsolutePath() + "\"")
				.start();
		} else {
			Path sh = Files.createTempFile("dm-update-", ".sh");
			String script = "#!/bin/sh\n"
				+ "while true; do\n"
				+ "  rm -f \"" + old + "\" 2>/dev/null\n"
				+ "  if [ ! -f \"" + old + "\" ]; then break; fi\n"
				+ "  sleep 1\n"
				+ "done\n"
				+ "mv -f \"" + staged + "\" \"" + dest + "\"\n"
				+ "rm -f \"$0\"\n";
			Files.writeString(sh, script);
			new ProcessBuilder("/bin/sh", sh.toAbsolutePath().toString()).start();
		}
		DriscolMod.LOGGER.info("Armed DriscolMod update swap: {} -> {}", stagedJar.getFileName(), destJar.getFileName());
	}

	private static String stripLeadingV(String tag) {
		String t = tag == null ? "" : tag.trim();
		if (t.startsWith("v") || t.startsWith("V")) {
			t = t.substring(1);
		}
		return t;
	}

	private static String versionFromFileName(String fileName) {
		// Expects dm-<version>.jar
		String name = fileName;
		if (name.toLowerCase(Locale.ROOT).endsWith(".jar")) {
			name = name.substring(0, name.length() - 4);
		}
		int dash = name.indexOf('-');
		return dash >= 0 && dash + 1 < name.length() ? name.substring(dash + 1) : name;
	}

	/** Numeric semver-ish comparison; ignores any non-numeric suffix. Returns >0 if a newer than b. */
	static int compareVersions(String a, String b) {
		int[] pa = parseVersion(a);
		int[] pb = parseVersion(b);
		int len = Math.max(pa.length, pb.length);
		for (int i = 0; i < len; i++) {
			int va = i < pa.length ? pa[i] : 0;
			int vb = i < pb.length ? pb[i] : 0;
			if (va != vb) {
				return Integer.compare(va, vb);
			}
		}
		return 0;
	}

	private static int[] parseVersion(String version) {
		if (version == null || version.isBlank()) {
			return new int[] {0};
		}
		String core = version.trim();
		int cut = core.length();
		for (int i = 0; i < core.length(); i++) {
			char c = core.charAt(i);
			if (c != '.' && (c < '0' || c > '9')) {
				cut = i;
				break;
			}
		}
		core = core.substring(0, cut);
		String[] parts = core.split("\\.");
		int[] out = new int[Math.max(1, parts.length)];
		for (int i = 0; i < parts.length; i++) {
			try {
				out[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
			} catch (NumberFormatException e) {
				out[i] = 0;
			}
		}
		return out;
	}
}
