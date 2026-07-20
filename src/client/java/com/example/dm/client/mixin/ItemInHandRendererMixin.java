package com.example.dm.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import com.example.dm.client.config.HeldItemSettings;

/**
 * Applies saved XYZ/rotation offsets at the start of first-person hand rendering.
 * Because swing math runs afterward in the same PoseStack, attack swings follow the
 * edited orientation (e.g. Y rotation steers which way the sword arcs).
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	@Inject(
		method = "renderArmWithItem",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
	)
	private void dm$applyHeldItemTransform(
		AbstractClientPlayer player,
		float frameInterp,
		float xRot,
		InteractionHand hand,
		float attack,
		ItemStack itemStack,
		float inverseArmHeight,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int lightCoords,
		CallbackInfo ci
	) {
		HeldItemSettings settings = HeldItemSettings.get();
		poseStack.translate(settings.posX, settings.posY, settings.posZ);
		poseStack.mulPose(Axis.XP.rotationDegrees(settings.rotX));
		poseStack.mulPose(Axis.YP.rotationDegrees(settings.rotY));
		poseStack.mulPose(Axis.ZP.rotationDegrees(settings.rotZ));
	}
}
