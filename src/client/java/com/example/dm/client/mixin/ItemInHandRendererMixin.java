package com.example.dm.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.example.dm.client.anim.SwingAnimator;
import com.example.dm.client.config.HeldItemSettings;

/**
 * Purely visual, client-side first-person tweaks. Applied at the item render step,
 * after vanilla has positioned and swung the item, so position/rotation/scale all
 * pivot around the item itself instead of the camera.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	@Inject(method = "renderItem", at = @At("HEAD"))
	private void dm$applyHeldItemTransform(
		LivingEntity mob,
		ItemStack itemStack,
		ItemDisplayContext type,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int lightCoords,
		CallbackInfo ci
	) {
		if (!type.firstPerson()) {
			return;
		}
		if (mob != Minecraft.getInstance().player) {
			return;
		}

		HeldItemSettings settings = HeldItemSettings.get();
		poseStack.translate(settings.appliedPosX(), settings.appliedPosY(), settings.appliedPosZ());
		poseStack.mulPose(Axis.XP.rotationDegrees(settings.appliedPitch()));
		poseStack.mulPose(Axis.YP.rotationDegrees(settings.appliedYaw()));
		poseStack.mulPose(Axis.ZP.rotationDegrees(settings.appliedRoll()));
		float scale = settings.appliedScale();
		poseStack.scale(scale, scale, scale);
	}

	/**
	 * Replaces the swing progress that drives the first-person arc with our own
	 * animator's value. The animator plays each arc fully at the chosen speed and
	 * never restarts mid-swing, so the real swing/attack timing stays untouched.
	 * {@code frameInterp} (arg 0) is the partial tick; {@code hand} (the InteractionHand arg)
	 * lets us animate only the hand that is actually swinging.
	 */
	@ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
	private float dm$driveSwingAnimation(
		float attack,
		@Local(argsOnly = true, ordinal = 0) float frameInterp,
		@Local(argsOnly = true) InteractionHand hand
	) {
		if (SwingAnimator.isActive()) {
			return hand == SwingAnimator.hand() ? SwingAnimator.sample(frameInterp) : 0.0F;
		}
		// No animation in progress: keep both hands at rest so a real swing we didn't
		// capture can't flash the vanilla arc mid-way.
		return 0.0F;
	}
}
