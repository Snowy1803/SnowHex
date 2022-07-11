package st.infos.elementalcube.snowhex;

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
		if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
			Taskbar.getTaskbar().setIconImage(new ImageIcon(HexFrame.class.getResource("/img/icon.png")).getImage());
		}
		HexFrame.main(args);
	}
}
