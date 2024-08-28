package me.nahkd.calligraphy.addon.axiom.tool.control;

import java.util.List;

import com.mojang.serialization.Codec;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;

public final class FloatRangeSliderControl implements ToolControl<float[]> {
	private static final Codec<float[]> CODEC = Codec.FLOAT.listOf(2, 2).xmap(l -> new float[] { l.get(0), l.get(1) }, a -> List.of(a[0], a[1]));
	private String id;
	private float start;
	private float end;
	private float min;
	private float max;
	private String label;
	private String format;

	public FloatRangeSliderControl(String id, float initialStart, float initialEnd, float min, float max, String label, String format) {
		this.id = id;
		this.label = label;
		this.start = initialStart;
		this.end = initialEnd;
		this.min = min;
		this.max = max;
		this.format = format;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public float[] get() {
		return new float[] { start, end };
	}

	public float getStart() { return start; }
	public float getEnd() { return end; }

	@Override
	public void set(float[] value) {
		if (value.length < 2) return;
		start = value[0];
		end = value[1];
	}

	public float getMin() { return min; }
	public void setMin(float min) { this.min = min; }
	public float getMax() { return max; }
	public void setMax(float max) { this.max = max; }
	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }
	public String getFormat() { return format; }
	public void setFormat(String format) { this.format = format; }

	// ImGui internal states
	private boolean hoveringStartHandle = false, hoveringEndHandle = false;

	@Override
	public void imgui() {
		ImGuiStyle style = ImGui.getStyle();
		float width = ImGui.calcItemWidth();
		final float height = 30;
		final float padding = 2f;
		float handleWidth = 20f;

		ImGui.pushID(id);
		ImGui.invisibleButton("##rangeslider", width, height);
		if (!ImGui.isItemVisible()) return;

		ImVec2 rectMin = ImGui.getItemRectMin();
		ImVec2 rectMax = ImGui.getItemRectMax();
		float sliderBodyWidth = width - handleWidth * 2;
		float minMaxDelta = max - min;
		float minHandleX = start * sliderBodyWidth / minMaxDelta;
		float maxHandleX = end * sliderBodyWidth / minMaxDelta;

		boolean active = ImGui.isItemActive();
		boolean hovering = ImGui.isItemHovered();

		if (active) {
			float delta = ImGui.getIO().getMouseDeltaX();
			float deltaVal = delta * minMaxDelta / sliderBodyWidth;

			if (hoveringStartHandle) {
				start = Math.max(Math.min(start + deltaVal, end), min);
			} else if (hoveringEndHandle) {
				end = Math.max(Math.min(end + deltaVal, max), start);
			} else {
				start += deltaVal;
				end += deltaVal;
				if (start < min) start = min;
				if (start > max) start = max;
				if (end < min) end = min;
				if (end > max) end = max;
			}
		} else {
			hoveringStartHandle = ImGui.isMouseHoveringRect(rectMin.x + minHandleX, rectMin.y, rectMin.x + minHandleX + handleWidth, rectMax.y);
			hoveringEndHandle = ImGui.isMouseHoveringRect(rectMin.x + maxHandleX + handleWidth, rectMin.y, rectMin.x + maxHandleX + handleWidth * 2, rectMax.y);
		}

		ImDrawList drawList = ImGui.getWindowDrawList();
		drawList.addRectFilled(rectMin.x, rectMin.y, rectMax.x, rectMax.y, ImColor.rgba(style.getColor(active ? ImGuiCol.FrameBgActive : hovering ? ImGuiCol.FrameBgHovered : ImGuiCol.FrameBg)), style.getFrameRounding());
		drawList.addRectFilled(
				rectMin.x + padding + minHandleX, rectMin.y + padding,
				rectMin.x + minHandleX + handleWidth, rectMax.y - padding,
				ImColor.rgba(style.getColor(hoveringStartHandle ? ImGuiCol.SliderGrabActive : ImGuiCol.SliderGrab)), style.getFrameRounding());
		drawList.addRectFilled(
				rectMin.x + maxHandleX + handleWidth, rectMin.y + padding,
				rectMin.x - padding + maxHandleX + handleWidth * 2, rectMax.y - padding,
				ImColor.rgba(style.getColor(hoveringEndHandle ? ImGuiCol.SliderGrabActive : ImGuiCol.SliderGrab)), style.getFrameRounding());
		drawList.addRectFilled(
				rectMin.x + minHandleX + handleWidth, rectMin.y + padding,
				rectMin.x + maxHandleX + handleWidth, rectMax.y - padding,
				ImColor.rgba(style.getColor(ImGuiCol.FrameBgHovered)), style.getFrameRounding());

		String formatted = format.formatted(start, end);
		float textWidth = ImGui.getFont().calcTextSizeAX(ImGui.getFontSize(), width, 0f, format);
		drawList.addText((rectMin.x + rectMax.x - textWidth) / 2f, (rectMin.y + rectMax.y - ImGui.getFontSize()) / 2f, ImColor.rgba(style.getColor(ImGuiCol.Text)), formatted);

		ImGui.popID();
		ImGui.sameLine(0, style.getItemInnerSpacingX());
		ImGui.text(label);
	}

	@Override
	public Codec<float[]> getValueCodec() {
		return CODEC;
	}
}
