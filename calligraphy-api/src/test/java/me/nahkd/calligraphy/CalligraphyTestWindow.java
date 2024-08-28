package me.nahkd.calligraphy;

import java.awt.Component;
import java.awt.Frame;
import java.lang.reflect.Field;
import java.nio.file.Path;

public class CalligraphyTestWindow {
	public static void main(String[] args) throws Throwable {
		// Launch with "--add-opens java.desktop/java.awt=ALL-UNNAMED --add-exports java.desktop/sun.awt.windows=ALL-UNNAMED"
		Calligraphy.setup(Path.of("run", "natives"));
		Frame frame = new Frame("Calligraphy Test Window");
		frame.setSize(500, 500);
		frame.setVisible(true);

		Field field$peer = Component.class.getDeclaredField("peer"); field$peer.setAccessible(true);
		Object peer = field$peer.get(frame);
		Field field$hwnd = Class.forName("sun.awt.windows.WComponentPeer").getDeclaredField("hwnd"); field$hwnd.setAccessible(true);
		long hwnd = (long)field$hwnd.get(peer);

		Calligraphy.initialize((pressure, maxPressure, tiltX, tiltY, flags) -> {
			System.out.println(pressure + "/" + maxPressure + " | Tilt: (" + tiltX + "; " + tiltY + ") | Flags: " + flags);
		}, hwnd);
	}
}
