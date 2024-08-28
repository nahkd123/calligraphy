package me.nahkd.calligraphy.addon.axiom.tool;

import java.util.Collection;
import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import com.moulberry.axiomclientapi.Effects;
import com.moulberry.axiomclientapi.pathers.BallShape;
import com.moulberry.axiomclientapi.pathers.ToolPatherUnique;
import com.moulberry.axiomclientapi.regions.BlockRegion;
import com.moulberry.axiomclientapi.regions.BooleanRegion;
import com.moulberry.axiomclientapi.service.RegionProvider;
import com.moulberry.axiomclientapi.service.ToolPatherProvider;
import com.moulberry.axiomclientapi.service.ToolService;

import me.nahkd.calligraphy.Calligraphy;
import me.nahkd.calligraphy.addon.axiom.tool.control.FloatRangeSliderControl;
import me.nahkd.calligraphy.addon.axiom.tool.control.FloatSliderControl;
import me.nahkd.calligraphy.addon.axiom.tool.control.ToolControl;
import me.nahkd.calligraphy.api.PenPacket;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;

public class SimpleBrushTool implements CalligraphyAxiomTool {
	private final BlockRegion blockRegion;
	private final BooleanRegion previewRegion;
	private ToolPatherProvider patherProvider;
	private ToolService toolService;
	private boolean using = false;
	private int currentPreviewRadius = 0;

	private FloatRangeSliderControl size = new FloatRangeSliderControl("size", 1f, 10f, 0f, 100f, "Size", "%.1f -> %.1f");
	private FloatSliderControl curveExponent = new FloatSliderControl("curveExponent", 1f, 0f, 10f, "P. Curve Exp", "%.4f");
	private Collection<ToolControl<?>> controls = List.of(size, curveExponent);

	public SimpleBrushTool(RegionProvider regionProvider, ToolPatherProvider patherProvider, ToolService toolService) {
		this.patherProvider = patherProvider;
		this.toolService = toolService;
		this.blockRegion = regionProvider.createBlock();
		this.previewRegion = regionProvider.createBoolean();
	}

	@Override
	public String name() {
		return "Calligraphy: Simple Brush";
	}

	@Override
	public Collection<ToolControl<?>> getControls() {
		return controls;
	}

	public int getSizeFromPressure(short pressure, short maxPressure) {
		if (!Calligraphy.isPlatformSupported()) return (int) size.getEnd();
		float delta = size.getEnd() - size.getStart();
		float calculated = size.getStart() + (float) (Math.pow(pressure / (double) maxPressure, curveExponent.get().doubleValue()) * delta);
		return (int) Math.max(calculated, 0f);
	}

	@Override
	public void reset() {
		blockRegion.clear();
		using = false;
	}

	@Override
	public boolean callUseTool() {
		reset();
		using = true;
		return true;
	}

	@Override
	public void render(Camera camera, float tickDelta, long time, MatrixStack poseStack, Matrix4f projection) {
		if (!using) {
			// Preview
			BlockHitResult hitResult = toolService.raycastBlock();
			int maxSize = (int) size.getEnd();
			if (hitResult == null) return;

			if (currentPreviewRadius != maxSize ) {
				previewRegion.clear();
				BallShape.SPHERE.fillRegion(previewRegion, maxSize);
			}

			previewRegion.render(camera, hitResult.getPos().add(0.5, 0.5, 0.5), poseStack, projection, time, Effects.BLUE);
			return;
		}

		if (toolService.isMouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
			PenPacket pen = Calligraphy.getLiveData();
			int radius = getSizeFromPressure(pen.pressure(), pen.maxPressure());
			if (radius <= 0) return;

			BlockState block = toolService.getActiveBlock();
			ToolPatherUnique pather = patherProvider.createUnique(radius, BallShape.SPHERE);
			pather.update((x, y, z) -> blockRegion.addBlock(x, y, z, block));

			float opacity = (float) Math.sin(time / 1000000f / 50f / 8f);
			blockRegion.render(camera, Vec3d.ZERO, poseStack, projection, 0.75f + opacity * 0.25f, 0.3f - opacity * 0.2f);
			return;
		}

		toolService.pushBlockRegionChange(blockRegion);
		reset();
	}
}
