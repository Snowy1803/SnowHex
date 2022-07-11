package st.infos.elementalcube.snowhex.ui;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowylangapi.Lang;

public class PropertiesFrame extends JDialog implements ActionListener {
	private static final long serialVersionUID = 6706395565004321114L;
	private HexFrame parent;
	private JLabel fallback;
	
	public PropertiesFrame(HexFrame parent) {
		super(parent, Lang.getString("frame.properties"), false);
		this.parent = parent;
		// utility style on macos
		rootPane.putClientProperty("Window.style", "small");
		if (System.getProperty("os.name").contains("Mac"))
			setAlwaysOnTop(true);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
		getRootPane().getActionMap().put("close", new AbstractAction() {
			private static final long serialVersionUID = 5031761572638786701L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		setSize(300, 300);
		setLocation(parent.getX() + parent.getWidth() + 5, parent.getY() + parent.getHeight() - 300);
		updateContent();
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setVisible(true);
	}
	
	private void updateContent() {
		HexPanel editor = parent.getEditor();
		JComponent comp = editor.getColorer() == null ? null : editor.getColorer().getTokenProperties(editor);
		Container old = getContentPane();
		if (comp == null) {
			if (fallback == null) {
				fallback = new JLabel("No data", SwingConstants.CENTER);
				fallback.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
			}
			Token t = editor.getClosestToken();
			fallback.setText(t == null || t.getToolTip() == null ? "No data" : "<html>" + t.getToolTip());
			comp = fallback;
		}
		if (old != comp) {
			setContentPane(comp);
			revalidate();
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		updateContent();
	}
}
