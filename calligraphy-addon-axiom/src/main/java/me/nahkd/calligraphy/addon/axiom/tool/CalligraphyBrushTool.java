package me.nahkd.calligraphy.addon.axiom.tool;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import com.moulberry.axiomclientapi.CustomTool;
import com.moulberry.axiomclientapi.Effects;
import com.moulberry.axiomclientapi.pathers.BallShape;
import com.moulberry.axiomclientapi.pathers.ToolPatherUnique;
import com.moulberry.axiomclientapi.regions.BlockRegion;
import com.moulberry.axiomclientapi.regions.BooleanRegion;
import com.moulberry.axiomclientapi.service.RegionProvider;
import com.moulberry.axiomclientapi.service.ToolPatherProvider;
import com.moulberry.axiomclientapi.service.ToolService;

import imgui.ImGui;
import me.nahkd.calligraphy.Calligraphy;
import me.nahkd.calligraphy.api.PenPacket;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;

public class CalligraphyBrushTool implements CustomTool {
	private final BlockRegion blockRegion;
	private final BooleanRegion previewRegion;
	private ToolPatherProvider patherProvider;
	private ToolService toolService;
	private boolean using = false;

	private int[] radiusMin = { 0 }, radiusMax = { 10 };
	private int currentPreviewRadius = 0;

	public CalligraphyBrushTool(RegionProvider regionProvider, ToolPatherProvider patherProvider, ToolService toolService) {
		this.patherProvider = patherProvider;
		this.toolService = toolService;
		this.blockRegion = regionProvider.createBlock();
		this.previewRegion = regionProvider.createBoolean();
	}

	@Override
	public String name() {
		return "Brush [+ Pen Tablet]";
	}

	@Override
	public void displayImguiOptions() {
		ImGui.dragIntRange2("Radius", radiusMin, radiusMax, 1, 0, 100, "Min %d", "Max %d");
		ImGui.labelText("Pressure", Calligraphy.getLiveData().pressure() + "/" + Calligraphy.getLiveData().maxPressure());
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
			if (hitResult == null) return;

			if (currentPreviewRadius != radiusMax[0]) {
				previewRegion.clear();
				BallShape.SPHERE.fillRegion(previewRegion, radiusMax[0]);
			}

			previewRegion.render(camera, hitResult.getPos().add(0.5, 0.5, 0.5), poseStack, projection, time, Effects.OUTLINE);
			return;
		}

		if (toolService.isMouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
			PenPacket pen = Calligraphy.getLiveData();
			int radiusDelta = radiusMax[0] - radiusMin[0];
			int radius = radiusMin[0] + (pen.pressure() * radiusDelta / pen.maxPressure());
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
