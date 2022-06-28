package st.infos.elementalcube.snowhex;

import java.awt.Image;
import java.awt.Taskbar;

import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import st.infos.elementalcube.snowhex.ui.HexFrame;

public class Main {
	public static void main(String[] args) {
		System.setProperty("apple.awt.application.name", "SnowHex");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "SnowHex");
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		// System.setProperty("apple.awt.application.appearance", "system");

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		try {
			Class<?> c = Class.forName("java.awt.Taskbar", false, null);
			if (Taskbar.isTaskbarSupported()) {
				Taskbar.getTaskbar().setIconImage(new ImageIcon(HexFrame.class.getResource("/img/icon.png")).getImage());
			}
		} catch (ReflectiveOperationException | SecurityException | IllegalArgumentException | UnsupportedOperationException e) {
			/* Java 8 */
			e.printStackTrace();
			try {
				Class<?> c = Class.forName("com.apple.eawt.Application", false, null);
				c.getMethod("setDockIconImage", Image.class).invoke(c.getMethod("getApplication").invoke(null),
						new ImageIcon(HexFrame.class.getResource("/img/icon.png")).getImage());
			} catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e1) {
				/* Not on mac */
				e1.printStackTrace();
			}
		}
		HexFrame.main(args);
	}
}
