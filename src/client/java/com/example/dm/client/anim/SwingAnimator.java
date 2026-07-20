package com.example.dm.client.anim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;

import com.example.dm.client.config.HeldItemSettings;

/**
 * Client-only, purely visual first-person swing clock.
 *
 * <p>Vanilla drives the swing arc from the real {@code swingTime}, which resets every
 * time you click. That makes low speeds look cut off and makes spam-clicking restart
 * the arc. Instead we run our own progress 0..1 that plays the full arc at the chosen
 * speed. A new click only restarts the visual arc once it has passed the configurable
 * reset threshold (e.g. 80%); clicks before that are ignored visually. Nothing here
 * touches the real swing/attack timing — spamming still attacks normally in game.
 */
public final class SwingAnimator {
	/** Baseline arc length in ticks at speed 1.0 (matches vanilla default WHACK swing). */
	private static final float BASE_TICKS = 6.0F;

	private static boolean active;
	private static float progress;
	private static float prevProgress;
	private static InteractionHand hand = InteractionHand.MAIN_HAND;

	private static boolean prevSwinging;
	private static int prevSwingTime;

	private SwingAnimator() {
	}

	public static boolean isActive() {
		return active;
	}

	public static InteractionHand hand() {
		return hand;
	}

	/** Interpolated swing progress for smooth rendering between ticks. */
	public static float sample(float partialTick) {
		return Mth.lerp(partialTick, prevProgress, progress);
	}

	public static void clientTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			active = false;
			prevSwinging = false;
			prevSwingTime = 0;
			return;
		}

		boolean swingingNow = player.swinging;
		int swingTimeNow = player.swingTime;
		// A fresh in-game swing: either swinging just started, or swingTime was reset by a new click.
		boolean newSwingEdge = (swingingNow && !prevSwinging) || (swingingNow && swingTimeNow < prevSwingTime);
		prevSwinging = swingingNow;
		prevSwingTime = swingTimeNow;

		if (newSwingEdge) {
			float threshold = HeldItemSettings.get().appliedResetFraction();
			// Start when idle, or restart only once the arc has passed the reset threshold.
			// Clicks before the threshold are ignored visually (but still attack in game).
			if (!active || progress >= threshold) {
				startArc(player);
			}
		}

		if (active) {
			prevProgress = progress;
			float speed = HeldItemSettings.get().appliedSwingSpeed();
			float durationTicks = Math.max(1.0F, BASE_TICKS / speed);
			progress += 1.0F / durationTicks;
			if (progress >= 1.0F) {
				progress = 1.0F;
				active = false;
			}
		}
	}

	private static void startArc(LocalPlayer player) {
		active = true;
		progress = 0.0F;
		prevProgress = 0.0F;
		hand = player.swingingArm != null ? player.swingingArm : InteractionHand.MAIN_HAND;
	}
}
