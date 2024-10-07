package me.nahkd.calligraphy.addon.axiom.mixin;

import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import me.nahkd.calligraphy.addon.axiom.debug.CalligraphyDebugger;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "render", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/DrawContext;draw()V",
			shift = At.Shift.BEFORE))
		// @formatter:off
		private void calligraphy$renderDebuggingOverlay(
			RenderTickCounter tickCounter, boolean tick, CallbackInfo ci,
			// Locals
			boolean isFinishedLoading,
			Matrix4fStack matrixStack,
			DrawContext ctx
		) {
		CalligraphyDebugger.renderDebuggingOverlay(ctx, matrixStack);
	}
}
