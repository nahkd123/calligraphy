package me.nahkd.calligraphy.addon.axiom.tool.control;

import com.mojang.serialization.Codec;

import imgui.ImGui;

public final class FloatSliderControl implements ToolControl<Float> {
	private String id;
	private float[] handle;
	private String label;
	private float min;
	private float max;
	private String format;

	public FloatSliderControl(String id, float initial, float min, float max, String label, String format) {
		this.id = id;
		this.handle = new float[] { initial };
		this.min = min;
		this.max = max;
		this.format = format;
		this.label = label;
	}

	public FloatSliderControl(String id, float initial, float min, float max, String label) { this(id, initial, min, max, label, "%f"); }
	public FloatSliderControl(String id, float initial, String label) { this(id, initial, 0f, 1f, label); }

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Float get() { return handle[0]; }

	@Override
	public void set(Float value) { handle[0] = value; }

	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }
	public float getMin() { return min; }
	public void setMin(float min) { this.min = min; }
	public float getMax() { return max; }
	public void setMax(float max) { this.max = max; }

	@Override
	public void imgui() {
		ImGui.sliderFloat(label, handle, min, max, format);
	}

	@Override
	public Codec<Float> getValueCodec() {
		return Codec.FLOAT;
	}
}