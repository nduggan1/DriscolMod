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
 * the arc. Instead we run our own progress 0..1 that always plays the full arc at the
 * chosen speed, ignores new clicks until the current arc finishes, and only chains into
 * the next arc once the current one completes. The real swing/attack timing is untouched.
 */
public final class SwingAnimator {
	/** Baseline arc length in ticks at speed 1.0 (matches vanilla default WHACK swing). */
	private static final float BASE_TICKS = 6.0F;

	private static boolean active;
	private static boolean pending;
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
			pending = false;
			prevSwinging = false;
			prevSwingTime = 0;
			return;
		}

		boolean swingingNow = player.swinging;
		int swingTimeNow = player.swingTime;
		boolean newSwingEdge = (swingingNow && !prevSwinging) || (swingingNow && swingTimeNow < prevSwingTime);
		prevSwinging = swingingNow;
		prevSwingTime = swingTimeNow;

		if (!active) {
			if (swingingNow) {
				startArc(player);
			}
		} else if (newSwingEdge) {
			// A click landed mid-animation: remember it, but don't restart the current arc.
			pending = true;
		}

		if (active) {
			prevProgress = progress;
			float speed = HeldItemSettings.get().appliedSwingSpeed();
			float durationTicks = Math.max(1.0F, BASE_TICKS / speed);
			progress += 1.0F / durationTicks;
			if (progress >= 1.0F) {
				progress = 1.0F;
				active = false;
				// Only after the full arc completes do we allow the next swing.
				if (pending || swingingNow) {
					pending = false;
					startArc(player);
				}
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
