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
 * Applies dampened position/rotation/scale at the start of first-person hand rendering.
 * Swing math runs afterward, so attack arcs follow the edited orientation.
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
		poseStack.translate(settings.appliedPosX(), settings.appliedPosY(), settings.appliedPosZ());
		poseStack.mulPose(Axis.XP.rotationDegrees(settings.appliedRotX()));
		poseStack.mulPose(Axis.YP.rotationDegrees(settings.appliedRotY()));
		poseStack.mulPose(Axis.ZP.rotationDegrees(settings.appliedRotZ()));
		float scale = settings.appliedScale();
		poseStack.scale(scale, scale, scale);
	}
}
