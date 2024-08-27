package me.nahkd.calligraphy;

import java.nio.file.Path;

import org.lwjgl.glfw.GLFWNativeWin32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.nahkd.calligraphy.event.PenEvents;
import me.nahkd.calligraphy.nativelib.OperatingSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class CalligraphyMod implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Calligraphy");

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing client pre-start...");
		Calligraphy.setup(getModConfigFolder().resolve("natives"));

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			LOGGER.info("Initializing Calligraphy Pen Tablet Mod...");
			OperatingSystem os = OperatingSystem.getCurrent();
			long handle = os == OperatingSystem.WINDOWS ? GLFWNativeWin32.glfwGetWin32Window(client.getWindow().getHandle()) : 0L;
			Calligraphy.initialize(PenEvents.RAW.invoker(), handle);
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			LOGGER.info("Uninitializing Calligraphy Pen Tablet Mod...");
			Calligraphy.uninitialize();
		});
	}

	public static Path getModConfigFolder() {
		return FabricLoader.getInstance().getConfigDir().resolve("calligraphy");
	}
}
