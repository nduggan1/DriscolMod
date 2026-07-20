package com.example.dm.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

import com.example.dm.client.config.HeldItemSettings;

/**
 * Speeds up / slows down the local player's swing animation duration (visual).
 * Higher swingSpeed = shorter duration = faster swing.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
	private void dm$applySwingSpeed(CallbackInfoReturnable<Integer> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || self != player) {
			return;
		}

		float speed = HeldItemSettings.get().appliedSwingSpeed();
		int original = cir.getReturnValueI();
		int adjusted = Math.max(1, Math.round(original / speed));
		cir.setReturnValue(adjusted);
	}
}
