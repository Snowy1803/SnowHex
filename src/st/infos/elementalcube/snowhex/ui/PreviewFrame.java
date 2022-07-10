package st.infos.elementalcube.snowhex.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import st.infos.elementalcube.snowhex.HexDocument;
import st.infos.elementalcube.snowylangapi.Lang;

public class PreviewFrame extends JDialog implements ActionListener {
	private static final long serialVersionUID = 6706395565004321114L;
	private HexFrame parent;
	private Object img;
	
	public PreviewFrame(HexFrame parent) {
		super(parent, Lang.getString("frame.preview"), false);
		this.parent = parent;
		setContentPane(new PreviewPanel());
		img = parent.getEditor().getPreviewImage();
		// utility style on macos
		rootPane.putClientProperty("Window.style", "small");
		rootPane.putClientProperty("apple.awt.draggableWindowBackground", true);
		if (System.getProperty("os.name").contains("Mac"))
			setAlwaysOnTop(true);
		// window is not focusable but it still works (when parent is focused)
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
		getRootPane().getActionMap().put("close", new AbstractAction() {
			private static final long serialVersionUID = 5031761572638786701L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		setSize(300, 300);
		setLocation(parent.getX() + parent.getWidth() + 5, parent.getY());
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setFocusableWindowState(false);
		setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof HexDocument) {
			img = parent.getEditor().getPreviewImage();
			getContentPane().repaint();
		}
	}
	
	private class PreviewPanel extends JPanel {
		private static final long serialVersionUID = 1753395134745081528L;
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			if (img instanceof Image) {
				Image img = (Image) PreviewFrame.this.img;
				if ((double) getWidth() / img.getWidth(null) <= (double) getHeight() / img.getHeight(null)) {
					g2d.drawImage(img, 0, 0, getWidth(), img.getHeight(null) * getWidth() / img.getWidth(null), this);
				} else {
					g2d.drawImage(img, 0, 0, img.getWidth(null) * getHeight() / img.getHeight(null), getHeight(), this);
				}
			} else if (img != null) {
				String s1 = Lang.getString("frame.preview.null");
				g2d.drawString(s1, (int) ((getWidth() - g2d.getFont().getStringBounds(s1, g2d.getFontRenderContext()).getWidth()) / 2), getHeight()
						/ 2 - g2d.getFont().getSize());
				String s2 = img.toString();
				g2d.drawString(s2, (int) ((getWidth() - g2d.getFont().getStringBounds(s2, g2d.getFontRenderContext()).getWidth()) / 2), getHeight()
						/ 2 + g2d.getFont().getSize());
			}
		}
	}
}
