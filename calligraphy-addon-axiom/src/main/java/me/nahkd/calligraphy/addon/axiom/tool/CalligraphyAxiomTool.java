package me.nahkd.calligraphy.addon.axiom.tool;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.moulberry.axiomclientapi.CustomTool;

import imgui.ImGui;
import me.nahkd.calligraphy.Calligraphy;
import me.nahkd.calligraphy.addon.axiom.tool.control.ToolControl;
import me.nahkd.calligraphy.api.PenPacket;

public interface CalligraphyAxiomTool extends CustomTool {
	default Collection<ToolControl<?>> getControls() {
		return Collections.emptyList();
	}

	default void displayOptionsList() {
		if (ImGui.treeNode("Options")) {
			ImGui.beginDisabled();
			ImGui.button("Save brush preset");
			ImGui.button("Load brush preset");
			ImGui.endDisabled();
			ImGui.textWrapped("Brush preset saving will be implemented in the future!");
			// TODO implement
			ImGui.treePop();
		}
	}

	default void displayDiagnosticInfo() {
		if (ImGui.treeNode("Diagnostic")) {
			ImGui.textWrapped("Confirm your pen tablet is working with Calligraphy");
			PenPacket live = Calligraphy.getLiveData();

			ImGui.progressBar(live.pressure() / (float) live.maxPressure(), 0f, 0f, live.pressure() + "/" + live.maxPressure());
			ImGui.sameLine(0f, ImGui.getStyle().getItemInnerSpacingX());
			ImGui.text("Pressure");

			ImGui.labelText("Tilt X", live.tiltX() + " deg");
			ImGui.labelText("Tilt Y", live.tiltY() + " deg");

			ImGui.labelText("Eraser?", live.isEraser() ? "Yes" : "No");
			ImGui.labelText("Pen Button?", live.isAuxButton() ? "Yes" : "No");

			ImGui.treePop();
		}
	}

	@Override
	default void displayImguiOptions() {
		for (ToolControl<?> control : getControls()) control.imgui();
		displayOptionsList();
		displayDiagnosticInfo();
	}

	default Map<String, Object> exportToolData() {
		Map<String, Object> data = new HashMap<>();
		getControls().forEach(c -> data.put(c.getId(), c.get()));
		return data;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	default void importToolData(Map<String, Object> data) {
		getControls().forEach(c -> {
			if (!data.containsKey(c.getId())) return;
			((ToolControl) c).set(data.get(c.getId()));
		});
	}

	default Codec<Map<String, Object>> createToolCodec() {
		return Codec.dispatchedMap(Codec.STRING, key -> getControls().stream().filter(c -> c.getId().equals(key)).findAny().get().getValueCodec());
	}
}
