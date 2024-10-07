package me.nahkd.calligraphy;

import java.nio.file.Path;

import org.lwjgl.glfw.GLFWNativeWin32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.nahkd.calligraphy.windows.WindowsInkDriver;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class CalligraphyMod implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Calligraphy");

	private WindowsInkDriver windowsInkDriver = null;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing client pre-start...");
		WindowsInkDriver.setup(getModConfigFolder().resolve("natives"));

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			TabletSyncEvents.releaseQueueSync();
			LOGGER.info("Initializing Calligraphy...");

			// Windows only
			if (WindowsInkDriver.isSupported()) {
				LOGGER.info("Setting up Windows Ink driver, using game window...");
				long hwnd = GLFWNativeWin32.glfwGetWin32Window(client.getWindow().getHandle());
				Calligraphy.getDriver().addDriver(windowsInkDriver = new WindowsInkDriver(hwnd));
			}

			LOGGER.info("Loaded {} pen tablet drivers!", Calligraphy.getDriver().getDriversCount());
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			LOGGER.info("Uninitializing Calligraphy...");

			if (windowsInkDriver != null) {
				LOGGER.info("Removing Windows Ink driver...");
				Calligraphy.getDriver().removeDriver(windowsInkDriver);
				windowsInkDriver.close();
			}

			LOGGER.info("Goodbye!");
		});

		TabletSyncEvents.CONNECT.register(tablet -> LOGGER.info("Tablet connected: {}", tablet.getName()));
		TabletSyncEvents.DISCONNECT.register(tablet -> LOGGER.info("Tablet disconnected: {}", tablet.getName()));
		TabletSyncEvents.init();
	}

	public static Path getModConfigFolder() {
		return FabricLoader.getInstance().getConfigDir().resolve("calligraphy");
	}
}
