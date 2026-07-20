package com.example.dm.client.mixin;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

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
	 * Remaps the swing progress that drives the first-person swing arc only.
	 * This changes how fast the animation *looks*; the real swing timing
	 * (swingTime / attack cooldown) is never touched.
	 */
	@ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
	private float dm$scaleSwingAnimation(float attack) {
		float speed = HeldItemSettings.get().appliedSwingSpeed();
		if (speed == 1.0F || attack <= 0.0F) {
			return attack;
		}
		return Math.min(attack * speed, 1.0F);
	}
}
