package me.nahkd.calligraphy.addon.axiom;

import java.util.Optional;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moulberry.axiomclientapi.service.RegionProvider;
import com.moulberry.axiomclientapi.service.ToolPatherProvider;
import com.moulberry.axiomclientapi.service.ToolRegistryService;
import com.moulberry.axiomclientapi.service.ToolService;

import me.nahkd.calligraphy.addon.axiom.tool.SimpleBrushTool;
import net.fabricmc.api.ClientModInitializer;

public final class CalligraphyAxiomAddon implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Calligraphy - Axiom Addon");
	private static CalligraphyAxiomAddon instance;
	private RegionProvider regionProvider;
	private ToolPatherProvider patherProvider;
	private ToolService toolService;
	private ToolRegistryService toolRegistryService;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing Calligraphy Axiom Addon...");
		instance = this;
		regionProvider = findOrThrow(RegionProvider.class);
		patherProvider = findOrThrow(ToolPatherProvider.class);
		toolService = findOrThrow(ToolService.class);
		toolRegistryService = findOrThrow(ToolRegistryService.class);

		LOGGER.info("Registering tools...");
		toolRegistryService.register(new SimpleBrushTool(regionProvider, patherProvider, toolService));
	}

	private static <T> T findOrThrow(Class<T> type) {
		Optional<T> serviceOpt = ServiceLoader.load(type).findFirst();
		if (serviceOpt.isEmpty()) throw new RuntimeException("Unable to load service: " + type);
		return serviceOpt.get();
	}

	public static CalligraphyAxiomAddon getInstance() {
		return instance;
	}

	public RegionProvider getRegionProvider() { return regionProvider; }
	public ToolPatherProvider getPatherProvider() { return patherProvider; }
	public ToolRegistryService getToolRegistryService() { return toolRegistryService; }
	public ToolService getToolService() { return toolService; }
}
